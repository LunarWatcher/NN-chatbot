package io.github.lunarwatcher.chatbot

import io.github.lunarwatcher.chatbot.bot.chat.BMessage
import io.github.lunarwatcher.chatbot.bot.commands.AbstractCommand
import io.github.lunarwatcher.chatbot.bot.commands.Command
import io.github.lunarwatcher.chatbot.bot.commands.User

class CrashLogs() : AbstractCommand("logs", listOf(), "Prints logs. Useful for screwups"){
    override fun handleCommand(input: String, user: User): BMessage? {
        //TODO
        return BMessage("Not implemented yet :(", true)
    }
}