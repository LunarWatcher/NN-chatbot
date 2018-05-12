package io.github.lunarwatcher.chatbot.bot.command;


import io.github.lunarwatcher.chatbot.*;
import io.github.lunarwatcher.chatbot.bot.Bot;
import io.github.lunarwatcher.chatbot.bot.chat.BMessage;
import io.github.lunarwatcher.chatbot.bot.commands.*;
import io.github.lunarwatcher.chatbot.bot.listener.*;
import io.github.lunarwatcher.chatbot.bot.sites.Chat;
import io.github.lunarwatcher.chatbot.bot.sites.discord.DiscordChat;
import io.github.lunarwatcher.chatbot.bot.sites.se.SEChat;
import lombok.Getter;
import lombok.val;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.HttpGet;

import java.io.IOException;
import java.util.*;

import static io.github.lunarwatcher.chatbot.Constants.RELOCATION_VOTES;

public class CommandCenter {
    public static String TRIGGER;

    @Getter
    public Map<CmdInfo, Command> commands;
    public Map<CmdInfo, Command> sfCommands;

    public List<Listener> listeners;
    //List<Listener> listeners;
    public Chat site;
    public static TaughtCommands tc;
    public static Bot bot;
    public Database db;
    private StatusListener statusListener;
    public CrashLogs crash;

    public CommandCenter(Properties botProps, boolean shrugAlt, boolean truncated, Chat site) {
        this.db = site.getDatabase();
        if (tc == null) {
            tc = new TaughtCommands(db);
        }
        this.site = site;
        TRIGGER = botProps.getProperty("bot.trigger");
        commands = new HashMap<>();
        sfCommands = new HashMap<>();

        LocationCommand location = new LocationCommand();
        Alive alive = new Alive();

        addCommand(new HelpCommand(this, truncated));
        addCommand(new ShrugCommand(shrugAlt ? "¯\\\\_(ツ)_/¯" : "¯\\_(ツ)_/¯"));
        addCommand(new AboutCommand());
        addCommand(new Learn(tc, this));
        addCommand(new UnLearn(tc, this));
        addCommand(new UpdateRank(site));
        addCommand(new CheckCommand(site));
        addCommand(new BanUser(site));
        addCommand(new Unban(site));
        addCommand(new SaveCommand(site));
        addCommand(alive);
        addCommand(new WhoMade(this));
        addCommand(new ChangeCommandStatus(this));
        addCommand(new RandomNumber());
        addCommand(new LMGTFY());
        addCommand(new UpdateRank(site));
        addCommand(new DebugRanks(site));
        addCommand(new Kill(site));
        addCommand(new Lick(site));
        addCommand(new Give(site));
        addCommand(new Ping());
        addCommand(new Appul());
        addCommand(new BasicPrintCommand("(╯°□°）╯︵ ┻━┻", "tableflip", new ArrayList<>(), "The tables have turned..."));
        addCommand(new BasicPrintCommand("┬─┬ ノ( ゜-゜ノ)", "unflip", new ArrayList<>(), "The tables have turned..."));
        addCommand(new TimeCommand());
        addCommand(new KillBot(site));
        addCommand(new NetStat(site));
        addCommand(location);
        crash = new CrashLogs(site);
        addCommand(crash);
        addCommand(new DogeCommand());
        addCommand(new RepeatCommand());
        addCommand(new Blame(site));
        addCommand(new WakeCommand());
        addCommand(new WhoIs(site));
        addCommand(new BlacklistRoom(site));
        addCommand(new UnblacklistRoom(site));
        addCommand(new TellCommand(site));
        addCommand(new NPECommand(site));
        addCommand(new RevisionCommand());
        addCommand(new NetIpCommand());
        addCommand(new GitHubCommand());
        addCommand(new WikiCommand());
        addCommand(new CatCommand());
        addCommand(new DogCommand());
        statusListener = new StatusListener(site, db);
        addCommand(new StatusCommand(statusListener, site));

        listeners = new ArrayList<>();
        listeners.add(new WaveListener());
        listeners.add(new TestListener());
        listeners.add(new MorningListener());
        listeners.add(new BasicListener(shrugAlt ? "¯\\\\_(ツ)_/¯" : "¯\\_(ツ)_/¯", "^\\/shrug$", "Discord-shrug", "Shrugs @ /shrug"));


        listeners.add(statusListener);
        /**
         * Pun not intended:
         */
        MentionListener ml = new MentionListener(site);
        listeners.add(new KnockKnock(ml));
        listeners.add(new Train(5));
        listeners.add(ml);


    }

    public void loadInterconnected(){
        addCommand(new JoinTwitch());
        addCommand(new LeaveTwitch());
    }

    public void loadSE() {
        if(site instanceof SEChat){
            addCommand(new Summon(RELOCATION_VOTES, (SEChat) site));
            addCommand(new UnSummon(RELOCATION_VOTES, (SEChat) site));
            addCommand(new AddHome((SEChat) site));
            addCommand(new RemoveHome((SEChat) site));
            addCommand(new SERooms((SEChat) site));
        }
    }

