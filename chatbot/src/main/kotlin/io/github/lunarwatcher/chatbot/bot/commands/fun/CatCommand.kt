package io.github.lunarwatcher.chatbot.bot.commands.`fun`

import io.github.lunarwatcher.chatbot.Configurations
import io.github.lunarwatcher.chatbot.bot.chat.Message
import io.github.lunarwatcher.chatbot.bot.chat.ReplyMessage
import io.github.lunarwatcher.chatbot.bot.commands.AbstractCommand
import io.github.lunarwatcher.chatbot.utils.Http
import jodd.jerry.Jerry
import org.apache.http.impl.client.HttpClients

class CatCommand : AbstractCommand("cat", listOf("kitten"), desc = "Sends a random cat picture in chat"){
    private var http = Http(HttpClients.createDefault())

    override fun handleCommand(message: Message): ReplyMessage? {
        //TODO add support for API keys
        val response = http.get(API_URL)
        if(response.statusCode > 400)
            return ReplyMessage("API returned status code ${response.statusCode}", false)

        val jDoc = Jerry.jerry(response.body)
        val imgElement = jDoc.`$`("img")?.get(0)
                ?: return ReplyMessage("Image not found? BlameCommand ${Configurations.CREATOR}", true)
        if(!imgElement.hasAttribute("src"))
            return ReplyMessage("Image not found? BlameCommand ${Configurations.CREATOR}", true)
        return ReplyMessage(imgElement.getAttribute("src")!!, false)
    }

    companion object {
        //TODO add API key support
        const val API_URL = "https://thecatapi.com/api/images/get?format=html"
    }
}