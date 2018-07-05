package io.github.lunarwatcher.chatbot.bot.commands.admin

import io.github.lunarwatcher.chatbot.BotCore
import io.github.lunarwatcher.chatbot.Configurations
import io.github.lunarwatcher.chatbot.bot.chat.Message
import io.github.lunarwatcher.chatbot.bot.chat.ReplyMessage
import io.github.lunarwatcher.chatbot.bot.command.CommandCenter
import io.github.lunarwatcher.chatbot.bot.commands.AbstractCommand
import io.github.lunarwatcher.chatbot.info
import io.github.lunarwatcher.chatbot.utils.Utils
import org.slf4j.LoggerFactory

class ShutdownCommand : AbstractCommand("shutdown", listOf("gotosleep", "goaway", "sleep", "die"), "Shuts down the bot. Rank 10 only", rankRequirement = 10){
    val logger = LoggerFactory.getLogger(this::class.java)

    override fun handleCommand(message: Message): List<ReplyMessage>? {
        val site = message.chat

        if(Utils.getRank(message.user.userID, site.config) < 10)
            return listOf(ReplyMessage("I'm afraid I can't let you do that, User.", true))
        val content = splitCommand(message.content);
        val location: String? = content["--location"]
        val timehash: String? = content["--timehash"]

        val confirmed = message.content.contains("--confirm") && content["--confirm"] != null

        if(!confirmed){
            return listOf(ReplyMessage("You sure 'bout that? Run the command with --confirm to shut me down", true))
        }
        if(location != null && timehash != null){
            if(location.contains(Configurations.INSTANCE_LOCATION) && timehash.contains(BotCore.LOCATION)){
                CommandCenter.bot.kill()
                System.exit(0);
            }else{
                "Shutdown ignored: a different instance instance and timestamp was requested".info(logger)
                return null;
            }
        }else if(location != null){
            if(location.contains(Configurations.INSTANCE_LOCATION)){
                CommandCenter.bot.kill()
                System.exit(0);
            }else{
                "Shutdown ignored; a different instance was requested".info(logger);
                return null;
            }
        }else if(timehash != null){
            if(timehash.contains(BotCore.LOCATION)){
                CommandCenter.bot.kill()
                System.exit(0);
            }else{
                "Shutdown ignored: a different timestamp was requested (this: ${BotCore.LOCATION}, found $location)".info(logger)
            }
        }else{
            CommandCenter.bot.kill()
            System.exit(0);
        }
        return listOf(CommandCenter.NO_MESSAGE)
    }
}