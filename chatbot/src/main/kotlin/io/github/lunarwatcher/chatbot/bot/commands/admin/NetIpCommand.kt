package io.github.lunarwatcher.chatbot.bot.commands.admin

import io.github.lunarwatcher.chatbot.Configurations
import io.github.lunarwatcher.chatbot.bot.chat.Message
import io.github.lunarwatcher.chatbot.bot.chat.ReplyMessage
import io.github.lunarwatcher.chatbot.bot.commands.AbstractCommand

class NetIpCommand : AbstractCommand("ip", listOf(), rankRequirement = 10){
    override fun handleCommand(message: Message): ReplyMessage? {
        if(!canUserRun(message.user, message.chat))
            return ReplyMessage("I'm afraid I can't let you do that, User", true)
        val content = (splitCommand(message.content)["content"]?.trim()) ?: "You have to supply a new IP"
        println(content)
        if(!"""\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}""".toRegex().containsMatchIn(content) || !containsOutOfRange(content)){
            return ReplyMessage("Invalid IP", true)
        }

        Configurations.NEURAL_NET_IP = content;
        return ReplyMessage("NN IP set to $content", true)
    }

    private fun containsOutOfRange(input: String) : Boolean{
        try {
            val parts = input.split(".")
            for (part in parts) {
                val numP = part.toInt()
                if(numP < 0 || numP > 255) {
                    System.out.println(numP)//Not printed
                    return false;
                }
            }
        }catch(e: Exception) {
            e.printStackTrace()//Not printed
            return false
        }
        return true;
    }
}