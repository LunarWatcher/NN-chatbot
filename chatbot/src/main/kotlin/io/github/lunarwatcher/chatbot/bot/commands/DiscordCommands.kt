package io.github.lunarwatcher.chatbot.bot.commands

import io.github.lunarwatcher.chatbot.bot.chat.BMessage
import io.github.lunarwatcher.chatbot.bot.command.CommandCenter
import io.github.lunarwatcher.chatbot.bot.command.CommandCenter.TRIGGER
import io.github.lunarwatcher.chatbot.bot.sites.discord.DiscordChat
import io.github.lunarwatcher.chatbot.utils.Utils

class NSFWState(val chat: DiscordChat) : AbstractCommand("nsfwtoggle", listOf(),
        "Changes the state of whether or not the bot is allowed to show NSFW content on the server",
        "`" + TRIGGER + "nsfwtoggle true` to enable and equivalently with false to disable"){

    override fun handleCommand(input: String, user: User): BMessage? {
        if(!matchesCommand(input)){
            return null;
        }

        val arg = splitCommand(input);

        if(Utils.getRank(user.userID, chat.config) < 7){
            return BMessage("You have to be rank 7 or higher to do that", true);
        }

        if(arg.isEmpty())
            return null;

        try{
            //Extremely basic check to assert it's possible to cast the argument to a boolean value
            arg["content"]?.toBoolean();
        }catch(e: ClassCastException){
            return BMessage("The new value has to be a boolean!", true);
        }
        val guild: Long = chat.getAssosiatedGuild(user.roomID);
        if(guild == -1L)
            return BMessage("You fucked up somewhere", false);
        return if(chat.getNsfw(guild) == arg["content"]?.toBoolean()){
            BMessage("The guild already has NSFW mode " + (if(arg["content"]?.toBoolean() ?: return null) "enabled" else "disabled"), false);
        }else{
            chat.setNsfw(guild, arg["content"]?.toBoolean() ?: return null);
            BMessage("Successfully changed NSFW mode", false);
        }
    }
}

class DiscordSummon(var clientID: String) : AbstractCommand("summon", listOf("join")){
    override fun handleCommand(input: String, user: User): BMessage? = BMessage("To make me join a server, use this URL: https://discordapp.com/oauth2/authorize?client_id=$clientID&scope=bot&permissions=0", true)
}