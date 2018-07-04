package io.github.lunarwatcher.chatbot.bot.sites.se;

public class SEUser {
    public int userID;
    public String userName;
    public int rep;
    public boolean moderator,
            roomOwner;
    public boolean inRoom;

    public SEUser(int userID, String userName, int rep, boolean mod, boolean ro, boolean inRoom){
        this.userID = userID;
        this.userName = userName;
        this.rep = rep;
        this.moderator = mod;
        this.roomOwner = ro;
        this.inRoom = inRoom;
    }
}
