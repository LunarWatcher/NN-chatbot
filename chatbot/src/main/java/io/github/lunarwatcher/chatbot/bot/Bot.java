package io.github.lunarwatcher.chatbot.bot;


import io.github.lunarwatcher.chatbot.*;
import io.github.lunarwatcher.chatbot.bot.command.CommandCenter;
import io.github.lunarwatcher.chatbot.bot.sites.Chat;
import io.github.lunarwatcher.chatbot.bot.sites.Host;
import io.github.lunarwatcher.chatbot.bot.sites.discord.DiscordChat;
import io.github.lunarwatcher.chatbot.bot.sites.se.SEChat;
import io.github.lunarwatcher.chatbot.bot.sites.twitch.TwitchChat;
import io.github.lunarwatcher.chatbot.data.CentralBlacklistStorage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.util.*;

public class Bot {
    Database database;
    Properties botProps;
    Map<String, SiteConfig> sites;
    List<Chat> chats = new ArrayList<>();
    Timer timer;

    public Bot(Database db, Properties botProps, Map<String, SiteConfig> sites) {
        this.database = db;
        this.botProps = botProps;
        this.sites = sites;
        CommandCenter.Companion.setBot(this);
        CommandCenter.Companion.initialize(botProps, db);
        WelcomeMessages.Companion.initialize(db);
        timer = new Timer();
        timer.scheduleAtFixedRate(
                new SaveTask(this),
                10 * 60 * 1000,
                10 * 60 * 1000);//

    }

    public void initialize() throws Exception {
        for(Map.Entry<String, SiteConfig> entry : sites.entrySet()){
            String site = entry.getKey();
            SiteConfig credentialManager = entry.getValue();

            if(site.equals("discord")) {
                chats.add(new DiscordChat(botProps, database, credentialManager));
            }else if(site.equals("twitch")){
                chats.add(new TwitchChat(botProps, database, credentialManager));
            }else if(site.equals("stackexchange")
                    || site.equals("stackoverflow")
                    || site.equals("metastackexchange")){
                chats.add(new SEChat(botProps, database, Host.getHostForName(site), credentialManager));
            }
        }

        CommandCenter.INSTANCE.postSiteInit();

    }

    public void kill(){
        System.out.println("Killing");
        save();
        for(Chat s : chats) {
            if (s instanceof SEChat) {
                ((SEChat) s).leaveAll();
                ((SEChat) s).stop();
            }else if(s instanceof DiscordChat){
                ((DiscordChat) s).close();
            }else if(s instanceof TwitchChat){
                ((TwitchChat) s).stop();
            }
        }


        if (LogStorage.INSTANCE.getLogs().size() != 0) {
            try {
                FileOutputStream fis = new FileOutputStream(new File("logs.txt"));
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(fis));
                for (String log : LogStorage.INSTANCE.getLogs()){
                    writer.write(log);
                    writer.write("\n");
                }
                fis.close();
                writer.close();
            }catch(IOException e){
                //Ignore
            }
        }

        timer.cancel();

    }


    public @Nullable Chat getChatByName(@NotNull String chatName){
        return getChatByName(chatName, false);
    }

    public @Nullable Chat getChatByName(@NotNull String chatName, boolean caseSensitive){
        for(Chat c : chats){
            if(caseSensitive){
                if(c.getName().equals(chatName))
                    return c;
            }else {
                if (c.getName().equalsIgnoreCase(chatName))
                    return c;
            }
        }
        return null;
    }

    public void save(){
        if(chats.size() == 0)
            return;
        for(Chat s : chats){
            s.save();
        }
        CommandCenter.Companion.saveTaught();
        CentralBlacklistStorage.Companion.getInstance(database).save();
        //noinspection ConstantConditions
        WelcomeMessages.Companion.getINSTANCE().save();
        database.commit();

    }

    public List<Chat> getChats(){
        return chats;
    }

    public Database getDatabase(){
        return database;
    }
}
