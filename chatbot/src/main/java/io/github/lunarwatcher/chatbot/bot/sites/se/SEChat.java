package io.github.lunarwatcher.chatbot.bot.sites.se;


import io.github.lunarwatcher.chatbot.Constants;
import io.github.lunarwatcher.chatbot.Database;
import io.github.lunarwatcher.chatbot.SiteConfig;
import io.github.lunarwatcher.chatbot.User;
import io.github.lunarwatcher.chatbot.bot.chat.Message;
import io.github.lunarwatcher.chatbot.bot.chat.ReplyMessage;
import io.github.lunarwatcher.chatbot.bot.chat.SEEvents;
import io.github.lunarwatcher.chatbot.bot.command.CommandCenter;
import io.github.lunarwatcher.chatbot.bot.command.CommandGroup;
import io.github.lunarwatcher.chatbot.bot.events.ScheduledEvent;
import io.github.lunarwatcher.chatbot.bot.exceptions.RoomNotFoundException;
import io.github.lunarwatcher.chatbot.bot.sites.Chat;
import io.github.lunarwatcher.chatbot.bot.sites.Host;
import io.github.lunarwatcher.chatbot.data.BotConfig;
import io.github.lunarwatcher.chatbot.utils.Http;
import io.github.lunarwatcher.chatbot.utils.Response;
import io.github.lunarwatcher.chatbot.utils.Utils;
import lombok.Getter;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.glassfish.tyrus.client.ClientManager;
import org.glassfish.tyrus.client.ClientProperties;
import org.glassfish.tyrus.container.jdk.client.JdkClientContainer;

import javax.websocket.WebSocketContainer;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.github.lunarwatcher.chatbot.Constants.stopMessage;

public class SEChat implements Chat {
    private static final boolean truncated = false;
    private static final List<CommandGroup> groups = Arrays.asList(CommandGroup.STACKEXCHANGE);

    private String fKey;
    public static CloseableHttpClient httpClient;
    public static WebSocketContainer webSocket;
    public static Http http;

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
    private Timer timer = new Timer();
    private Host host;
    private SiteConfig credentialManager;

