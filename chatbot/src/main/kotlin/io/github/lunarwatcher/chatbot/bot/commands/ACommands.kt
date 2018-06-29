package io.github.lunarwatcher.chatbot.bot.commands

import io.github.lunarwatcher.chatbot.*
import io.github.lunarwatcher.chatbot.bot.ReplyBuilder
import io.github.lunarwatcher.chatbot.bot.chat.BMessage
import io.github.lunarwatcher.chatbot.bot.command.CommandCenter
import io.github.lunarwatcher.chatbot.bot.sites.Chat
import io.github.lunarwatcher.chatbot.bot.sites.discord.DiscordChat
import io.github.lunarwatcher.chatbot.bot.sites.se.SEChat
import io.github.lunarwatcher.chatbot.utils.Utils
import org.slf4j.LoggerFactory
import sun.security.krb5.Config
import java.io.IOException

class AddHome : AbstractCommand("home", listOf(),
        "Adds a home room - Admins only", "Adds a home room for the bot on this site"){
    override fun handleCommand(input: String, user: User): BMessage? {
        val site: SEChat = if(user.chat is SEChat){
            user.chat as SEChat
        }else
            return BMessage("Invalid instance of Chat. Blame ${Configurations.CREATOR}. Debug info: Found ${user.chat::class.java}", true)
        ;
        if(!Utils.isAdmin(user.userID, site.config)){
            return BMessage("I'm afraid I can't let you do that, User", true);
        }

        val raw = input.split(" ");
        val iRoom = try {
            if (raw.size == 1)
                user.roomID.toInt()
            else
                raw[1].toInt()
        }catch(e: NumberFormatException){
            return BMessage("Not a valid room ID!", true);
        }catch (e: ClassCastException){
            return BMessage("Not a valid room ID!", true);
        }

        val added = site.config.addHomeRoom(iRoom.toLong());

        if(!added){
            return BMessage("Room was not added as a home room", true);
        }else{
            site.joinRoom(iRoom);
        }
        return BMessage("Room added as a home room", true);
    }
}

class RemoveHome : AbstractCommand("remhome", listOf(),
        "Removes a home room - Admins only", "Removes a home room for the bot on this site"){
    override fun handleCommand(input: String, user: User): BMessage? {
        val site: SEChat = if(user.chat is SEChat){
            user.chat as SEChat
        }else
            return BMessage("Invalid instance of Chat. Blame ${Configurations.CREATOR}. Debug info: Found ${user.chat::class.java}", true)
        ;
        if(!Utils.isAdmin(user.userID, site.config)){
            return BMessage("I'm afraid I can't let you do that, User", true);
        }

        val raw = input.split(" ");
        val iRoom = try {
            if (raw.size == 1)
                user.roomID.toInt()
            else
                raw[1].toInt()
        }catch(e: NumberFormatException){
            return BMessage("Not a valid room ID!", true);
        }catch (e: ClassCastException){
            return BMessage("Not a valid room ID!", true);
        }

        if(Utils.isHardcodedRoom(iRoom, site)){
            return BMessage("Unfortunately for you, that's a hard-coded room. These cannot be removed by command. " +
                    "They are listed in bot.properties if you want to remove it. Please note that if there are no rooms supplied, " +
                    "it defaults to one even if it's empty", true);
        }

        val added = site.config.removeHomeRoom(iRoom.toLong());

        if(!added){
            return BMessage("Room was not removed as a home room", true);
        }

        if(Constants.LEAVE_ROOM_ON_UNHOME){
            val bmwrap = site.leaveRoom(iRoom);
            return if(!bmwrap){
                BMessage("Room was removed as a home room, but I could not leave the room!", true)
            }else{
                BMessage("Room successfully removed as a home room and left", true);
            }

        }
        return BMessage("Room removed as a home room", true);
    }
}

class UpdateRank : AbstractCommand("setRank", listOf("demote", "promote"), "Changes a users rank."){

