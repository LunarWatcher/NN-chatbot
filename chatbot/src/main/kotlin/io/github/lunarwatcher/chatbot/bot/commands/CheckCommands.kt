package io.github.lunarwatcher.chatbot.bot.commands

import io.github.lunarwatcher.chatbot.bot.chat.BMessage
import io.github.lunarwatcher.chatbot.bot.command.CommandCenter.TRIGGER
import io.github.lunarwatcher.chatbot.bot.sites.Chat
import io.github.lunarwatcher.chatbot.utils.Utils
import org.tritonus.share.TCircularBuffer
import sun.java2d.cmm.kcms.CMM.checkStatus

val USER_BANNED = 0;
val USER_ADMIN = 1;
val USER_PRIV = 2;
val USER_NORMAL = 3;

class CheckCommand(var site: Chat) : AbstractCommand("getRank", listOf(), "Checks as user's role",
        "Supported roles: `admin`, `normal`, `privileged`(/`priv`) and `banned`. Unknown roles defaults to a normal check." +
                " Example usage: `" + TRIGGER + "check admin 6296561`"){

    override fun handleCommand(input: String, user: User): BMessage? {
        if(!matchesCommand(input)){
            return null;
        }

        val split = splitCommand(input);
        if(split["content"] == null)
            return BMessage("You have to specify a user to check", true);
        try{
            split["content"]?.toLong()
        }catch(e: ClassCastException){
            return BMessage("Please specify a valid user ID", true);
        }catch(e: NumberFormatException){
            return BMessage("Please specify a valid user ID", true);
        }
        val usr = split["content"]?.toLong() ?: return null
        val rank: Int = Utils.getRank(usr, site.config)
        val username: String? = site.config.ranks[usr]?.username

        return BMessage("The user " + (username ?: usr) + "'s rank is $rank", true)
    }

}
