package io.github.lunarwatcher.chatbot.bot.commands.`fun`

import io.github.lunarwatcher.chatbot.bot.ReplyBuilder
import io.github.lunarwatcher.chatbot.bot.chat.Message
import io.github.lunarwatcher.chatbot.bot.chat.ReplyMessage
import io.github.lunarwatcher.chatbot.bot.commands.AbstractCommand
import io.github.lunarwatcher.chatbot.randomItem
import io.github.lunarwatcher.chatbot.utils.Utils
import org.apache.commons.lang3.StringUtils

class DogeCommand : AbstractCommand("doge", listOf(), desc="Such doge. Much command."){
    val doges = mutableListOf("such", "very", "much", "so", "many")
    override fun handleCommand(message: Message): List<ReplyMessage>? {

        val raw = message.content.split(" ", limit=2)
        val converted = if (raw.size < 2) defaultMsg else raw[1]

        val msg = ReplyBuilder()
        val what = converted.split(",").map{ it.trim() }.filter{ it.isNotEmpty() && it.isNotBlank() }
        if (what.isEmpty()){
            return listOf(ReplyMessage("Much message.user. Few arguments. Such attempt", true))
        }


        if (Utils.random.nextBoolean())
            msg.fixedInput().append(StringUtils.repeat(" ", Utils.random.nextInt(10))).append("wow").nl()

        val maxIndex = Math.min(what.size, 10)//Limit at 10. Because i
        for (i in 0 until maxIndex){
            msg.fixedInput().append(StringUtils.repeat(" ", Utils.random.nextInt(15))).append(doges.randomItem()).append(" " + what[i]).nl()
        }
        return listOf(ReplyMessage(msg.toString(), false));
    }

    companion object {
        const val defaultMsg = "user, fail, pro"
    }
}