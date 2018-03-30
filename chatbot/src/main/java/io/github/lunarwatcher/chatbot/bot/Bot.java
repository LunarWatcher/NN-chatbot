package io.github.lunarwatcher.chatbot.bot;


import io.github.lunarwatcher.chatbot.Database;
import io.github.lunarwatcher.chatbot.Site;
import io.github.lunarwatcher.chatbot.bot.command.CommandCenter;
import io.github.lunarwatcher.chatbot.bot.sites.Chat;
import io.github.lunarwatcher.chatbot.bot.sites.discord.DiscordChat;
import io.github.lunarwatcher.chatbot.bot.sites.se.SEChat;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.glassfish.tyrus.client.ClientManager;
import org.glassfish.tyrus.client.ClientProperties;
import org.glassfish.tyrus.container.jdk.client.JdkClientContainer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class Bot {
    Database database;
    Properties botProps;
    List<Site> sites;
    List<Chat> chats = new ArrayList<>();


    public Bot(Database db, Properties botProps, List<Site> sites) {
        this.database = db;
        this.botProps = botProps;
        this.sites = sites;

        CommandCenter.bot = this;
    }

    public void initialize() throws IOException {
        for(Site site : sites){
            if(site.getName().equals("discord")) {
                chats.add(new DiscordChat(site, botProps, database));
            }else {
                CloseableHttpClient httpClient = HttpClients.createDefault();
                ClientManager websocketClient = ClientManager.createClient(JdkClientContainer.class.getName());
                websocketClient.setDefaultMaxSessionIdleTimeout(0);
                websocketClient.getProperties().put(ClientProperties.RETRY_AFTER_SERVICE_UNAVAILABLE, true);
                chats.add(new SEChat(site, httpClient, websocketClient, botProps, database));
            }
        }

    }

    public void kill(){
        System.out.println("Killing");
        save();
        for(Chat s : chats) {
            if (s instanceof SEChat) {
                ((SEChat) s).leaveAll();
            }
        }

    }

    public void save(){
        for(Chat s : chats){
            s.save();
        }
        CommandCenter.saveTaught();
        database.commit();
    }

    public List<Chat> getChats(){
        return chats;
    }
}
