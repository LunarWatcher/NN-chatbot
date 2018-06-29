package io.github.lunarwatcher.chatbot;

import io.github.lunarwatcher.chatbot.bot.Bot;
import org.joda.time.Instant;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.List;

import static io.github.lunarwatcher.chatbot.Constants.*;
import static io.github.lunarwatcher.chatbot.utils.Utils.assertion;

/**
 * Changelog:
 *
 * <table>
 *     <tr><th>Version</th><th>Description</th></tr>
 *     <tr>
 *         <td>2</td>
 *         <td>Added field 'something'</td>
 *     </tr>
 *     <tr>
 *         <td>3</td>
 *         <td>Added field 'somethingElse'</td>
 *     </tr>
 * </table>
 */
public class BotCore {
    /**
     * The username the bot goes by. On some sites (like Discord) the actual username has a number after it
     * meaning the site specific username isn't the same as the username it actually goes by.
     */
    public static String GLOBAL_USERNAME;
    public static String LOCATION = "Undefined";
    public static final Instant STARTED_AT = Instant.now();
    public static Bot bot;
    public static void main(String[] args) throws IOException {
        LOCATION = Long.toHexString(System.currentTimeMillis());

        Properties botProps = new Properties();
        InputStream stream = new FileInputStream(new File("bot.properties"));
        botProps.load(stream);
        stream.close();
        Configurations.CREATOR = botProps.getProperty("about.creator");
        Configurations.GITHUB = botProps.getProperty("about.github");
        Configurations.REVISION = botProps.getProperty("about.revision");
        Configurations.CREATOR_GITHUB = botProps.getProperty("about.creatorGithub");
        Configurations.INSTANCE_LOCATION = botProps.getProperty("about.instanceLocation");
        Configurations.NEURAL_NET_IP = botProps.getProperty("bot.nnip");
        Properties credentials = new Properties();
        InputStream creds = new FileInputStream(new File("creds.properties"));
        credentials.load(creds);
        creds.close();
        List<Site> availableSites = new ArrayList<>();
        for(Map.Entry<Object, Object> entry : botProps.entrySet()){
            if(((String) entry.getKey()).startsWith("bot.site")){
                String name = ((String)entry.getKey()).replace("bot.site.", "");
                String url = (String) entry.getValue();
                String username = null, password = null, email = null;
                long userID = 0;

                for(Map.Entry<Object, Object> siteDet : credentials.entrySet()){
                    String identifier = ((String) siteDet.getKey()).replace(name + ".user.", "");
                    switch (identifier) {
                        case IDENTIFIER_EMAIL:
                            email = (String) siteDet.getValue();
                            break;
                        case IDENTIFIER_ID:
                            try {
                                userID = Long.parseLong(((String) siteDet.getValue()));
                            } catch (NumberFormatException e) {
                                if (name.toLowerCase().equals("twitch")) {
                                    userID = -1;
                                }
                            }
                            break;
                        case IDENTIFIER_PASSWORD:
                            password = (String) siteDet.getValue();
                            break;
                        case IDENTIFIER_USERNAME:
                            username = (String) siteDet.getValue();
                            break;
                    }
                }

                if(username == null || password == null || email == null){
                    System.out.println("Invalid config for site " + name + "!");
                }else {
                    System.out.println("Valid config for site " + name);
                    SiteConfig siteConfig = new SiteConfig(username.replace(" ", ""), password, email, userID, true);//TODO replace last argument with loaded data
                    availableSites.add(new Site(name, url, siteConfig));
                }
            }
        }
        GLOBAL_USERNAME = botProps.getProperty("bot.globalUsername");
        assertion(GLOBAL_USERNAME != null, "The global username cannot be null!");

        //Used to take input before the bot boots
        Scanner scanner = new Scanner(System.in);
        boolean quiet = false;
        boolean specificSite = false;

        List<Site> sites = new ArrayList<>();
        String input;
        String database = DEFAULT_DATABASE;

        String LROUH = (String) botProps.get("bot.home.leave");
        if(LROUH != null)
            Constants.LEAVE_ROOM_ON_UNHOME = Boolean.parseBoolean(LROUH);

        if(sites.size() == 0)
            sites = availableSites;

        for(Site s : sites){
            System.out.println(s.getUrl());
        }
        Path db = Paths.get(database);
        Database jsonDB = new Database(db);
        // jsonDB.commit();
        bot = new Bot(jsonDB, botProps, sites);
        bot.initialize();


        //Detect shutdown and save whatever is needed
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            //WARNING: This does not work when the IDE kills the process. It only works when the app is terminated
            //in a regular fashion (through System.exit or having no threads running)
            System.out.println("Shutdown detected. Saving...");
            bot.kill();
        }));

        while(true){
            String command = scanner.nextLine();
            if(eqR(command, "~!exit", "~!stop", "~!break", "~!kill", "~!end")){
                bot.kill();
                scanner.close();
                System.exit(1);
            }else if(eq(command, "~!save")){
                bot.save();
            } else{
                System.out.print("Unknown command: " + command);
            }
        }


    }
    public static boolean eq(String o, String t){
        return o.equals(t);
    }

    public static boolean eqR(String o, String... options){
        for(String opt : options){
            if(eq(o, opt))
                return true;
        }
        return false;
    }
}
