package io.github.lunarwatcher.chatbot.bot.commands.`fun`

import io.github.lunarwatcher.chatbot.bot.chat.Message
import io.github.lunarwatcher.chatbot.bot.chat.ReplyMessage
import io.github.lunarwatcher.chatbot.bot.commands.AbstractCommand

class Spongebobify : AbstractCommand("spongebobify", listOf(), "mAKe teXT mocKiNg") {

    override fun handleCommand(message: Message): List<ReplyMessage>? {
        val inp = splitCommand(message.content)
        val content = inp["content"] ?:
            return listOf(ReplyMessage(createMockingString("Let's try calling this command without an argument! ") + "(Please supply something to spongebobify)", false))

        return listOf(ReplyMessage(createMockingString(content), false))
    }

    private fun createMockingString(input: String) : String {
        return input.split("").joinToString (separator = "") { if (Math.random() >= .5) it.toUpperCase() else it.toLowerCase() }
    }


}