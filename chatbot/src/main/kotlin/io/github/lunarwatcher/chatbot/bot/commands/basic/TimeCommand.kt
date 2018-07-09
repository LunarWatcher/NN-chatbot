package io.github.lunarwatcher.chatbot.bot.commands.basic

import io.github.lunarwatcher.chatbot.DATE_PATTERN
import io.github.lunarwatcher.chatbot.bot.chat.Message
import io.github.lunarwatcher.chatbot.bot.chat.ReplyMessage
import io.github.lunarwatcher.chatbot.bot.command.CommandCenter
import io.github.lunarwatcher.chatbot.bot.commands.AbstractCommand
import io.github.lunarwatcher.chatbot.formatter
import org.joda.time.DateTimeZone
import org.joda.time.Instant
import org.joda.time.format.DateTimeFormat
import java.util.*

class TimeCommand : AbstractCommand("time", listOf(), "What time is it?", help="Displays the current time at the bots location.\n" +
        "`-get` as an argument without content displays all the available timezones.\n" +
        "Supplying a timezone (see `${CommandCenter.TRIGGER}time -get` for the available ones) shows the current time in that timezone"){

    override fun handleCommand(message: Message): List<ReplyMessage>? {
        val raw = splitCommand(message.content)
        val content = raw["content"]
        if(content == null && raw.size == 1)
            return listOf(ReplyMessage(formatter.format(System.currentTimeMillis()), true));

        if(content != null && (content.trim().toLowerCase().contains("139") || content.trim().toLowerCase().contains("java"))){
            return listOf(ReplyMessage("Morning", true))
        }else if(raw["--get"] != null)
            return listOf(ReplyMessage("Available timezones: " + DateTimeZone.getAvailableIDs(), false))
        else if(content != null && (content.trim().toLowerCase().contains("internet")))
            return listOf(ReplyMessage("Morning UIT (Universal Internet Time)", true));

        return try{
            val applicable = DateTimeZone.forID(content)
            val formatter = DateTimeFormat.forPattern(DATE_PATTERN)
                    .withLocale(Locale.ENGLISH)
                    .withZone(applicable)
            listOf(ReplyMessage(Instant().toString(formatter), true))
        }catch(e: IllegalArgumentException){
            listOf(ReplyMessage(e.message, true))
        }
    }
}