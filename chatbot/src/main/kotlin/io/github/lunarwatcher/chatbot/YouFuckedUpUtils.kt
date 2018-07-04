package io.github.lunarwatcher.chatbot

import io.github.lunarwatcher.chatbot.LogStorage.logs
import io.github.lunarwatcher.chatbot.bot.ReplyBuilder
import io.github.lunarwatcher.chatbot.bot.chat.Message
import io.github.lunarwatcher.chatbot.bot.chat.ReplyMessage
import io.github.lunarwatcher.chatbot.bot.commands.AbstractCommand
import org.apache.commons.lang3.StringUtils

object LogStorage{
    val logs = mutableListOf<String>()

    fun crash(e: Throwable){
        val base = e.toString()
        val reason = e.localizedMessage
        var result = "$base: $reason\n"
        for(element in e.stackTrace){
            result += StringUtils.repeat(" ", 4) + "at $element\n"
        }
        logs.add(result)

    }

}

class CrashLogs : AbstractCommand("logs", listOf(), "Prints logs. Useful for screwups", rankRequirement = 5){

    fun crash(e: Throwable){
        LogStorage.crash(e)

    }

    override fun handleCommand(message: Message): ReplyMessage? {
        if (!canUserRun(message.user, message.chat)){
            return ReplyMessage("You need rank 5 or higher to view crash logs.", true);
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
            return ReplyMessage(reply.toString(), false)

        }
        return ReplyMessage("No logs. All good! :D", true)
    }
}

