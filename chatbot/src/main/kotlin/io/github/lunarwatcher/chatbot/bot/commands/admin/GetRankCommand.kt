package io.github.lunarwatcher.chatbot.bot.commands.admin

import io.github.lunarwatcher.chatbot.Constants
import io.github.lunarwatcher.chatbot.bot.chat.Message
import io.github.lunarwatcher.chatbot.bot.chat.ReplyMessage
import io.github.lunarwatcher.chatbot.bot.command.CommandCenter
import io.github.lunarwatcher.chatbot.bot.commands.AbstractCommand
import io.github.lunarwatcher.chatbot.utils.Utils

class GetRankCommand : AbstractCommand("getRank", listOf(), "Checks as user's role",
        "Supported roles: `admin`, `normal`, `privileged`(/`priv`) and `banned`. Unknown roles defaults to a normal check." +
                " Example usage: `" + CommandCenter.TRIGGER + "check admin 6296561`"){

    override fun handleCommand(message: Message): List<ReplyMessage>? {
        val site = message.chat

        val content = splitCommand(message.content)["content"] ?: return listOf(ReplyMessage("You have to tell me who to check the rank of, and to what", true))
        val split = content.split(" ")
        if(split.size != 1)
            return listOf(ReplyMessage("Requires 1 argument. found ${split.size}. (Pro tip: usernames are written without spaces)", true));
        try {
            val username = split[0]
            var uid = username.toLongOrNull()

            if (uid == null) {
                val r = site.getUidForUsernameInRoom(username, message.roomID);
                if (r.isEmpty())
                    return listOf(ReplyMessage("No users with the username `$username` found", true))
                else if(r.size > 1){
                    return listOf(ReplyMessage("More than one user with the username `$username` found. Please use the UID. (found: $r)", true));
                }
                uid = r[0]!!
            }

            val rank: Int = Utils.getRank(uid, site.config)
            val final = if(username.toLongOrNull() == null) username else uid.toString()
            if (rank == 0)
                return listOf(ReplyMessage((if (final.toLongOrNull() == null) final else "The user $final").toString() + " is ${Constants.Ranks.getRank(rank).toLowerCase()}", true))

            return listOf(ReplyMessage((if (final.toLongOrNull() == null) final else "The user $final").toString() + "'s rank is ${Constants.Ranks.getRank(rank).toLowerCase()}", true))
        }catch(e: IndexOutOfBoundsException){
            return listOf(ReplyMessage("Not enough arguments!", true))
        }
    }

}