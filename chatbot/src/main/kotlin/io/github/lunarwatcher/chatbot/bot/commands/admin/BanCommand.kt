package io.github.lunarwatcher.chatbot.bot.commands.admin

import io.github.lunarwatcher.chatbot.bot.chat.Message
import io.github.lunarwatcher.chatbot.bot.chat.ReplyMessage
import io.github.lunarwatcher.chatbot.bot.commands.AbstractCommand
import io.github.lunarwatcher.chatbot.utils.Utils

class BanCommand : AbstractCommand("ban", listOf(), "Bans a user from using the bot. Only usable by hardcoded bot admins"){
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

            if (Utils.isHardcodedAdmin(uid, site)) {
                return listOf(ReplyMessage("You can't ban other hardcoded moderators", true));
            }

            val currentRank = Utils.getRank(uid, site.config)
            val cuRank = Utils.getRank(message.user.userID, site.config)

            if (currentRank >= cuRank)
                return listOf(ReplyMessage("You can't change the rank of users with the same or higher rank as yourself", true));

            if (cuRank < 8) {
                return listOf(ReplyMessage("You can't ban or unban users with your current rank", true));
            }

            if (currentRank == 10)
                return listOf(ReplyMessage("You can't ban bot admins...", true));

            site.config.addRank(uid, 0);

            return listOf(ReplyMessage("Updated user status", true));
        }catch(e: IndexOutOfBoundsException){
            return listOf(ReplyMessage("Not enough arguments", true))
        }
    }
}