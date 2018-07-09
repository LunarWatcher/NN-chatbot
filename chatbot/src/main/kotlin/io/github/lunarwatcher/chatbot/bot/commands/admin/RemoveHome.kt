package io.github.lunarwatcher.chatbot.bot.commands.admin

import io.github.lunarwatcher.chatbot.Configurations
import io.github.lunarwatcher.chatbot.Constants
import io.github.lunarwatcher.chatbot.bot.chat.Message
import io.github.lunarwatcher.chatbot.bot.chat.ReplyMessage
import io.github.lunarwatcher.chatbot.bot.commands.AbstractCommand
import io.github.lunarwatcher.chatbot.bot.sites.se.SEChat
import io.github.lunarwatcher.chatbot.utils.Utils

class RemoveHome : AbstractCommand("remhome", listOf(),
        "Removes a home room - Admins only", "Removes a home room for the bot on this site"){
    override fun handleCommand(message: Message): List<ReplyMessage>? {
        val site: SEChat = if(message.chat is SEChat){
            message.chat as SEChat
        }else
            return listOf(ReplyMessage("Invalid instance of Chat. BlameCommand ${Configurations.CREATOR}. Debug info: Found ${message.chat::class.java}", true))

        if(!Utils.isAdmin(message.user.userID, site.config)){
            return listOf(ReplyMessage("I'm afraid I can't let you do that, User", true));
        }

        val raw = message.content.split(" ");
        val iRoom = try {
            if (raw.size == 1)
                message.roomID.toInt()
            else
                raw[1].toInt()
        }catch(e: NumberFormatException){
            return listOf(ReplyMessage("Not a valid room ID!", true));
        }catch (e: ClassCastException){
            return listOf(ReplyMessage("Not a valid room ID!", true));
        }

        if(Utils.isHardcodedRoom(iRoom, site)){
            return listOf(ReplyMessage("Unfortunately for you, that's a hard-coded room. These cannot be removed by command. " +
                    "They are listed in bot.properties if you want to remove it. Please note that if there are no rooms supplied, " +
                    "it defaults to one even if it's empty", true));
        }

        val added = site.config.removeHomeRoom(iRoom.toLong());

        if(!added){
            return listOf(ReplyMessage("Room was not removed as a home room", true));
        }

        if(Constants.LEAVE_ROOM_ON_UNHOME){
            val bmwrap = site.leaveRoom(iRoom);
            return if(!bmwrap){
                listOf(ReplyMessage("Room was removed as a home room, but I could not leave the room!", true))
            }else{
                listOf(ReplyMessage("Room successfully removed as a home room and left", true));
            }

        }
        return listOf(ReplyMessage("Room removed as a home room", true));
    }
}