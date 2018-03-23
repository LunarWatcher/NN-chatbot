package io.github.lunarwatcher.chatbot.bot.sites.discord;

import io.github.lunarwatcher.chatbot.Constants;
import io.github.lunarwatcher.chatbot.Database;
import io.github.lunarwatcher.chatbot.Site;
import io.github.lunarwatcher.chatbot.bot.chat.BMessage;
import io.github.lunarwatcher.chatbot.bot.command.CommandCenter;
import io.github.lunarwatcher.chatbot.bot.commands.AbstractCommand;
import io.github.lunarwatcher.chatbot.bot.commands.BotConfig;
import io.github.lunarwatcher.chatbot.bot.commands.User;
import io.github.lunarwatcher.chatbot.bot.sites.Chat;
import io.github.lunarwatcher.chatbot.utils.Utils;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import sx.blah.discord.api.ClientBuilder;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageEditEvent;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;
import sx.blah.discord.handle.obj.IChannel;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.github.lunarwatcher.chatbot.Constants.DEFAULT_NSFW;
import static io.github.lunarwatcher.chatbot.bot.command.CommandCenter.TRIGGER;

public class DiscordChat implements Chat{
    Site site;
    CommandCenter commands;
    IDiscordClient client;
    Properties botProps;
    @Getter
    private Database db;
    private List<IChannel> channels;
    private BotConfig config;
    public List<Long> hardcodedAdmins = new ArrayList<>();
    List<Long> notifiedBanned = new ArrayList<>();
    Map<Long, Boolean> nsfw = new HashMap<>();
    /**
     * Not stored in memory as it is a good idea to check for updates every once in a while, and with a given bot reboot
     * rate keeping this in memory and not saved in the database is the best way for getting updates for usernames.
     */
    List<Long> checkedUsers = new ArrayList<>();

    public DiscordChat(Site site, Properties botProps, Database db) throws IOException {
        this.site = site;
        this.db = db;
        this.botProps = botProps;
        logIn();
        commands = new CommandCenter(botProps, false, this);
        commands.loadDiscord();
        commands.loadNSFW();

        channels = new ArrayList<>();

        config = new BotConfig(this);

        load();

        Utils.loadHardcodedAdmins(this);

    }

    public void load(){
        Utils.loadConfig(config, db);

        List<Object> sfw = db.getList("sfw");
        if(sfw != null) {
            for (Object o : sfw) {
                Map<String, Boolean> entry = (Map<String, Boolean>) o;
                for (Map.Entry<String, Boolean> e : entry.entrySet()) {
                    long guildID = Long.parseLong(e.getKey());
                    boolean allowsNSFW = e.getValue();
                    this.nsfw.put(guildID, allowsNSFW);
                }
            }
        }

    }

    public void save(){
        Utils.saveConfig(config, db);

        List<Map<String, Boolean>> sites = new ArrayList<>();

        for(Map.Entry<Long, Boolean> entry : nsfw.entrySet()){
            Map<String, Boolean> data = new HashMap<>();
            data.put(entry.getKey().toString(), entry.getValue());
            sites.add(data);
        }

        db.put("sfw", sites);

    }

    @Override
    public void logIn() throws IOException {
        client = new ClientBuilder()
                .withToken(site.getConfig().getEmail())
                .online()
                .build();
        client.getDispatcher().registerListener(this);
        client.login();

    }

    @EventSubscriber
    public void onMessageEdited(MessageEditEvent event){
        MessageReceivedEvent rEvent = new MessageReceivedEvent(event.getMessage());
        this.onMessageReceived(rEvent);
    }

