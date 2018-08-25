package io.github.lunarwatcher.chatbot.bot.chat;

import io.github.lunarwatcher.chatbot.KUtilsKt;
import io.github.lunarwatcher.chatbot.User;
import io.github.lunarwatcher.chatbot.bot.sites.Chat;
import io.github.lunarwatcher.chatbot.bot.sites.Host;
import io.github.lunarwatcher.chatbot.bot.sites.twitch.TwitchChat;
import org.jetbrains.annotations.NotNull;

import static io.github.lunarwatcher.chatbot.bot.command.CommandCenter.TRIGGER;


public class Message {
    private String content;
    private long messageID;
    private long roomID;
    private Chat chat;
    private Host host;
    private User user;
    private boolean nsfwSite;

    public Message(String content, long messageId, long roomId, User user, boolean nsfwSite, Chat chat, Host host){
        cleanContentAndSet(content);
        this.messageID = messageId;
        this.roomID = roomId;
        this.chat = chat;
        this.host = host;
        this.user = user;
        this.nsfwSite = nsfwSite;

    }

    public void cleanContentAndSet(String original){
        content = KUtilsKt.cleanInput(original);
    }

    @NotNull
    public Chat getChat(){
        return chat;
    }

    public String getContent(){
        return content;
    }

    public long getMessageID(){
        return messageID;
    }

    public long getRoomID(){
        return roomID;
    }

    public int getIntRoomId(){
        return (int) roomID;
    }

    public User getUser(){
        return user;
    }

    public Host getHost(){
        return host;
    }

    public boolean getNsfwSite(){
        return nsfwSite;
    }

    public void substring(int start){
        content = content.substring(start);
    }

    public void substring(int start, int end){
        content = content.substring(start, end);
    }

    public Message prefixTrigger(){
        if(chat instanceof TwitchChat){
            content = "!!" + content;
        }else{
            content = TRIGGER + (content.trim());
        }
        return this;
    }

    public Message prefixTriggerAndRemovePing(){
        content = content.split(" ", 2)[1];
        if(chat instanceof TwitchChat){
            content = "!!" + content;
        }else{
            content = TRIGGER + (content.trim());
        }
        return this;
    }

    public Message clone(){
        return new Message(content, messageID, roomID, user, nsfwSite, chat, host);
    }

}