    override fun handleCommand(input: String, user: User): BMessage? {
        val site = user.chat
        val command = splitCommand(input);
        val content = command["content"] ?: return BMessage("You have to tell me who to change the rank of, and to what", true)
        val split = content.split(" ")
        if(split.size != 2)
            return BMessage("Requires 2 arguments. found ${split.size}. (Pro tip: usernames are written without spaces)", true);
        try {
            val username = split[0]
            var uid = username.toLongOrNull()
            val newRank = split[1].toIntOrNull() ?: return BMessage("Invalid rank: ${split[1]}", true)

            if(uid == null){
                val r = getRankOrMessage(username, site)
                if (r is BMessage)
                    return r;
                uid = r as Long
            }

            if (newRank < 0 || newRank > 10)
                return BMessage("That's not a valid rank within the range [0, 10]", true);


            val currentRank = Utils.getRank(uid, site.config)

            val cuRank = Utils.getRank(user.userID, site.config)

            if(cuRank == newRank){
                return BMessage("The user already has the rank $newRank", true)
            }

            if(uid == user.userID){
                if(newRank < currentRank){
                    if(command["--confirm"] != null){
                        site.config.addRank(uid, newRank, user.userName)
                        return BMessage("Successfully lowered your rank from $cuRank to $newRank", true)
                    }else{
                        return BMessage("Warning: You're attempting to lower your own rank. If you do, an admin has to promote you if you want your previous rank. Add --confirm to the end of the message to confirm this action.", true)
                    }
                }else{
                    return BMessage("Can't increase your own rank. Nice try though.", true)
                }
            }
            if ((newRank == 0 || currentRank == 0) && cuRank < 8) {
                return BMessage("You can't ban or unban users with your current rank", true);
            }
            if (currentRank >= cuRank && !Utils.isHardcodedAdmin(user.userID, site))
                return BMessage("You can't change the rank of users with the same or higher rank as yourself", true);

            if (newRank >= cuRank && cuRank != 10)
                return BMessage("You can't promote other users to the same or higher rank as yourself", true)



            if (cuRank < 6)
                return BMessage("You can't change ranks with your current rank", true);

            site.config.addRank(uid, newRank, null);
            return BMessage("Rank for the user has successfully been updated", true);
        }catch(e: IndexOutOfBoundsException){
            return BMessage("Not enough arguments", true)
        }
    }
}

class BanUser : AbstractCommand("ban", listOf(), "Bans a user from using the bot. Only usable by hardcoded bot admins"){
    override fun handleCommand(input: String, user: User): BMessage? {
        val site = user.chat

        val content = splitCommand(input)["content"] ?: return BMessage("You have to tell me who to ban", true)
        val split = content.split(" ")
        if(split.size != 1)
            return BMessage("Requires 1 arguments. found ${split.size}. (Pro tip: usernames are written without spaces)", true);
        try {
            val username = split[0]
            var uid = username.toLongOrNull()

            if (uid == null) {
                val r = getRankOrMessage(username, site)
                if (r is BMessage)
                    return r;
                uid = r as Long
            }

            if (Utils.isHardcodedAdmin(uid, site)) {
                return BMessage("You can't ban other hardcoded moderators", true);
            }

            val currentRank = Utils.getRank(uid, site.config)
            val cuRank = Utils.getRank(user.userID, site.config)

            if (currentRank >= cuRank)
                return BMessage("You can't change the rank of users with the same or higher rank as yourself", true);

            if (cuRank < 8) {
                return BMessage("You can't ban or unban users with your current rank", true);
            }

            if (currentRank == 10)
                return BMessage("You can't ban bot admins...", true);

            site.config.addRank(uid, 0, null);

            return BMessage("Updated user status", true);
        }catch(e: IndexOutOfBoundsException){
            return BMessage("Not enough arguments", true)
        }
    }
}

class Unban : AbstractCommand("unban", listOf(), "Unbans a banned user. Only usable by hardcoded bot admins"){
    override fun handleCommand(input: String, user: User): BMessage? {
        val site = user.chat
        val content = splitCommand(input)["content"] ?: return BMessage("You have to tell me who to ban", true)
        val split = content.split(" ")
        if(split.size != 1)
            return BMessage("Requires 1 arguments. found ${split.size}. (Pro tip: usernames are written without spaces)", true);
        try {
            val username = split[0]
            var uid = username.toLongOrNull()

            if (uid == null) {
                val r = getRankOrMessage(username, site)
                if (r is BMessage)
                    return r;
                uid = r as Long
            }

            if (Utils.isHardcodedAdmin(uid, site)) {
                return BMessage("You can't ban other hardcoded moderators", true);
            }

            val currentRank = Utils.getRank(uid, site.config)

            val cuRank = Utils.getRank(user.userID, site.config)

            if (currentRank >= cuRank)
                return BMessage("You can't ban users with the same or higher rank as yourself", true);

            if (cuRank < 8) {
                return BMessage("You can't ban or unban users with your current rank", true);
            }

            if (currentRank == 10)
                return BMessage("You can't ban bot admins...", true);

            site.config.addRank(uid, 1, null);

            return BMessage("Updated user status", true);
        }catch(e: IndexOutOfBoundsException){
            return BMessage("Not enough arguments", true)
        }
    }
}

