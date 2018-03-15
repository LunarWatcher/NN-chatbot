package io.github.lunarwatcher.chatbot;

import java.util.HashMap;
import java.util.Map;

public final class Constants {
    public static final boolean AUTO_BOOT = false;
    public static boolean LEAVE_ROOM_ON_UNHOME;
    public static final String DATE_FORMAT = "dd-MM-yyyy HH:mm:ss";
    public static final String IDENTIFIER_USERNAME = "name";
    public static final String IDENTIFIER_EMAIL = "email";
    public static final String IDENTIFIER_PASSWORD = "password";
    public static final String IDENTIFIER_ID = "id";
    public static final String DEFAULT_DATABASE = "memory.json";
    public static final String NO_HELP = "No help was supplied for this command";
    public static final String NO_DESCRIPTION = "No description was supplied for this command";
    public static final long SAVE_INTERVAL = 30000;
    public static final int RELOCATION_VOTES = 3;
    public static final String LEARNED_COMMANDS = "learned";
    public static final boolean DEFAULT_NSFW = true;
    public static final int DEFAULT_RANK = 1;
    public static final String WAVE_REGEX = "(^|\\s)(o/|\\\\o)(\\s|$)";
    public static final boolean AUTO_SAVE_WHEN_PUT = true;

    public static final String[] joinMessages = {
            "As you command, I'll leave for that room as soon as possible",
            "Joined",
            "You can't tell me what to do! Oh wait, you can? I guess I should get over there...",
            "Are there going to be quests there too? :D"
    };

    public static final String[] leaveMessages = {
            "Less to worry about I guess",
            "But I really liked it there!",
            "I wasn't finished with my quest yet! *walks away angry*",
            "Alright, left the room"
    };

    public static final String[] hrMessages = {
            "I can't *just* leave my home!",
            "You can't make me",
            "There are still quests to finish, I can't leave yet!",
            "Yeah... not doing that."
    };

    public static final String[] learnedMessages = {
            "The more I learn, the better",
            "Oh, THAT's how I do that command!",
            "I already knew that! Yeah... *coughs*"
    };

    public static final String[] forgotMessage = {
            "Forgotten",
            "I can't remember that one anymore"
    };

    public static final String[] killMessages = {
            "%s will no longer be a problem",
            "*shoots %s*. **HEADSHOT!**",
            "Unfortunately, %s killed me instead",
            "%s has been disposed of.",
            "%s has been crushed by a piano",
            "A pack of wild wolves ate %s",
            "%s committed suicide instead of `git commit`.",
            "%s? They were trampled to death at a football match",
            "Why did you kill %s? Who on meta is going to clean it up?",
            "Too little MLP watching killed %s",
            "%s was killed from not watching MLP",
            "Using Internet Explorer killed %s",
            "%s stepped on a landmine",
            "%s has been given a lethal injection.",
            "%s? Their computer blew up",
            "%s didn't watch their step and fell off a cliff",
            "%s traveled back in time and killed themselves",
            "https://imgs.xkcd.com/comics/dangers.png",
            "*Sending poisoned dinner to %s...*",
            "Dr. Who killed %s",
            "Sonic killed %s",
            "%s spontaneously combusted.",
            "%s? They went into the forest and never came back",
            "%s fell in front of a train",
            "%s was pushed off a bridge",
            "%s was involved in an airplane \"accident\"",
            "Ninjas surrounded %s one day... That is all.",
            "Poor %s. They fell into an endless pit",
            "Mutants killed %s",
            "%s died from licking something they shouldn't have licked"
    };

    public static final String[] lickMessages = {
            "*Licks %s.* Tastes like chicken!",
            "*Licks %s.* Tastes like a wet dog! *shivers*",
            "*Licks %s.* Tastes like pee",
            "*Licks %s.* Tastes like shit",
            "*Licks %s.* Tastes like pizza!",
            "*Licks %s.* Tastes like printer ink",
            "*Licks %s.* Tastes like someone forgot to shower",
            "*Licks %s.* Tastes like a cat!",
            "I'm not licking %s!",
            "*Licks %s.* *shivers*",
            "*Licks %s. Washes mouth with soap*",
            "*Licks %s. Dies from the awful taste*",
            "*Licks %s. Tongue gets stuck*"
    };

    public static String BANNED_USERS(String site){
        return "banned-users-" + site;
    }

    public static String ADMIN_USERS(String site){
        return "admin-users-" + site;
    }

    public static String PRIVILEGE_USERS(String site){
        return "privilege-users-" + site;
    }

    public static String HOME_ROOMS(String site){
        return "home-rooms-" + site;
    }

    public static String RANKS(String site){
        return "ranks-" + site;
    }

    public static class Ranks{
        public static Map<Integer, String> ranks;

        static {
            ranks = new HashMap<>();
            ranks.put(0, "Banned");
            ranks.put(1, "User");
            //TODO be creative here later
            ranks.put(10, "Owner");
        }

        public static String getRank(int level){
            return ranks.get(level);
        }
    }
}
