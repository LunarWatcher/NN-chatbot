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
import java.awt.Container

class ShutdownCommand : AbstractCommand("shutdown", listOf("gotosleep", "goaway", "sleep", "die"), "Shuts down the bot. Rank 10 only", rankRequirement = 10){
    val logger = LoggerFactory.getLogger(this::class.java)

    override fun handleCommand(message: Message): List<ReplyMessage>? {
        val site = message.chat

        if(Utils.getRank(message.user.userID, site.config) < 10)
            return listOf(ReplyMessage("I'm afraid I can't let you do that, User.", true))
        val content = splitCommand(message.content);
        var location: String? = content["--location"]
        var timehash: String? = content["--timehash"]
        val caseInsensitive = content["--notCaseInsensitive"] == null
        if(content["content"] != null && location == null){
            location = content["content"]?.trim();
        }

        val confirmed = message.content.contains("--confirm") && content["--confirm"] != null

        if(!confirmed){
            if(location != null)
                if(!Configurations.INSTANCE_LOCATION.contains(location, caseInsensitive))
                    return listOf(CommandCenter.NO_MESSAGE);
            if(timehash != null)
                if(!BotCore.LOCATION.startsWith(timehash, caseInsensitive))
                    return listOf(CommandCenter.NO_MESSAGE)
            return listOf(ReplyMessage("You sure 'bout that? Run the command with --confirm to shut me down", true))
        }
        if(location != null && timehash != null){
            if(Configurations.INSTANCE_LOCATION.contains(location, caseInsensitive) && BotCore.LOCATION.contains(timehash, caseInsensitive)){
                CommandCenter.bot.kill()
                System.exit(0);
            }else{
                "Shutdown ignored: a different instance instance and timestamp was requested".info(logger)
            }
        }else if(location != null){
            logger.info("requested location: " + location)
            if(Configurations.INSTANCE_LOCATION.contains(location, caseInsensitive)){
                CommandCenter.bot.kill()
                System.exit(0);
            }else{
                "Shutdown ignored; a different instance was requested".info(logger);
            }
        }else if(timehash != null){
            if(BotCore.LOCATION.startsWith(timehash, caseInsensitive)){
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