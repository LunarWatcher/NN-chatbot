package io.github.lunarwatcher.chatbot.bot.commands.`fun`

import io.github.lunarwatcher.chatbot.Configurations
import io.github.lunarwatcher.chatbot.bot.chat.Message
import io.github.lunarwatcher.chatbot.bot.chat.ReplyMessage
import io.github.lunarwatcher.chatbot.bot.commands.AbstractCommand
import io.github.lunarwatcher.chatbot.utils.HttpHelper

class CatCommand : AbstractCommand("cat", listOf("kitten"), desc = "Sends a random cat picture in chat"){
    private var cookies = mutableMapOf<String, String>()
    override fun handleCommand(message: Message): List<ReplyMessage>? {
        //TODO add support for API keys
        val response = HttpHelper.get(API_URL, cookies)
        if(response.statusCode() > 400)
            return listOf(ReplyMessage("API returned status code ${response.statusCode()}", false))
        val parsing = response.parse().getElementsByTag("img");
        if(parsing.size == 0)
            return listOf(ReplyMessage("No images found. :c", true))

        val imgElement = parsing[0].absUrl("src")
                ?: return listOf(ReplyMessage("Image not found. Blame ${Configurations.CREATOR}", true))

        return listOf(ReplyMessage(imgElement, false));
    }

    companion object {
        //TODO add API key support
        const val API_URL = "https://thecatapi.com/api/images/get?format=html"
    }
}