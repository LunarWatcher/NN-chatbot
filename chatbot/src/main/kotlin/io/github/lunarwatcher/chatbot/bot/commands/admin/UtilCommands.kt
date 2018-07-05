package io.github.lunarwatcher.chatbot.bot.commands.admin

import io.github.lunarwatcher.chatbot.bot.chat.Message
import io.github.lunarwatcher.chatbot.bot.chat.ReplyMessage
import io.github.lunarwatcher.chatbot.bot.command.CommandCenter
import io.github.lunarwatcher.chatbot.bot.commands.AbstractCommand
import io.github.lunarwatcher.chatbot.utils.Utils

class SaveCommand : AbstractCommand("save", listOf(), "Saves the database"){
    override fun handleCommand(message: Message): List<ReplyMessage>? {
        val site = message.chat
        ;
        if(Utils.getRank(message.user.userID, site.config) < 8)
            return listOf(ReplyMessage("I'm afraid I can't let you do that, User", true));


        CommandCenter.bot.save();

        return listOf(ReplyMessage("Saved.", true));
    }
}
