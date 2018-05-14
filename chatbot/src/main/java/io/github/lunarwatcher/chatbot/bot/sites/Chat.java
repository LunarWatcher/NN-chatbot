package io.github.lunarwatcher.chatbot.bot.sites;

import io.github.lunarwatcher.chatbot.Database;
import io.github.lunarwatcher.chatbot.Site;
import io.github.lunarwatcher.chatbot.bot.chat.Message;
import io.github.lunarwatcher.chatbot.bot.command.CommandCenter;
import io.github.lunarwatcher.chatbot.bot.command.CommandGroup;
import io.github.lunarwatcher.chatbot.bot.commands.BotConfig;

import java.io.IOException;
import java.util.List;
import java.util.Properties;

public interface Chat {
    void logIn() throws IOException;
    void save();
    void load();

    BotConfig getConfig();
    String getName();
    List<Long> getHardcodedAdmins();
    Site getSite();
    Properties getBotProps();
    Database getDatabase();
    CommandCenter getCommands();

    /**
     * Since the username systems for each site is different, this method aims to add abstraction to get it.
     * The uid is a long to handle everything, but if it's necessary to use as an int, casting locally is the
     * way to go
     * @param uid The user ID to retrieve
     * @return The username, or a stringified form of the UID if not found.
     */
    String getUsername(long uid);
    void leaveServer(long serverId);
    boolean getTruncated();
    List<CommandGroup> getCommandGroup();
}
