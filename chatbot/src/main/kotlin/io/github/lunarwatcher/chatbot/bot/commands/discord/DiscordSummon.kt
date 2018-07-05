package io.github.lunarwatcher.chatbot.bot.commands.discord

import io.github.lunarwatcher.chatbot.Configurations
import io.github.lunarwatcher.chatbot.bot.chat.Message
import io.github.lunarwatcher.chatbot.bot.chat.ReplyMessage
import io.github.lunarwatcher.chatbot.bot.commands.AbstractCommand
import io.github.lunarwatcher.chatbot.bot.sites.discord.DiscordChat

class DiscordSummon : AbstractCommand("summon", listOf("join")){
    override fun handleCommand(message: Message): List<ReplyMessage>?{
        val site: DiscordChat = if(message.chat is DiscordChat){
            message.chat as DiscordChat
        }else
            return listOf(ReplyMessage("Invalid site. BlameCommand ${Configurations.CREATOR}", true))

        return listOf(ReplyMessage("To make me join a server, use this URL: <https://discordapp.com/oauth2/authorize?client_id=${site.clientID}&scope=bot&permissions=0>", true))
    }
}