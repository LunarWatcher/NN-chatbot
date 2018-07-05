package io.github.lunarwatcher.chatbot.bot.listener

import io.github.lunarwatcher.chatbot.Configurations
import io.github.lunarwatcher.chatbot.Constants
import io.github.lunarwatcher.chatbot.bot.chat.Message
import io.github.lunarwatcher.chatbot.bot.chat.ReplyMessage
import io.github.lunarwatcher.chatbot.bot.command.CommandCenter
import io.github.lunarwatcher.chatbot.bot.commands.basic.NetStat
import io.github.lunarwatcher.chatbot.bot.sites.Chat
import io.github.lunarwatcher.chatbot.bot.sites.twitch.TwitchChat
import io.github.lunarwatcher.chatbot.safeGet
import io.github.lunarwatcher.chatbot.utils.HttpHelper
import java.net.UnknownHostException

@Suppress("NAME_SHADOWING")
class MentionListener(val netStat: NetStat) : AbstractListener("ping", "Reacts to pings") {
    var ignoreNext = false;
    var lastCheck: Long = 0
    private var cookies = mutableMapOf<String, String>()

    override fun handleInput(message: Message): List<ReplyMessage>? {
        val site = message.chat

        if(!isMentioned(message.content, site)){
            return null;
        }
        if(ignoreNext){
            ignoreNext = false;
            return null
        }

        if(isMentionedFull(message.content, site)){
            //Improve performance on ping-only matches
        }else if(isMentionedStart(message.content, site)){
            val split = message.content.split(" ", limit = 2).safeGet(1)
            if(split != null) {
                if((netStat.alive || System.currentTimeMillis() - lastCheck > 10 * 1000)) {
                    lastCheck = System.currentTimeMillis()
                    try{
                        if(!netStat.alive)
                            netStat.checkForHostExistence()
                        try {
                            val response = HttpHelper.post("http://${Configurations.NEURAL_NET_IP
                                    ?: "127.0.0.1"}:" + Constants.FLASK_PORT + "/predict", cookies, "message", split)
                            val reply: String = response.body().substring(1, response.body().length - 2)
                            netStat.alive = true;
                            return listOf(ReplyMessage(reply, true));
                        } catch (e: Exception) {
                            netStat.alive = false
                        }
                    }catch(e: UnknownHostException){

                    }

                }

                val res = site.commands.handleCommands(message.prefixTriggerAndRemovePing())
                if (res.isNotEmpty())
                    return listOf(res[0])
            }
        }

        return listOf(ReplyMessage("How can I `${if (message.chat is TwitchChat) "!!" else CommandCenter.TRIGGER}help`?", true))

    }

    /**
     * TODO This is a TODO as this is critical for function if a new site is added. Please read:
     * This defaults to a `@Username` system. Should the site run a system similar to discord (@username is formatted
     * in the API as @1234 (userID instead of username) this has to be edited to add support for that site. Otherwise
     * it won't support it properly
     */
    fun isMentioned(input: String, site: Chat) : Boolean{
        return when(site.name){
            "discord" -> input.toLowerCase().startsWith("<@" + site.credentialManager.userID + ">".toLowerCase())
                    || input.toLowerCase().startsWith("<@!" + site.credentialManager.userID + ">".toLowerCase());
            "stackexchange" -> containsUsername(input, site);
            "stackoverflow" -> containsUsername(input, site);
            /**
             * Asserts a full match on MSE; experimental to see which is better on sites where the length of the username isn't fixed
             */
            "metastackexchange" -> input.toLowerCase().contains("@${site.credentialManager.username.toLowerCase()}\\W".toRegex())
            "twitch" -> containsUsername(input, site)
            else ->{
                println("WARNING: mention on unregistered site. Defaulting to \"@username\"");
                input.contains("@" + site.credentialManager.username)
            };
        }
    }

    fun isMentionedStart(input: String, site: Chat) : Boolean{
        return when(site.name){
            "discord" -> input.toLowerCase().startsWith("<@" + site.credentialManager.userID + ">".toLowerCase())
                    || input.toLowerCase().startsWith("<@!" + site.credentialManager.userID + ">".toLowerCase());
            "stackexchange" -> containsUsername("^" + input.split(" ")[0], site);//^ asserts start of the string in regex.
            "stackoverflow" -> containsUsername("^" + input.split(" ")[0], site);//^ asserts start of the string in regex.
            "metastackexchange" -> containsUsername("^" + input.split(" ")[0], site);//^ asserts start of the string in regex.
            "twitch" -> containsUsername("^" + input.split(" ")[0], site)
            else ->{
                println("WARNING: mention on unregistered site. Defaulting to \"@username\"");
                input.contains("@" + site.credentialManager.username)
            };
        }
    }

    fun isMentionedFull(input: String, site: Chat) : Boolean{
        return when(site.name){
            "discord" -> input.toLowerCase() == "<@" + site.credentialManager.userID + ">".toLowerCase()
                    || input.toLowerCase() == "<@!" + site.credentialManager.userID + ">".toLowerCase();
            "stackexchange", "stackoverflow",
            "metastackexchange", "twitch" ->
                input.toLowerCase() == site.credentialManager.username.toLowerCase();
            else ->{
                println("WARNING: mention on unregistered site. Defaulting to \"@username\"");
                input.toLowerCase() == "@" + site.credentialManager.username.toLowerCase()
            };
        }
    }

    private fun containsUsername(input: String, site: Chat) : Boolean = (site.credentialManager.username.length downTo 3).any {
        input.toLowerCase().contains("${("@" + site.credentialManager.username.substring(0, it).toLowerCase())}\\b".toRegex())
    };

    fun ignoreNext() {
        ignoreNext = true;
    }

    fun done(){
        ignoreNext = false;
    }
}
