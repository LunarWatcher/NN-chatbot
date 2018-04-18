package io.github.lunarwatcher.chatbot.bot.commands

import io.github.lunarwatcher.chatbot.bot.chat.BMessage
import io.github.lunarwatcher.chatbot.bot.sites.Chat

object Quotes{
    val quotes = mutableMapOf<String, String>()
}
class QuoteCommand(val site: Chat) : AbstractCommand("quote", listOf("whoSaid"), desc="Saves a quote"){
    override fun handleCommand(input: String, user: User): BMessage? {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}

class TellCommand(val site: Chat) : AbstractCommand("tell", listOf(), desc="Tells someone something"){
    override fun handleCommand(input: String, user: User): BMessage? {
        val content = splitCommand(input)["content"] ?: return BMessage("*tells you you're using the command wrong", true)
        return try {
            val who = content.split(" ")[0].trim()
            if(who.startsWith("@"))
                return BMessage("You already told them when you pinged them.", true)
            val what = content.split(" ", limit = 2)[1].trim()
            BMessage(" @${who.replace(" ", "")} $what", false)
        }catch(e: IndexOutOfBoundsException){
            BMessage("*tells you you're using the command wrong", true)
        }
    }
}