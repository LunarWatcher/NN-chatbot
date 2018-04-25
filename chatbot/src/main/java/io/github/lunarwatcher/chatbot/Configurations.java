package io.github.lunarwatcher.chatbot;

public class Configurations {
    public static String CREATOR;
    public static String CREATOR_GITHUB;
    public static String GITHUB;
    public static String REVISION;
    /**
     * Not to be confused with {@link BotCore#LOCATION} - this location is the specific instance it runs. {@link BotCore#LOCATION}
     * is a specific location within the location, only used to debug multiple instance issues.
     */
    public static String INSTANCE_LOCATION;
    public static String NEURAL_NET_IP = "127.0.0.1";
}
