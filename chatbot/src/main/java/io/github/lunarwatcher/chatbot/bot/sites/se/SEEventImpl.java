package io.github.lunarwatcher.chatbot.bot.sites.se;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.lunarwatcher.chatbot.User;
import io.github.lunarwatcher.chatbot.bot.chat.Message;
import io.github.lunarwatcher.chatbot.bot.sites.se.data.StarMessage;
import io.github.lunarwatcher.chatbot.bot.sites.se.data.UserAction;
import io.github.lunarwatcher.chatbot.socket.EventHandler;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

import static io.github.lunarwatcher.chatbot.utils.Utils.correctBackslash;
import static io.github.lunarwatcher.chatbot.utils.Utils.removeQuotation;

public class SEEventImpl {
    private SEChat chat;

    public SEEventImpl(SEChat chat) {
        this.chat = chat;
    }

    public void registerAll(){
        chat.registerCallback(1, onMessageEditedOrReceived);
        chat.registerCallback(2, onMessageEditedOrReceived);
        chat.registerCallback(3, onUserLeaveOrJoin);
        chat.registerCallback(4, onUserLeaveOrJoin);
        chat.registerCallback(6, messageStarEvent);
    }

    private EventHandler onMessageEditedOrReceived = new EventHandler(){
        @Override
        public void onEventReceived(@NotNull SERoom origin, int eventId, @NotNull JsonNode rawNode, @NotNull JsonNode eventNode){
            String content = eventNode.get("content").toString();
            content = removeQuotation(content);
            content = correctBackslash(content);

            long messageID = eventNode.get("message_id").asLong();
            int userid = eventNode.get("user_id").asInt();
            String username = eventNode.get("user_name").toString();

            username = removeQuotation(username);
            User user = new User(userid, username);
            Message message = new Message(content, messageID, origin.getId(), user,
                    false, chat, chat.getHost());

            chat.newMessages.add(message);
        }
    };

    private EventHandler onUserLeaveOrJoin = new EventHandler() {
        @Override
        public void onEventReceived(@NotNull SERoom origin, int eventId, @NotNull JsonNode rawEvent, @NotNull JsonNode eventNode) {
            int userid = eventNode.get("user_id").asInt();
            String username = eventNode.get("user_name").toString();
            username = removeQuotation(username);

            UserAction action = new UserAction(eventId, userid, username, origin.getId());
            chat.actions.add(action);

            if(eventId == 3){
                //Look for new users
                if(origin.getPingableUsers().stream().noneMatch((user) -> user.getUserID() == userid)){
                    origin.refreshPingableUsers();
                }
            }

            chat.addUsername(userid, username);
        }
    };

    private EventHandler messageStarEvent = new EventHandler() {
        @Override
        public void onEventReceived(@NotNull SERoom origin, int eventId, @NotNull JsonNode rawEvent, @NotNull JsonNode eventNode) {
            long messageID = eventNode.get("message_id").asLong();
            int stars = eventNode.get("message_stars").asInt();

            StarMessage message = new StarMessage(messageID, origin.getId(), stars);
            chat.starredMessages.add(message);
        }
    };

    private EventHandler onEvent15 = new EventHandler() {
        @Override
        public void onEventReceived(@NotNull SERoom origin, int eventId, @NotNull JsonNode rawEvent, @NotNull JsonNode eventNode) {
            try {
                if (eventNode.get("target_user_id").intValue() != chat.getCredentialManager().getUserID())
                    return;
            }catch(NullPointerException e){
                //No target user; meaning the state of the room was changed.
                return;
            }

            System.out.println(eventNode.get("content"));
            System.out.println(eventNode);
            if(eventNode.get("content").textValue().matches("((?i)priv \\d+ created)")){
                System.out.println("Kicked");
                origin.incrementKickCount();
                if(origin.getKickCount() >= 2){
                    chat.leaveRoom(origin.getId());
                    //Reset after leaving so it can re-join if the problem gets sorted out
                    origin.setKickCount(0);
                }else{
                    try {
                        origin.closeSocket();
                    }catch(IOException e){
                        e.printStackTrace();
                    }
                    new Thread(() -> {
                        rejoinRoomAfterSleep(60000, origin);
                    }).start();
                }

            }else if(eventNode.get("content").textValue().matches("((?i)access now [a-zA-Z]+)")){
                System.out.println(eventNode.get("content").textValue());
            }
        }
    };

    private void rejoinRoomAfterSleep(int sleep, SERoom room){
        try {
            Thread.sleep(sleep);
        }catch(InterruptedException ignored){

        }
        try {
            room.createSession();
        }catch(Exception e){
            e.printStackTrace();
        }
    }

}
