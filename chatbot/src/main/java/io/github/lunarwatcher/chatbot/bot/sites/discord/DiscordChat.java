package io.github.lunarwatcher.chatbot.bot.sites.discord;

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
    public static Map<String, Pattern> regMatch = new HashMap<>();
    private List<RMatch> regex;
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

        regex = new ArrayList<>();
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

        List<Object> reg = db.getList("regex");
        if(reg != null){

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
                        event.getChannel().sendMessage("You're banned from interacting with me <@" + event.getAuthor().getLongID() + ">");
                    }
                }
                return;
            }
            if (msg.startsWith(TRIGGER + "stats")) {
                String cmd = msg.replace(TRIGGER + "stats ", "");
                RMatch match = null;

                for (RMatch m : regex) {
                    if (m.usern.toLowerCase().equals(cmd.toLowerCase())) {
                        match = m;
                        break;
                    }
                }

                if (match != null) {
                    event.getChannel().sendMessage(match.message());
                } else {
                    event.getChannel().sendMessage("User not listed. Yet :smirk:");
                }
            } else {
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
            }
            long uid = event.getAuthor().getLongID();
            String uname = event.getAuthor().getName();
            RMatch u = null;

            for (RMatch m : regex) {
                if (m.userid == uid) {
                    u = m;
                }
            }

            if (u == null) {
                u = new RMatch(uid, uname);
                regex.add(u);
            }

            u.match(event.getMessage().getContent());

        }catch(Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public BotConfig getConfig() {
        return config;
    }

    public class RMatch{
        public long userid;
        public String usern;
        public Map<String, Long> occurences;
        public long totalMessages;
        public long hits;

        public RMatch(long uid, String name){
            this.userid = uid;
            this.usern = name;

            occurences = new HashMap<>();


            occurences.put("geis+?noo+?b", 0L);
            occurences.put("geis+", 0L);
            occurences.put("noo+?b", 0L);
            occurences.put("lo+l", 0L);
            occurences.put("lmf*?ao+", 0L);
            occurences.put("(ha+(ha+)+)", 0L);
            occurences.put(":sloth:", 0L);
            occurences.put(":thinking:", 0L);
            occurences.put("zoe", 0L);

            if(regMatch.size() == 0){
                for(Map.Entry<String, Long> occurence : occurences.entrySet()){
                    regMatch.put(occurence.getKey(), Pattern.compile(occurence.getKey()));

                }
            }
        }

        public void match(String input){
            totalMessages++;
            for(Map.Entry<String, Pattern> reg : regMatch.entrySet()){
                Pattern p = reg.getValue();

                Matcher m = p.matcher(input);
                if (m.find()) {
                    occurences.put(reg.getKey(), occurences.get(reg.getKey()) + 1);
                    hits++;
                    break;
                }
            }
        }

        public String message(){
            StringBuilder sb = new StringBuilder();
            sb.append("Regex reactions for user \"" + usern + "\"").append("\n");
            for(Map.Entry<String, Long> e : occurences.entrySet()){
                sb.append(e.getKey() + " - : - " + e.getValue()).append("\n");
            }
            sb.append("This user sent ").append(totalMessages).append(" in which there were found ").append(hits).append(" matches.").append("\n");
            sb.append("The total match rate is ").append((double)((double)hits / (double)totalMessages) * 100).append("%").append("\n");
            return sb.toString();
        }
    }

    public static class Match extends AbstractCommand {
        public Match() {
            super("stats", new ArrayList<>(), "Get the status for a user", TRIGGER + "stats <username>");
        }
        @Override
        public BMessage handleCommand(@NotNull String input, @NotNull User user) {
            return null;
        }
    }

    public String getName(){
        return site.getName();
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

    public String getUsername(long uid){
        return Long.toString(uid);
    }
}