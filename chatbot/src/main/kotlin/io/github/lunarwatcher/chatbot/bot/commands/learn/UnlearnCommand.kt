package io.github.lunarwatcher.chatbot.bot.commands.learn

import io.github.lunarwatcher.chatbot.bot.chat.Message
import io.github.lunarwatcher.chatbot.bot.chat.ReplyMessage
import io.github.lunarwatcher.chatbot.bot.command.CommandCenter
import io.github.lunarwatcher.chatbot.bot.commands.AbstractCommand
import io.github.lunarwatcher.chatbot.utils.Utils

class UnlearnCommand(val commands: TaughtCommands, val center: CommandCenter) : AbstractCommand("unlearn", listOf("forget"), "Forgets a taught command", rankRequirement = 1){

    override fun handleCommand(message: Message): ReplyMessage? {

        val `in` = splitCommand(message.content)

        val name = `in`["content"] ?: return ReplyMessage("I need to know what to forget", true);

        if(center.isBuiltIn(name, message.chat)){
            return ReplyMessage("You can't make me forget something that's hard-coded. :>", true);
        }

        if(!commands.doesCommandExist(name)){
            return ReplyMessage("I can't forget what I never knew", true);
        }

        if(commands.get(name)!!.nsfw && !message.nsfwSite)
            return ReplyMessage("Can't forget a command that's not available to this site", true)

        commands.removeCommand(name);
        center.refreshBuckets()

        return ReplyMessage(Utils.getRandomForgottenMessage(), true);
    }
}