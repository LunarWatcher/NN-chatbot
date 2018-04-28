package io.github.lunarwatcher.chatbot.bot.commands

import io.github.lunarwatcher.chatbot.bot.chat.BMessage
import io.github.lunarwatcher.chatbot.bot.command.CommandCenter
import io.github.lunarwatcher.chatbot.bot.sites.twitch.TwitchChat

class JoinTwitch : AbstractCommand("joinTwitch", listOf("join-twitch"), rankRequirement = 5){
    override fun handleCommand(input: String, user: User): BMessage? {
        if(!canUserRun(user)){
            return lowRank();
        }

        val twitch = CommandCenter.bot.getChatByName("twitch") ?: return BMessage("Twitch is not set up.", true)

        val where = splitCommand(input)["content"] ?: return BMessage("You have to tell me where to join", true)
        if(twitch !is TwitchChat)
            return BMessage("The chat instance with the name \"twitch\" is not an instance of \"TwitchChat\". TL;DR: I'm broken", true)

        if(!twitch.doesChannelExist(where))
            return BMessage("That channel does not appear to exist", true)

        if(twitch.isInChannel(where))
            return BMessage("I'm already there", true)

        val result = twitch.joinChannel(where)
        if(!result)
            return BMessage("Something went wrong when trying to join", true)
        return BMessage("Joined $where!", true)
    }
}

class LeaveTwitch : AbstractCommand("leaveTwitch", listOf("leave-twitch"), rankRequirement = 5){
    override fun handleCommand(input: String, user: User): BMessage? {
        val item = if(user.args.isNotEmpty()){
             user.args.firstOrNull { it.first == "permissions" }?.second
        } else null


        if(!canUserRun(user) && item == null){
            return lowRank();
        }else if(!canUserRun(user) && item != null){
            if(item != "broadcaster" && item != "moderator"){
                return BMessage("You don't have the rank necessary to do this; get someone with rank 5 or higher (or one of the stream moderators/the broadcaster) to do it.", true)
            }
            //The user would normally not be allowed to run the command, but since they're a moderator or the broadcaster, they get
            //a bigger say in leaving the channel.
        }

        val twitch = CommandCenter.bot.getChatByName("twitch") ?: return BMessage("Twitch is not set up.", true)
        val where = splitCommand(input)["content"] ?: return BMessage("You have to tell me where to leave", true)

        if(twitch !is TwitchChat)
            return BMessage("The chat instance with the name \"twitch\" is not an instance of \"TwitchChat\". TL;DR: I'm broken", true)

        if(!twitch.doesChannelExist(where))
            return BMessage("That channel does not appear to exist", true)

        if(!twitch.isInChannel(where))
            return BMessage("I'm not there", true)

        val result = twitch.leaveChannel(where)
        if(!result)
            return BMessage("Something went wrong when trying to leave.", true)
        return BMessage("Left $where.", true)
    }
}