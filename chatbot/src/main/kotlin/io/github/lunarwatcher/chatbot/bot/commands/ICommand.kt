package io.github.lunarwatcher.chatbot.bot.commands

import io.github.lunarwatcher.chatbot.User
import io.github.lunarwatcher.chatbot.bot.chat.Message
import io.github.lunarwatcher.chatbot.bot.chat.ReplyMessage
import io.github.lunarwatcher.chatbot.bot.command.CommandGroup
import io.github.lunarwatcher.chatbot.bot.sites.Chat

interface ICommand{
    val name: String;
    val aliases: List<String>
    val desc: String;
    val help: String;
    val rankRequirement: Int
    val nsfw: Boolean
    var commandGroup: CommandGroup
    /**
     * Check if the input starts with the name or one of the command's aliases
     */
    fun matchesCommand(input: String) : Boolean;
    /**
     * Handle a given command
     */
    fun handleCommand(message: Message) : List<ReplyMessage>?;
    fun canUserRun(user: User, chat: Chat) : Boolean

}