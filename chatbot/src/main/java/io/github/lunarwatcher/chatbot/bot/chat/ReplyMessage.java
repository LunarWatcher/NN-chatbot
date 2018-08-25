package io.github.lunarwatcher.chatbot.bot.chat;


/**
 * Extremely basic message not containing any information about where it is, where it's going, etc
 */

public class ReplyMessage {
    private String content;
    private boolean replyIfPossible;


    public ReplyMessage(String content){
        this(content, false);
    }
    /**
     *
     * @param content The content of a message to send
     * @param replyIfPossible replyIfPossible - replies to a message in supported platforms
     */
    public ReplyMessage(String content, boolean replyIfPossible){
        if(content == null)
            content = "";

        this.content = content;
        this.replyIfPossible = replyIfPossible;
    }

    public String getContent(){
        return content;
    }

    public boolean getReplyIfPossible(){
        return replyIfPossible;
    }

    public void setReplyFormat(String ping){
        content = ping + " " + content;
    }

    public ReplyMessage postfixString(String string){
        content = content + string;

        return this;
    }
}
