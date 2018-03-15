package io.github.lunarwatcher.chatbot;

import io.github.lunarwatcher.chatbot.bot.Bot;
import io.github.lunarwatcher.chatbot.bot.command.CommandCenter;
import io.github.lunarwatcher.chatbot.bot.commands.HelpCommand;
import io.github.lunarwatcher.chatbot.bot.commands.ShrugCommand;
import io.github.lunarwatcher.chatbot.bot.ui.UI;
import io.github.lunarwatcher.chatbot.utils.Utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.github.lunarwatcher.chatbot.Constants.*;
import static io.github.lunarwatcher.chatbot.utils.Utils.assertion;

public class BotCore {
    /**
     * The username the bot goes by. On some sites (like Discord) the actual username has a number after it
     * meaning the site specific username isn't the same as the username it actually goes by.
     */
    public static String GLOBAL_USERNAME;

    public BotCore() {

    }
    public static Bot bot;

    public static void test(){
        //Test stuff here

    }
    public static void main(String[] args) throws IOException /*Too lazy to create a try-catch*/{
        test();

        Properties botProps = new Properties();
        InputStream stream = new FileInputStream(new File("bot.properties"));
        botProps.load(stream);
        stream.close();

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
                    if(identifier.equals(IDENTIFIER_EMAIL)){
                        email = (String) siteDet.getValue();
                    }else if(identifier.equals(IDENTIFIER_ID)){
                        userID = Long.parseLong(((String) siteDet.getValue()));
                    }else if(identifier.equals(IDENTIFIER_PASSWORD)){
                        password = (String) siteDet.getValue();
                    }else if(identifier.equals(IDENTIFIER_USERNAME)){
                        username = (String) siteDet.getValue();
                    }
                }

                if(username == null || password == null || email == null){
                    System.out.println("Invalid config for site " + name + "!");
                }else {
                    System.out.println("Valid config for site " + name);
                    SiteConfig siteConfig = new SiteConfig(username, password, email, userID, true);//TODO replace last argument with loaded data
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

        dropPrep();
        boolean ti = true;
        while(ti && !Constants.AUTO_BOOT){
            System.out.print("Command: ");
            input = scanner.nextLine();
            if(input.equals("start") || input.equals("-b")){
                ti = false;

                break;
            }
            if(input.contains("quiet")){
                quiet = Utils.invertBoolean(quiet);
            }else if(input.contains("-s")){
                int size = sites.size();
                String site = input.replace("-s ", "");
                for(Site s : availableSites){
                    if(s.is(site)){
                        specificSite = true;
                        sites.add(s);
                    }
                }
                if(size == sites.size()){
                    System.err.println("Site not added, not listed in bot.properties");
                }
            }else if(input.contains("db")){
                if(input.contains("-reset")){
                    database = DEFAULT_DATABASE;
                }else {
                    if(input.replace("db ", "").contains(".json")) {
                        database = input.replace("db ", "");

                    }else{
                        System.out.println("The database has to be in .json format!");
                    }

                }
                System.out.println("Current database: " + database);

            }
        }

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

        UI.database = jsonDB;
        UI.bot = bot;
        UI.botProps = botProps;
//
        new UI(args);
        //the UI essentially blocks the thread. When the window is closed, the app should close too
        //So no arguments are to be checked here
    }

    public static void dropPrep(){
        System.out.println("###########################");
        System.out.println("Available commands:");
        System.out.println("quiet - toggles quiet boot");
        System.out.println("-s <url> - boot only in specific sites");
        System.out.println("start | -b - starts the bot");
        System.out.println("db - sets a custom database");
        System.out.println("    -reset - resets the database to the default one");
        System.out.println("###########################");
    }

    public class SaveThread extends Thread{
        private Database db;

        public SaveThread(Database d){
            this.db = d;
        }
        public void run(){
            if(db == null)
                return;

            while(true){
                try{
                    Thread.sleep(Constants.SAVE_INTERVAL);
                }catch(Exception e){

                }

                if(db == null)
                    break;
                db.commit();
            }
        }
    }

}
