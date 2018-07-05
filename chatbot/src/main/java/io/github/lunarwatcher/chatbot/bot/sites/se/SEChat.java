package io.github.lunarwatcher.chatbot.bot.sites.se;


import com.fasterxml.jackson.databind.JsonNode;
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
import io.github.lunarwatcher.chatbot.bot.sites.se.data.StarMessage;
import io.github.lunarwatcher.chatbot.bot.sites.se.data.UserAction;
import io.github.lunarwatcher.chatbot.data.BotConfig;
import io.github.lunarwatcher.chatbot.socket.EventHandler;
import io.github.lunarwatcher.chatbot.utils.HttpHelper;
import io.github.lunarwatcher.chatbot.utils.Utils;
import org.glassfish.tyrus.client.ClientManager;
import org.glassfish.tyrus.client.ClientProperties;
import org.glassfish.tyrus.container.jdk.client.JdkClientContainer;
import org.jetbrains.annotations.NotNull;
import org.jsoup.Connection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.websocket.WebSocketContainer;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static io.github.lunarwatcher.chatbot.Constants.stopMessage;

public class SEChat implements Chat {
    private Map<Integer, List<EventHandler>> callbacks = new HashMap<>();

    private static final Logger logger = LoggerFactory.getLogger(SEChat.class);
    private static final boolean truncated = false;
    private static final List<CommandGroup> groups = Arrays.asList(CommandGroup.STACKEXCHANGE);
    private Map<String, String> cookies = new HashMap<>();
    private static final Pattern OPEN_ID_PATTERN = Pattern.compile("(https://openid.stackexchange.com/user/.*?)\"");
    public static final String FKEY_SELECTOR = "input[name='fkey']";
    private String fkey;
    public static WebSocketContainer webSocket;

    public BlockingQueue<Message> newMessages = new LinkedBlockingQueue<>();
    public List<StarMessage> starredMessages = new ArrayList<>();
    public List<UserAction> actions = new ArrayList<>();
    List<Integer> notifiedBanned = new ArrayList<>();

    public List<Integer> roomsToleave = new ArrayList<>();
    private List<SERoom> rooms = new ArrayList<>();

    CommandCenter commands;
    private Database db;
    private BotConfig config;

    public List<Integer> hardcodedRooms = new ArrayList<>();
    public List<Long> hardcodedAdmins = new ArrayList<>();
    public Properties botProps;
    private Thread thread;
    private Timer timer = new Timer();
    private Host host;
    private SiteConfig credentialManager;
    private SEEventImpl events;

