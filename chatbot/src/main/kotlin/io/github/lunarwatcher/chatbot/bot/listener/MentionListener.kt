package io.github.lunarwatcher.chatbot.bot.listener

import io.github.lunarwatcher.chatbot.Configurations
import io.github.lunarwatcher.chatbot.Constants
import io.github.lunarwatcher.chatbot.bot.chat.BMessage
import io.github.lunarwatcher.chatbot.bot.command.CommandCenter
import io.github.lunarwatcher.chatbot.bot.command.CommandCenter.TRIGGER
import io.github.lunarwatcher.chatbot.bot.commands.User
import io.github.lunarwatcher.chatbot.bot.sites.Chat
import io.github.lunarwatcher.chatbot.bot.sites.se.SEChat
import io.github.lunarwatcher.chatbot.utils.Http
import jodd.jerry.Jerry
import org.apache.http.impl.client.HttpClients
import java.io.IOException
import java.net.SocketException

@Suppress("NAME_SHADOWING")
class MentionListener(val site: Chat) : AbstractListener("ping", "Reacts to pings") {
    var ignoreNext = false;
    val http: Http

    init{
        val httpClient = HttpClients.createDefault()
        http = Http(httpClient)
    }

    override fun handleInput(input: String, user: User): BMessage? {
        if(input.startsWith(CommandCenter.TRIGGER))
            return null;
        if(!isMentioned(input)){
            return null;
        }

        if (site is SEChat){
            try {
                //To avoid this stupidity: https://i.imgur.com/N8lZ4Q9.png
                //TODO fix - currently gives a 302 status code.
                if(site.mentionIds.size > 0) {
                    val ids = site.mentionIds.joinToString(",")
                    site.mentionIds.clear()
                    site.http.post("${site.site.url}/messages/ack", arrayOf("Content-Type", "text/plain"), "fkey", site.rooms.first { it.id == user.roomID }.fKey, "id", ids)

                }
            }catch(e: Exception){
                site.commands.crash.crash(e)
            }
        }

        if(ignoreNext){
            ignoreNext = false;
            return null;
        }
        val split = splitCommand(input)
        return if(split.keys.contains("content")){
            var message = split["content"] ?: ""
            message = message.clean()
            try{
                val response = http.post("http://127.0.0.1:" + Constants.FLASK_PORT + "/predict", "message", message)
                val reply: String = response.body.substring(1, response.body.length - 2)
                BMessage(reply, true)
            }catch (e: IOException){
                BMessage("How can I `${CommandCenter.TRIGGER}help`?", true)
            }catch(e: SocketException){
                //A VPN or something else is preventing you from connecting to localhost. Nothing much to do about it
                BMessage("How can I `${CommandCenter.TRIGGER}help`?", true)
            }
        }else
            getMessage(input) ?: BMessage("How can I " + TRIGGER + "help?", true);
    }

    /**
     * Commands or whatever you wanna call it that's handled through pings. Returns null
     * if there's no reply specified for the input
     */
    fun getMessage(input: String) : BMessage?{
        //Shadowing name by intent
        val input = input.toLowerCase();
        return when {
            input.contains("help") -> BMessage("You can see my commands by doing //help", false)
            //TODO add more stuff here
            else -> {
                null;
            }
        }
    }

    /**
     * TODO This is a TODO as this is critical for function if a new site is added. Please read:
     * This defaults to a `@Username` system. Should the site run a system similar to discord (@username is formatted
     * in the API as @1234 (userID instead of username) this has to be edited to add support for that site. Otherwise
     * it won't support it properly
     */
    fun isMentioned(input: String) : Boolean{
        return when(site.name){
            "discord" -> input.toLowerCase().startsWith("<@" + site.site.config.userID + ">".toLowerCase())
                    || input.toLowerCase().startsWith("<@!" + site.site.config.userID + ">".toLowerCase());
            "stackexchange" -> containsUsername(input);
            "stackoverflow" -> containsUsername(input);
            "metastackexchange" -> containsUsername(input);
            else ->{
                println("Else");
                input.contains("@" + site.site.config.username)
            };
        }
    }

    fun isMentionedStart(input: String) : Boolean{
        return when(site.name){
            "discord" -> input.toLowerCase().startsWith("<@" + site.site.config.userID + ">".toLowerCase())
                    || input.toLowerCase().startsWith("<@!" + site.site.config.userID + ">".toLowerCase());
            "stackexchange" -> containsUsername("^" + input.split(" ")[0]);//^ asserts start of the string in regex.
            "stackoverflow" -> containsUsername("^" + input.split(" ")[0]);//^ asserts start of the string in regex.
            "metastackexchange" -> containsUsername("^" + input.split(" ")[0]);//^ asserts start of the string in regex.
            else ->{
                println("Else");
                input.contains("@" + site.site.config.username)
            };
        }
    }


    fun containsUsername(input: String) : Boolean = (site.site.config.username.length downTo 3).any {
        input.toLowerCase().contains("${("@" + site.site.config.username.substring(0, it).toLowerCase())}\\b".toRegex())
    };

    fun ignoreNext(){
        ignoreNext = true;
    }
}

fun String.clean() : String{
    return this.replace("&quot;", "\"").replace("&#39;", "'")
            .replace("&gt;", ">").replace("&lt;", "<")
}