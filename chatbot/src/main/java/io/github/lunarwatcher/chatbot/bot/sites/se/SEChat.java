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
import io.github.lunarwatcher.chatbot.bot.exceptions.RoomNotFoundException;
import io.github.lunarwatcher.chatbot.bot.sites.Chat;
import io.github.lunarwatcher.chatbot.utils.Http;
import io.github.lunarwatcher.chatbot.utils.Response;
import io.github.lunarwatcher.chatbot.utils.Utils;
import lombok.Getter;
import org.apache.http.impl.client.CloseableHttpClient;

import javax.websocket.WebSocketContainer;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
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
    @Getter
    String fKey;
    @Getter
    CloseableHttpClient httpClient;
    @Getter
    WebSocketContainer webSocket;
    @Getter
    Http http;

    public List<Message> newMessages = new ArrayList<>();
    public List<SERoom.StarMessage> starredMessages = new ArrayList<>();
    public List<SERoom.UserAction> actions = new ArrayList<>();
    List<Integer> notifiedBanned = new ArrayList<>();

    private List<Integer> roomsToleave = new ArrayList<>();
    private List<SERoom> rooms = new ArrayList<>();
    CommandCenter commands;
    List<Integer> joining = new ArrayList<>();
    @Getter
    private Database db;
    private BotConfig config;

    public List<Integer> hardcodedRooms = new ArrayList<>();
    public List<Long> hardcodedAdmins = new ArrayList<>();
    List<Long> checkedUsers = new ArrayList<>();

    public Properties botProps;

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
        }
        //Ignore unchecked cast warning
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

        for(SERoom room : rooms){
            System.out.println("In room " + room.getId());
        }
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

    public BMWrapper joinRoom(int rid){
        try {
            for(SERoom room : rooms){
                if(room.getId() == rid){
                    return new BMWrapper("I'm already there...", true, true, ExceptionClass.ALREADY_JOINED);
                }
            }
            SERoom room = new SERoom(rid, this);
            addRoom(room);

            return new BMWrapper(Utils.getRandomJoinMessage(), true, false, ExceptionClass.NONE);

        }catch(IOException e){
            return new BMWrapper("An IOException occured when attempting to join", true, true, ExceptionClass.IO);
        }catch(RoomNotFoundException e){
            return new BMWrapper("That's not a real room or I can't write there", true, true, ExceptionClass.NOT_FOUND);
        }catch(Exception e){
            e.printStackTrace();
        }

        return new BMWrapper("Something bad happened when joining the room", true, true, ExceptionClass.GENERAL);
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
        }catch(Exception e){

        }
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

        Utils.saveConfig(config, db);

        db.commit();

        if (commands.crash.getLogs().size() != 0) {
            try {
                FileOutputStream fis = new FileOutputStream(new File("logs.txt"));
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(fis));
                for (String log : commands.crash.getLogs()){
                    writer.write(log);
                    writer.write("\n");
                }
                fis.close();
                writer.close();
            }catch(IOException e){
                //Ignore
            }
        }
    }

    public void handleNewMessage() {
        try {
            for (int x = newMessages.size() - 1; x >= 0; x--) {

                Message m = newMessages.get(x);
                newMessages.remove(x);
                if (!checkedUsers.contains(Long.parseLong(Integer.toString(m.userid)))) {
                    checkedUsers.add(Long.parseLong(Integer.toString(m.userid)));
                    commands.hookupToRanks(m.userid, m.username);
                }
                if (m.userid == site.getConfig().getUserID())
                    continue;
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
                    continue;
                }

                User user = new User(getName(), m.userid, m.username, m.roomID, false);
                List<BMessage> replies = commands.parseMessage(m.content, user, false);
                if (replies != null && getRoom(m.roomID) != null) {
                    for (BMessage bm : replies) {
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

            newMessages.clear();
            starredMessages.clear();
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
        } catch (IOException e) {
            commands.crash.crash(e);

        }
    }

    public void load(){
        Utils.loadConfig(config, db);
    }

    public BotConfig getConfig(){
        return config;
    }

    public class BMWrapper extends BMessage{
        public boolean exception;
        public ExceptionClass exceptionType;

        public BMWrapper(String content, boolean rip, boolean exception, ExceptionClass exceptionType) {
            super(content, rip);
            this.exception = exception;
            this.exceptionType = exceptionType;
        }
    }

    public enum ExceptionClass{
        NOT_FOUND,
        IO,
        GENERAL,
        ALREADY_JOINED,
        NONE//To NPE's
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
}
