package io.github.lunarwatcher.chatbot.bot.sites.se;


import io.github.lunarwatcher.chatbot.Constants;
import io.github.lunarwatcher.chatbot.Database;
import io.github.lunarwatcher.chatbot.Site;
import io.github.lunarwatcher.chatbot.bot.chat.BMessage;
import io.github.lunarwatcher.chatbot.bot.chat.Message;
import io.github.lunarwatcher.chatbot.bot.chat.SEEvents;
import io.github.lunarwatcher.chatbot.bot.command.CommandCenter;
import io.github.lunarwatcher.chatbot.bot.commands.BotConfig;
import io.github.lunarwatcher.chatbot.bot.commands.User;
import io.github.lunarwatcher.chatbot.bot.events.ScheduledEvent;
import io.github.lunarwatcher.chatbot.bot.exceptions.RoomNotFoundException;
import io.github.lunarwatcher.chatbot.bot.sites.Chat;
import io.github.lunarwatcher.chatbot.utils.Http;
import io.github.lunarwatcher.chatbot.utils.Response;
import io.github.lunarwatcher.chatbot.utils.Utils;
import lombok.Getter;
import org.apache.http.impl.client.CloseableHttpClient;

import javax.websocket.WebSocketContainer;
import java.io.*;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The Stack Exchange network is a massive blob of communities, and there are at least three known chat domains:
 * * chat.stackoverflow.com
 * * chat.meta.stackexchange.com
 * * chat.stackexchange.com
 *
 * The last one is mostly universal for all the sites, while the others are specific for each site. MSE and SO has
 * more or less the same core architecture, but the login on regular SE is different from SO and MSE. The general
 * system in chat is the same, but because there are differences, this is made abstract to allow for customization
 * for a specific site.
 */
public class SEChat implements Chat {
    public static boolean NSFW = false;
    @Getter
    Site site;

    private String fKey;
    @Getter
    CloseableHttpClient httpClient;
    @Getter
    WebSocketContainer webSocket;
    Http http;

    public BlockingQueue<Message> newMessages = new LinkedBlockingQueue<>();
    public List<SERoom.StarMessage> starredMessages = new ArrayList<>();
    public List<SERoom.UserAction> actions = new ArrayList<>();
    List<Integer> notifiedBanned = new ArrayList<>();

    public List<Integer> roomsToleave = new ArrayList<>();
    private List<SERoom> rooms = new ArrayList<>();
    public List<Integer> mentionIds = new ArrayList<>();

    CommandCenter commands;
    List<Integer> joining = new ArrayList<>();
    @Getter
    private Database db;
    private BotConfig config;

    public List<Integer> hardcodedRooms = new ArrayList<>();
    public List<Long> hardcodedAdmins = new ArrayList<>();
    public Properties botProps;
    private Thread thread;
    private Message stopMessage = new Message("", 0, 0, "", 0);
    Timer timer = new Timer();

