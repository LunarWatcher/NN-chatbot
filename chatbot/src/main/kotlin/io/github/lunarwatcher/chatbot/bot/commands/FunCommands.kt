package io.github.lunarwatcher.chatbot.bot.commands

import com.google.common.net.UrlEscapers
import io.github.lunarwatcher.chatbot.bot.chat.BMessage
import io.github.lunarwatcher.chatbot.bot.command.CommandCenter
import io.github.lunarwatcher.chatbot.bot.sites.Chat
import io.github.lunarwatcher.chatbot.utils.Utils
import kotlinx.coroutines.experimental.*
import java.net.URI
import java.net.URLEncoder
import java.util.*
import java.util.concurrent.TimeoutException
import javax.script.ScriptEngineManager

@Suppress("NAME_SHADOWING")
class RandomNumber : AbstractCommand("random", listOf("dice"), "Generates a random number"){
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
                    return BMessage("Too many arguments", true)
                }
            }
        }catch(e: NumberFormatException){
            return BMessage("Invalid number", true)
        }catch(e: ClassCastException){
            return BMessage("Invalid number", true)
        }catch(e: Exception){
            return BMessage("Something went terribly wrong", true);
        }
    }

    fun randomNumber(limit: Int, count: Int): String{
        val count = if(count > 200) 200 else count;
        val builder = StringBuilder()
        for(i in 0 until count){
            builder.append((if(i == count - 1) random.nextInt(limit) else random.nextInt(limit).toString() + ", "))
        }
        return builder.toString();
    }
}


const val GOOGLE_LINK = "https://www.google.com/search?q=";
const val DDG_LINK = "https://www.duckduckgo.com/?q=";

class LMGTFY : AbstractCommand("lmgtfy", listOf("searchfor", "google"), "Sends a link to Google in chat"){
    override fun handleCommand(input: String, user: User): BMessage? {
        if(!matchesCommand(input)){
            return null;
        }

        var query = splitCommand(input)["content"] ?: ""
        if(query.isEmpty())
            return BMessage("You have to supply a query", true);

        if(input.contains("--ddg") || input.contains("--duckduckgo")){
            query = query.replace("--ddg", "").replace("--duckduckgo", "").trim()
            if(query.isEmpty())
                return BMessage("You have to supply a query", true);

            return BMessage(DDG_LINK + URLEncoder.encode(query, "UTF-8"), true)
        }

        return BMessage(GOOGLE_LINK + URLEncoder.encode(query, "UTF-8"), true)
    }
}


class Kill : AbstractCommand("kill", listOf("assassinate"), "They must be disposed of!"){

    override fun handleCommand(input: String, user: User): BMessage? {
        val chat = user.chat
        if(!matchesCommand(input))
            return null;
        val split = splitCommand(input);

        if (split.size < 2 || !split.keys.contains("content")){
            return BMessage("You have to tell me who to dispose of", true);
        }
        val name: String = split["content"] ?: return BMessage("You have to tell me who to dispose of", true);

        if(chat.name == "discord"){
            if(name.toLowerCase().contains("<@!" + chat.site.config.userID + ">")){
                return BMessage("I'm not killing myself.", true);
            }
        }else{
            if(name.toLowerCase().contains(("@" + chat.site.config.username).toLowerCase())){
                return BMessage("I'm not killing myself", true);
            }
        }

        return BMessage(Utils.getRandomKillMessage(name), true);
    }
}

class Lick : AbstractCommand("lick", listOf(), "Licks someone. Or something"){
    override fun handleCommand(input: String, user: User): BMessage? {
        if(!matchesCommand(input))
            return null;
        val split = splitCommand(input);
        if (split.size < 2 || !split.keys.contains("content")){
            return BMessage("You have to tell me who to lick", true);
        }

        val name: String = split["content"] ?: return BMessage("You have to tell me who to lick", true);

        return BMessage(Utils.getRandomLickMessage(name), true);
    }
}

class Give : AbstractCommand("give", listOf(), "Gives someone something"){

    override fun handleCommand(input: String, user: User): BMessage? {
        if(!matchesCommand(input)){
            return null;
        }
        val inp = splitCommand(input);
        val split = inp["content"]?.split(" ", limit=2) ?: return BMessage("You have to tell me what to give and to who", false);
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
            content = "@$content";
        return BMessage("*pings $content*", false);
    }
}

class Blame : AbstractCommand("blame", listOf(), help="Someone must be blamed for this!"){
    private val random = Random();

    override fun handleCommand(input: String, user: User): BMessage? {
        val site: Chat = user.chat

        val problem = splitCommand(input)["content"]

        val blamable = site.config.ranks.values.map{it.username}.filter{it != null}
        if(blamable.isEmpty())
            return BMessage("I don't know!!", true)
        return if(problem == null)
            BMessage("It is ${blamable[random.nextInt(blamable.size)]}'s fault!", true);
        else BMessage("blames ${blamable[random.nextInt(blamable.size)]} for $problem", true)

    }
}

class WikiCommand : AbstractCommand("wiki", listOf(), desc="Links to Wikipedia", help="Gets a wikipedia page. Use `--lang \"languagecode\"` to link to a specific site", rankRequirement = 1){
    override fun handleCommand(input: String, user: User): BMessage? {
        val split = splitCommand(input)

        val lang = split["--lang"] ?: "en"
        val content = split["content"]?.replace(" ", "_") ?: return BMessage("https://www.google.com/teapot", true)
        val article = UrlEscapers.urlPathSegmentEscaper()
                .escape(content)
        return BMessage("https://$lang.wikipedia.org/wiki/" + URLEncoder.encode(article, "UTF-8"), false)
    }
}

class DefineCommand : AbstractCommand("define", listOf(), desc="Links the definition of a word", help="Do `${CommandCenter.TRIGGER}define word` to get the definition for a word", rankRequirement = 1){
    override fun handleCommand(input: String, user: User): BMessage?
            = BMessage("https://en.wiktionary.org/wiki/${URLEncoder.encode(splitCommand(input)["content"] ?:"invalid", "UTF-8")}",
            false)
}

class Appul : AbstractCommand("appul", listOf("apple"), "Apples."){
    override fun handleCommand(input: String, user: User): BMessage? {
        if(!matchesCommand(input)) return null;
        return BMessage("I LUV APPULS!", true);
    }
}