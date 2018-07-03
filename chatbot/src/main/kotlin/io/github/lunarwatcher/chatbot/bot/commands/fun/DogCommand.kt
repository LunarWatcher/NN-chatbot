package io.github.lunarwatcher.chatbot.bot.commands.`fun`

import io.github.lunarwatcher.chatbot.Configurations
import io.github.lunarwatcher.chatbot.bot.chat.Message
import io.github.lunarwatcher.chatbot.bot.chat.ReplyMessage
import io.github.lunarwatcher.chatbot.bot.commands.AbstractCommand
import io.github.lunarwatcher.chatbot.utils.Http
import org.apache.http.impl.client.HttpClients

class DogCommand : AbstractCommand("dog", listOf("woof", "bark", "puppy"), desc = "Sends a random dog picture in chat"){
    private var http = Http(HttpClients.createDefault())

    override fun handleCommand(message: Message): ReplyMessage? {
        val response = http.get(API_URL)
        if(response.statusCode > 400)
            return ReplyMessage("API returned status code ${response.statusCode}", false)

        val json = response.bodyAsJson
        val url = json.get("message") ?: return ReplyMessage("Image not found? BlameCommand ${Configurations.CREATOR}", true)
        return ReplyMessage(url.textValue(), false)
    }

    companion object {
        const val API_URL = "https://dog.ceo/api/breeds/image/random"
    }
}