package io.github.lunarwatcher.chatbot.bot.sites.se.data;

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
