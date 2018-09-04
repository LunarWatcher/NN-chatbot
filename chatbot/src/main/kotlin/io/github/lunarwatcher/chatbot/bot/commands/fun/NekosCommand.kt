package io.github.lunarwatcher.chatbot.bot.commands.`fun`

import com.github.natanbc.reliqua.limiter.factory.RateLimiterFactory
import io.github.lunarwatcher.chatbot.bot.chat.Message
import io.github.lunarwatcher.chatbot.bot.chat.ReplyMessage
import io.github.lunarwatcher.chatbot.bot.command.CommandGroup
import io.github.lunarwatcher.chatbot.bot.commands.AbstractCommand
import io.github.lunarwatcher.chatbot.randomItem
import okhttp3.OkHttpClient
import pw.aru.api.nekos4j.image.ImageCache
import pw.aru.api.nekos4j.internal.Nekos4JImpl
import java.io.File

class NekosCommand : AbstractCommand("neko", listOf(), "NEKOS!!!!", "Run with no args for a random picture, or use a supported category.", 1, false, CommandGroup.COMMON){
    val nekos: Nekos4JImpl
    val cache: ImageCache
    val dir: File

    init{
        dir = File("nekocache/")
        cache = ImageCache.directory(dir)
        nekos = Nekos4JImpl(OkHttpClient(), RateLimiterFactory.directFactory(), false, "Mozilla", cache)

    }

    override fun handleCommand(message: Message): List<ReplyMessage>? {
        val content = splitCommand(message.content)["content"] ?: "random"
        val which = get(content)
        /**
         * "ns" stands for "not supported". It's a static return variable from [get], and since it's not used for anything
         * but internal indication that the image category isn't found, it might as well be short.
         */
        if(which == "ns"){
            return listOf(ReplyMessage("The category you selected isn't available. The supported ones are: ${ sfwEndpoints.joinToString(", ")}"))
        }
        val image = nekos.imageProvider.getRandomImage(which).execute();
        return listOf(ReplyMessage(image.url))
    }

    fun destroy(){
        dir.delete()
    }

    companion object {
        private val sfwEndpoints = listOf(
                "tickle", "slap", "poke",
                "pat", "neko", "meow",
                "lizard", "kiss", "hug",
                "fox_girl", "feed", "cuddle",
                "ngif", "holo", "smug",
                "baka", "kemonomimi"
        )

        private fun get(which: String) : String{
            if(which.equals("random", true)){
                return sfwEndpoints.randomItem()
            }

            return if(which.toLowerCase() in sfwEndpoints) which else "ns"
        }
    }
}

