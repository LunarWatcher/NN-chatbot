package io.github.lunarwatcher.chatbot.bot.sites.se;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.lunarwatcher.chatbot.bot.chat.Message;
import io.github.lunarwatcher.chatbot.bot.chat.SEEvents;
import io.github.lunarwatcher.chatbot.bot.exceptions.NoAccessException;
import io.github.lunarwatcher.chatbot.bot.exceptions.RoomNotFoundException;
import io.github.lunarwatcher.chatbot.utils.Response;
import io.github.lunarwatcher.chatbot.utils.Utils;
import lombok.AllArgsConstructor;
import lombok.Getter;

import javax.websocket.*;
import java.io.Closeable;
import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class SERoom implements Closeable {
    private int id;
    private SEChat parent;
    @Getter
    Session session;

    private String fkey;
    private Long lastMessage;
    boolean disconnected = false;
    private boolean intended = false;
    private int kickCount = 0;
    private boolean breakRejoin = false;
    public SERoom(int id, SEChat parent) throws Exception {
        this.id = id;
        this.parent = parent;

        createSession();
    }

    public void createSession() throws Exception{
        Response connect = parent.getHttp().get(SEEvents.getRoom(parent.getSite().getUrl(), id));
        if(connect.getStatusCode() == 404){
            parent.leaveRoom(id);
            throw new RoomNotFoundException("SERoom not found!");
        }

        if(!connect.getBody().contains("<textarea id=\"input\">")){
            throw new NoAccessException("No write access in the room!");
        }
        fkey = Utils.parseHtml(connect.getBody());


        ClientEndpointConfig config = ClientEndpointConfig.Builder.create()
                .configurator(new ClientEndpointConfig.Configurator() {
                    @Override
                    public void beforeRequest(Map<String, List<String>> headers) {
                        headers.put("Origin", Arrays.asList(parent.getSite().getUrl()));
                    }
                }).build();


        session = parent.getWebSocket().connectToServer(new Endpoint() {
            @Override
            public void onOpen(Session session, EndpointConfig config) {
                disconnected = false;
                intended = false;

                session.addMessageHandler(String.class, SERoom.this::receiveMessage);

            }
            public void onClose(Session session, CloseReason closeReason) {
                System.out.println("Connection closed: " + closeReason);
                disconnected = true;
                if(!intended)
                    respawn();

            }
            @Override
            public void onError(Session session, Throwable error){
                System.out.println("Error (src: " + id + " @" + parent.site.getName() + ": Error. " + error.getMessage());
                intended = false;
            }

        }, config, new URI(getWSURL()));
    }

    private int attempts = 0;
    public void respawn(){
        System.out.println("Attempting to respawn");
        try{
            Thread.sleep(100);
            createSession();
            System.out.println("Success!!!");
        }catch(NoAccessException | IOException e){
            persistentRespawn();
        } catch (Exception e){
            e.printStackTrace();
            //Other problem that isn't just disconnecting. The room doesn't exist, etc. Don't reconnect.
            //Remove from the list of rooms in the parent
            parent.getRooms().removeIf(it -> it.id == id);
        }
    }

    private void persistentRespawn(){
        attempts++;
        while(true) {
            try {
                Thread.sleep((long)(10000f * (attempts <= 0 ? 1 : attempts >= 6 ? 6 : attempts == 1 ? 0.5f : attempts)));//The ternary ihere is to avoid problems with thread access
            } catch (InterruptedException ignore) {
            }

            if(breakRejoin)
                break;
            try {
                respawnUnsafe();
            }catch(NoAccessException e){
                continue;
            }catch(RoomNotFoundException e){
                System.out.println("Room not found. Stopping.");
                break;
            } catch(Exception ex){
                continue;
            }
            break;
        }
        attempts = 0;
    }

    public void respawnUnsafe() throws Exception{
        createSession();
    }

    public String getWSURL() throws IOException{
        Response response = parent.http.post(parent.getSite().getUrl() + "/ws-auth",
                "roomid", id,
                "fkey", fkey
        );

        String url = null;
        try {
            url = response.getBodyAsJson().get("url").asText();
        }catch(Exception e){
            e.printStackTrace();
        }

        return url + "?l=" + System.currentTimeMillis();
    }

    public void receiveMessage(String input){
        try {

            ObjectMapper mapper = new ObjectMapper();
            JsonNode actualObj = mapper.readTree(input).get("r" + id);

            if(actualObj == null)
                return;

            actualObj = actualObj.get("e");
            if(actualObj == null)
                return;

            for(JsonNode event : actualObj) {
                try{
                    long time = event.get("time_stamp").asLong();
                    if(time > lastMessage)
                        lastMessage = time;
                }catch(Exception ignored){

                }
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
                    Message message = new Message(content, messageID, id, username, userid);

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
                        parent.commands.crash.crash(e);
                    }
                }else if(eventCode == 10){
                    //The message was deleted. Ignore it

                }else if(eventCode == 15){
                    try {
                        if (event.get("target_user_id").intValue() != parent.site.getConfig().getUserID())
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
    int tries = 0;
    public void sendMessage(String message) throws IOException{
        Response response = parent.getHttp().post(parent.getUrl() + "/chats/" + id + "/messages/new",
                "text", message,
                "fkey", fkey
        );

        if (response.getStatusCode() == 404) {
				/*
				 * We already checked to make sure the room exists. So, if a 404
				 * response is returned when trying to send a message, it likely
				 * means that the bot's permission to post messages has been
				 * revoked.
				 *
				 * If a 404 response is returned from this request, the response
				 * body reads:
				 * "The room does not exist, or you do not have permission"
				 */
            System.err.println("Room not found, or you can't access it: " + id);
        }else if (response.getStatusCode() == 409){
            tries++;
            if (tries > 4){
                //To avoid StackOverflowExceptions and repeated attempts on failed messages
                return;
            }

            new java.util.Timer().schedule(new java.util.TimerTask() {
                        @Override
                        public void run() {
                            try {
                                sendMessage(message);
                            }catch(IOException e){
                                parent.commands.crash.crash(e);
                            }
                        }
                    },
                    tries * 1000 * (tries - 1 <= 0 ? 1 : tries - 1));

        }else{
            tries = 0;
        }
    }

    public void reply(String message, long targetMessage) throws IOException{
        sendMessage(":" + targetMessage + " " + message);
    }

    @Override
    public void close() throws IOException {
        intended = true;
        parent.getHttp().post(SEEvents.leaveRoom(parent.getSite().getUrl(), id),
                "fkey", fkey);
        session.close();
    }

    @AllArgsConstructor
    public class UserAction{
        public int eventID, userID;
        public String username;
        public int room;
    }

    @AllArgsConstructor
    public class StarMessage{
        public long messageID;
        public int room;
        public int stars;

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
}
