package io.github.lunarwatcher.chatbot.bot.commands.`fun`

import io.github.lunarwatcher.chatbot.bot.chat.Message
import io.github.lunarwatcher.chatbot.bot.chat.ReplyMessage
import io.github.lunarwatcher.chatbot.bot.commands.AbstractCommand
import java.util.*

@Suppress("NAME_SHADOWING")
class RandomNumber : AbstractCommand("random", listOf("dice"), "Generates a random number"){
    val random: Random = Random(System.currentTimeMillis());

    override fun handleCommand(message: Message): List<ReplyMessage>? {
        try {
            val split = message.content.split(" ");
            return when {
                split.size == 1 -> listOf(ReplyMessage(randomNumber(6, 1), true));
                split.size == 2 -> listOf(ReplyMessage(randomNumber(split[1].toInt(), 1), true));
                split.size >= 3 -> listOf(ReplyMessage(randomNumber(split[1].toInt(), split[2].toInt()), true));
                else -> {
                    listOf(ReplyMessage("Too many arguments", true))
                }
            }
        }catch(e: NumberFormatException){
            return listOf(ReplyMessage("Invalid number", true))
        }catch(e: ClassCastException){
            return listOf(ReplyMessage("Invalid number", true))
        }catch(e: Exception){
            return listOf(ReplyMessage("Something went terribly wrong", true));
        }
    }

    fun randomNumber(limit: Int, count: Int): String{
        val count = if(count > 200) 200 else count;
        val builder = StringBuilder()
        for(i in 0 until count){
            builder.append((if(i == count - 1) random.nextInt(limit) else random.nextInt(limit).toString() + ", "))
        }
        return builder.toString();
    }
}