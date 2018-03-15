package io.github.lunarwatcher.chatbot.bot.exceptions;

/**
 * Extremely basic class to handle unwanted exceptions that include room issues like not found, not permitted, etc
 *
 */
public class RoomNotFoundException extends Exception {
    public RoomNotFoundException(String s) {
        super(s);
    }
}
