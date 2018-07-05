package io.github.lunarwatcher.chatbot.bot.commands.`fun`

import io.github.lunarwatcher.chatbot.bot.chat.Message
import io.github.lunarwatcher.chatbot.bot.chat.ReplyMessage
import io.github.lunarwatcher.chatbot.bot.commands.AbstractCommand

class TellCommand : AbstractCommand("tell", listOf(), desc="Tells someone something"){
    override fun handleCommand(message: Message): List<ReplyMessage>? {
        val content = splitCommand(message.content)["content"] ?: return listOf(ReplyMessage("*tells you you're using the command wrong", true))
        return try {
            val who = content.split(" ")[0].trim()
            if(who.startsWith("@"))
                return listOf(ReplyMessage("You already told them when you pinged them.", true))
            val what = content.split(" ", limit = 2)[1].trim()
            listOf(ReplyMessage(" @${who.replace(" ", "")} $what", false));
        }catch(e: IndexOutOfBoundsException){
            listOf(ReplyMessage("*tells you you're using the command wrong", true));
        }
    }
}