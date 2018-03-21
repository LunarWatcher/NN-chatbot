package io.github.lunarwatcher.chatbot.bot.chat;


/**
 * Extremely basic message not containing any information about where it is, where it's going, etc
 */

public class BMessage {
    public String content;
    public boolean replyIfPossible;

    /**
     *
     * @param content The content of a message to send
     * @param rip replyIfPossible - replies to a message in supported platforms
     */
    public BMessage(String content, boolean rip){
        this.content = content;
        this.replyIfPossible = rip;
    }

}
