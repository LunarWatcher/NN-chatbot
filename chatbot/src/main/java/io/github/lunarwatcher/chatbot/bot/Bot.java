package io.github.lunarwatcher.chatbot.bot;


import io.github.lunarwatcher.chatbot.*;
import io.github.lunarwatcher.chatbot.bot.command.CommandCenter;
import io.github.lunarwatcher.chatbot.bot.commands.CentralBlacklistStorage;
import io.github.lunarwatcher.chatbot.bot.sites.Chat;
import io.github.lunarwatcher.chatbot.bot.sites.discord.DiscordChat;
import io.github.lunarwatcher.chatbot.bot.sites.se.SEChat;
import io.github.lunarwatcher.chatbot.bot.sites.twitch.TwitchChat;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.glassfish.tyrus.client.ClientManager;
import org.glassfish.tyrus.client.ClientProperties;
import org.glassfish.tyrus.container.jdk.client.JdkClientContainer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Timer;

public class Bot {
    Database database;
    Properties botProps;
    List<Site> sites;
    List<Chat> chats = new ArrayList<>();
    Timer timer;

    public Bot(Database db, Properties botProps, List<Site> sites) {
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

    public void initialize() throws IOException {
        for(Site site : sites){
            if(site.getName().equals("discord")) {
                chats.add(new DiscordChat(site, botProps, database));
            }else if(site.getName().equals("twitch")){
                chats.add(new TwitchChat(site, botProps, database));
            }else {
                CloseableHttpClient httpClient = HttpClients.createDefault();
                ClientManager websocketClient = ClientManager.createClient(JdkClientContainer.class.getName());
                websocketClient.setDefaultMaxSessionIdleTimeout(0);
                websocketClient.getProperties().put(ClientProperties.RETRY_AFTER_SERVICE_UNAVAILABLE, true);
                chats.add(new SEChat(site, httpClient, websocketClient, botProps, database));
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
        for(Chat c : chats){
            if(c.getSite().getName().toLowerCase().equals(chatName))
                return c;
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
}
