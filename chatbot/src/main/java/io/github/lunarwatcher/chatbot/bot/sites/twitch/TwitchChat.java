package io.github.lunarwatcher.chatbot.bot.sites.twitch;

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
import me.philippheuer.twitch4j.TwitchClient;
import me.philippheuer.twitch4j.TwitchClientBuilder;
import me.philippheuer.twitch4j.endpoints.ChannelEndpoint;
import me.philippheuer.twitch4j.events.EventSubscriber;
import me.philippheuer.twitch4j.events.event.AbstractChannelEvent;
import me.philippheuer.twitch4j.events.event.irc.ChannelMessageEvent;
import me.philippheuer.twitch4j.message.commands.CommandPermission;
import me.philippheuer.twitch4j.model.Channel;
import me.philippheuer.twitch4j.model.tmi.Chatter;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

public class TwitchChat implements Chat {
    private static final Host host = Host.TWITCH;

    private static final boolean truncated = true;
    private static final List<CommandGroup> groups = Arrays.asList(CommandGroup.TWITCH, CommandGroup.NSFW);

    private TwitchClient client;
    CommandCenter commandCenter;
    private Properties botProps;
    private BotConfig config;
    private Database db;
    public List<Long> hardcodedAdmins = new ArrayList<>();
    List<Long> notifiedBanned = new ArrayList<>();
    public List<Channel> mappedChannels = new ArrayList<>();
    private String username;
    private SiteConfig credentialManager;

    public TwitchChat(Properties botProps, Database db, SiteConfig credentialManager) throws IOException{
        this.botProps = botProps;
        this.db = db;
        this.credentialManager = credentialManager;

        commandCenter = CommandCenter.INSTANCE;

        config = new BotConfig(this);

        logIn();
        load();
    }

    @Override
    public void logIn() throws IOException {
        Properties credentials = new Properties();
        InputStream creds = new FileInputStream(new File("creds.properties"));
        credentials.load(creds);
        creds.close();

        client = TwitchClientBuilder.init().withClientId(credentialManager.getEmail())
                .withClientSecret(credentialManager.getPassword())
                .withCredential(credentials.getProperty("twitch.oauth.credential"))
                .withListener(this)
                .withConfigurationDirectory(null)
                .withAutoSaveConfiguration(false)
                .connect();
        username = credentialManager.getUsername();

    }


    @Override
    public void save() {
        Utils.saveConfig(config, db);
        db.put("twitch-channels", mappedChannels.stream().map(Channel::getName).collect(Collectors.toList()));
    }

    @Override
    public void load() {
        Utils.loadConfig(config, db);
        Utils.loadHardcodedAdmins(this);

        List<Object> channels = db.getList("twitch-channels");
        if(channels != null) {
            for (Object o : channels) {
                @SuppressWarnings("unchecked")
                String channel = (String) o;
                System.out.println("Joining channel " + o);
                joinChannel(channel);
            }
        }
    }

    @Override
    public BotConfig getConfig() {
        return config;
    }

    @Override
    public String getName() {
        return host.getName();
    }

    @Override
    public List<Long> getHardcodedAdmins() {
        return hardcodedAdmins;
    }

    @Override
    public Properties getBotProps() {
        return botProps;
    }

    @Override
    public Database getDatabase() {
        return db;
    }

    @Override
    public CommandCenter getCommands() {
        return commandCenter;
    }

    @Override
    public String getUsername(long uid) {
        return client.getChannelEndpoint(uid).getChannel().getName();
    }

    @Override
    public List<Long> getUidForUsernameInRoom(String username, long server) {
        return Arrays.asList(client.getUserEndpoint().getUserIdByUserName(username).orElse(0L));
    }

    @Override
    public void leaveServer(long serverId) {/*Useless stub atm*/}

    public Long getUID(String username){
        Optional<Long> user = client.getUserEndpoint().getUserIdByUserName(username);
        return user.orElse(-1L);
    }

    @EventSubscriber
    public void onChannelMessage(ChannelMessageEvent event){
        System.out.println("Message received: " + event.toString());
        if(!isInChannel(event.getChannel().getName())){
            System.out.println("Caught unjoined channel; leaving");
            event.getClient().getMessageInterface().partChannel(event.getChannel().getName());
            return;
        }
        try {
            getCommands().hookupToRanks(event.getUser().getId(), this);

            String message = event.getMessage();
            if(message.startsWith("!!") && !CommandCenter.Companion.getTRIGGER().equals("!!"))
                message = CommandCenter.Companion.getTRIGGER() + message.substring(2);

            if (Utils.isBanned(event.getUser().getId(), config)) {
                if (CommandCenter.Companion.isCommand(message)) {
                    boolean mf = false;

                    for (Long u : notifiedBanned) {
                        if (u == event.getUser().getId()) {
                            mf = true;
                            break;
                        }
                    }

                    if (!mf) {
                        notifiedBanned.add(event.getUser().getId());
                        sendMessage(event,Constants.BANNED_REPLY + " " + constructPing(event));
                    }
                }
                return;
            }

            if(message.equals("@" + username + " trigger")) {
                if (!CommandCenter.Companion.getTRIGGER().equals("!!")) {
                    sendMessage(event, "Unfortunately, using " + CommandCenter.Companion.getTRIGGER() + " here isn't an option as Twitch sees that as integrated commands (because /whatever triggers internal commands). " +
                            "As a result, this site uses !! as the trigger, but converts it to " + CommandCenter.Companion.getTRIGGER() + " for processing. TL;DR: the trigger here on Twitch is \"!!\"");
                }else{
                    sendMessage(event, "The command trigger is \"" + CommandCenter.Companion.getTRIGGER() + "\"");
                }
            }
            Message messageWrapper = new Message(message, 0, event.getChannel().getId(), new User(event.getUser().getId(),
                    event.getUser().getDisplayName(), new Pair<>("permission", computePermission(event.getPermissions()))), true, this, host);
            List<ReplyMessage> messages = commandCenter.parseMessage(messageWrapper);

            if(messages != null && messages.size() != 0){
                for(ReplyMessage msg : messages){
                    if(msg == Constants.bStopMessage)
                        return;
                    if(msg == CommandCenter.Companion.getNO_MESSAGE()){
                        continue;
                    }
                    if(msg.getReplyIfPossible()){
                        replyTo(event, msg.getContent());
                    }else{
                        sendMessage(event, msg.getContent());
                    }
                }
            }else{
                if(CommandCenter.Companion.isCommand(message)){
                    sendMessage(event, Constants.INVALID_COMMAND + " (!!help)");
                }
            }
        }catch(IOException e){
            e.printStackTrace();
        }
    }