    public SEChat(Site site, CloseableHttpClient httpClient, WebSocketContainer webSocket, Properties botProps, Database database) throws IOException {
        this.site = site;
        this.db = database;
        this.httpClient = httpClient;
        this.webSocket = webSocket;
        this.botProps = botProps;

        config = new BotConfig(this);
        load();

        for(Map.Entry<Object, Object> s : botProps.entrySet()){
            String key = (String) s.getKey();

            if(key.equals("bot.homes." + site.getName())){
                String[] rooms = ((String) s.getValue()).split(",");

                for(String room : rooms){
                    try{
                        hardcodedRooms.add(Integer.parseInt(room.trim()));
                    }catch(ClassCastException | NumberFormatException e){
                        System.err.println("The room supplied could not be parsed as a number: " + room);
                    }
                }
                break;
            }
        }

        for(Integer x : hardcodedRooms){
            join(x);
            config.addHomeRoom(x);
        }


        Utils.loadHardcodedAdmins(this);

        for(Integer room : config.getHomes()){
            join(room);
            System.out.println("Trying to join " + room + "@" + site.getName());
        }
        //Ignore unchecked cast warning
        //noinspection unchecked
        List<Integer> data = (List<Integer>) database.get(getName() + "-rooms");
        if(data != null){
            for(int x : data)
                join(x);
        }else{
            //No current rooms
            if(config.getHomes().size() == 0) {
                //No manually added home rooms
                if (hardcodedRooms.size() == 0) {
                    //No hard-coded rooms
                    hardcodedRooms.add((site.getName().equals("metastackexchange") ? 721 : 1));

                }
            }
        }
        data = null;

        commands = new CommandCenter(botProps, true, this);
        commands.loadSE();
        http = new Http(httpClient);

        logIn();
        joining.clear();

        thread = new Thread(() -> {
            try {

                while (true) {

                    try {
                        Message m = newMessages.take();//This blocks the thread when it's empty, preventing continous looping.

                        if(m == stopMessage)
                            break;
                        handleMessage(m);//To keep this thread clean, use a separate method for message handling

                        if (roomsToleave.size() != 0) {
                            for (int r = roomsToleave.size() - 1; r >= 0; r--) {
                                if (r == 0 && roomsToleave.size() == 0)
                                    break;
                                for (int i = rooms.size() - 1; i >= 0; i--) {
                                    if (rooms.get(i).getId() == roomsToleave.get(r)) {
                                        int rtl = roomsToleave.get(r);
                                        roomsToleave.remove(r);
                                        rooms.get(i).close();
                                        rooms.remove(i);
                                        System.out.println("Left room " + rtl);
                                        break;
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        commands.crash.crash(e);
                    }
                }
            }catch(Exception e){
                commands.crash.crash(e);
            }
        });
        thread.start();
    }

    public void logIn() throws IOException {
        if(site == null)
            return;
        String targetUrl = (site.getName().equals("stackexchange") ? SEEvents.getSELogin(site.getUrl()) : SEEvents.getLogin(site.getUrl()));

        if (site.getName().equals("stackexchange")) {
            Response se = http.post(targetUrl, "from", "https://stackexchange.com/users/login#log-in");
            targetUrl = se.getBody();
        }

        String fKey = Utils.parseHtml(http.get(targetUrl).getBody());

        Response response = null;

        if(fKey == null){
            System.out.println("No fKey found!");
            return;
        }
        this.fKey = fKey;

        if(site.getName().equals("stackexchange")){
            targetUrl = "https://openid.stackexchange.com/affiliate/form/login/submit";
            response = http.post(targetUrl, "email", site.getConfig().getEmail(), "password", site.getConfig().getPassword(), "fkey", fKey, "affId", "11");
            String TUREG = "(var target = .*?;)";
            Pattern pattern = Pattern.compile(TUREG);
            Matcher m = pattern.matcher(response.getBody());
            response = http.get(m.find() ? m.group(0).replace("var target = ", "").replace("'", "").replace(";", "") : null);
        }else{
           response = http.post(targetUrl, "email", site.getConfig().getEmail(), "password", site.getConfig().getPassword(), "fkey", fKey);
        }


        int statusCode = response.getStatusCode();

        //SE doesn't redirect automatically, but the page does exist. Allow both 200 and 302 status codes.
        if(statusCode != 200 && statusCode != 302){
            throw new IllegalAccessError();
        }

        for(int i = joining.size() - 1; i >= 0; i--){
            try {
                addRoom(new SERoom(joining.get(i), this));
            }catch(RoomNotFoundException e){
                //Uncontrolled event like room doesn't exist, can't write in the room, etc
                System.out.println("Cannot join room");
            }catch(IllegalArgumentException e){
                System.out.println("Room not available!");
            }catch(Exception ex){
                ex.printStackTrace();
            }
        }

    }

    public String getUrl(){
        return site.getUrl();
    }

    public String getName(){
        return site.getName();
    }


    public SERoom getRoom(int id){
        for(SERoom r : rooms){
            if(r.getId() == id)
                return r;
        }
        return null;
    }

    public List<SERoom> getRooms(){
        return rooms;
    }

    public BMessage joinRoom(int rid){
        try {
            for (SERoom room : rooms) {
                if (room.getId() == rid) {
                    return new BMessage("I'm already there...", true);
                }
            }
            try {
                Response response = http.get(site.getUrl() + "/rooms/" + rid);
                if (response.getStatusCode() == 404)
                    throw new RoomNotFoundException("");
            }catch(RoomNotFoundException e){
                throw e;//re-throw for the outer catch statement
            }catch(Exception e){
                commands.crash.crash(e);
                return new BMessage("An exception occured when trying to check the validity of the room", true);
            }
            SERoom room = new SERoom(rid, this);
            addRoom(room);

            return new BMessage(Utils.getRandomJoinMessage(), true);

        }catch(IOException e){
            return new BMessage("An IOException occured when attempting to join", true);
        }catch(RoomNotFoundException e){
            return new BMessage("That's not a real room or I can't write there", true);
        }catch(Exception e){
            commands.crash.crash(e);
            e.printStackTrace();
        }

        return new BMessage("Something bad happened when joining the room", true);
    }

    public boolean leaveRoom(int rid){
        for(Integer room : hardcodedRooms)
            if(room == rid)
                return false;

        try{
            for(int i = rooms.size() - 1; i >= 0; i--){
                if(rooms.get(i).getId() == rid){
                    roomsToleave.add(rooms.get(i).getId());
                    save();
                    return true;
                }
            }
        }catch(Exception ignored){}
        return false;
    }

    public void save(){
        if(db != null){
            List<Integer> rooms = new ArrayList<>();
            for(SERoom room : this.rooms){
                rooms.add(room.getId());
            }

            db.put(getName() + "-rooms", rooms);
        }
        commands.save();
        Utils.saveConfig(config, db);

        db.commit();


    }

    public void load(){
        Utils.loadConfig(config, db);
    }

    public BotConfig getConfig(){
        return config;
    }

    public void addRoom(SERoom room){
        for(SERoom s : rooms){
            if(s.getId() == room.getId()){
                return;
            }
        }

        rooms.add(room);
    }

    public List<Long> getHardcodedAdmins(){
        return hardcodedAdmins;
    }

    public Properties getBotProps(){
        return botProps;
    }


    public void leaveAll(){
        for(SERoom s : rooms){
            try {
                s.close();
            }catch(IOException e){
                System.out.println("Failed to leave room");
            }
        }
    }

    public void join(int i){
        for(Integer x : joining){
            if(x == i){
                return;
            }
        }

        joining.add(i);
    }

    public Database getDatabase(){
        return db;
    }

    public String getUsername(long uid){
        return Long.toString(uid);
    }

    public CommandCenter getCommands(){
        return commands;
    }

    public Http getHttp(){
        return http;
    }

    public String getFKey(){
        return fKey;
    }

    public void stop(){
        newMessages.add(stopMessage);
    }

    public void handleMessage(Message m) throws IOException {

        commands.hookupToRanks(m.userid, m.username);

        if (m.userid == site.getConfig().getUserID())
            return;
        if (Utils.isBanned(m.userid, config)) {
            if (CommandCenter.isCommand(m.content)) {
                boolean mf = false;

                for (Integer u : notifiedBanned) {
                    if (u == m.userid) {
                        mf = true;
                        break;
                    }
                }

                if (!mf) {
                    notifiedBanned.add(m.userid);
                    SERoom s = getRoom(m.roomID);
                    System.out.println(m.username + " : " + m.content);
                    if (s != null) {
                        s.reply(Constants.BANNED_REPLY, m.messageID);
                    }
                }
            }
            return;
        }

        User user = new User(getName(), m.userid, m.username, m.roomID, false);
        List<BMessage> replies = commands.parseMessage(m.content, user, false);
        if (replies != null && getRoom(m.roomID) != null) {
            for (BMessage bm : replies) {
                if(bm.content == null)
                    continue;
                if (bm.content.length() >= 500 && !bm.content.contains("\n")) {
                    bm.content += "\n.";
                }
                if (bm.replyIfPossible) {
                    getRoom(m.roomID).reply(bm.content, m.messageID);
                } else {
                    getRoom(m.roomID).sendMessage(bm.content);
                }
            }
        } else {
            if (CommandCenter.isCommand(m.content)) {
                SERoom r = getRoom(m.roomID);
                if (r != null) {
                    r.reply("Maybe you should consider looking up the manual", m.messageID);
                } else {
                    System.err.println("Room is null!");
                }
            }
        }
    }

    @Override
    public void leaveServer(int serverId) {
        leaveRoom(serverId);
    }

    private void scheduleEvent(ScheduledEvent event){
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                event.run();
                scheduleEvent(event);
            }
        }, event.planNext());
    }
}
