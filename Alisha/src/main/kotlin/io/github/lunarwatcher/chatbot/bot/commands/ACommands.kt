package io.github.lunarwatcher.chatbot.bot.commands

import io.github.lunarwatcher.chatbot.Constants
import io.github.lunarwatcher.chatbot.bot.Bot
import io.github.lunarwatcher.chatbot.bot.ReplyBuilder
import io.github.lunarwatcher.chatbot.bot.chat.BMessage
import io.github.lunarwatcher.chatbot.bot.command.CommandCenter
import io.github.lunarwatcher.chatbot.bot.command.CommandCenter.tc
import io.github.lunarwatcher.chatbot.bot.sites.Chat
import io.github.lunarwatcher.chatbot.bot.sites.se.SEChat
import io.github.lunarwatcher.chatbot.utils.Utils
import sun.java2d.cmm.kcms.CMM.checkStatus

class AddHome(val site: SEChat) : AbstractCommand("home", listOf(),
        "Adds a home room - Admins only", "Adds a home room for the bot on this site"){
    override fun handleCommand(input: String, user: User): BMessage? {
        if(!matchesCommand(input))
            return null;
        if(!Utils.isAdmin(user.userID, site.config)){
            return BMessage("I'm afraid I can't let you do that, User", true);
        }

        val raw = input.split(" ");
        var iRoom: Int;
        if(raw.size == 1)
            iRoom = user.roomID
        else
            iRoom = raw[1].toInt()

        val added = site.config.addHomeRoom(iRoom);

        if(!added){
            return BMessage("Room was not added as a home room", true);
        }else{
            site.joinRoom(iRoom);
        }
        return BMessage("Room added as a home room", true);
    }
}

