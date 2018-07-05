package io.github.lunarwatcher.chatbot.bot.commands.`fun`

import io.github.lunarwatcher.chatbot.bot.chat.Message
import io.github.lunarwatcher.chatbot.bot.chat.ReplyMessage
import io.github.lunarwatcher.chatbot.bot.commands.AbstractCommand
import io.github.lunarwatcher.chatbot.bot.sites.Chat
import java.util.*

class BlameCommand : AbstractCommand("blame", listOf(), help="Someone must be blamed for this!"){
    private val random = Random();

    override fun handleCommand(message: Message): List<ReplyMessage>? {
        val site: Chat = message.chat

        val problem = splitCommand(message.content)["content"]

        val blamable = site.getUsersInServer(message.roomID)
        if(blamable.isEmpty())
            return listOf(ReplyMessage("I don't know!!", true))
        return if(problem == null)
            listOf(ReplyMessage("It is ${blamable[random.nextInt(blamable.size)].userName}'s fault!", true));
        else listOf(ReplyMessage("blames ${blamable[random.nextInt(blamable.size)].userName} for $problem", true));

    }
}
