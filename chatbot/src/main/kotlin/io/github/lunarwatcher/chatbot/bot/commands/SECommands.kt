package io.github.lunarwatcher.chatbot.bot.commands

import io.github.lunarwatcher.chatbot.Configurations
import io.github.lunarwatcher.chatbot.bot.chat.BMessage
import io.github.lunarwatcher.chatbot.bot.exceptions.RoomNotFoundException
import io.github.lunarwatcher.chatbot.bot.sites.se.SEChat
import io.github.lunarwatcher.chatbot.utils.Utils

class Summon(val votes: Int) : AbstractCommand("summon", listOf("join"),
        "Summon the bot to a room", "Joins a room after $votes votes"){
    var vts: MutableMap<Int, MutableList<Long>> = mutableMapOf();

    override fun handleCommand(input: String, user: User): BMessage? {

        val chat: SEChat = if(user.chat is SEChat){
            user.chat as SEChat
        }else
            return BMessage("Invalid instance of Chat. Blame ${Configurations.CREATOR}. Debug info: Found ${user.chat::class.java}", true)
        if(!matchesCommand(input)){
            return null;
        }

        var votes = this.votes;

        if(Utils.isAdmin(user.userID, chat.config))
            votes = 1;

        try{
            val raw = input.split(" ")[1];
            val iRoom: Int = raw.toInt();

            if(CentralBlacklistStorage.getInstance(chat.database).isBlacklisted(chat.site.name, iRoom))
                return BMessage("Not gonna happen.", true);

            try {

                val response = chat.http.get("${chat.site.url}/rooms/$iRoom")
                if (response.statusCode == 404)
                    throw RoomNotFoundException("Room not found")
                if (!response.body.contains("<textarea id=\"input\">")) {
                    throw RoomNotFoundException("No write access in the room")
                }
            } catch (e: RoomNotFoundException) {
                return BMessage("An exception occured while checking the validity of the room: " + (e.message ?: "No message"), true);
            } catch (e: Exception) {
                chat.commands.crash.crash(e)
                return BMessage("An exception occured when trying to check the validity of the room", true)
            }

            chat.rooms.filter { it.id == iRoom }
                    .forEach { return BMessage("I'm already in that room", true) }

            var users: MutableList<Long>? = vts[iRoom];

            if(users == null){
                vts[iRoom] = mutableListOf(user.userID)
                users = vts[iRoom];

            }else{

                for(uid in users){
                    if(uid == user.userID){
                        return BMessage("Can't vote multiple times for joining :D", true);
                    }
                }
                users.add(user.userID);
                vts[iRoom] = users;
            }

            return if(users!!.size >= votes){
                val message= chat.joinRoom(iRoom);
                vts.remove(iRoom);

                message

            }else{
                BMessage((votes - users.size).toString() + " more " + (if(votes - users.size == 1 ) "vote" else "votes") + " required", true);
            }

        }catch (e: IndexOutOfBoundsException){
            return BMessage("You have to specify a room...", true);
        }catch(e: ClassCastException){
            return BMessage("That's not a valid room ID", true);
        }catch(e: Exception){
            chat.commands.crash.crash(e);
            return BMessage("Something bad happened :/", true);
        }
    }
}

class UnSummon(val votes: Int) : AbstractCommand("unsummon", listOf("leave", "gtfo"),
        "Makes the bot leave a specified room", "Leaves a room after $votes votes"){
    var vts: MutableMap<Int, MutableList<Long>> = mutableMapOf();

    override fun handleCommand(input: String, user: User): BMessage? {
        val chat: SEChat = if(user.chat is SEChat){
            user.chat as SEChat
        }else
            return BMessage("Invalid instance of Chat. Blame ${Configurations.CREATOR}. Debug info: Found ${user.chat::class.java}", true)
        if(!matchesCommand(input)){
            return null;
        }
        var votes = this.votes;

        if(Utils.isAdmin(user.userID, chat.config))
            votes = 1;

        try{
            val raw = input.split(" ")
            val iRoom : Int = if(raw.size == 1)
                user.roomID.toInt()
            else
                raw[1].toInt()

            if(chat.getRoom(iRoom) == null){
                return BMessage("I'm not in that room...", true);
            }

            if(Utils.isHome(iRoom, chat.config)){
                return BMessage(Utils.getRandomHRMessage(), true);
            }

            var users: MutableList<Long>? = vts[iRoom];

            if(users == null){
                vts[iRoom] = mutableListOf(user.userID)
                users = vts[iRoom];

            }else{

                for(uid in users){
                    if(uid == user.userID){
                        return BMessage("Can't vote multiple times for leaving :D", true);
                    }
                }
                users.add(user.userID);
                vts[iRoom] = users;
            }

            return if(users!!.size >= votes){
                val succeeded = chat.leaveRoom(iRoom);

                vts.remove(iRoom);
                if(!succeeded){
                    BMessage("Something happened when trying to leave", true);
                }else{
                    BMessage(Utils.getRandomLeaveMessage(), true)
                }

            }else{
                BMessage((votes - users.size).toString() + " more " + (if(votes - users.size == 1 ) "vote" else "votes") + " required", true);
            }

        }catch (e: IndexOutOfBoundsException){
            return BMessage("You have to specify a room...", true);
        }catch(e: ClassCastException){
            return BMessage("That's not a valid room ID", true);
        }catch(e: Exception){
            return BMessage("Something bad happened :/", true);
        }
    }
}

