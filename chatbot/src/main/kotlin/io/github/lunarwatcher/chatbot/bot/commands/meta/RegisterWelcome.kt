package io.github.lunarwatcher.chatbot.bot.commands.meta

import io.github.lunarwatcher.chatbot.WelcomeMessages
import io.github.lunarwatcher.chatbot.bot.chat.Message
import io.github.lunarwatcher.chatbot.bot.chat.ReplyMessage
import io.github.lunarwatcher.chatbot.bot.commands.AbstractCommand
import io.github.lunarwatcher.chatbot.bot.sites.discord.DiscordChat

class RegisterWelcome : AbstractCommand("registerWelcome", listOf("set-welcome-message", "setWelcomeMessage", "setWelcome", "set-welcome", "register-welcome"),
        desc="Registers a welcome message for a room, or removes it by giving the string literal \"null\" as an argument.",
        help="Call the command with the string literal \"null\" (without quotation) to remove the current message. " +
                "Call it with \"get\" (without quotation) to get the current message (if it exists)",
        rankRequirement = 5){
    override fun handleCommand(message: Message): ReplyMessage? {
        if(!canUserRun(message.user, message.chat)){
            return lowRank()
        }
        val room = if(message.chat is DiscordChat)
            message.user.args.firstOrNull { it.first == "guildID" }?.second?.toLong() ?: (message.chat as DiscordChat).getChannel(message.roomID)?.guild?.longID ?: return null
        else message.roomID

        val content = splitCommand(message.content)["content"] ?: return ReplyMessage("What should I set the welcome message to?", true)
        if(content == "null"){//The string literal "null" is for clearing the message
            WelcomeMessages.INSTANCE!!.removeMessage(message.chat.name, room)
            return ReplyMessage("Successfully removed welcome message for channel $room", true)
        }else if(content == "get"){
            return ReplyMessage("The current welcome message for this room is: ${WelcomeMessages.INSTANCE!!.getMessage(message.chat.name, room)
                    ?: "Undefined"}", true)
        }
        WelcomeMessages.INSTANCE!!.addMessage(message.chat.name, room, content)
        return ReplyMessage("Successfully registered welcome message.", true)
    }

}