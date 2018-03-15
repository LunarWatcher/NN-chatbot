package io.github.lunarwatcher.chatbot.bot.sites;

import io.github.lunarwatcher.chatbot.Database;
import io.github.lunarwatcher.chatbot.Site;
import io.github.lunarwatcher.chatbot.bot.chat.Message;
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

}
