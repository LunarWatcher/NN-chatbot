package io.github.lunarwatcher.chatbot.bot.commands.stackexchange

import io.github.lunarwatcher.chatbot.Configurations
import io.github.lunarwatcher.chatbot.bot.chat.Message
import io.github.lunarwatcher.chatbot.bot.chat.ReplyMessage
import io.github.lunarwatcher.chatbot.bot.commands.AbstractCommand
import io.github.lunarwatcher.chatbot.bot.sites.se.SEChat
import io.github.lunarwatcher.chatbot.utils.Utils

class LeaveCommand(val votes: Int) : AbstractCommand("unsummon", listOf("leave", "gtfo"),
        "Makes the bot leave a specified room", "Leaves a room after $votes votes"){
    var vts: MutableMap<Int, MutableList<Long>> = mutableMapOf();

    override fun handleCommand(message: Message): List<ReplyMessage>? {
        val chat: SEChat = if(message.chat is SEChat){
            message.chat as SEChat
        }else
            return listOf(ReplyMessage("Invalid instance of Chat. BlameCommand ${Configurations.CREATOR}. Debug info: Found ${message.chat::class.java}", true))
        if(!matchesCommand(message.content)){
            return null;
        }
        var votes = this.votes;

        if(Utils.isAdmin(message.user.userID, chat.config))
            votes = 1;

        try{
            val raw = message.content.split(" ")
            val iRoom : Int = if(raw.size == 1)
                message.roomID.toInt()
            else
                raw[1].toInt()

            if(chat.getRoom(iRoom) == null){
                return listOf(ReplyMessage("I'm not in that room...", true));
            }

            if(Utils.isHome(iRoom, chat.config)){
                return listOf(ReplyMessage(Utils.getRandomHRMessage(), true));
            }

            var users: MutableList<Long>? = vts[iRoom];

            if(users == null){
                vts[iRoom] = mutableListOf(message.user.userID)
                users = vts[iRoom];

            }else{

                for(uid in users){
                    if(uid == message.user.userID){
                        return listOf(ReplyMessage("Can't vote multiple times for leaving :D", true));
                    }
                }
                users.add(message.user.userID);
                vts[iRoom] = users;
            }

            return if(users!!.size >= votes){
                val succeeded = chat.leaveRoom(iRoom);

                vts.remove(iRoom);
                if(!succeeded){
                    listOf(ReplyMessage("Something happened when trying to leave", true));
                }else{
                    listOf(ReplyMessage(Utils.getRandomLeaveMessage(), true))
                }

            }else{
                listOf(ReplyMessage((votes - users.size).toString() + " more " + (if (votes - users.size == 1) "vote" else "votes") + " required", true));
            }

        }catch (e: IndexOutOfBoundsException){
            return listOf(ReplyMessage("You have to specify a room...", true));
        }catch(e: ClassCastException){
            return listOf(ReplyMessage("That's not a valid room ID", true));
        }catch(e: Exception){
            return listOf(ReplyMessage("Something bad happened :/", true));
        }
    }
}