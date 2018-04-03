package io.github.lunarwatcher.chatbot.bot.exceptions;

public class NoAccessException extends RoomNotFoundException {
    public NoAccessException(String s) {
        super(s);
    }
}
