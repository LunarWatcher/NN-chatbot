package io.github.lunarwatcher.chatbot

import com.google.common.collect.Lists
import io.github.lunarwatcher.chatbot.bot.ReplyBuilder
import io.github.lunarwatcher.chatbot.bot.chat.BMessage
import io.github.lunarwatcher.chatbot.bot.commands.AbstractCommand
import io.github.lunarwatcher.chatbot.bot.commands.Command
import io.github.lunarwatcher.chatbot.bot.commands.User
import io.github.lunarwatcher.chatbot.bot.sites.Chat
import io.github.lunarwatcher.chatbot.utils.Utils
import java.util.*

class CrashLogs(val site: Chat) : AbstractCommand("logs", listOf(), "Prints logs. Useful for screwups"){
    val logs = mutableListOf<String>();

    fun crash(e: Exception){
        logs.add(e.toString())
    }

    override fun handleCommand(input: String, user: User): BMessage? {
        if (Utils.getRank(user.userID, site.config) < 8){
            return BMessage("You need rank 8 or higher to view crash logs", true);
        }
        if (logs.size != 0){
            val reply = ReplyBuilder()
            logs.map{
                val items = it.split("\n")
                for (item in items){
                    reply.fixedInput().append(item).nl()
                }
            }
            logs.clear()
            return BMessage(reply.toString(), false)

        }
        return BMessage("No logs. All good! :D", true)
    }
}
