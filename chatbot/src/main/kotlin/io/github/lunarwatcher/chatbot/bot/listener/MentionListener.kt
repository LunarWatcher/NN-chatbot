package io.github.lunarwatcher.chatbot.bot.listener

import io.github.lunarwatcher.chatbot.Configurations
import io.github.lunarwatcher.chatbot.Constants
import io.github.lunarwatcher.chatbot.bot.chat.BMessage
import io.github.lunarwatcher.chatbot.bot.command.CommandCenter
import io.github.lunarwatcher.chatbot.bot.commands.User
import io.github.lunarwatcher.chatbot.bot.sites.Chat
import io.github.lunarwatcher.chatbot.bot.sites.se.SEChat
import io.github.lunarwatcher.chatbot.bot.sites.twitch.TwitchChat
import io.github.lunarwatcher.chatbot.clean
import io.github.lunarwatcher.chatbot.utils.Http
import org.apache.http.impl.client.HttpClients
import java.io.IOException
import java.net.SocketException

@Suppress("NAME_SHADOWING")
class MentionListener : AbstractListener("ping", "Reacts to pings") {
    var ignoreNext = false;
    val http: Http

    init{
        val httpClient = HttpClients.createDefault()
        http = Http(httpClient)
    }

    override fun handleInput(input: String, user: User): BMessage? {
        val site = user.chat
        if(input.startsWith(CommandCenter.TRIGGER))
            return null;

        if(!isMentioned(input, site)){
            return null;
        }
        if(ignoreNext){
            ignoreNext = false;
            return null;
        }

        val split = splitCommand(input)
        if(split.keys.contains("content")){
            var message = split["content"] ?: ""
            message = message.clean()
            try{
                val response = http.post("http://${Configurations.NEURAL_NET_IP ?: "127.0.0.1"}:" + Constants.FLASK_PORT + "/predict", "message", message)
                val reply: String = response.body.substring(1, response.body.length - 2)
                return BMessage(reply, true)
            }catch (e: IOException){

            }catch(e: SocketException){
                //A VPN or something else is preventing you from connecting to localhost. Nothing much to do about it

            }

            val res = site.commands.parseMessage(CommandCenter.TRIGGER + message, user, user.nsfwSite)
            if(res != null && res.isNotEmpty())
                return res[0]
        }

        return BMessage("How can I `${if(user.chat is TwitchChat) "!!" else CommandCenter.TRIGGER}help`?", true)

    }

    /**
     * TODO This is a TODO as this is critical for function if a new site is added. Please read:
     * This defaults to a `@Username` system. Should the site run a system similar to discord (@username is formatted
     * in the API as @1234 (userID instead of username) this has to be edited to add support for that site. Otherwise
     * it won't support it properly
     */
    fun isMentioned(input: String, site: Chat) : Boolean{
        return when(site.name){
            "discord" -> input.toLowerCase().startsWith("<@" + site.site.config.userID + ">".toLowerCase())
                    || input.toLowerCase().startsWith("<@!" + site.site.config.userID + ">".toLowerCase());
            "stackexchange" -> containsUsername(input, site);
            "stackoverflow" -> containsUsername(input, site);
            /**
             * Asserts a full match on MSE; experimental to see which is better on sites where the length of the username isn't fixed
             */
            "metastackexchange" -> input.toLowerCase().contains("@${site.site.config.username.toLowerCase()}\\W".toRegex())
            "twitch" -> containsUsername(input, site)
            else ->{
                println("WARNING: mention on unregistered site. Defaulting to \"@username\"");
                input.contains("@" + site.site.config.username)
            };
        }
    }

    fun isMentionedStart(input: String, site: Chat) : Boolean{
        return when(site.name){
            "discord" -> input.toLowerCase().startsWith("<@" + site.site.config.userID + ">".toLowerCase())
                    || input.toLowerCase().startsWith("<@!" + site.site.config.userID + ">".toLowerCase());
            "stackexchange" -> containsUsername("^" + input.split(" ")[0], site);//^ asserts start of the string in regex.
            "stackoverflow" -> containsUsername("^" + input.split(" ")[0], site);//^ asserts start of the string in regex.
            "metastackexchange" -> containsUsername("^" + input.split(" ")[0], site);//^ asserts start of the string in regex.
            "twitch" -> containsUsername("^" + input.split(" ")[0], site)
            else ->{
                println("WARNING: mention on unregistered site. Defaulting to \"@username\"");
                input.contains("@" + site.site.config.username)
            };
        }
    }


    private fun containsUsername(input: String, site: Chat) : Boolean = (site.site.config.username.length downTo 3).any {
        input.toLowerCase().contains("${("@" + site.site.config.username.substring(0, it).toLowerCase())}\\b".toRegex())
    };

    fun ignoreNext() {
        ignoreNext = true;
    }

    fun done(){
        ignoreNext = false;
    }
}
