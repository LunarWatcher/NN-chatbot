package io.github.lunarwatcher.chatbot.bot.sites.se.data;

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
