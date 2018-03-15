package io.github.lunarwatcher.chatbot.bot.chat;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
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
}
