package io.github.lunarwatcher.chatbot.bot.commands.twitch

import io.github.lunarwatcher.chatbot.bot.chat.Message
import io.github.lunarwatcher.chatbot.bot.chat.ReplyMessage
import io.github.lunarwatcher.chatbot.bot.command.CommandCenter
import io.github.lunarwatcher.chatbot.bot.commands.AbstractCommand
import io.github.lunarwatcher.chatbot.bot.sites.twitch.TwitchChat

class LeaveTwitch : AbstractCommand("leaveTwitch", listOf("leave-twitch"), rankRequirement = 5){
    override fun handleCommand(message: Message): ReplyMessage? {
        val item = if(message.user.args.isNotEmpty()){
            message.user.args.firstOrNull { it.first == "permissions" }?.second
        } else null


        if(!canUserRun(message.user, message.chat) && item == null){
            return lowRank();
        }else if(!canUserRun(message.user, message.chat) && item != null){
            if(item != "broadcaster" && item != "moderator"){
                return ReplyMessage("You don't have the rank necessary to do this; get someone with rank 5 or higher (or one of the stream moderators/the broadcaster) to do it.", true)
            }
            //The user would normally not be allowed to run the command, but since they're a moderator or the broadcaster, they get
            //a bigger say in leaving the channel.
        }

        val twitch = CommandCenter.bot.getChatByName("twitch") ?: return ReplyMessage("Twitch is not set up.", true)
        val where = splitCommand(message.content)["content"] ?: return ReplyMessage("You have to tell me where to leave", true)

        if(twitch !is TwitchChat)
            return ReplyMessage("The chat instance with the name \"twitch\" is not an instance of \"TwitchChat\". TL;DR: I'm broken", true)

        if(!twitch.doesChannelExist(where))
            return ReplyMessage("That channel does not appear to exist", true)

        if(!twitch.isInChannel(where))
            return ReplyMessage("I'm not there", true)

        val result = twitch.leaveChannel(where)
        if(!result)
            return ReplyMessage("Something went wrong when trying to leave.", true)
        return ReplyMessage("Left $where.", true)
    }
}