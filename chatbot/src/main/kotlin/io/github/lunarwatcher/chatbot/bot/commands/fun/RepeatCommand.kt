package io.github.lunarwatcher.chatbot.bot.commands.`fun`

import io.github.lunarwatcher.chatbot.bot.chat.Message
import io.github.lunarwatcher.chatbot.bot.chat.ReplyMessage
import io.github.lunarwatcher.chatbot.bot.commands.AbstractCommand
import io.github.lunarwatcher.chatbot.utils.Utils

class RepeatCommand : AbstractCommand("echo", listOf("repeat", "say")){
    override fun handleCommand(message: Message): List<ReplyMessage>?{
        val content = splitCommand(message.content)["content"] ?: return listOf(ReplyMessage("What?", true))
        if(content.trim().isEmpty()) return listOf(ReplyMessage("What?", true))

        if(message.chat.name == "twitch" && content.startsWith("/") && Utils.getRank(message.user.userID, message.chat.config) < 9)
            return listOf(ReplyMessage("No", true))

        return listOf(ReplyMessage(content, false));
    }
}