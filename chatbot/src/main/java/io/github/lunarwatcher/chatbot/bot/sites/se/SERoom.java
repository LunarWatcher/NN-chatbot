package io.github.lunarwatcher.chatbot.bot.sites.se;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.lunarwatcher.chatbot.User;
import io.github.lunarwatcher.chatbot.bot.chat.Message;
import io.github.lunarwatcher.chatbot.bot.chat.SEEvents;
import io.github.lunarwatcher.chatbot.bot.exceptions.NoAccessException;
import io.github.lunarwatcher.chatbot.bot.exceptions.RoomNotFoundException;
import io.github.lunarwatcher.chatbot.utils.HttpHelper;
import io.github.lunarwatcher.chatbot.utils.JsonUtils;
import io.github.lunarwatcher.chatbot.utils.Utils;
import org.jsoup.Connection;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ratpack.handling.internal.DefaultContext;

import javax.websocket.*;
import java.io.Closeable;
import java.io.IOException;
import java.net.URI;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class SERoom implements Closeable {
    private static final Logger logger = LoggerFactory.getLogger(SERoom.class);
    private static final int MAX_TIMEOUT = 30000;
    private ScheduledExecutorService taskExecutor = Executors.newSingleThreadScheduledExecutor();

    private int id;
    private SEChat parent;
    private Session session;

    private String fkey;
    private Long lastMessage;
    boolean disconnected = false;
    private boolean intended = false;
    private int kickCount = 0;
    private boolean breakRejoin = false;
    private int tries = 0;
    private List<Long> latestMessages = new ArrayList<>();

    public SERoom(int id, SEChat parent) throws Exception {
        System.out.println("Room created: " + id + " at " + parent.getName());
        this.id = id;
        this.parent = parent;

        createSession();
        stayAlive();
    }

    public void createSession() throws Exception{
        Connection.Response connect = HttpHelper.get(SEEvents.getRoom(parent.getHost().getChatHost(), id),
                parent.getCookies());
        int statusCode = connect.statusCode();

        if(statusCode == 404){
            parent.leaveRoom(id);
            throw new RoomNotFoundException("SERoom not found!");
        }

        fkey = connect.parse().select(SEChat.FKEY_SELECTOR).first().val();
        ClientEndpointConfig config = ClientEndpointConfig.Builder.create()
                .configurator(new ClientEndpointConfig.Configurator() {
                    @Override
                    public void beforeRequest(Map<String, List<String>> headers) {
                        headers.put("Origin", Collections.singletonList(parent.getHost().getChatHost()));
                    }
                }).build();



        session = parent.getWebSocket().connectToServer(new Endpoint() {
            @Override
            public void onOpen(Session session, EndpointConfig config) {
                session.addMessageHandler(String.class, SERoom.this::receiveMessage);
            }
            @Override
            public void onError(Session session, Throwable error){
                System.out.println("Error (src: " + id + " @ " + parent.getName() + ": Error. " + error.getMessage());

            }

        }, config, new URI(getWSURL()));
    }

    public void stayAlive(){
        taskExecutor.scheduleAtFixedRate(() -> {
            if (System.currentTimeMillis() - lastMessage > MAX_TIMEOUT) {
                try {
                    close();
                }catch(IOException e){
                    e.printStackTrace();
                }
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) { }
                try {
                    createSession();
                }catch(Exception e){
                    e.printStackTrace();
                }
            }
        }, MAX_TIMEOUT, MAX_TIMEOUT, TimeUnit.MILLISECONDS);
    }

    public String getWSURL() throws IOException{
        Connection.Response response = HttpHelper.post(parent.getHost().getChatHost() + "/ws-auth", true, parent.getCookies(),
                "roomid", Integer.toString(id),
                "fkey", fkey
        );

        String url = null;
        try {
            url = new ObjectMapper().readTree(response.body()).get("url").asText();
        }catch(Exception e){
            e.printStackTrace();
        }

        if(url == null)
            throw new NullPointerException();


        /**
         * This should be a better approach than System.currentTimeMillis. If it's in any way behind, connection
         * fails.
         */
        String time = JsonUtils.convertToJson(HttpHelper.post(parent.getHost()
                .getChatHost() + "/chats/" + id  + "/events", true, parent.getCookies(), "fkey", fkey)).get("time").toString();
        return url + "?l=" + time;
    }

    public void receiveMessage(String input){
        lastMessage = System.currentTimeMillis();
        try {
            JsonNode node = JsonUtils.convertToJson(input);

            Iterator<Map.Entry<String, JsonNode>> values = node.fields();
            List<Map.Entry<String, JsonNode>> listedValues = new ArrayList<>();
            values.forEachRemaining(listedValues::add);

            listedValues.stream().filter(n -> n.getKey().equals("r" + id))
                    .filter(Objects::nonNull)
                    .filter(n -> n.getKey().equals("e"))
                    .filter(Objects::nonNull).forEach(event -> {
                System.out.println(event.getKey() + " -> " + event.getValue());
            });

            ObjectMapper mapper = new ObjectMapper();
            JsonNode actualObj = mapper.readTree(input).get("r" + id);

            if(actualObj == null)
                return;

            actualObj = actualObj.get("e");
            if(actualObj == null)
                return;

            for(JsonNode event : actualObj) {
                JsonNode et = event.get("event_type");
                if (et == null)
                    continue;

                int eventCode = et.asInt();


                if (eventCode == 1 || eventCode == 2) {
                    String content = event.get("content").toString();
                    content = removeQuotation(content);
                    content = correctBackslash(content);

                    //New message or edited message

                    long messageID = event.get("message_id").asLong();
                    int userid = event.get("user_id").asInt();
                    String username = event.get("user_name").toString();

                    username = removeQuotation(username);
                    User user = new User(userid, username);
                    Message message = new Message(content, messageID, id, user, false, parent, parent.getHost());

                    parent.newMessages.add(message);
                } else if (eventCode == 3 || eventCode == 4) {
                    int userid = event.get("user_id").asInt();
                    String username = event.get("user_name").toString();
                    username = removeQuotation(username);

                    UserAction action = new UserAction(eventCode, userid, username, id);
                    parent.actions.add(action);
                } else if (eventCode == 6) {
                    long messageID = event.get("message_id").asLong();
                    int stars = event.get("message_stars").asInt();

                    StarMessage message = new StarMessage(messageID, id, stars);
                    parent.starredMessages.add(message);
                }else if(eventCode == 8){
                    try {
                        if (!parent.mentionIds.contains(event.get("message_id").asInt())) {
                            parent.mentionIds.add(event.get("message_id").asInt());
                        }
                    }catch(Exception e){
                        parent.commands.getCrash().crash(e);
                    }
                }else if(eventCode == 10){
                    //The message was deleted. Ignore it

                }else if(eventCode == 15){
                    try {
                        if (event.get("target_user_id").intValue() != parent.getCredentialManager().getUserID())
                            return;
                    }catch(NullPointerException e){
                        //No target user; meaning the state of the room was changed.
                        return;
                    }

                    System.out.println(event.get("content"));
                    System.out.println(event);
                    if(event.get("content").textValue().matches("((?i)priv \\d+ created)")){
                        System.out.println("Kicked");
                        kickCount++;
                        if(kickCount == 2){
                            breakRejoin = true;
                            close();
                        }

                    }else if(event.get("content").textValue().matches("((?i)access now [a-zA-Z]+)")){
                        System.out.println(event.get("content").textValue());
                    }
                }else{
                    //These are printed using the error stream to make sure they are easy to spot. These are critical
                    //to find more events in the SE network
                    System.err.println("Unknown event:");
                    System.err.println(event.toString());
                }

                // Event reference sheet:,
                //1: message
                //2: edited
                //3: join
                //4: leave
                //5: room name/description changed
                //6: star
                //7: Debug message (?)
                //8: ping - if called, ensure that the content does not contain a ping to the bot name if 1 is called
                //        - WARNING: Using event 8 will trigger in every single active room.
                //9:
                //10: deleted
                //11:
                //12:
                //13:
                //14:
                //15: Access level changed (kicks, RO added, read/write status changed, etc)
                //16:
                //17: Invite
                //18: reply
                //19: message moved out
                //20: message moved in

                //34: Username/profile picture changed

            }

        }catch(IOException e){
            e.printStackTrace();
        }
    }

    public String removeQuotation(String input){
        return input.substring(1, input.length() - 1);
    }

    //TODO refactor
    public long sendMessage(String message) throws IOException{

        Connection.Response response = HttpHelper.post(parent.getUrl() + "/chats/" + id + "/messages/new", parent.getCookies(),
                "text", message,
                "fkey", fkey
        );

        if (response.statusCode() == 404) {
            System.err.println("Room not found, or you can't access it: " + id);
            return -1;
        }else if (response.statusCode() == 409){
            tries++;
            if (tries > 5){
                //To avoid StackOverflowExceptions and repeated attempts on failed messages
                return -1;
            }

            new java.util.Timer().schedule(new java.util.TimerTask() {
                        @Override
                        public void run() {
                            try {
                                sendMessage(message);
                            }catch(IOException e){
                                parent.commands.getCrash().crash(e);
                            }
                        }
                    },
                    tries * 1000 * (tries - 1 <= 0 ? 1 : tries - 1));

        }else{
            tries = 0;
        }

        long id = JsonUtils.convertToJson(response)
                .get("id").longValue();
        while(latestMessages.size() >= 20)
            latestMessages.remove(0);
        latestMessages.add(id);

        return id;
    }

    public void reply(String message, long targetMessage) throws IOException{
        sendMessage(":" + targetMessage + " " + message);
    }

    @Override
    public void close() throws IOException {
        intended = true;
        HttpHelper.post(SEEvents.leaveRoom(parent.getHost().getChatHost(), id), parent.getCookies(),
                "fkey", fkey);
        session.close();
    }

    public boolean deleteMessage(long message){
        try {
            Connection.Response response = HttpHelper.post(parent.getHost().getChatHost() + "/messages/" + message + "/delete", parent.getCookies());
            return response.body().equals("ok");
        }catch(IOException e){
            e.printStackTrace();
            return false;
        }

    }

    public class UserAction{
        public int eventID, userID;
        public String username;
        public int room;

        public UserAction(int eventID, int userID, String username, int room){
            this.eventID = eventID;
            this.userID = userID;
            this.username = username;
            this.room = room;
        }

    }

    public class StarMessage{
        public long messageID;
        public int room;
        public int stars;

        public StarMessage(long messageID, int room, int stars){
            this.messageID = messageID;
            this.room = room;
            this.stars = stars;
        }
    }
    public int getId(){
        return id;
    }

    public String correctBackslash(String input){
        return input.replace("\\\\", "\\");
    }

    public SEChat getParent(){
        return parent;
    }

    public String getFKey(){
        return fkey;
    }

    public boolean getDisconnected(){
        return disconnected;
    }

    public boolean getIntended(){
        return intended;
    }

    public Session getSession(){
        return session;
    }
}
