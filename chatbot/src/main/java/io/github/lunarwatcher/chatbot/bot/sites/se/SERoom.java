package io.github.lunarwatcher.chatbot.bot.sites.se;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.lunarwatcher.chatbot.User;
import io.github.lunarwatcher.chatbot.bot.chat.SEEvents;
import io.github.lunarwatcher.chatbot.bot.exceptions.RoomNotFoundException;
import io.github.lunarwatcher.chatbot.utils.HttpHelper;
import io.github.lunarwatcher.chatbot.utils.JsonUtils;
import org.jetbrains.annotations.NotNull;
import org.jsoup.Connection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.websocket.ClientEndpointConfig;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.Session;
import java.io.Closeable;
import java.io.IOException;
import java.net.URI;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class SERoom implements Closeable {
    private static final Logger logger = LoggerFactory.getLogger(SERoom.class);
    private static final Pattern pattern409 = Pattern.compile("again in (\\d+) seconds?");

    public static final int MAX_RETRIES = 5;
    private static final int MAX_TIMEOUT = 30000;

    private List<User> pingableUsers = new ArrayList<>();
    private Map<String, String> cookies;

    private ScheduledExecutorService taskExecutor = Executors.newSingleThreadScheduledExecutor();

    private int id;
    private SEChat parent;
    private Session session;

    private String fkey;
    private Long lastMessage;
    private int kickCount = 0;
    private int tries = 0;
    private List<Long> latestMessages = new ArrayList<>();

    public SERoom(int id, SEChat parent, Map<String, String> cookies) throws Exception {
        System.out.println("Room created: " + id + " at " + parent.getName());
        this.id = id;
        this.parent = parent;
        this.cookies = cookies;

        createSession();
        persistentSocket();

        refreshPingableUsers();


    }

    public void createSession() throws Exception{
        Connection.Response connect = HttpHelper.get(SEEvents.getRoom(parent.getHost().getChatHost(), id),
                cookies);
        int statusCode = connect.statusCode();

        if(statusCode == 404){
            parent.leaveRoom(id);
            throw new RoomNotFoundException("SERoom not found!");
        }

        fkey = connect.parse().select(SEChat.FKEY_SELECTOR).val();

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

                error.printStackTrace();
                parent.commands.getCrash().crash(error);

            }

        }, config, new URI(getWSURL()));
    }

    public void persistentSocket(){
        taskExecutor.scheduleAtFixedRate(() -> {
            if (System.currentTimeMillis() - lastMessage > MAX_TIMEOUT) {
                try {
                    closeSocket();
                }catch(IOException e){
                    e.printStackTrace();
                }
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException ignored) {

                }
                try {
                    createSession();
                }catch(Exception e){

                    e.printStackTrace();
                }
            }
        }, MAX_TIMEOUT, MAX_TIMEOUT, TimeUnit.MILLISECONDS);
    }

    public String getWSURL() throws IOException{
        String time = JsonUtils.convertToJson(HttpHelper.post(parent.getHost()
                .getChatHost() + "/chats/" + id  + "/events", true, cookies, "fkey", fkey)).get("time").toString();

        String url = post(parent.getHost().getChatHost() + "/ws-auth", 10,
                "fkey", fkey,
                "roomid", Integer.toString(id)
        ).get("url").asText();

        if(url == null)
            throw new NullPointerException();

        return url + "?l=" + time;
    }

    public void receiveMessage(String input){
        lastMessage = System.currentTimeMillis();
        try {
            JsonNode node = JsonUtils.convertToJson(input);

            Iterator<Map.Entry<String, JsonNode>> values = node.fields();
            values.forEachRemaining(event->{
                if(event.getKey().equals("r" + id)){
                    JsonNode eventNode = event.getValue().get("e");
                    if(eventNode != null) {
                        for (JsonNode dataNode : eventNode) {
                            JsonNode eventType = dataNode.get("event_type");

                            parent.dispatchEventToCallback(this, eventType.asInt(), event.getValue(), dataNode);

                        }
                    }
                }
            });

        }catch(IOException e){
            e.printStackTrace();
        }
    }

    public CompletionStage<Long> sendMessage(@NotNull String message) {
        return CompletableFuture.supplyAsync(() -> {
            System.out.println("Sending message: \"" + message + "\"");
            return postForUid(MAX_RETRIES, message);

        }, taskExecutor).whenComplete((res, throwable) -> {
            if (res != null){
                newMessage(res);
            }
            if (throwable != null){
                parent.commands.getCrash().crash(throwable);
                throwable.printStackTrace();
            }
        });
    }

    public CompletionStage<Long> reply(@NotNull String message, long targetMessage) {
        return sendMessage(":" + targetMessage + " " + message);
    }

    @Override
    public void close() throws IOException {
        leave();
        closeSocket();
    }

    public void leave() throws IOException{
        HttpHelper.post(SEEvents.leaveRoom(parent.getHost().getChatHost(), id), cookies,
                "fkey", fkey);
    }

    public void closeSocket() throws IOException{
        session.close();
    }

    public boolean deleteMessage(long message){
        try {
            Connection.Response response = HttpHelper.post(parent.getHost().getChatHost() + "/messages/" + message + "/delete", cookies);
            return response.body().equals("ok");
        }catch(IOException e){
            e.printStackTrace();
            return false;
        }

    }

    public int getId(){
        return id;
    }

    public SEChat getParent(){
        return parent;
    }

    public String getFKey(){
        return fkey;
    }

    public Session getSession(){
        return session;
    }

    public long getOldestMessageInStack(){
        if(latestMessages.size() == 0)
            return -1;
        return latestMessages.get(0);
    }

    public long getNewestMessageInStack(){
        if(latestMessages.size() == 0)
            return -1;
        return latestMessages.get(latestMessages.size() - 1);
    }

    public long getMessageInStackAtIndex(int index){
        if(index < 0 || index > 20 || index >= latestMessages.size())
            return -1;
        return latestMessages.get(index);
    }

    public void newMessage(long messageId){
        while(latestMessages.size() >= 20)
            latestMessages.remove(0);
        latestMessages.add(messageId);
    }

    public JsonNode post(@NotNull String url, int retries, String... data){
        Connection.Response response;
        try {
            response = HttpHelper.post(url, true, cookies, data);
            System.out.println(response.body());
        }catch(IOException e){
            e.printStackTrace();
            throw new RuntimeException(e);
        }

        if(response.statusCode() == 200){
            try{
                return JsonUtils.convertToJson(response.body());
            }catch(IOException e){
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }

        Matcher matcher = pattern409.matcher(response.body());
        if(retries > 0 && matcher.find()){
            long delay = Long.parseLong(matcher.group(1));
            try{
                Thread.sleep(delay * 1000);
            }catch(InterruptedException ignored){

            }
            return post(url, retries - 1, data);
        }else{
            throw new RuntimeException("Failed to send POST request");
        }

    }

    public long postForUid(int retries, @NotNull String message){
        return post(parent.getHost().getChatHost() + "/chats/" + id + "/messages/new", retries, "text", message, "fkey", fkey).get("id").longValue();
    }

    public boolean postForSuccess(String url, int retries, String... data){
        return post(url, retries, data).asText().equals("ok");
    }

    public CompletionStage<Boolean> delete(long messageId){
        return  CompletableFuture.supplyAsync(() -> postForSuccess(parent.getHost().getChatHost() + "/messages/" + messageId + "/delete", MAX_RETRIES, "fkey", fkey), taskExecutor)
                .whenComplete((res, throwable) -> {
            if (res != null){
                System.out.println(res ? "Deleted message" : "Failed to delete message");
            }
            if (throwable != null){
                parent.commands.getCrash().crash(throwable);
                throwable.printStackTrace();
            }
        });
    }

    public CompletionStage<Boolean> edit(long messageId, String newContent){
        return  CompletableFuture.supplyAsync(() -> postForSuccess(parent.getHost().getChatHost() + "/messages/" + messageId, MAX_RETRIES, "fkey", fkey, "text", newContent), taskExecutor)
                .whenComplete((res, throwable) -> {
                    if (res != null){
                        System.out.println(res ? "Edited message" : "Failed to edit message");
                    }
                    if (throwable != null){
                        parent.commands.getCrash().crash(throwable);
                        throwable.printStackTrace();
                    }
                });
    }

    public List<User> getPingableUsers() {
        if(!this.pingableUsers.isEmpty()){
            return this.pingableUsers;
        }

        return refreshPingableUsers();
    }

    List<User> refreshPingableUsers(){
        try {
            List<User> users = new ArrayList<>();

            String json = HttpHelper.get(parent.getHost().getChatHost() + "/rooms/pingable/" + id, cookies).body();

            JsonNode node = JsonUtils.convertToJson(json);
            //Raw format:
            // [ ..., [165415,"Zoe",1530731454,1530698908], ...]

            //Split into the child arrays
            Iterator<JsonNode> it = node.elements();
            it.forEachRemaining(n ->{
                Iterator<JsonNode> fields = n.elements();
                long userId = fields.next().asLong();
                String username = fields.next().asText();
                //noinspection unchecked
                users.add(new User(userId, username));
            });
            this.pingableUsers = users;
            parent.addUsernames(pingableUsers);
            return users;

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public int getKickCount(){
        return kickCount;
    }

    public void setKickCount(int newVal){
        this.kickCount = newVal;
    }

    public void incrementKickCount(){
        kickCount++;
    }

}
