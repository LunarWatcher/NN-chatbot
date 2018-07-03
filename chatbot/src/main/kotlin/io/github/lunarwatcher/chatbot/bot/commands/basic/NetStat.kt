package io.github.lunarwatcher.chatbot.bot.commands.basic

import io.github.lunarwatcher.chatbot.Configurations
import io.github.lunarwatcher.chatbot.Constants
import io.github.lunarwatcher.chatbot.bot.chat.Message
import io.github.lunarwatcher.chatbot.bot.chat.ReplyMessage
import io.github.lunarwatcher.chatbot.bot.commands.AbstractCommand
import io.github.lunarwatcher.chatbot.utils.Utils
import java.net.InetAddress
import java.net.UnknownHostException

class NetStat : AbstractCommand("netStat", listOf("netstat"), "Tells you the status of the neural network", rankRequirement = 1){
    var alive = false
    private var lastRun = 0L
    override fun handleCommand(message: Message): ReplyMessage? {
        if(Utils.getRank(message.user.userID, message.chat.config) < 5 && System.currentTimeMillis() - lastRun < 60000){
            return ReplyMessage("As a security precaution, users with rank 5 or lower can only run the command when it's been more than 1 minute since the last execution", true)
        }
        lastRun = System.currentTimeMillis()
        return try {
            InetAddress.getByName("${Configurations.NEURAL_NET_IP}:" + Constants.FLASK_PORT);
            alive = true;
            ReplyMessage("The server is booting, or is online.", true);
        }catch(e: UnknownHostException){
            alive = false;
            ReplyMessage("The server is offline.", true)
        }
    }
}