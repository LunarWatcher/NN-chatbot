package io.github.lunarwatcher.chatbot.bot.commands.`fun`

import io.github.lunarwatcher.chatbot.bot.chat.Message
import io.github.lunarwatcher.chatbot.bot.chat.ReplyMessage
import io.github.lunarwatcher.chatbot.bot.commands.AbstractCommand
import java.net.URLEncoder

class LMGTFY : AbstractCommand("lmgtfy", listOf("searchfor", "google"), "Sends a link to Google in chat"){
    override fun handleCommand(message: Message): ReplyMessage? {
        var query = splitCommand(message.content)["content"] ?: ""
        if(query.isEmpty())
            return ReplyMessage("You have to supply a query", true);

        if(message.content.contains("--ddg") || message.content.contains("--duckduckgo")){
            if(query.isEmpty())
                return ReplyMessage("You have to supply a query", true);

            return ReplyMessage(DDG_LINK + URLEncoder.encode(query, "UTF-8"), true)
        }

        return ReplyMessage(GOOGLE_LINK + URLEncoder.encode(query, "UTF-8"), true)
    }

    companion object {
        const val GOOGLE_LINK = "https://www.google.com/search?q=";
        const val DDG_LINK = "https://www.duckduckgo.com/?q=";
    }
}