    public SEChat(Properties botProps, Database database, Host host, SiteConfig credentialManager) throws IOException {
        this.db = database;
        this.host = host;
        this.credentialManager = credentialManager;

        initConnections();

        this.botProps = botProps;

        config = new BotConfig(this);
        load();

        for(Map.Entry<Object, Object> s : botProps.entrySet()){
            String key = (String) s.getKey();

            if(key.equals("bot.homes." + host.getName())){
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

        for(long room : config.getHomes()){
            join((int) room);
            System.out.println("Trying to join " + room + "@" + host.getName());
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
                    hardcodedRooms.add((host.getName().equals("metastackexchange") ? 721 : 1));

                }
            }
        }
        data = null;

        commands = CommandCenter.Companion.getINSTANCE();


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
                        commands.getCrash().crash(e);
                    }
                }
            }catch(Exception e){
                commands.getCrash().crash(e);
            }
        });
        thread.start();
    }

    public void logIn() throws IOException {

        String targetUrl = (host.getName().equals("stackexchange")
                ? SEEvents.getSELogin(Objects.requireNonNull(host.getMainSiteHost()))
                : SEEvents.getLogin(Objects.requireNonNull(host.getMainSiteHost())));

        if (getName().equals("stackexchange")) {
            Response se = http.post(targetUrl, "from", "https://stackexchange.com/users/login#log-in");
            targetUrl = se.getBody();
        }

        String fKey = Utils.parseHtml(http.get(targetUrl).getBody());

        Response response;

        if(fKey == null){
            System.out.println("No fKey found!");
            return;
        }
        this.fKey = fKey;

        if(getName().equals("stackexchange")){
            //TODO handle the new system
            targetUrl = "https://openid.stackexchange.com/affiliate/form/login/submit";
            response = http.post(targetUrl, "email", credentialManager.getEmail(), "password", credentialManager.getPassword(), "fkey", fKey, "affId", "11");
            String TUREG = "(var target = .*?;)";
            Pattern pattern = Pattern.compile(TUREG);
            Matcher m = pattern.matcher(response.getBody());
            response = http.get(m.find() ? m.group(0).replace("var target = ", "").replace("'", "").replace(";", "") : null);
        }else{
           response = http.post(targetUrl, "email", credentialManager.getEmail(), "password", credentialManager.getPassword(), "fkey", fKey);
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
                e.printStackTrace();
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
        return host.getChatHost();
    }

    public String getName(){
        return host.getName();
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

    public ReplyMessage joinRoom(int rid){
        try {
            for (SERoom room : rooms) {
                if (room.getId() == rid) {
                    return new ReplyMessage("I'm already there...", true);
                }
            }
            try {
                Response response = http.get(host.getChatHost() + "/rooms/" + rid);
                if (response.getStatusCode() == 404)
                    throw new RoomNotFoundException("");
            }catch(RoomNotFoundException e){
                throw e;//re-throw for the outer catch statement
            }catch(Exception e){
                commands.getCrash().crash(e);
                return new ReplyMessage("An exception occured when trying to check the validity of the room", true);
            }
            SERoom room = new SERoom(rid, this);
            addRoom(room);

            return new ReplyMessage(Utils.getRandomJoinMessage(), true);

        }catch(IOException e){
            return new ReplyMessage("An IOException occured when attempting to join", true);
        }catch(RoomNotFoundException e){
            return new ReplyMessage("That's not a real room or I can't write there", true);
        }catch(Exception e){
            commands.getCrash().crash(e);
            e.printStackTrace();
        }

        return new ReplyMessage("Something bad happened when joining the room", true);
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

    @Override
    public List<Long> getUidForUsernameInRoom(String username, long server) {
        return Collections.singletonList(0L);
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

    public Host getHost(){
        return host;
    }

    public void stop(){
        newMessages.add(stopMessage);
    }

    @Override
    public void leaveServer(long serverId) {
        leaveRoom((int) serverId);
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

    public boolean getTruncated(){
        return truncated;
    }
    public List<CommandGroup> getCommandGroup(){
        return groups;
    }

    public SiteConfig getCredentialManager(){
        return credentialManager;
    }

    @Override
    public List<User> getUsersInServer(long server) {
        //TODO
        return null;
    }

    public WebSocketContainer getWebSocket(){
        return webSocket;
    }

    private void initConnections(){
        if(httpClient == null)
            httpClient = HttpClients.createDefault();
        if(webSocket == null) {
            ClientManager wsClient = ClientManager.createClient(JdkClientContainer.class.getName());
            wsClient.setDefaultMaxSessionIdleTimeout(0);
            wsClient.getProperties().put(ClientProperties.RETRY_AFTER_SERVICE_UNAVAILABLE, true);
            webSocket = wsClient;
        }

        if(http == null)
            http = new Http(httpClient);
    }

    public void handleMessage(Message message) throws IOException {

        commands.hookupToRanks(message.getUser().getUserID(),this);

        if (message.getUser().getUserID() == credentialManager.getUserID())
            return;
        if (Utils.isBanned(message.getUser().getUserID(), config)) {
            if (CommandCenter.Companion.isCommand(message.getContent())) {
                if (notifiedBanned.contains((int) message.getUser().getUserID())) {
                    notifiedBanned.add((int) message.getUser().getUserID());
                    SERoom s = getRoom((int)message.getRoomID());
                    System.out.println(message.getUser().getUserName() + " : " + message.getContent());
                    if (s != null) {
                        s.reply(Constants.BANNED_REPLY, message.getMessageID());
                    }
                }
            }
            return;
        }

        List<ReplyMessage> replies = commands.parseMessage(message);

        if (replies != null && getRoom((int)message.getRoomID()) != null) {
            for (ReplyMessage replyMessage : replies) {
                if(replyMessage == Constants.bStopMessage)
                    return;
                if(replyMessage == CommandCenter.Companion.getNO_MESSAGE()){
                    continue;
                }

                if(replyMessage.getContent() == null)
                    continue;
                if (replyMessage.getContent().length() >= 500 && !replyMessage.getContent().contains("\n")) {
                    replyMessage.postfixString("\n.");
                }
                if (replyMessage.getReplyIfPossible()) {
                    getRoom(message.getIntRoomId()).reply(replyMessage.getContent(), message.getMessageID());
                } else {
                    getRoom(message.getIntRoomId()).sendMessage(replyMessage.getContent());
                }
            }
        } else {
            if (CommandCenter.Companion.isCommand(message.getContent())) {
                SERoom r = getRoom(message.getIntRoomId());
                if (r != null) {
                    r.reply(Constants.INVALID_COMMAND + " (//help)", message.getMessageID());
                } else {
                    System.err.println("Room is null!");
                }
            }
        }
    }
}
