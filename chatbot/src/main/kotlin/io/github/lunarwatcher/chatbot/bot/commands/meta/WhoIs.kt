package io.github.lunarwatcher.chatbot.bot.commands.meta

import io.github.lunarwatcher.chatbot.bot.chat.Message
import io.github.lunarwatcher.chatbot.bot.chat.ReplyMessage
import io.github.lunarwatcher.chatbot.bot.commands.AbstractCommand
import io.github.lunarwatcher.chatbot.bot.sites.Chat

class WhoIs : AbstractCommand("whois", listOf("identify")){
    override fun handleCommand(message: Message): List<ReplyMessage>? {
        val site: Chat = message.chat
        val who = splitCommand(message.content)["content"]?.trim() ?: return listOf(ReplyMessage("You have to tell me who to identify", true))
        if(who.isEmpty()) return listOf(ReplyMessage("You have to tell me who to identify", true))
        val username: String
        val uid = if(who.matches(DIGIT_REGEX)){
            username = site.getUsername(who.toLong());
            who.toLong()

        }else {
            username = who
            val r = site.getUidForUsernameInRoom(who, message.roomID);
            if (r.isEmpty())
                return listOf(ReplyMessage("No users with the username `$who` found", true))
            else if(r.size > 1){
                return listOf(ReplyMessage("More than one user with the username `$who` found. Please use the UID. (found: $r)", true));
            }
            r[0]!!
        }
        return listOf(ReplyMessage("""${
        if (site.name == "stackoverflow" || site.name == "metastackexchange")
            "[$username](${site.host.mainSiteHost}/users/$uid)"
        else if (site.name == "stackexchange") {
            "[$username](${site.host.chatHost}/users/$uid)"
        } else username
        } (UID $uid)""".trimIndent().replace("\n", ""), true));
    }

    companion object {
        val DIGIT_REGEX = "(\\d+)".toRegex()
    }
}