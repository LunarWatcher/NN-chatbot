package io.github.lunarwatcher.chatbot.bot.commands.`fun`

import io.github.lunarwatcher.chatbot.bot.chat.Message
import io.github.lunarwatcher.chatbot.bot.chat.ReplyMessage
import io.github.lunarwatcher.chatbot.bot.commands.AbstractCommand

class GiveCommand : AbstractCommand("give", listOf(), "Gives someone something"){

    override fun handleCommand(message: Message): List<ReplyMessage>? {
        val inp = splitCommand(message.content);
        val split = inp["content"]?.split(" ", limit=2) ?: return listOf(ReplyMessage("You have to tell me what to give and to who", false));
        if(split.size != 2)
            return listOf(ReplyMessage("You have to tell me what to give and to who", true));

        val who = split[0].trim()
        val what = split[1].trim()
        return listOf(ReplyMessage("*gives $what to $who*", false));

    }
}