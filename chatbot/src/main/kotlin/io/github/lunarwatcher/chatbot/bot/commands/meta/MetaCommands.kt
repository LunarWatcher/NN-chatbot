package io.github.lunarwatcher.chatbot.bot.commands.meta

import io.github.lunarwatcher.chatbot.BotCore
import io.github.lunarwatcher.chatbot.Configurations
import io.github.lunarwatcher.chatbot.LogStorage
import io.github.lunarwatcher.chatbot.bot.ReplyBuilder
import io.github.lunarwatcher.chatbot.bot.chat.Message
import io.github.lunarwatcher.chatbot.bot.chat.ReplyMessage
import io.github.lunarwatcher.chatbot.bot.command.CommandCenter
import io.github.lunarwatcher.chatbot.bot.commands.AbstractCommand
import io.github.lunarwatcher.chatbot.getRevision


class RevisionCommand : AbstractCommand("rev", listOf("revision")){
    override fun handleCommand(message: Message): ReplyMessage? {
        return try{
            ReplyMessage(getRevision() + " (@version ${Configurations.REVISION})", true)
        }catch(e: Exception){
            LogStorage.crash(e)
            ReplyMessage("Unknown revision", true)
        }
    }
}

class AboutCommand : AbstractCommand("about", listOf("whoareyou", "owner"), "Let me tell you a little about myself...", "Use `" + CommandCenter.TRIGGER + "about` to show the info", rankRequirement = 1){

    override fun handleCommand(message: Message): ReplyMessage? {
        val reply = ReplyBuilder();
        reply.append("Hiya! I'm Alisha, a chatbot designed by [${Configurations.CREATOR}](${Configurations.CREATOR_GITHUB}). ")
                .append("I'm open-source and the code is available on [Github](${Configurations.GITHUB}). Running version ${Configurations.REVISION}")

        return ReplyMessage(reply.toString(), true)
    }
}

class GitHubCommand : AbstractCommand("github", listOf("source", "code", "sourceCode", "gh"), desc="Sends the link to GitHub in chat (also available through the about command). " +
        "Raise any concerns there."){
    override fun handleCommand(message: Message): ReplyMessage? = ReplyMessage("Here you go: ${Configurations.GITHUB}", true)
}

class TestCommand : AbstractCommand("test", listOf("items"), desc = "Returns the contents of splitCommand", help = "Play around with the bot"){
    override fun handleCommand(message: Message): ReplyMessage? = ReplyMessage("Received arguments: " + splitCommand(message.content), false)
}

class LocationCommand : AbstractCommand("location", listOf(), help="Shows the current bot location"){
    override fun handleCommand(message: Message): ReplyMessage? {
        return ReplyMessage("${Configurations.INSTANCE_LOCATION} (${BotCore.LOCATION})", true)
    }
}
