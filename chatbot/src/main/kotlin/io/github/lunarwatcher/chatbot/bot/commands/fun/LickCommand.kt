package io.github.lunarwatcher.chatbot.bot.commands.`fun`

import io.github.lunarwatcher.chatbot.bot.chat.Message
import io.github.lunarwatcher.chatbot.bot.chat.ReplyMessage
import io.github.lunarwatcher.chatbot.bot.commands.AbstractCommand
import io.github.lunarwatcher.chatbot.utils.Utils

class LickCommand : AbstractCommand("lick", listOf(), "Licks someone. Or something"){
    override fun handleCommand(message: Message): ReplyMessage? {
        val split = splitCommand(message.content);
        if (split.size < 2 || !split.keys.contains("content")){
            return ReplyMessage("You have to tell me who to lick", true);
        }

        val name: String = split["content"] ?: return ReplyMessage("You have to tell me who to lick", true);

        return ReplyMessage(Utils.getRandomLickMessage(name), true);
    }
}

