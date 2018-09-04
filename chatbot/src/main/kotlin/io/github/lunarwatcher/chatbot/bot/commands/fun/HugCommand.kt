package io.github.lunarwatcher.chatbot.bot.commands.`fun`

import io.github.lunarwatcher.chatbot.bot.chat.Message
import io.github.lunarwatcher.chatbot.bot.chat.ReplyMessage
import io.github.lunarwatcher.chatbot.bot.command.CommandGroup
import io.github.lunarwatcher.chatbot.bot.commands.AbstractCommand
import io.github.lunarwatcher.chatbot.bot.sites.discord.DiscordChat
import io.github.lunarwatcher.chatbot.randomItem

class HugCommand : AbstractCommand("hug", listOf(), "Hugs someone <3", rankRequirement = 1, commandGroup = CommandGroup.COMMON){
    override fun handleCommand(message: Message): List<ReplyMessage>? {
        val content = splitCommand(message.content)["content"]
        if(content == null || content.isEmpty() || content.isBlank())
            return listOf(ReplyMessage("*Gives you a hug <3*", true))
        val who = message.user.userName
        val target = if(message.chat is DiscordChat){
            if(content.contains(pingRegex))
                pingRegex.findAll(content)
                        .map {
                            val uid = it.groupValues[1].toLongOrNull()
                            if(uid == null) ""
                            else {
                                val user = (message.chat as DiscordChat).client.getUserByID(uid)
                                user.name
                            }
                        }
                        .filter { it != null }
                        .joinToString(", ")
                        .trim()
            else content

        }else{
            content.replace("@", "")
        }

        if(content.trim().isEmpty() || content.trim().isBlank()){
            return listOf(ReplyMessage("*Gives you a hug <3*", true));
        }

        return listOf(ReplyMessage(hugs.randomItem().format(who, target)))
    }

    companion object {
        val hugs = listOf(
                "*%s hugs %s*", "%s gives %s a warm hug*",
                "*%s soaks %s into their floof owo*"
        )

        private val pingRegex = "<!?@(\\d+)>".toRegex()
    }
}