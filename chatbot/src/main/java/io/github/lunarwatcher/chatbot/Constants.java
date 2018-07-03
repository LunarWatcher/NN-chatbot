package io.github.lunarwatcher.chatbot;

import io.github.lunarwatcher.chatbot.bot.chat.ReplyMessage;
import io.github.lunarwatcher.chatbot.bot.chat.Message;

import java.util.HashMap;
import java.util.Map;

public final class Constants {
    public static boolean LEAVE_ROOM_ON_UNHOME;
    public static final String DATE_FORMAT = "dd-MM-yyyy HH:mm:ss";
    public static final String IDENTIFIER_USERNAME = "name";
    public static final String IDENTIFIER_EMAIL = "email";
    public static final String IDENTIFIER_PASSWORD = "password";
    public static final String IDENTIFIER_ID = "id";
    public static final String DEFAULT_DATABASE = "memory.json";
    public static final String NO_HELP = "No help was supplied for this command";
    public static final String NO_DESCRIPTION = "No description was supplied for this command";
    public static final int RELOCATION_VOTES = 3;
    public static final String LEARNED_COMMANDS = "learned";
    public static final boolean DEFAULT_NSFW = true;
    public static final int DEFAULT_RANK = 1;
    public static final String WAVE_REGEX = "(^|\\s)(o/|\\\\o)(\\s|$)";
    public static final boolean AUTO_SAVE_WHEN_PUT = true;
    public static final String BANNED_REPLY = "Go away.";
    public static final int FLASK_PORT = 8213;
    public static final String INVALID_COMMAND = "Maybe you should consider looking up the manual.";
    public static final Message stopMessage = new Message("", 0, 0, new User(0, ""), false, null, null);
    public static final ReplyMessage bStopMessage = new ReplyMessage("", false);

    public static final String[] joinMessages = {
            "Joined",
    };

    public static final String[] leaveMessages = {
            "Less to worry about I guess",
            "Alright, I'm out!",
    };

    public static final String[] hrMessages = {
            "I can't leave a home room"
    };

    public static final String[] learnedMessages = {
            "Learned."
    };

    public static final String[] forgotMessage = {
            "Forgotten"
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
            "Mutants killed %s","%s didn't watch their step and fell off a cliff",
            "%s was beamed into space", "PIRATES! %s was ejected along with the rest of the crew",
            "%s died from licking something they shouldn't have licked", "It's all over the news today, %s was killed by an angry mob!",
            "Unfortunately, %s hasn't been heard from in a few days",
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
            "*Licks %s. Tongue gets stuck*",
            "*Licks %s.* *dies*",
            "*Licks %s. passes out*",
            "*Licks %s.* Tastes AWFUL! *goes back in time to avoid licking it in the first place*",
            "*Licks %s.* Tastes like coffee!",
            "*Licks %s.* Tastes like a cow!", "*Licks %s. Beams self into space*",
            "*Licks %s.* Tastes like chocolate!!"

    };

    public static String[] wakeMessages = {
            "*throws pebbles at %s's window*", "*throws cold water on %s*", "*cranks up the music to 11*. Oh, I'm sorry, did I wake you %s?",
            "*flips %s's bed...*", "*throws %s out the plane*"
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
            ranks.put(1, "1");
            ranks.put(2, "2");//TODO
            ranks.put(3, "3");
            ranks.put(4, "4");
            ranks.put(5, "5");
            ranks.put(6, "6");
            ranks.put(7, "7");
            ranks.put(8, "8");
            ranks.put(9, "9");
            ranks.put(10, "An owner");
        }

        public static String getRank(int level){
            return ranks.get(level);
        }
    }


}
