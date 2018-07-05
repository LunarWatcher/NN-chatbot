package io.github.lunarwatcher.chatbot.bot.commands.`fun`

import io.github.lunarwatcher.chatbot.Constants
import io.github.lunarwatcher.chatbot.bot.chat.Message
import io.github.lunarwatcher.chatbot.bot.chat.ReplyMessage
import io.github.lunarwatcher.chatbot.bot.commands.AbstractCommand
import java.util.*

class WakeCommand : AbstractCommand("wake", listOf(), desc="HEY! Wake up!"){
    val random = Random()

    override fun handleCommand(message: Message): List<ReplyMessage>? {

        val who = splitCommand(message.content)["content"] ?: return listOf(ReplyMessage("You have to tell me who to wake up!", true))
        return listOf(ReplyMessage(Constants.wakeMessages[random.nextInt(Constants.wakeMessages.size)].format(who), true));
    }
}
