package io.github.lunarwatcher.chatbot.bot.chat;

import io.github.lunarwatcher.chatbot.KUtilsKt;
import lombok.AllArgsConstructor;
import lombok.Getter;


public class Message {
    /**
     * The message to send
     */
    @Getter
    public String content;
    /**
     * message id
     */
    public long messageID;

    /**
     * Room
     */
    public int roomID;


    public String username;
    public int userid;

    public Message(String content, long messageId, int roomId, String username, int userId){
        cleanContentAndSet(content);
        this.messageID = messageId;
        this.roomID = roomId;
        this.username = username;
        this.userid = userId;
    }

    public void cleanContentAndSet(String original){
        content = KUtilsKt.cleanInput(original);
    }
}