class SaveCommand : AbstractCommand("save", listOf(), "Saves the database"){
    override fun handleCommand(input: String, user: User): BMessage? {
        val site = user.chat
        ;
        if(Utils.getRank(user.userID, site.config) < 8)
            return BMessage("I'm afraid I can't let you do that, User", true);


        CommandCenter.bot.save();

        return BMessage("Saved.", true);
    }
}

class WhoMade : AbstractCommand("whoMade", listOf("creatorof"), "Gets the user ID of the user who created a command"){
    override fun handleCommand(input: String, user: User): BMessage? {
        ;
        try {
            val arg = splitCommand(input);

            println(arg)
            if(arg.size < 2)
                return BMessage("Missing arguments!", true);

            if(CommandCenter.INSTANCE.isBuiltIn(arg["content"], user.chat)){
                return BMessage("It's a built-in command, meaning it was made by the project developer(s)", true);
            }

            if(CommandCenter.tc.doesCommandExist(arg["content"] ?: return null)){
                CommandCenter.tc.commands
                        .forEach {
                            if(it.name == arg["content"])
                                return BMessage("The command `" + arg["content"] + "` was made by a user with the User ID "
                                        + grabLink(it.creator, it.site) + ". The command was created on " +
                                        (if(it.site == "Unknown") "an unknown site"
                                        else it.site) + ".", true)
                        }
            }
        }catch(e: ClassCastException){
            return BMessage(e.message, false);
        }

        return BMessage("That command doesn't appear to exist.", true);
    }

    fun grabLink(id: Long, site: String): String{
        return when(site){
            "discord" -> "<@$id>"
            "stackexchange" -> "[$id](https://stackexchange.com/users/$id)"
            "stackoverflow" -> "[$id](https://stackoverflow.com/users/$id)"
            "metastackexchange" -> "[$id](https://meta.stackexchange.com/users/$id)"
            else -> id.toString()
        }
    }
}

class DebugRanks : AbstractCommand("rankdebug", listOf(), "Debugs ranks", rankRequirement = 1){
    override fun handleCommand(input: String, user: User): BMessage? {
        val site = user.chat

        val reply = ReplyBuilder(site.name == "discord")
        reply.fixedInput().append("Username - user ID - rank").nl();
        for(rankInfo in site.config.ranks){
            val ri = rankInfo.value;
            reply.fixedInput().append(ri.username).append(" - ").append(ri.uid).append(" - ").append(ri.rank).nl()
        }

        return BMessage(reply.toString(), false);
    }
}

class KillBot : AbstractCommand("shutdown", listOf("gotosleep", "goaway", "sleep", "die"), "Shuts down the bot. Rank 10 only", rankRequirement = 10){
    val logger = LoggerFactory.getLogger(this::class.java)

    override fun handleCommand(input: String, user: User): BMessage? {
        val site = user.chat

        if(Utils.getRank(user.userID, site.config) < 10)
            return BMessage("I'm afraid I can't let you do that, User.", true)
        val content = splitCommand(input);
        val location: String? = content["--location"]
        val timehash: String? = content["--timehash"]

        val confirmed = input.contains("--confirm") && content["--confirm"] != null

        if(!confirmed){
            return BMessage("You sure 'bout that? Run the command with --confirm to shut me down", true)
        }
        if(location != null && timehash != null){
            if(location.contains(Configurations.INSTANCE_LOCATION) && timehash.contains(BotCore.LOCATION)){
                System.exit(0);
            }else{
                "Shutdown ignored: a different instance instance and timestamp was requested".info(logger)
                return null;
            }
        }else if(location != null){
            if(location.contains(Configurations.INSTANCE_LOCATION)){
                System.exit(0);
            }else{
                "Shutdown ignored; a different instance was requested".info(logger);
                return null;
            }
        }else if(timehash != null){
            if(timehash.contains(BotCore.LOCATION)){
                System.exit(0);
            }else{
                "Shutdown ignored: a different timestamp was requested (this: ${BotCore.LOCATION}, found $location)".info(logger)
            }
        }else{
            System.exit(0);
        }
        return CommandCenter.NO_MESSAGE;
    }
}

fun getRankOrMessage(username: String, site: Chat): Any{
    val list = site.config.ranks.entries.filter{ (_, v)->
        v.username?.toLowerCase()?.trim()?.replace(" ", "") == username.toLowerCase()
    }.map{(_, v) -> v.uid}

    return when(list.size){
        0-> BMessage("You have to supply a valid user ID/indexed username", true);
        1-> list[0]
        else-> BMessage("Ambiguous username. Found ${list.size} users with that username. Please specify with the user ID. (found: ${list.joinToString(",")})", true)
    }
}

