package io.github.lunarwatcher.chatbot.bot.commands

import io.github.lunarwatcher.chatbot.Constants
import io.github.lunarwatcher.chatbot.bot.chat.BMessage
import io.github.lunarwatcher.chatbot.bot.command.CommandCenter
import io.github.lunarwatcher.chatbot.bot.command.CommandCenter.Companion.TRIGGER
import io.github.lunarwatcher.chatbot.bot.sites.Chat
import io.github.lunarwatcher.chatbot.utils.Utils

class CheckCommand : AbstractCommand("getRank", listOf(), "Checks as user's role",
        "Supported roles: `admin`, `normal`, `privileged`(/`priv`) and `banned`. Unknown roles defaults to a normal check." +
                " Example usage: `" + TRIGGER + "check admin 6296561`"){

    override fun handleCommand(input: String, user: User): BMessage? {
        val site = user.chat
        if(!matchesCommand(input)){
            return null;
        }

        val content = splitCommand(input)["content"] ?: return BMessage("You have to tell me who to check the rank of, and to what", true)
        val split = content.split(" ")
        if(split.size != 1)
            return BMessage("Requires 1 argument. found ${split.size}. (Pro tip: usernames are written without spaces)", true);
        try {
            val username = split[0]
            var uid = username.toLongOrNull()

            if (uid == null) {
                val r = getRankOrMessage(username, site)
                if (r is BMessage)
                    return r;
                uid = r as Long
            }

            val rank: Int = Utils.getRank(uid, site.config)
            val final = if(username.toLongOrNull() == null) username else uid.toString()
            if (rank == 0)
                return BMessage((if(final.toLongOrNull() == null) final else "The user $final").toString() + " is ${Constants.Ranks.getRank(rank).toLowerCase()}", true)

            return BMessage((if(final.toLongOrNull() == null) final else "The user $final").toString() + "'s rank is ${Constants.Ranks.getRank(rank).toLowerCase()}", true)
        }catch(e: IndexOutOfBoundsException){
            return BMessage("Not enough arguments!", true)
        }
    }

}
