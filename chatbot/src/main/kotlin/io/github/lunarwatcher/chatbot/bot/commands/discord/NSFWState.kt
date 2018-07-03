package io.github.lunarwatcher.chatbot.bot.commands.discord

import io.github.lunarwatcher.chatbot.Configurations
import io.github.lunarwatcher.chatbot.bot.chat.Message
import io.github.lunarwatcher.chatbot.bot.chat.ReplyMessage
import io.github.lunarwatcher.chatbot.bot.command.CommandCenter
import io.github.lunarwatcher.chatbot.bot.commands.AbstractCommand
import io.github.lunarwatcher.chatbot.bot.sites.discord.DiscordChat
import io.github.lunarwatcher.chatbot.utils.Utils

class NSFWState : AbstractCommand("nsfwtoggle", listOf(),
        "Changes the state of whether or not the bot is allowed to show NSFW content on the server",
        "`" + CommandCenter.TRIGGER + "nsfwtoggle true` to enable and equivalently with false to disable"){

    override fun handleCommand(message: Message): ReplyMessage? {

        val chat: DiscordChat = if(message.chat is DiscordChat){
            message.chat as DiscordChat
        }else
            return ReplyMessage("Invalid site. BlameCommand ${Configurations.CREATOR}", true)

        val arg = splitCommand(message.content);

        if(Utils.getRank(message.user.userID, chat.config) < 7){
            return ReplyMessage("You have to be rank 7 or higher to do that", true);
        }

        if(arg.isEmpty())
            return ReplyMessage("New state plz", true);

        val guild: Long = message.roomID;
        if(guild == -1L)
            return null;

        if(arg["--get"] != null)
            return ReplyMessage("NSFW mode is " + (if (chat.getNsfw(guild)) "enabled" else "disabled"), false);
        else if(arg["--server"] != null){
            val newState = try{
                //Extremely basic check to assert it's possible to cast the argument to a boolean value
                arg["content"]?.toBoolean() ?: !chat.getNsfw(guild)
            }catch(e: ClassCastException){
                val raw = arg["content"]
                when (raw) {
                    in trues -> true
                    in falses -> false
                    else -> return ReplyMessage("The new value has to be a boolean!", true)
                };
            }



            return ReplyMessage("NSFW mode is now ${if (newState) "enabled" else "disabled"} for the entire server", false);
        }
        val newState = try{
            //Extremely basic check to assert it's possible to cast the argument to a boolean value
            arg["content"]?.toBoolean() ?: !chat.getNsfw(guild)
        }catch(e: ClassCastException){
            val raw = arg["content"]
            when (raw) {
                in trues -> true
                in falses -> false
                else -> return ReplyMessage("The new value has to be a boolean!", true)
            };
        }

        return if(chat.getNsfw(guild) == newState){
            ReplyMessage("The guild already has NSFW mode " + (if (newState) "enabled" else "disabled"), false);
        }else{
            chat.setNsfw(guild, newState);
            ReplyMessage("Successfully changed NSFW mode", false);
        }
    }

    companion object {
        val trues = listOf("enabled", "on")
        val falses = listOf("disabled", "off")
    }
}