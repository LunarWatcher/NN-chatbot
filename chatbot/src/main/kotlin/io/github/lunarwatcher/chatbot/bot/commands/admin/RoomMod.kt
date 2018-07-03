package io.github.lunarwatcher.chatbot.bot.commands.admin

import io.github.lunarwatcher.chatbot.Constants
import io.github.lunarwatcher.chatbot.bot.chat.Message
import io.github.lunarwatcher.chatbot.bot.chat.ReplyMessage
import io.github.lunarwatcher.chatbot.bot.commands.AbstractCommand
import io.github.lunarwatcher.chatbot.data.CentralBlacklistStorage
import io.github.lunarwatcher.chatbot.utils.Utils

class BlacklistRoom : AbstractCommand("ban-room", listOf(), "Blacklists a room"){
    override fun handleCommand(message: Message): ReplyMessage? {
        val site = message.chat
        val content = splitCommand(message.content)["content"] ?: return ReplyMessage("You have to tell me which room", true);
        val where = site.name
        val which = content.toIntOrNull() ?: return ReplyMessage("You have to tell me which room to blacklist", true)
        val rank = Utils.getRank(message.user.userID, site.config)

        if(rank < 8)
            return ReplyMessage("You need rank ${Constants.Ranks.getRank(rank)} (numeric: $rank) or higher to use this feature", true)

        val result = CentralBlacklistStorage.getInstance(site.database).blacklist(where, which)
        if(!result)
            return ReplyMessage("Room already blacklisted", true)
        site.leaveServer(which.toLong());
        return ReplyMessage("Room blacklisted", true);
    }
}

class UnblacklistRoom : AbstractCommand("unban-room", listOf(), "Removes the blacklisting of a room."){
    override fun handleCommand(message: Message): ReplyMessage? {
        val site = message.chat
        val content = splitCommand(message.content)["content"] ?: return ReplyMessage("You have to tell me which room", true);
        val where = site.name
        val which = content.toIntOrNull() ?: return ReplyMessage("You have to tell me which room to unblock", true)
        val rank = Utils.getRank(message.user.userID, site.config)

        if(rank < 8)
            return ReplyMessage("You need rank ${Constants.Ranks.getRank(rank)} (numeric: $rank) or higher to use this feature", true)

        val result = CentralBlacklistStorage.getInstance(site.database).unblacklist(where, which)
        if(!result)
            return ReplyMessage("The room isn't blacklisted", true)

        return ReplyMessage("Room unblocked", true);
    }
}