package io.github.lunarwatcher.chatbot.bot.commands

import io.github.lunarwatcher.chatbot.bot.chat.BMessage
import io.github.lunarwatcher.chatbot.bot.sites.se.SEChat
import io.github.lunarwatcher.chatbot.utils.Utils

class Summon(val votes: Int, val chat: SEChat) : AbstractCommand("summon", listOf("join"),
        "Summon the bot to a room", "Joins a room after $votes votes"){
    var vts: MutableMap<Int, MutableList<Long>> = mutableMapOf();

    override fun handleCommand(input: String, user: User): BMessage? {
        if(!matchesCommand(input)){
            return null;
        }
        var votes = this.votes;

        if(Utils.isAdmin(user.userID, chat.config))
            votes = 1;

        try{
            val raw = input.split(" ")[1];
            var iRoom: Int = raw.toInt();


            chat.rooms.filter { it.id == iRoom }
                    .forEach { return BMessage("I'm already in that room", true) }

            var users: MutableList<Long>? = vts.get(iRoom);

            if(users == null){
                vts.put(iRoom, mutableListOf(user.userID))
                users = vts.get(iRoom);

            }else{

                for(uid in users){
                    if(uid == user.userID){
                        return BMessage("Can't vote multiple times for joining :D", true);
                    }
                }
                users.add(user.userID);
                vts.put(iRoom,users);
            }

            if(users!!.size >= votes){
                var message: SEChat.BMWrapper = chat.joinRoom(iRoom);
                vts.remove(iRoom);

                if(!message.exception) {
                    return message;
                }else{

                    return BMessage(Utils.getRandomJoinMessage(), true)
                }

            }else{
                return BMessage((votes - users.size).toString() + " more " + (if(votes - users.size == 1 ) "vote" else "votes") + " required", true);
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

class UnSummon(val votes: Int, val chat: SEChat) : AbstractCommand("unsummon", listOf("leave", "gtfo"),
        "Makes the bot leave a specified room", "Leaves a room after $votes votes"){
    var vts: MutableMap<Int, MutableList<Long>> = mutableMapOf();

    override fun handleCommand(input: String, user: User): BMessage? {

        if(!matchesCommand(input)){
            return null;
        }
        var votes = this.votes;

        if(Utils.isAdmin(user.userID, chat.config))
            votes = 1;

        try{
            val raw = input.split(" ")
            val iRoom : Int = if(raw.size == 1)
                user.roomID
            else
                raw[1].toInt()

            if(chat.getRoom(iRoom) == null){
                return BMessage("I'm not in that room...", true);
            }

            if(Utils.isHome(iRoom, chat.config)){
                return BMessage(Utils.getRandomHRMessage(), true);
            }

            var users: MutableList<Long>? = vts.get(iRoom);

            if(users == null){
                vts.put(iRoom, mutableListOf(user.userID))
                users = vts.get(iRoom);

            }else{

                for(uid in users){
                    if(uid == user.userID){
                        return BMessage("Can't vote multiple times for leaving :D", true);
                    }
                }
                users.add(user.userID);
                vts.put(iRoom,users);
            }

            if(users!!.size >= votes){
                val succeeded = chat.leaveRoom(iRoom);

                vts.remove(iRoom);
                if(!succeeded){
                    return BMessage("Something happened when trying to leave", true);
                }else{
                    return BMessage(Utils.getRandomLeaveMessage(), true)
                }

            }else{
                return BMessage((votes - users.size).toString() + " more " + (if(votes - users.size == 1 ) "vote" else "votes") + " required", true);
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

