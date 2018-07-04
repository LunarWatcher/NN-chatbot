package io.github.lunarwatcher.chatbot.bot.sites.discord;

import io.github.lunarwatcher.chatbot.Constants;
import io.github.lunarwatcher.chatbot.Database;
import io.github.lunarwatcher.chatbot.SiteConfig;
import io.github.lunarwatcher.chatbot.User;
import io.github.lunarwatcher.chatbot.bot.chat.Message;
import io.github.lunarwatcher.chatbot.bot.chat.ReplyMessage;
import io.github.lunarwatcher.chatbot.bot.command.CommandCenter;
import io.github.lunarwatcher.chatbot.bot.command.CommandGroup;
import io.github.lunarwatcher.chatbot.bot.sites.Chat;
import io.github.lunarwatcher.chatbot.bot.sites.Host;
import io.github.lunarwatcher.chatbot.data.BotConfig;
import io.github.lunarwatcher.chatbot.utils.Utils;
import kotlin.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import sx.blah.discord.api.ClientBuilder;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageEditEvent;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;
import sx.blah.discord.handle.obj.*;
import sx.blah.discord.util.DiscordException;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static io.github.lunarwatcher.chatbot.Constants.DEFAULT_NSFW;

public class DiscordChat implements Chat{
    public static final Host host = Host.DISCORD;

    private static final boolean truncated = true;
    private static final List<CommandGroup> groups = Arrays.asList(CommandGroup.DISCORD, CommandGroup.NSFW);
    CommandCenter commands;
    IDiscordClient client;
    Properties botProps;
    private Database db;
    private List<IChannel> channels;
    private BotConfig config;
    public List<Long> hardcodedAdmins = new ArrayList<>();
    List<Long> notifiedBanned = new ArrayList<>();
    Map<Long, Boolean> nsfw = new HashMap<>();
    public String clientID;
    private SiteConfig credentialManager;

    public DiscordChat(Properties botProps, Database db, SiteConfig credentialManager) throws IOException {
        this.db = db;
        this.botProps = botProps;
        this.credentialManager = credentialManager;

        logIn();

        commands = CommandCenter.INSTANCE;
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
                @SuppressWarnings("unchecked")
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
        commands.save();
        db.put("sfw", sites);

    }

    @SuppressWarnings("RedundantThrows")
    @Override
    public void logIn() throws IOException {
        client = new ClientBuilder()
                .withToken(credentialManager.getEmail())
                .setMaxReconnectAttempts(30)
                .set5xxRetryCount(30)
                .build();
        client.getDispatcher().registerListener(this);
        client.login();
        client.changePresence(StatusType.ONLINE, ActivityType.PLAYING, CommandCenter.TRIGGER + "help");

        clientID = client.getApplicationClientID();
    }

    @SuppressWarnings("unused")
    @EventSubscriber
    public void onMessageEdited(MessageEditEvent event){
        MessageReceivedEvent rEvent = new MessageReceivedEvent(event.getMessage());
        this.onMessageReceived(rEvent);
    }

    @EventSubscriber
    public void onMessageReceived(MessageReceivedEvent event){
        try {

            commands.hookupToRanks(event.getAuthor().getLongID(), this);

            String msg = event.getMessage().getContent();

            if (Utils.isBanned(event.getAuthor().getLongID(), config)) {
                if (CommandCenter.Companion.isCommand(msg)) {
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

                User user = new User(event.getAuthor().getLongID(), event.getAuthor().getName(),
                        new Pair<>("guildID", Long.toString(event.getGuild().getLongID())));
                Message message = new Message(event.getMessage().getContent(), event.getMessageID(),
                        event.getChannel().getLongID(), user,
                        event.getChannel().isNSFW() || getNsfw(event.getGuild().getLongID()),
                        this, host);

                List<ReplyMessage> replies = commands.parseMessage(message);

                if (replies == null) {
                    if (CommandCenter.Companion.isCommand(msg)) {
                        event.getChannel().sendMessage(Constants.INVALID_COMMAND + " (//help)");
                    }
                } else {
                    for (ReplyMessage r : replies) {
                        if(r == Constants.bStopMessage)
                            return;
                        if(r == CommandCenter.Companion.getNO_MESSAGE()){
                            continue;
                        }
                        if(r.getReplyIfPossible()){
                            r.setReplyFormat("<@" + event.getAuthor().getLongID() + ">");
                        }
                        List<String> items = new ArrayList<>();
                        if (r.getContent().length() > 2000) {
                            boolean fixedFont = r.getContent().startsWith("```");

                            int i = 0;
                            int total = r.getContent().length();
                            while (i < total) {

                                int remaining = total - i;
                                int sub = 0;
                                if (remaining >= 2000) {
                                    sub = 2000;
                                    if(fixedFont)
                                        sub -= 3;
                                    if(!r.getContent().substring(i, i + sub).startsWith("```") && fixedFont)
                                        sub -= 3;
                                } else {
                                    sub = remaining;
                                }
                                String subbed = r.getContent().substring(i, i + sub);
                                if(!subbed.startsWith("```") && fixedFont)
                                    subbed = "```" + subbed;
                                if(!subbed.endsWith("```") && fixedFont)
                                    subbed += "```";
                                items.add(subbed);
                                i += sub;
                            }

                            for (String item : items) {
                                event.getChannel().sendMessage(item);
                                try {
                                    Thread.sleep(150);
                                } catch (InterruptedException ignored) {
                                }
                            }

                        } else {
                            event.getChannel().sendMessage(r.getContent());
                        }
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
        return host.getName();
    }

    public String getUsername(long uid){
        if(client != null){
            try {
                return client.getUserByID(uid).getName();
            }catch(Exception ignored){}
        }
        return Long.toString(uid);
    }

    @Override
    public List<Long> getUidForUsernameInRoom(String username, long server) {
        List<IUser> users = client.getUsersByName(username);
        return users.stream().map(IIDLinkedObject::getLongID).collect(Collectors.toList());
    }

    public List<Long> getHardcodedAdmins(){
        return hardcodedAdmins;
    }

    public Properties getBotProps(){
        return botProps;
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

    public CommandCenter getCommands(){
        return commands;
    }


    @Override
    public void leaveServer(long id){
        client.getGuildByID(id).leave();
    }

    public void close(){
        try {
            client.logout();
        }catch(DiscordException ignored){ /* Logout throws an exception during shutdown (because the shutdown is detected and it's logged out before this method is called)
        as a result, the exception needs to be caught for the shutdown to work properly*/ }
    }

    public boolean getTruncated(){
        return truncated;
    }
    public List<CommandGroup> getCommandGroup(){
        return groups;
    }

    @Nullable
    public IChannel getChannel(long id){
        System.out.println(id);
        IChannel channel = client.getChannelByID(id);
        System.out.println(channel);
        return channel;
    }

    public SiteConfig getCredentialManager(){
        return credentialManager;
    }

    @NotNull
    @Override
    public List<User> getUsersInServer(long server) {
        List<IUser> discordUsers = client.getChannelByID(server).getUsersHere();
        return discordUsers.stream().map((iUser -> new User(iUser.getLongID(), iUser.getName()))).collect(Collectors.toList());
    }

    @Override
    public Host getHost() {
        return host;
    }

}