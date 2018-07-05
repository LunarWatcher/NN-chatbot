package io.github.lunarwatcher.chatbot.bot.commands.admin

import io.github.lunarwatcher.chatbot.bot.chat.Message
import io.github.lunarwatcher.chatbot.bot.chat.ReplyMessage
import io.github.lunarwatcher.chatbot.bot.commands.AbstractCommand
import io.github.lunarwatcher.chatbot.utils.Utils

class UnbanCommand : AbstractCommand("unban", listOf(), "Unbans a banned user. Only usable by hardcoded bot admins"){
    override fun handleCommand(message: Message): List<ReplyMessage>? {
        val site = message.chat
        val content = splitCommand(message.content)["content"] ?: return listOf(ReplyMessage("You have to tell me who to ban", true))
        val split = content.split(" ")
        if(split.size != 1)
            return listOf(ReplyMessage("Requires 1 arguments. found ${split.size}. (Pro tip: usernames are written without spaces)", true));
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

            val currentRank = Utils.getRank(uid, site.config)

            val cuRank = Utils.getRank(message.user.userID, site.config)

            if(cuRank < 6)
                return listOf(ReplyMessage("You need rank 6 or higher to unban people", true));

            if(currentRank != 0){
                return listOf(ReplyMessage("They aren't banned", true));
            }

            site.config.addRank(uid, 1);

            return listOf(ReplyMessage("Updated user status", true));
        }catch(e: IndexOutOfBoundsException){
            return listOf(ReplyMessage("Not enough arguments", true))
        }
    }
}