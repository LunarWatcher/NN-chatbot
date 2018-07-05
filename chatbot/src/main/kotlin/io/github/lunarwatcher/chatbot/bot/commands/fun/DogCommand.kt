package io.github.lunarwatcher.chatbot.bot.commands.`fun`

import io.github.lunarwatcher.chatbot.Configurations
import io.github.lunarwatcher.chatbot.bot.chat.Message
import io.github.lunarwatcher.chatbot.bot.chat.ReplyMessage
import io.github.lunarwatcher.chatbot.bot.commands.AbstractCommand
import io.github.lunarwatcher.chatbot.utils.HttpHelper
import io.github.lunarwatcher.chatbot.utils.JsonUtils
import org.apache.http.impl.client.HttpClients

class DogCommand : AbstractCommand("dog", listOf("woof", "bark", "puppy"), desc = "Sends a random dog picture in chat"){
    private var cookies = mutableMapOf<String, String>()

    override fun handleCommand(message: Message): List<ReplyMessage>? {
        val response = HttpHelper.get(API_URL, cookies)
        if(response.statusCode() > 400)
            return listOf(ReplyMessage("API returned status code ${response.statusCode()}", false))

        val json = JsonUtils.convertToJson(response)

        val url = json.get("message") ?: return listOf(ReplyMessage("Image not found? BlameCommand ${Configurations.CREATOR}", true))
        return listOf(ReplyMessage(url.textValue(), false))
    }

    companion object {
        const val API_URL = "https://dog.ceo/api/breeds/image/random"
    }
}