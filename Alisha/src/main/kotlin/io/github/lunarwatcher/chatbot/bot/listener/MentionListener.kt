package io.github.lunarwatcher.chatbot.bot.listener

import io.github.lunarwatcher.chatbot.bot.chat.BMessage
import io.github.lunarwatcher.chatbot.bot.command.CommandCenter.TRIGGER
import io.github.lunarwatcher.chatbot.bot.commands.User
import io.github.lunarwatcher.chatbot.bot.sites.Chat

class MentionListener(val site: Chat) : AbstractListener("ping", "Reacts to pings") {
    var ignoreNext = false;

    override fun handleInput(input: String, user: User): BMessage? {

        if(!isMentioned(input)){
            return null;
        }
        if(ignoreNext){
            ignoreNext = false;
            return null;
        }

        return getMessage(input) ?: BMessage("How can I " + TRIGGER + "help?", true);
    }

    /**
     * Commands or whatever you wanna call it that's handled through pings. Returns null
     * if there's no reply specified for the input
     */
    fun getMessage(input: String) : BMessage?{
        //Shadowing name by intent
        val input = input.toLowerCase();
        return when {
            input.contains("help") -> BMessage("Depends on what kind of help you need. I'm just a bot after all :>", false)
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
            "discord" -> input.toLowerCase().contains("<@!" + site.site.config.userID + ">".toLowerCase());
            "stackexchange" -> containsUsername(input);
            "stackoverflow" -> containsUsername(input);
            "metastackexchange" -> containsUsername(input);
            else ->{
                println("Else");
                input.contains("@" + site.site.config.username)
            };
        }
    }


    fun containsUsername(input: String) : Boolean = (site.site.config.username.length downTo 3).any { input.toLowerCase().contains(("@" + site.site.config.username.substring(0, it).toLowerCase())) };

    fun ignoreNext(){
        ignoreNext = true;
    }
}