fun getUsername(type: String, site: Chat): String?{
    if(type.toLongOrNull() == null)
        return null
    val list = site.config.ranks.entries.filter{(_, v) -> v.uid == type.toLong()}.map {(_, v) -> v.username}
    return when(list.size){
        0-> null
        else-> list[0]
    }
}

class NPECommand : AbstractCommand("npe", listOf(), "Throws an NPE"){
    override fun handleCommand(input: String, user: User): BMessage?{
        val site = user.chat
        if(Utils.getRank(user.userID, site.config) < 10)
            return BMessage("Rank 10 only! This feature could potentially kill the bot, which is why rank 10 is required", true)
        throw NullPointerException("Manually requested exception from NPECommand")
    }
}

class RevisionCommand : AbstractCommand("rev", listOf("revision")){
    override fun handleCommand(input: String, user: User): BMessage? {
        return try{
            BMessage(getRevision() + " (@version ${Configurations.REVISION})", true)
        }catch(e: Exception){
            LogStorage.crash(e)
            BMessage("Unknown revision", true)
        }
    }
}

class NetIpCommand : AbstractCommand("ip", listOf(), rankRequirement = 10){
    override fun handleCommand(input: String, user: User): BMessage? {
        if(!canUserRun(user))
            return BMessage("I'm afraid I can't let you do that, User", true)
        val content = (splitCommand(input)["content"]?.trim()) ?: "You have to supply a new IP"
        println(content)
        if(!"""\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}""".toRegex().containsMatchIn(content) || !containsOutOfRange(content)){
            return BMessage("Invalid IP", true)
        }

        Configurations.NEURAL_NET_IP = content;
        return BMessage("NN IP set to $content", true)
    }

    private fun containsOutOfRange(input: String) : Boolean{
        try {
            val parts = input.split(".")
            for (part in parts) {
                val numP = part.toInt()
                if(numP < 0 || numP > 255) {
                    System.out.println(numP)//Not printed
                    return false;
                }
            }
        }catch(e: Exception) {
            e.printStackTrace()//Not printed
            return false
        }
        return true;
    }
}

class ForceIndex : AbstractCommand("force-index", listOf(), desc="Forces indexing of a user, server, or chat room.", rankRequirement = 7){
    override fun handleCommand(input: String, user: User): BMessage? {
        val chat = user.chat
        val content = splitCommand(input)["content"] ?: return BMessage("Index what?", true)

        return if(chat is DiscordChat){

            val uid = content.toLongOrNull()
            if(uid == null){
                when (content.toLowerCase()){
                    "server" -> {
                        val users = chat.getChannel(user.roomID)?.guild?.users ?: return BMessage("Indexing failed: channel or guild not found", true)
                        for(usr in users){
                            val id = usr.longID;
                            val name = usr.name;
                            CommandCenter.INSTANCE.hookupToRanks(id, name, chat);
                        }
                        BMessage("Successfully indexed guild", true);
                    }
                    "channel" -> {
                        val users = chat.getChannel(user.roomID)?.usersHere ?: return BMessage("Indexing failed: channel not found", true);
                        for(usr in users){
                            val id = usr.longID;
                            val name = usr.name;
                            CommandCenter.INSTANCE.hookupToRanks(id, name, chat);
                        }

                        BMessage("Successfully indexed channel", true)
                    }
                    else -> {
                        val users = chat.getChannel(user.roomID)?.guild?.users ?: return BMessage("Indexing failed: channel or guild not found", true)
                        val usr = users.firstOrNull {
                            it.name.toLowerCase().replace(" ", "") == content.toLowerCase().replace(" ", "")
                        } ?: return BMessage("User not found!", true)
                        CommandCenter.INSTANCE.hookupToRanks(usr.longID, usr.name, chat)
                        BMessage("Successfully indexed user " + usr.name, true)
                    }
                }
            }else{
                val users = chat.getChannel(user.roomID)?.guild?.users ?: return BMessage("Indexing failed: channel or guild not found", true)
                val usr = users.firstOrNull { it.longID == uid } ?: return BMessage("User not found!", true)
                val username = usr.name
                CommandCenter.INSTANCE.hookupToRanks(uid, username, chat)
                BMessage("Successfully indexed user " + usr.name, true)
            }
        }else if(chat is SEChat){
            BMessage("Not implemented yet", true);
        }else{
            BMessage("Unsupported site for force indexing", true)
        }
    }
}