    public void sendMessage(AbstractChannelEvent event, String message){
        //This works for some reason.
        if(message.length() <= 500) {
            event.sendMessage(message);
        }else{
            for(int i = 0; i < message.length(); i += Math.min(400, message.length())){
                String part = message.substring(i, i + Math.min(400, message.length() - i));
                event.sendMessage(part);
                try{
                    Thread.sleep(5000);
                }catch(InterruptedException ignore){}
            }
        }
    }

    public void replyTo(ChannelMessageEvent event, String message){
        sendMessage(event, "@" + event.getUser().getDisplayName() + " " + message);
    }

    public String constructPing(ChannelMessageEvent event){
        return "@" + event.getUser().getDisplayName();
    }

    public int getIdForChannel(Channel channel){
        return (int) ((long)channel.getId());//cast to primitive before casting to int; a PITA
    }

    public boolean doesChannelExist(String uName){
        return client.getUserEndpoint().getUserIdByUserName(uName).isPresent();
    }

    public boolean isInChannel(String uName){
        for(Channel channel : mappedChannels){
            if(channel.getName().toLowerCase().equals(uName.toLowerCase()))
                return true;
        }
        return false;
    }

    public boolean joinChannel(String uName){
        if(isInChannel(uName)){
            return false;
        }

        if(!doesChannelExist(uName)){
            return false;
        }

        Optional<Long> id = client.getUserEndpoint().getUserIdByUserName(uName);
        if(!id.isPresent())
            return false;
        ChannelEndpoint endPoint = client.getChannelEndpoint(id.get());
        endPoint.registerEventListener();
        mappedChannels.add(endPoint.getChannel());
        return true;
    }

    public boolean leaveChannel(String uName){
        if(!isInChannel(uName))
            return false;

        Optional<Long> id = client.getUserEndpoint().getUserIdByUserName(uName);
        if(!id.isPresent())
            return false;
        ChannelEndpoint endpoint = client.getChannelEndpoint(id.get());
        try {
            endpoint.getTwitchClient().getMessageInterface().partChannel(endpoint.getChannel().getName());
        }catch(NullPointerException e){
            //Ignore
        }
        mappedChannels.removeIf(c -> c.getName().toLowerCase()
                .equals(uName.toLowerCase()));
        return true;
    }

    /**
     * Simplifies permissions into three strings; everyone, moderator and broadcaster. There are more permissions, but these
     * don't affect tbe functions of the bot itself, so they're ignored.
     * @param permissions A Set of CommandPermissions
     * @return one of the levels everyone, moderator or broadcaster
     */
    public String computePermission(Set<CommandPermission> permissions){
        String lowestLevel = "everyone";
        for(CommandPermission permission : permissions) {
            switch (permission) {
                case MODERATOR:
                    /**
                     * If the user isn't currently marked as a broadcaster, set the rank to moderator
                     */
                    if(!lowestLevel.toLowerCase().equals("broadcaster"))
                        lowestLevel = "moderator";
                    break;
                case BROADCASTER:
                    lowestLevel = "broadcaster";
                    break;
            }
        }

        return lowestLevel;
    }

    public void stop(){
        try {
            client.disconnect();
        }catch(NullPointerException e){
            //Thrown if one of the fields (most likely the pubSub connection) is null.
            //Catch it; it's fine.
        }

    }

    // @EventSubscriber
    //public void onFollow(FollowEvent event) {
    //    sendMessage(event,"Thanks for following " + event.getUser().getDisplayName() + " ^w^");
    //}

    public boolean getTruncated(){
        return truncated;
    }
    public List<CommandGroup> getCommandGroup(){
        return groups;
    }
    public SiteConfig getCredentialManager(){
        return credentialManager;
    }

    @Override
    public List<User> getUsersInServer(long server) {
        Chatter chatters = client.getTMIEndpoint().getChatters(client.getChannelEndpoint(server).getChannel().getName());
        List<String> usernames = new ArrayList<>();
        usernames.addAll(chatters.getViewers());
        usernames.addAll(chatters.getAdmins());
        usernames.addAll(chatters.getModerators());

        return usernames.stream().map((username)-> new User(client.getChannelEndpoint(username).getChannel().getId(), username)).collect(Collectors.toList());
    }

    @Override
    public Host getHost() {
        return null;
    }

    @Override
    public boolean editMessage(long messageId, String newContent){
        return false;
    }

    @Override
    public boolean deleteMessage(long messageId) {
        return false;
    }

}
