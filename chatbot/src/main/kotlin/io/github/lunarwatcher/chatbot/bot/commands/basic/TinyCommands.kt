package io.github.lunarwatcher.chatbot.bot.commands.basic

import io.github.lunarwatcher.chatbot.bot.chat.Message
import io.github.lunarwatcher.chatbot.bot.chat.ReplyMessage
import io.github.lunarwatcher.chatbot.bot.command.CommandCenter
import io.github.lunarwatcher.chatbot.bot.commands.AbstractCommand
import io.github.lunarwatcher.chatbot.equalsAny

class ShrugCommand : AbstractCommand("shrug", listOf("dunno", "what"), "Shrugs", "Use `" + CommandCenter.TRIGGER + "shrug` to use the command", rankRequirement = 1){
    override fun handleCommand(message: Message): List<ReplyMessage>? {
        return listOf(ReplyMessage(if (message.chat.name.equalsAny("metastackexchange", "stackexchange", "stackoverflow"))
            "¯\\\\_(ツ)_/¯"
        else "¯\\_(ツ)_/¯"
                , false));
    }
}

open class BasicPrintCommand(val print: String, val reply: Boolean,  name: String, aliases: List<String>, desc: String) : AbstractCommand(name, aliases, desc){
    override fun handleCommand(message: Message): List<ReplyMessage>? {
        return listOf(ReplyMessage(print, false));
    }
}

class Alive : AbstractCommand("alive", listOf(), "Used to check if the bot is working"){
    override fun handleCommand(message: Message): List<ReplyMessage>? {
        return listOf(ReplyMessage("I'm pretty sure I am.", true));
    }
}