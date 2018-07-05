package io.github.lunarwatcher.chatbot.bot.commands.admin

import io.github.lunarwatcher.chatbot.bot.chat.Message
import io.github.lunarwatcher.chatbot.bot.chat.ReplyMessage
import io.github.lunarwatcher.chatbot.bot.commands.AbstractCommand
import io.github.lunarwatcher.chatbot.utils.Utils

class SetRankCommand : AbstractCommand("setRank", listOf("demote", "promote"), "Changes a users rank."){

    override fun handleCommand(message: Message): List<ReplyMessage>? {
        val site = message.chat
        val command = splitCommand(message.content);
        val content = command["content"] ?: return listOf(ReplyMessage("You have to tell me who to change the rank of, and to what", true))
        val split = content.split(" ")
        if(split.size != 2)
            return listOf(ReplyMessage("Requires 2 arguments. found ${split.size}. (Pro tip: usernames are written without spaces)", true));
        try {
            val username = split[0]
            var uid = username.toLongOrNull()
            val newRank = split[1].toIntOrNull() ?: return listOf(ReplyMessage("Invalid rank: ${split[1]}", true))

            if (uid == null) {
                val r = site.getUidForUsernameInRoom(username, message.roomID);
                if (r.isEmpty())
                    return listOf(ReplyMessage("No users with the username `$username` found", true))
                else if(r.size > 1){
                    return listOf(ReplyMessage("More than one user with the username `$username` found. Please use the UID. (found: $r)", true));
                }
                uid = r[0]!!
            }

            if (newRank < 0 || newRank > 10)
                return listOf(ReplyMessage("That's not a valid rank within the range [0, 10]", true));


            val currentRank = Utils.getRank(uid, site.config)

            val cuRank = Utils.getRank(message.user.userID, site.config)

            if(cuRank == newRank){
                return listOf(ReplyMessage("The user already has the rank $newRank", true))
            }

            if(uid == message.user.userID){
                return if(newRank < currentRank){
                    if(newRank == 0)
                        ReplyMessage("You can't ban yourself", true)
                    if(command["--confirm"] != null){
                        site.config.addRank(uid, newRank)
                        return listOf(ReplyMessage("Successfully lowered your rank from $cuRank to $newRank", true))
                    }else{
                        return listOf(ReplyMessage("Warning: You're attempting to lower your own rank. If you do, an admin has to promote you if you want your previous rank. Add --confirm to the end of the message to confirm this action.", true))
                    }
                }else{
                    return listOf(ReplyMessage("Can't increase your own rank. Nice try though.", true))
                }
            }
            if ((newRank == 0 || currentRank == 0) && cuRank < 8) {
                return listOf(ReplyMessage("You can't ban or unban users with your current rank", true));
            }
            if (currentRank >= cuRank && !Utils.isHardcodedAdmin(message.user.userID, site))
                return listOf(ReplyMessage("You can't change the rank of users with the same or higher rank as yourself", true));

            if (newRank >= cuRank && cuRank != 10)
                return listOf(ReplyMessage("You can't promote other users to the same or higher rank as yourself", true))



            if (cuRank < 6)
                return listOf(ReplyMessage("You can't change ranks with your current rank", true));

            site.config.addRank(uid, newRank);
            return listOf(ReplyMessage("Rank for the user has successfully been updated", true));
        }catch(e: IndexOutOfBoundsException){
            return listOf(ReplyMessage("Not enough arguments", true))
        }
    }
}