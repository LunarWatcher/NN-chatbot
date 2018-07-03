package io.github.lunarwatcher.chatbot.utils;

import io.github.lunarwatcher.chatbot.Constants;
import io.github.lunarwatcher.chatbot.Database;
import io.github.lunarwatcher.chatbot.RankInfo;
import io.github.lunarwatcher.chatbot.bot.sites.Chat;
import io.github.lunarwatcher.chatbot.bot.sites.se.SEChat;
import io.github.lunarwatcher.chatbot.bot.sites.twitch.TwitchChat;
import io.github.lunarwatcher.chatbot.data.BotConfig;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.util.DiscordException;
import sx.blah.discord.util.RequestBuffer;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Utils {
    private static final String fkeyHtmlRegex = "name=\"fkey\"\\s+(?>type=\"hidden\"\\s+)?value=\"([^\"]+)\"";
    /**
     * Regex for the fKey in HTML
     */

    private static final Pattern fkeyRegex = Pattern.compile(fkeyHtmlRegex);

    public static Random random;

    static {
        random = new Random(System.currentTimeMillis());
    }

    /**
     * Hidden constructor so this class can't be initialized
     */
    private Utils() {}

    /**
     * Inverts a boolean
     * @param input
     * @return
     */
    public static boolean invertBoolean(boolean input){
        return !input;
    }

    /**
     * Universal assertion technique that doesn't require the -enableAssertions flag
     * @param input Boolean check, throws exception if false
     * @param message Optional message
     */
    public static void assertion(boolean input, @Nullable String message){

        if(!input)
            throw new RuntimeException("Assertion failed! " + (message != null ? message : "No message provided"));
    }

    /**
     * Finds the fKey in a given input. It is used to handle requests to the API.
     * @param input the (most likely) HTML to find an fKey in
     * @return The fKey or null if it isn't found
     */
    public static String parseHtml(String input){
        Matcher m = fkeyRegex.matcher(input);
        return m.find() ? m.group(1) : null;
    }

    public static void sendMessage(IChannel channel, String message){
        RequestBuffer.request(() -> {
            try{
                channel.sendMessage(message);
            } catch (DiscordException e){
                System.err.println("Message could not be sent with error: ");
                e.printStackTrace();
            }catch(Exception e){
                e.printStackTrace();
            }
        });

    }

    public static String getRandomJoinMessage(){
        return Constants.joinMessages[random.nextInt(Constants.joinMessages.length)];
    }

    public static String getRandomLeaveMessage(){
        return Constants.leaveMessages[random.nextInt(Constants.leaveMessages.length)];
    }

    public static void saveConfig(BotConfig cf, Database db){
        Map<Long, RankInfo> ranks = cf.getRanks();
        List<Long> homes = cf.getHomes();

        String site = cf.getSite().getName();

        db.put(Constants.HOME_ROOMS(site), homes);

        List<Map<String, Object>> cRanks = new ArrayList<>();

        for(Map.Entry<Long, RankInfo> entry : ranks.entrySet()){
            Map<String, Object> m = new HashMap<>();
            m.put("uid", entry.getValue().getUid());
            m.put("rank", entry.getValue().getRank());
            cRanks.add(m);
        }
        db.put(Constants.RANKS(site), cRanks);
        db.commit();
    }

    @SuppressWarnings("unchecked")
    public static void loadConfig(BotConfig cf, Database db){
        String site = cf.getSite().getName();
        //Possible ClassCastException can occur from this
        try {
            List<Long> homes = (List<Long>) db.get(Constants.HOME_ROOMS(site));
            List<Map<String, Object>> dbRank = (List<Map<String, Object>>) db.get(Constants.RANKS(site));

            Map<Long, RankInfo> ranked = new HashMap<>();

            if(dbRank != null) {

                for (Map<String, Object> map : dbRank) {
                    long userID = 0;
                    int rank = 0;
                    String username = null;
                    for (Map.Entry<String, Object> entry : map.entrySet()) {

                        String key = entry.getKey();
                        Object value = entry.getValue();

                        if (key.equals("uid"))
                            userID = Long.parseLong(value.toString());
                        else if (key.equals("rank"))
                            rank = Integer.parseInt(value.toString());

                    }

                    ranked.put(userID, new RankInfo(userID, rank));
                }
            }else ranked = null;
            cf.set(homes, ranked);
        }catch(Exception e){
            e.printStackTrace();

        }
    }

    public static String getRandomKillMessage(String input){
        String unformatted = Constants.killMessages[random.nextInt(Constants.killMessages.length)];

        try{
            return String.format(unformatted, input);
        }catch(Exception e){
            return unformatted;
        }
    }

    public static String getRandomLickMessage(String input){
        String unformatted = Constants.lickMessages[random.nextInt(Constants.lickMessages.length)];

        try{
            return String.format(unformatted, input);
        }catch(Exception e){
            return unformatted;
        }
    }


    //Utility method for checking etc classes defined in BotConfig

    public static boolean isAdmin(long user, BotConfig conf){
        if(conf.getRanks().get(user) != null){
            //>= 7 is admin. Keeping this method even though there are differences how it's treated
            //because 7 is still more or less a universal admin. But for something that requires a level
            //8 admin or higher, that's a different scenario that requires a custom implementation
            return conf.getRanks().get(user).getRank() >= 7;
        }else{
            //Not found? defaults to 1, or a basic user. Hence they can't be banned here
            return false;
        }
    }

    public static boolean isBanned(long user, BotConfig conf){
        if(conf.getRanks().get(user) != null){
            //0 = banned
            return conf.getRanks().get(user).getRank() == 0;
        }else{
            //Not found? defaults to 1, or a basic user. Hence they can't be banned here
            return false;
        }
    }

    public static boolean isHome(int room, BotConfig conf){
        for(long u : conf.getHomes()){
            if(u == room){
                return true;
            }
        }
        return false;
    }

    public static String getRandomHRMessage(){
        return Constants.hrMessages[random.nextInt(Constants.hrMessages.length)];
    }

    public static boolean isHardcodedRoom(int room, SEChat site){
        for(int r : site.hardcodedRooms){
            if(r == room)
                return true;
        }
        return false;
    }

    public static boolean isHardcodedAdmin(long user, Chat chat){
        for(Long x : chat.getHardcodedAdmins()){
            if(x == user)
                return true;
        }

        return false;
    }

    public static void loadHardcodedAdmins(Chat c){

        for(Map.Entry<Object, Object> s : c.getBotProps().entrySet()){
            String key = (String) s.getKey();

            if(key.equals("bot."+ c.getName() + ".admin")){
                String[] admins = ((String) s.getValue()).split(",");
                if(c.getName().equals("twitch")){
                    if(c instanceof TwitchChat) {
                        for (String admin : admins) {
                            c.getHardcodedAdmins().add(((TwitchChat) c).getUID(admin));
                        }
                    }
                }else {


                    for (String admin : admins) {
                        try {
                            c.getHardcodedAdmins().add(Long.parseLong(admin));
                        } catch (ClassCastException e) {
                            System.err.println("The userID supplied could not be parsed as a number: " + admin);
                        }
                    }
                    break;
                }
            }
        }

        for(Long i : c.getHardcodedAdmins()){
            //assume the hard-coded admins can essentially be considered owners. Be careful with
            //adding users as admins in bot.properties as this will grant level 10 access to the bot
            c.getConfig().addRank(i, 10);
        }
    }

    @NotNull
    public static String createPing(String username){
        return "@" + (username.replace(" ", ""));
    }

    public static String getRandomLearnedMessage(){
        return Constants.learnedMessages[random.nextInt(Constants.learnedMessages.length)];
    }

    public static String getRandomForgottenMessage(){
        return Constants.forgotMessage[random.nextInt(Constants.forgotMessage.length)];
    }

    public static int getRank(long user, BotConfig config){
        if(user == config.getSite().getCredentialManager().getUserID())
            return 10;

        if(config.getRanks().get(user) != null){
            return config.getRanks().get(user).getRank();
        }else return Constants.DEFAULT_RANK;
    }

}
