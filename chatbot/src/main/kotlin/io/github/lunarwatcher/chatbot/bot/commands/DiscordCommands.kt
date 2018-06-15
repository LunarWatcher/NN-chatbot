package io.github.lunarwatcher.chatbot.bot.commands

import io.github.lunarwatcher.chatbot.Configurations
import io.github.lunarwatcher.chatbot.bot.chat.BMessage
import io.github.lunarwatcher.chatbot.bot.command.CommandCenter.Companion.TRIGGER
import io.github.lunarwatcher.chatbot.bot.sites.discord.DiscordChat
import io.github.lunarwatcher.chatbot.utils.Utils

class NSFWState : AbstractCommand("nsfwtoggle", listOf(),
        "Changes the state of whether or not the bot is allowed to show NSFW content on the server",
        "`" + TRIGGER + "nsfwtoggle true` to enable and equivalently with false to disable"){

    override fun handleCommand(input: String, user: User): BMessage? {

        val chat: DiscordChat = if(user.chat is DiscordChat){
            user.chat as DiscordChat
        }else
            return BMessage("Invalid site. Blame ${Configurations.CREATOR}", true)

        if(!matchesCommand(input)){
            return null;
        }

        val arg = splitCommand(input);

        if(Utils.getRank(user.userID, chat.config) < 7){
            return BMessage("You have to be rank 7 or higher to do that", true);
        }

        if(arg.isEmpty())
            return BMessage("New state plz", true);

        val guild: Long = user.roomID;
        if(guild == -1L)
            return null;

        if(arg["--get"] != null)
            return BMessage("NSFW mode is " + (if(chat.getNsfw(guild)) "enabled" else "disabled"), false);
        else if(arg["--server"] != null){
            val newState = try{
                //Extremely basic check to assert it's possible to cast the argument to a boolean value
                arg["content"]?.toBoolean() ?: !chat.getNsfw(guild)
            }catch(e: ClassCastException){
                val raw = arg["content"]
                when (raw) {
                    in trues -> true
                    in falses -> false
                    else -> return BMessage("The new value has to be a boolean!", true)
                };
            }



            return BMessage("NSFW mode is now ${if(newState) "enabled" else "disabled"} for the entire server", false);
        }
        val newState = try{
            //Extremely basic check to assert it's possible to cast the argument to a boolean value
            arg["content"]?.toBoolean() ?: !chat.getNsfw(guild)
        }catch(e: ClassCastException){
            val raw = arg["content"]
            when (raw) {
                in trues -> true
                in falses -> false
                else -> return BMessage("The new value has to be a boolean!", true)
            };
        }

        return if(chat.getNsfw(guild) == newState){
            BMessage("The guild already has NSFW mode " + (if(newState) "enabled" else "disabled"), false);
        }else{
            chat.setNsfw(guild, newState);
            BMessage("Successfully changed NSFW mode", false);
        }
    }

    companion object {
        val trues = listOf("enabled", "on")
        val falses = listOf("disabled", "off")
    }
}

class DiscordSummon : AbstractCommand("summon", listOf("join")){
    override fun handleCommand(input: String, user: User): BMessage?{
        val site: DiscordChat = if(user.chat is DiscordChat){
            user.chat as DiscordChat
        }else
            return BMessage("Invalid site. Blame ${Configurations.CREATOR}", true)

        return BMessage("To make me join a server, use this URL: <https://discordapp.com/oauth2/authorize?client_id=${site.clientID}&scope=bot&permissions=0>", true)
    }
}