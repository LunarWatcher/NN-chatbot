package io.github.lunarwatcher.chatbot.bot.commands.`fun`

import com.google.common.net.UrlEscapers
import io.github.lunarwatcher.chatbot.bot.chat.Message
import io.github.lunarwatcher.chatbot.bot.chat.ReplyMessage
import io.github.lunarwatcher.chatbot.bot.command.CommandCenter
import io.github.lunarwatcher.chatbot.bot.commands.AbstractCommand
import java.net.URLEncoder

class WikiCommand : AbstractCommand("wiki", listOf(), desc="Links to Wikipedia", help="Gets a wikipedia page. Use `--lang \"languagecode\"` to link to a specific site", rankRequirement = 1){
    override fun handleCommand(message: Message): ReplyMessage? {
        val split = splitCommand(message.content)

        val lang = split["--lang"]?.let{
            if(it.startsWith("\"") && it.endsWith("\"")){
                it.substring(1, it.length - 2)
            }
            it
        } ?: "en"
        val content = split["content"]?.replace(" ", "_") ?: return ReplyMessage("https://www.google.com/teapot", true)
        val article = UrlEscapers.urlPathSegmentEscaper()
                .escape(content)
        return ReplyMessage("https://$lang.wikipedia.org/wiki/" + URLEncoder.encode(article, "UTF-8"), false)
    }
}

class DefineCommand : AbstractCommand("define", listOf(), desc="Links the definition of a word", help="Do `${CommandCenter.TRIGGER}define word` to get the definition for a word", rankRequirement = 1){
    override fun handleCommand(message: Message): ReplyMessage?
            = ReplyMessage("https://en.wiktionary.org/wiki/${URLEncoder.encode(splitCommand(message.content)["content"]
            ?: "invalid", "UTF-8")}",
            false)
}