    public SEChat(Properties botProps, Database database, Host host, SiteConfig credentialManager) throws Exception {
        this.db = database;
        this.host = host;
        this.credentialManager = credentialManager;

        initConnections();
        logIn();

        startThread();
        events = new SEEventImpl(this);
        events.registerAll();

        commands = CommandCenter.Companion.getINSTANCE();

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
        }
        //Ignore unchecked cast warning
        //noinspection unchecked
        List<Integer> data = (List<Integer>) database.get(getName() + "-rooms");
        if(data != null){
            for(int x : data)
                join(x);
        }else{
            //No current rooms
            if(config.getHomes().size() == 0 && hardcodedRooms.size() == 0) {
                 hardcodedRooms.add((host.getName().equals("metastackexchange") ? 721 : 1));

            }
        }
        //noinspection UnusedAssignment
        data = null;
    }

    public void logIn() throws IOException {

        String targetUrl = (host.getName().equals("stackexchange")
                ? SEEvents.getSELogin(Objects.requireNonNull(host.getMainSiteHost()))
                : SEEvents.getLogin(Objects.requireNonNull(host.getMainSiteHost())));

        if (getName().equals("stackexchange")) {
            Connection.Response se = HttpHelper.post(targetUrl, cookies,"from", "https://stackexchange.com/users/login#log-in");
            targetUrl = se.body();
        }

        String fKey = HttpHelper.get(targetUrl, cookies).parse().select(FKEY_SELECTOR).first().val();

        Connection.Response response;

        if(fKey == null){
            System.out.println("No fKey found!");
            return;
        }
        this.fkey = fKey;

        if(getName().equals("stackexchange")){
            //TODO handle the new system
            // SE is removing OpenID, which means this would follow a different flow. This needs to be changed to handle
            // the new system then that time comes.
            targetUrl = "https://openid.stackexchange.com/affiliate/form/login/submit";
            response = HttpHelper.post(targetUrl, cookies, "email", credentialManager.getEmail(), "password", credentialManager.getPassword(), "fkey", fKey, "affId", "11");
            String TUREG = "(var target = .*?;)";
            Pattern pattern = Pattern.compile(TUREG);
            Matcher m = pattern.matcher(response.body());
            //noinspection ConstantConditions
            response = HttpHelper.get(m.find() ? m.group(0).replace("var target = ", "").replace("'", "").replace(";", "") : null, cookies);
        }else{
           response = HttpHelper.post(targetUrl, cookies, "email", credentialManager.getEmail(),
                   "password", credentialManager.getPassword(), "fkey", fKey);
        }


        int statusCode = response.statusCode();

        //SE doesn't redirect automatically, but the page does exist. Allow both 200 and 302 status codes.
        if(statusCode != 200 && statusCode != 302){
            throw new IllegalAccessError();
        }

        Connection.Response checkResponse = HttpHelper.get(host.getMainSiteHost() + "/users/current", cookies);
        if (checkResponse.parse().getElementsByClass("js-inbox-button").first() == null) {
            System.out.println(response.body());
            throw new IllegalStateException("Unable to login to Stack Exchange.");
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
                Connection.Response response = HttpHelper.get(host.getChatHost() + "/rooms/" + rid, cookies);
                if (response.statusCode() == 404)
                    throw new RoomNotFoundException("");
            }catch(RoomNotFoundException e){
                throw e;//re-throw for the outer catch statement
            }catch(Exception e){
                commands.getCrash().crash(e);
                return new ReplyMessage("An exception occured when trying to check the validity of the room", true);
            }
            join(rid);

            return new ReplyMessage(Utils.getRandomJoinMessage(), true);

        }catch(IOException e){
            return new ReplyMessage("An IOException occured when attempting to join", true);
        }catch(RoomNotFoundException e){
            return new ReplyMessage("That's not a real room or I can't write there", true);
        }catch(Exception e){
            commands.getCrash().crash(e);
            e.printStackTrace();
        }

        return new ReplyMessage("Something bad happened when joining the room. Run the `logs` command for more info.", true);
    }

    public boolean leaveRoom(int rid){
        for(Integer room : hardcodedRooms)
            if(room == rid)
                return false;

        try{
            List<SERoom> matches = rooms.stream().filter((item) -> item.getId() == rid).collect(Collectors.toList());
            if(matches == null || matches.isEmpty())
                return false;
            matches.forEach((room) -> {
                try{
                    room.close();
                }catch(IOException e){
                    e.printStackTrace();
                }
            });
            return rooms.removeIf((item) -> item.getId() == rid);
        }catch(Exception e){
            e.printStackTrace();
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

    public void startThread(){
        thread = new Thread(() -> {
            try {
                while (true) {

                    try {
                        Message m = newMessages.take();//This blocks the thread when it's empty, preventing continous looping.
                        if(m == stopMessage)
                            break;
                        handleMessage(m);//To keep this thread clean, use a separate method for message handling

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

    public void leaveAll(){
        for(SERoom s : rooms){
            try {
                s.close();
            }catch(IOException e){
                System.out.println("Failed to leave room " + s.getId() + " at " + getName());
            }
        }
    }

    public void join(int id) throws Exception{
        if(rooms.stream().anyMatch((room) -> room.getId() == id)){
            System.err.println("Duplicate averted");
            return;
        }
        SERoom room = new SERoom(id, this, cookies);
        addRoom(room);
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

    public String getFkey(){
        return fkey;
    }

    public Host getHost(){
        return host;
    }

    public void stop(){
        newMessages.add(stopMessage);
        try {
            thread.join(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
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

    @NotNull
    @Override
    public List<User> getUsersInServer(long server) {
        return rooms.stream().filter(room -> room.getId() == server).findAny().orElseThrow(IllegalArgumentException::new)
                .getPingableUsers();
    }

    public WebSocketContainer getWebSocket(){
        return webSocket;
    }

    private void initConnections(){
        if(webSocket == null) {
            ClientManager wsClient = ClientManager.createClient(JdkClientContainer.class.getName());
            wsClient.setDefaultMaxSessionIdleTimeout(0);
            wsClient.getProperties().put(ClientProperties.RETRY_AFTER_SERVICE_UNAVAILABLE, true);
            webSocket = wsClient;
        }

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
                        logger.debug("Notifying banned user...");
                        s.reply(Constants.BANNED_REPLY, message.getMessageID());
                    }
                }
            }
            return;
        }

        List<ReplyMessage> replies = commands.parseMessage(message);

        if (replies != null && getRoom((int)message.getRoomID()) != null && replies.size() != 0) {
            logger.debug("Sending messages");
            for (ReplyMessage replyMessage : replies) {
                logger.debug("Sending message: \"" + replyMessage.getContent() + "\"");
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

    public Map<String, String> getCookies(){
        return cookies;
    }

    @Override
    public boolean editMessage(long messageId, String newContent){
        if(rooms.size() == 0)
            return false;
        rooms.get(0).edit(messageId, newContent);
        return true;
    }
    @Override
    public boolean deleteMessage(long messageId){
        if(rooms.size() == 0)
            return false;
        rooms.get(0).delete(messageId);
        return true;
    }

    public CompletionStage<Boolean> editMessageCallback(long messageId, String newContent){
        if(rooms.size() == 0)
            return  CompletableFuture.supplyAsync(() -> false);
        return rooms.get(0).edit(messageId, newContent);
    }

    public CompletionStage<Boolean> deleteMessageCallback(long messageId){
        if(rooms.size() == 0)
            return  CompletableFuture.supplyAsync(() -> false);
        return rooms.get(0).delete(messageId);
    }

    public void registerCallback(int eventId, EventHandler eventCallback){
        List<EventHandler> callbacksForEvent = callbacks.get(eventId);
        if(callbacksForEvent == null)
            callbacksForEvent = new ArrayList<>();
        callbacksForEvent.add(eventCallback);
        callbacks.put(eventId, callbacksForEvent);
    }

    public void dispatchEventToCallback(SERoom origin, int eventId, JsonNode rawNode, JsonNode node){
        List<EventHandler> eventCallbacks = callbacks.get(eventId);
        if(eventCallbacks == null || eventCallbacks.isEmpty())
            return;
        eventCallbacks.forEach((callback) -> callback.onEventReceived(origin, eventId, rawNode, node));
    }
}