class RemoveHome(val site: SEChat) : AbstractCommand("remhome", listOf(),
        "Removes a home room - Admins only", "Removes a home room for the bot on this site"){
    override fun handleCommand(input: String, user: User): BMessage? {
        if(!matchesCommand(input))
            return null;
        if(!Utils.isAdmin(user.userID, site.config)){
            return BMessage("I'm afraid I can't let you do that, User", true);
        }

        val raw = input.split(" ");
        var iRoom: Int;
        iRoom = if(raw.size == 1)
            user.roomID
        else
            raw[1].toInt()

        if(Utils.isHardcodedRoom(iRoom, site)){
            return BMessage("Unfortunately for you, that's a hard-coded room. These cannot be removed by command. " +
                    "They are listed in bot.properties if you want to remove it. Please note that if there are no rooms supplied, " +
                    "it defaults to one even if it's empty", true);
        }

        val added = site.config.removeHomeRoom(iRoom);

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

class UpdateRank(val site: Chat) : AbstractCommand("promote", listOf("demote"), "Changes a users rank."){

    override fun handleCommand(input: String, user: User): BMessage? {
        if(!matchesCommand(input)){
            //final command match assertion check
            return null;
        }

        val split = input.split(" ")
        if(split.size < 3)
            return BMessage("Missing parameters. Found " + split.size + ", expected 3", true);
        var updatingUser: Long
        val newRank: Int
        updatingUser = try {
            split[1].toLong();

        }catch(e: ClassCastException){
            return BMessage("You have to supply a valid user ID and new rank", true);
        }catch(e: NumberFormatException){
            val usr = site.config.ranks.entries.firstOrNull{it.value.username == split[1]}
            usr?.value?.uid ?: return BMessage("You have to supply a valid user ID/indexed username", true);

        }
        try {

            newRank = split[2].toInt();

        }catch(e: ClassCastException){
            return BMessage("You have to supply a valid new rank", true);
        }catch(e: NumberFormatException){
            return BMessage("You have to supply a valid new rank", true);
        }
        if(newRank < 0 || newRank > 10)
            return BMessage("That's not a valid rank within the range [0, 10]", true);

        val currentRank = Utils.getRank(updatingUser, site.config)

        val cuRank = Utils.getRank(user.userID, site.config)

        if(currentRank >= cuRank && !Utils.isHardcodedAdmin(user.userID, site))
            return BMessage("You can't change the rank of users with the same or higher rank as yourself", true);

        if(newRank >= cuRank && cuRank != 10)
            return BMessage("You can't promote other users to the same or higher rank as yourself", true)

        if((newRank == 0 || currentRank == 0) && cuRank < 8){
            return BMessage("You can't ban or unban users with your current rank", true);
        }

        if(cuRank < 6)
            return BMessage("You can't change ranks with your current rank", true);

        site.config.addRank(updatingUser, newRank, null);
        return BMessage("Rank for the user has successfully been updated", true);
    }
}

class BanUser(val site: Chat) : AbstractCommand("ban", listOf(), "Bans a user from using the bot. Only usable by hardcoded bot admins"){
    override fun handleCommand(input: String, user: User): BMessage? {
        if(!matchesCommand(input))
            return null;

        val type = input.replace(name + " ", "");
        if(type == input)
            return BMessage("Specify a user to ban -_-", true);

        if(type.isEmpty()){
            return BMessage("Specify a user to ban", true);
        }
        val iUser: Long
        try {
            iUser = type.split(" ")[0].toLong()

        }catch(e: ClassCastException){
            return BMessage("Not a valid user ID!", true);
        }

        if(Utils.isHardcodedAdmin(iUser, site)){
            return BMessage("You can't ban other hardcoded moderators", true);
        }



        val currentRank = Utils.getRank(iUser, site.config)

        val cuRank = Utils.getRank(user.userID, site.config)

        if(currentRank >= cuRank)
            return BMessage("You can't change the rank of users with the same or higher rank as yourself", true);

        if(cuRank < 8){
            return BMessage("You can't ban or unban users with your current rank", true);
        }

        if(currentRank == 10)
            return BMessage("You can't ban bot admins...", true);

        site.config.addRank(iUser, 0, null);

        return BMessage("Updated user status", true);
    }
}

class Unban(val site: Chat) : AbstractCommand("unban", listOf(), "Unbans a banned user. Only usable by hardcoded bot admins"){
    override fun handleCommand(input: String, user: User): BMessage? {
        if(!matchesCommand(input))
            return null;

        val type = input.replace(name + " ", "");
        if(type == input)
            return BMessage("Specify a user to unban -_-", true);

        if(type.isEmpty()){
            return BMessage("Specify a user to unban", true);
        }
        val iUser: Long
        try {
            iUser = type.split(" ")[0].toLong()

        }catch(e: ClassCastException){
            return BMessage("Not a valid user ID!", true);
        }

        if(Utils.isHardcodedAdmin(iUser, site)){
            return BMessage("You can't ban other hardcoded moderators", true);
        }

        val currentRank = Utils.getRank(iUser, site.config)

        val cuRank = Utils.getRank(user.userID, site.config)

        if(currentRank >= cuRank)
            return BMessage("You can't ban users with the same or higher rank as yourself", true);

        if(cuRank < 8){
            return BMessage("You can't ban or unban users with your current rank", true);
        }

        if(currentRank == 10)
            return BMessage("You can't ban bot admins...", true);

        site.config.addRank(iUser, 0, null);

        return BMessage("Updated user status", true);
    }
}

class SaveCommand(val site: Chat) : AbstractCommand("save", listOf(), "Saves the database"){
    override fun handleCommand(input: String, user: User): BMessage? {
        if(!matchesCommand(input))
            return null;
        if(Utils.getRank(user.userID, site.config) < 8)
            return BMessage("I'm afraid I can't let you do that, User", true);


        CommandCenter.bot.save();

        return BMessage("Saved.", true);
    }
}

class WhoMade(val commands: CommandCenter) : AbstractCommand("WhoMade", listOf(), "Gets the user ID of the user who created a command"){
    override fun handleCommand(input: String, user: User): BMessage? {
        if(!matchesCommand(input))
            return null;
        try {
            val arg = splitCommand(input);

            println(arg)
            if(arg.size < 2)
                return BMessage("Missing arguments!", true);

            if(commands.isBuiltIn(arg["content"])){
                return BMessage("It's a built-in command, meaning it was made by the project developer(s)", true);
            }

            if(CommandCenter.tc.doesCommandExist(arg["content"] ?: return null)){
                CommandCenter.tc.commands.entries
                        .forEach {
                            if(it.value.name == arg["content"])
                                return BMessage("The command `" + arg["content"] + "` was made by a user with the User ID "
                                        + grabLink(it.value.creator, it.value.site) + ". The command was created on " +
                                        (if(it.value.site == "Unknown") "an unknown site"
                                        else it.value.site) + ".", true)
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

class DebugRanks(val site: Chat) : AbstractCommand("rankdebug", listOf(), "Debugs ranks"){
    override fun handleCommand(input: String, user: User): BMessage? {
        if(!matchesCommand(input))
            return null;

        if(Utils.getRank(user.userID, site.config) < 8)
            return BMessage("You can't do that", true);

        val reply: ReplyBuilder = ReplyBuilder(site.name == "discord")
        reply.fixedInput().append("Username - user ID - rank").nl();
        for(rankInfo in site.config.ranks){
            val ri = rankInfo.value;
            reply.fixedInput().append(ri.username).append(" - ").append(ri.uid).append(" - ").append(ri.rank).nl()
        }

        return BMessage(reply.toString(), false);
    }
}