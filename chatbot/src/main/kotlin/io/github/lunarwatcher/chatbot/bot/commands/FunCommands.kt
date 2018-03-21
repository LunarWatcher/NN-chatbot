package io.github.lunarwatcher.chatbot.bot.commands

import io.github.lunarwatcher.chatbot.bot.chat.BMessage
import io.github.lunarwatcher.chatbot.bot.sites.Chat
import io.github.lunarwatcher.chatbot.utils.Utils
import io.github.lunarwatcher.chatbot.utils.Utils.random
import java.util.*

class RandomNumber() : AbstractCommand("random", listOf("dice"), "Generates a random number"){
    val random: Random = Random(System.currentTimeMillis());

    override fun handleCommand(input: String, user: User): BMessage? {
        if(!matchesCommand(input)){
            return null;
        }
        try {
            val split = input.split(" ");
            when {
                split.size == 1 -> return BMessage(randomNumber(6, 1), true)
                split.size == 2 -> return BMessage(randomNumber(split[1].toInt(), 1), true)
                split.size >= 3 -> return BMessage(randomNumber(split[1].toInt(), split[2].toInt()), true)
                else -> {
                }
            }
        }catch(e: Exception){
            return BMessage("Something went terribly wrong", true);
        }
        return BMessage("You shouldn't see this", true);
    }

    fun randomNumber(limit: Int, count: Int): String{
        val count = if(count > 500) 500 else count;
        val builder: StringBuilder = StringBuilder()
        for(i in 0 until count){
            builder.append((if(i == count - 1) random.nextInt(limit) else random.nextInt(limit).toString() + ", "))
        }
        return builder.toString();
    }
}


val GOOGLE_LINK = "https://www.google.com/search?q=";
class LMGTFY : AbstractCommand("lmgtfy", listOf("searchfor", "google"), "Sends a link to Google in chat"){
    override fun handleCommand(input: String, user: User): BMessage? {
        if(!matchesCommand(input)){
            return null;
        }

        var query = removeName(input).replaceFirst(" ", "");
        if(query.isEmpty())
            return BMessage("You have to supply a query", true);
        query = query.replace(" ", "+")

        return BMessage(GOOGLE_LINK + query, false)
    }
}

class Kill(val chat: Chat) : AbstractCommand("kill", listOf("assassinate"), "They must be disposed of!"){
    val random = Random();

    override fun handleCommand(input: String, user: User): BMessage? {
        if(!matchesCommand(input))
            return null;
        val split = splitCommand(input);
        var user: String? = null;
        if (split.size < 2){
            val list = mutableListOf<RankInfo>();
            list.addAll(chat.config.ranks.values);
            val count = list.count { it.username != null };

            if(count == 0)
                return BMessage("You have to tell me who to dispose of", true);

            do{
                val entry = list[random.nextInt(list.size)];

                if(entry.username != null){
                    user = "@" + entry.username;
                    break;
                }
            }while(entry.username == null);
        }else{
            user = split["content"];
        }

        if(chat.name == "discord"){
            if(user!!.toLowerCase().contains("<@!" + chat.site.config.userID + ">")){
                return BMessage("I'm not killing myself.", true);
            }
        }else{
            if(user!!.toLowerCase().contains(("@" + chat.site.config.username).toLowerCase())){
                return BMessage("I'm not killing myself", true);
            }
        }

        return BMessage("> " +Utils.getRandomKillMessage(user), true);
    }
}

class Lick(val chat: Chat) : AbstractCommand("lick", listOf(), "Licks someone. Or something"){
    override fun handleCommand(input: String, user: User): BMessage? {
        if(!matchesCommand(input))
            return null;
        val split = splitCommand(input);
        var user: String? = null;
        if (split.size < 2){
            val list = mutableListOf<RankInfo>();
            list.addAll(chat.config.ranks.values);
            val count = list.count { it.username != null };

            if(count == 0)
                return BMessage("You have to tell me who to lick", true);

            do{
                val entry = list[random.nextInt(list.size)];

                if(entry.username != null){
                    user = "@" + entry.username;
                    break;
                }
            }while(entry.username == null);
        }else{
            user = split["content"];
        }

        return BMessage("> " +Utils.getRandomLickMessage(user), true);
    }
}

class Give(val chat: Chat) : AbstractCommand("give", listOf(), "Gives someone something"){

    override fun handleCommand(input: String, user: User): BMessage? {
        if(!matchesCommand(input)){
            return null;
        }
        val inp = splitCommand(input);
        var split = inp["content"]?.split(" ", limit=2) ?: return BMessage("You have to tell me what to give and to who", false);
        if(split.size != 2)
            return BMessage("You have to tell me what to give and to who", true);

        val who = split[0].trim()
        val what = split[1].trim()
        return BMessage("*gives $what to $who*", false);

    }
}

class Ping : AbstractCommand("ping", listOf("poke"), "Pokes someone"){
    override fun handleCommand(input: String, user: User): BMessage? {
        if(!matchesCommand(input)) return null;

        val inp = splitCommand(input);
        var content = inp["content"]?.replace(" ", "") ?: return null;
        if(!content.startsWith("@"))
            content = "@" + content;
        return BMessage("*pings $content*", false);
    }
}

class Appul : AbstractCommand("appul", listOf("apple"), "Apples."){
    override fun handleCommand(input: String, user: User): BMessage? {
        if(!matchesCommand(input)) return null;
        return BMessage("I LUV APPULS!", true);
    }
}