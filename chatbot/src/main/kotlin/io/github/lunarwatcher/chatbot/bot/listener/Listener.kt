package io.github.lunarwatcher.chatbot.bot.listener

import io.github.lunarwatcher.chatbot.bot.chat.Message
import io.github.lunarwatcher.chatbot.bot.chat.ReplyMessage
import io.github.lunarwatcher.chatbot.bot.command.CommandGroup

//TODO add listeners
interface Listener{
    val name: String;
    val description: String;
    var commandGroup: CommandGroup

    fun handleInput(message: Message) : ReplyMessage?;
}

