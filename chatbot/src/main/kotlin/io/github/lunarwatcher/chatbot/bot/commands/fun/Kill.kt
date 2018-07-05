package io.github.lunarwatcher.chatbot.bot.commands.`fun`

import io.github.lunarwatcher.chatbot.bot.chat.Message
import io.github.lunarwatcher.chatbot.bot.chat.ReplyMessage
import io.github.lunarwatcher.chatbot.bot.commands.AbstractCommand
import io.github.lunarwatcher.chatbot.utils.Utils

class Kill : AbstractCommand("kill", listOf("assassinate"), "They must be disposed of!"){

    override fun handleCommand(message: Message): List<ReplyMessage>? {
        val chat = message.chat
        val split = splitCommand(message.content);

        if (split.size < 2 || !split.keys.contains("content")){
            listOf(ReplyMessage("You have to tell me who to dispose of", true));
        }
        val name: String = split["content"] ?: return listOf(ReplyMessage("You have to tell me who to dispose of", true));

        if(chat.name == "discord"){
            if(name.toLowerCase().contains("<@!" + chat.credentialManager.userID + ">")){
                listOf(ReplyMessage("I'm not killing myself.", true));
            }
        }else{
            if(name.toLowerCase().contains(("@" + chat.credentialManager.username).toLowerCase())){
                listOf(ReplyMessage("I'm not killing myself", true));
            }
        }

        return listOf(ReplyMessage(Utils.getRandomKillMessage(name), true));
    }
}