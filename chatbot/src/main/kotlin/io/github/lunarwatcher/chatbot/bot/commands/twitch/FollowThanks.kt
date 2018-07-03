package io.github.lunarwatcher.chatbot.bot.commands.twitch

import io.github.lunarwatcher.chatbot.bot.chat.Message
import io.github.lunarwatcher.chatbot.bot.chat.ReplyMessage
import io.github.lunarwatcher.chatbot.bot.commands.AbstractCommand

class FollowThanks : AbstractCommand("follow-thanks", listOf("followThx", "followThanks"),
        help="Takes an argument; whether or not to enable. Only settable by stream broadcaster or rank 8 users",
        rankRequirement = 8){
    override fun handleCommand(message: Message): ReplyMessage? {
        if(!canUserRun(message.user, message.chat)){
            val cache = message.user.args.firstOrNull{it.first == "permissions"}

            if(cache != null){

            }else{
                return ReplyMessage("I'm afraid I can't let you do that, User.", true)
            }
        }
        TODO("Not finished")
    }
}