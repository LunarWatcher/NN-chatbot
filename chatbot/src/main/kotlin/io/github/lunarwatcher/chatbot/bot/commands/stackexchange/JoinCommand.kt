package io.github.lunarwatcher.chatbot.bot.commands.stackexchange

import io.github.lunarwatcher.chatbot.Configurations
import io.github.lunarwatcher.chatbot.bot.chat.Message
import io.github.lunarwatcher.chatbot.bot.chat.ReplyMessage
import io.github.lunarwatcher.chatbot.bot.commands.AbstractCommand
import io.github.lunarwatcher.chatbot.bot.exceptions.RoomNotFoundException
import io.github.lunarwatcher.chatbot.bot.sites.se.SEChat
import io.github.lunarwatcher.chatbot.data.CentralBlacklistStorage
import io.github.lunarwatcher.chatbot.utils.Utils

class JoinCommand(val votes: Int) : AbstractCommand("summon", listOf("join"),
        "JoinCommand the bot to a room", "Joins a room after $votes votes"){
    var vts: MutableMap<Int, MutableList<Long>> = mutableMapOf();

    override fun handleCommand(message: Message): ReplyMessage? {

        val chat: SEChat = if(message.chat is SEChat){
            message.chat as SEChat
        }else
            return ReplyMessage("Invalid instance of Chat. BlameCommand ${Configurations.CREATOR}. Debug info: Found ${message.chat::class.java}", true)

        var votes = this.votes;

        if(Utils.isAdmin(message.user.userID, chat.config))
            votes = 1;

        try{
            val raw = message.content.split(" ")[1];
            val iRoom: Int = raw.toInt();

            if(CentralBlacklistStorage.getInstance(chat.database).isBlacklisted(chat.name, iRoom))
                return ReplyMessage("Not gonna happen.", true);

            try {

                val response = chat.http.get("${chat.host.chatHost}/rooms/$iRoom")
                if (response.statusCode == 404)
                    throw RoomNotFoundException("Room not found")
                if (!response.body.contains("<textarea id=\"input\">")) {
                    throw RoomNotFoundException("No write access in the room")
                }
            } catch (e: RoomNotFoundException) {
                return ReplyMessage("An exception occured while checking the validity of the room: " + (e.message
                        ?: "No message"), true);
            } catch (e: Exception) {
                chat.commands.crash.crash(e)
                return ReplyMessage("An exception occured when trying to check the validity of the room", true)
            }

            chat.rooms.filter { it.id == iRoom }
                    .forEach { return ReplyMessage("I'm already in that room", true) }

            var users: MutableList<Long>? = vts[iRoom];

            if(users == null){
                vts[iRoom] = mutableListOf(message.user.userID)
                users = vts[iRoom];

            }else{

                for(uid in users){
                    if(uid == message.user.userID){
                        return ReplyMessage("Can't vote multiple times for joining :D", true);
                    }
                }
                users.add(message.user.userID);
                vts[iRoom] = users;
            }

            return if(users!!.size >= votes){
                val message= chat.joinRoom(iRoom);
                vts.remove(iRoom);

                message

            }else{
                ReplyMessage((votes - users.size).toString() + " more " + (if (votes - users.size == 1) "vote" else "votes") + " required", true);
            }

        }catch (e: IndexOutOfBoundsException){
            return ReplyMessage("You have to specify a room...", true);
        }catch(e: ClassCastException){
            return ReplyMessage("That's not a valid room ID", true);
        }catch(e: Exception){
            chat.commands.crash.crash(e);
            return ReplyMessage("Something bad happened :/", true);
        }
    }
}