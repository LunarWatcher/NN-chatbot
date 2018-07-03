package io.github.lunarwatcher.chatbot.bot.commands.`fun`

import io.github.lunarwatcher.chatbot.bot.chat.Message
import io.github.lunarwatcher.chatbot.bot.chat.ReplyMessage
import io.github.lunarwatcher.chatbot.bot.commands.AbstractCommand

class PingCommand : AbstractCommand("ping", listOf("poke"), "Pokes someone"){
    override fun handleCommand(message: Message): ReplyMessage? {

        val inp = splitCommand(message.content);
        var content = inp["content"]?.replace(" ", "") ?: return null;
        if(!content.startsWith("@"))
            content = "@$content";
        return ReplyMessage("*pings $content*", false);
    }
}