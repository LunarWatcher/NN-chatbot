package io.github.lunarwatcher.chatbot.bot.commands

import io.github.lunarwatcher.chatbot.bot.chat.BMessage

class FollowThanks : AbstractCommand("follow-thanks", listOf("followThx", "followThanks"),
        help="Takes an argument; whether or not to enable. Only settable by stream broadcaster or rank 8 users",
        rankRequirement = 8){
    override fun handleCommand(input: String, user: User): BMessage? {
        if(!canUserRun(user)){
            val cache = user.args.firstOrNull{it.first == "permissions"}

            if(cache != null){

            }else{
                return BMessage("I'm afraid I can't let you do that, User.", true)
            }
        }
        TODO("Not finished")
    }
}