    @EventSubscriber
    public void onMessageReceived(MessageReceivedEvent event){
        try {
            if (!checkedUsers.contains(event.getAuthor().getLongID())) {
                checkedUsers.add(event.getAuthor().getLongID());
                commands.hookupToRanks(event.getAuthor().getLongID(), event.getAuthor().getName());
            }
            String msg = event.getMessage().getContent();

            if (Utils.isBanned(event.getAuthor().getLongID(), config)) {
                if (CommandCenter.isCommand(msg)) {
                    boolean mf = false;

                    for (Long u : notifiedBanned) {
                        if (u == event.getAuthor().getLongID()) {
                            mf = true;
                            break;
                        }
                    }

                    if (!mf) {
                        notifiedBanned.add(event.getAuthor().getLongID());
                        event.getChannel().sendMessage(Constants.BANNED_REPLY + " <@" + event.getAuthor().getLongID() + ">");
                    }
                }
                return;
            }

            try {
                IChannel channel = null;

                for (IChannel chnl : channels) {
                    if (chnl.getLongID() == event.getChannel().getLongID()) {
                        channel = chnl;
                        break;
                    }
                }

                if (channel == null) {
                    channels.add(event.getChannel());
                }

                int index = 0;
                for (int i = 0; i < channels.size(); i++) {
                    if (channels.get(i).getLongID() == event.getChannel().getLongID()) {
                        index = i;
                        break;
                    }
                }

                User user = new User(site.getName(), event.getAuthor().getLongID(), event.getAuthor().getName(), index, getNsfw(event.getGuild().getLongID()));

                List<BMessage> replies = commands.parseMessage(msg, user, getNsfw(event.getGuild().getLongID()));
                if (replies == null) {
                    if (CommandCenter.isCommand(msg)) {
                        event.getChannel().sendMessage("Look up the manual maybe?");
                    }
                } else {
                    for (BMessage r : replies) {
                        List<String> items = new ArrayList<>();
                        if (r.content.length() > 2000) {
                            int i = 0;
                            int total = r.content.length();
                            while (i < total) {
                                int remaining = total - i;
                                int sub = 0;
                                if (remaining >= 2000) {
                                    sub = 2000;
                                } else {
                                    sub = remaining;
                                }
                                items.add(r.content.substring(i, i + sub));
                                i += sub;
                            }

                            for (int x = 0; x < (items.size() > 5 ? 5 : items.size()); x++) {
                                event.getChannel().sendMessage(items.get(x));
                                try {
                                    Thread.sleep(100);
                                } catch (InterruptedException e) {

                                }
                            }

                        } else
                            event.getChannel().sendMessage(r.content);
                    }
                }

            } catch (IOException e) {
                e.printStackTrace();
            }



        }catch(Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public BotConfig getConfig() {
        return config;
    }

    public String getName(){
        return site.getName();
    }

    public String getUsername(long uid){
        if(client != null){
            try {
                return client.getUserByID(uid).getName();
            }catch(Exception e){}//Ignore
        }
        return Long.toString(uid);
    }

    public List<Long> getHardcodedAdmins(){
        return hardcodedAdmins;
    }

    public Properties getBotProps(){
        return botProps;
    }
    public Site getSite(){
        return site;
    }

    public Database getDatabase(){
        return db;
    }

    public boolean getNsfw(long server){
        for(Map.Entry<Long, Boolean> entry : nsfw.entrySet()){
            if(entry.getKey() == server){
                return entry.getValue();
            }
        }

        nsfw.put(server, DEFAULT_NSFW);

        return DEFAULT_NSFW;
    }

    public void setNsfw(long server, boolean newState){
        nsfw.put(server, newState);
    }

    /**
     * Since the {@link User} class takes the room (here: channel) as an integer, the discord handler puts all the channels
     * into an ArrayList and then passes the index of a channel as the argument. This is then converted back into this method
     * where it gets the assosiated channel and returns the guild ID
     * @param channelIndex The channel index to get
     * @return The guild ID or -1 if not found
     */
    public long getAssosiatedGuild(int channelIndex){
        IChannel x;
        try {
            x = channels.get(channelIndex);
            if (x == null) {
                return -1;
            }
        }catch(IndexOutOfBoundsException e){
            return -1;
        }

        return x.getGuild().getLongID();
    }


}