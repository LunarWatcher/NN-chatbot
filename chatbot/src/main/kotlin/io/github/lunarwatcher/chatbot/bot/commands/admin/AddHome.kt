package io.github.lunarwatcher.chatbot.bot.commands.admin

import io.github.lunarwatcher.chatbot.Configurations
import io.github.lunarwatcher.chatbot.bot.chat.Message
import io.github.lunarwatcher.chatbot.bot.chat.ReplyMessage
import io.github.lunarwatcher.chatbot.bot.commands.AbstractCommand
import io.github.lunarwatcher.chatbot.bot.sites.se.SEChat
import io.github.lunarwatcher.chatbot.utils.Utils

class AddHome : AbstractCommand("home", listOf(),
        "Adds a home room - Admins only", "Adds a home room for the bot on this site"){
    override fun handleCommand(message: Message): ReplyMessage? {
        val site: SEChat = if(message.chat is SEChat){
            message.chat as SEChat
        }else
            return ReplyMessage("Invalid instance of Chat. BlameCommand ${Configurations.CREATOR}. Debug info: Found ${message.chat::class.java}", true)
        ;
        if(!Utils.isAdmin(message.user.userID, site.config)){
            return ReplyMessage("I'm afraid I can't let you do that, User", true);
        }

        val raw = message.content.split(" ");
        val iRoom = try {
            if (raw.size == 1)
                message.roomID.toInt()
            else
                raw[1].toInt()
        }catch(e: NumberFormatException){
            return ReplyMessage("Not a valid room ID!", true);
        }catch (e: ClassCastException){
            return ReplyMessage("Not a valid room ID!", true);
        }

        val added = site.config.addHomeRoom(iRoom.toLong());

        if(!added){
            return ReplyMessage("Room was not added as a home room", true);
        }else{
            site.joinRoom(iRoom);
        }
        return ReplyMessage("Room added as a home room", true);
    }
}