    public void loadDiscord() {

        if(site instanceof DiscordChat) {
            addCommand(new NSFWState((DiscordChat) site));
            addCommand(new DiscordSummon(((DiscordChat)site).clientID));
        }
    }

    /**
     * method used to load commands/listeners that are considered NSFW on some sites. This doesn't necessarily mean actually
     * NSFW, it basically means the commands that aren't wanted on sites like StackExchange. On most Discord servers
     * it's different so that's enabled by default.
     * <p>
     * NSFW is up to whoever forks this bot, the method could even be called from the constructor to automatically
     * load it. The general usage here is for anything that involves swearing and the real NSFW, as the SE network
     * doesn't exactly allow swearing (it'll get the bot banned when in the SE network).
     * <p>
     * Whether or not this needs to be used separately depends on whether or not what the bot says being NSFW
     * doesn't matter. If it can say anything on a given site without problems this method can be removed in general.
     * But since this also is meant to be used with the SE network, it isn't going to work
     * <p>
     * These are the hard-coded ones that are unwanted in the SE network and similar sites
     */
    public void loadNSFW() {

    }

    public List<BMessage> parseListener(String message, User user, boolean nsfw) throws IOException{
        if (message == null)
            return null;
        message = message.replace("&#8238;", "");
        message = message.replace("\u202E", "");
        message = message.trim();
        String om = message;
        List<BMessage> replies = new ArrayList<>();

        for(Listener l : listeners){
            BMessage x = l.handleInput(om, user);
            if(x != null){
                replies.add(x);
            }
        }

        if(replies.size() == 0)
            replies = null;

        return replies;
    }


    public List<BMessage> parseMessage(String message, User user, boolean nsfw) throws IOException {
        if (message == null)
            return null;
        message = message.replace("&#8238;", "");
        message = message.replace("\u202E", "");
        message = message.trim();
        message = message.replaceAll(" +", " ");
        message = KUtilsKt.cleanInput(message);


        String om = message;
        List<BMessage> replies = new ArrayList<>();
        try {
            if (isCommand(message)) {
                message = message.substring(TRIGGER.length());

                //Get rid of white space to avoid problems down the line
                message = message.trim();

                String name = message.split(" ")[0];

                Command c = get(name);
                if (c != null) {
                    BMessage x = c.handleCommand(message, user);
                    if (x != null) {
                        //There are still some commands that could return null here
                        replies.add(x);
                    }
                }

                LearnedCommand lc = tc.get(name);
                if (lc != null) {
                    //If the command is NSFW but the site doesn't allow it, don't handle the command
                    if (lc.getNsfw() && !nsfw)
                        System.out.println("command ignored");
                    else {

                        BMessage x = lc.handleCommand(message, user);
                        if (x != null)
                            replies.add(x);
                    }
                }
            }

            for (Listener l : listeners) {
                BMessage x = l.handleInput(om, user);
                if (x != null) {
                    replies.add(x);
                }
            }

            if (replies.size() == 0)
                replies = null;
        }catch(Exception e){
            crash.crash(e);
            if (replies == null)
                replies = new ArrayList<>();
            replies.add(new BMessage("Something bad happened while processing. Do `" + TRIGGER + "logs` to see the logs", true));
        }
        return replies;
    }

    public static boolean isCommand(String input){
        if(input.startsWith(TRIGGER)) {
            val stripped = input.substring(TRIGGER.length()).replace("\n", "");

            if (stripped.length() == 0 || stripped.trim().length() == 0)
                return false;
            return stripped.matches("(?!\\s+)(.*)");
        }
        return false;
    }

    public static String[] splitCommand(String input, String commandName){
        String[] retVal = new String[2];
        retVal[0] = commandName;
        retVal[1] = input.replace(commandName + " ", "");
        return retVal;
    }

    public void manualCommandInjection(Command c){
        if(c == null)
            return;

        addCommand(c);
    }

    public boolean isBuiltIn(String cmdName){
        if(cmdName == null)
            return false;
        return get(cmdName) != null;
    }

    public void save(){

        statusListener.save();
    }

    public static void saveTaught(){
        if(tc != null)
            tc.save();

    }

    public void addCommand(Command c){
        String name = c.getName();
        List<String> aliases = c.getAliases();
        commands.putIfAbsent(new CmdInfo(name, aliases), c);
    }

    public Command get(String key){
        return (Command) MapUtils.Companion.get(key, commands);
    }

    public Command get(CmdInfo key){
        return (Command) MapUtils.Companion.get(key, commands);
    }

    @SuppressWarnings("ConstantConditions")
    public void hookupToRanks(long user, String username){
        if(site.getConfig().getRank(user) == null){
            //This code exists in an attempt to map every. Single. User. who uses the bot or even talk around it
            //This will build up a fairly big database, but that's why there is (going to be) a purge method
            //for the database
            site.getConfig().addRank(user, Constants.DEFAULT_RANK, username);
        }else{
            if(site.getConfig().getRank(user).getUsername() == null
                    || !site.getConfig().getRank(user).getUsername().equals(username)){
                site.getConfig().addRank(user, site.getConfig().getRank(user).getRank(), username);
            }
        }
    }

}
