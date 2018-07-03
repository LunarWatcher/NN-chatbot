package io.github.lunarwatcher.chatbot.bot.commands.admin

import io.github.lunarwatcher.chatbot.bot.ReplyBuilder
import io.github.lunarwatcher.chatbot.bot.chat.Message
import io.github.lunarwatcher.chatbot.bot.chat.ReplyMessage
import io.github.lunarwatcher.chatbot.bot.command.CommandCenter
import io.github.lunarwatcher.chatbot.bot.commands.AbstractCommand
import io.github.lunarwatcher.chatbot.utils.Utils

class WhoMade : AbstractCommand("whoMade", listOf("creatorof"), "Gets the user ID of the user who created a command"){
    override fun handleCommand(message: Message): ReplyMessage? {
        ;
        try {
            val arg = splitCommand(message.content);

            println(arg)
            if(arg.size < 2)
                return ReplyMessage("Missing arguments!", true);

            if(CommandCenter.INSTANCE.isBuiltIn(arg["content"], message.chat)){
                return ReplyMessage("It's a built-in command, meaning it was made by the project developer(s)", true);
            }

            if(CommandCenter.tc.doesCommandExist(arg["content"] ?: return null)){
                CommandCenter.tc.commands
                        .forEach {
                            if(it.name == arg["content"])
                                return ReplyMessage("The command `" + arg["content"] + "` was made by a user with the User ID "
                                        + grabLink(it.creator, it.site) + ". The command was created on " +
                                        (if (it.site == "Unknown") "an unknown site"
                                        else it.site) + ".", true)
                        }
            }
        }catch(e: ClassCastException){
            return ReplyMessage(e.message, false);
        }

        return ReplyMessage("That command doesn't appear to exist.", true);
    }

    fun grabLink(id: Long, site: String): String{
        return when(site){
            "discord" -> "<@$id>"
            "stackexchange" -> "[$id](https://stackexchange.com/users/$id)"
            "stackoverflow" -> "[$id](https://stackoverflow.com/users/$id)"
            "metastackexchange" -> "[$id](https://meta.stackexchange.com/users/$id)"
            else -> id.toString()
        }
    }
}

class DebugRanks : AbstractCommand("rankdebug", listOf(), "Debugs ranks", rankRequirement = 1){
    override fun handleCommand(message: Message): ReplyMessage? {
        val site = message.chat

        val reply = ReplyBuilder(site.name == "discord")
        reply.fixedInput().append("Username - user ID - rank").nl();
        for(rankInfo in site.config.ranks){
            val ri = rankInfo.value;
            reply.fixedInput().append(ri.uid).append(" - ").append(ri.rank).nl()
        }

        return ReplyMessage(reply.toString(), false);
    }
}

class NPECommand : AbstractCommand("npe", listOf(), "Throws an NPE"){
    override fun handleCommand(message: Message): ReplyMessage?{
        val site = message.chat
        if(Utils.getRank(message.user.userID, site.config) < 10)
            return ReplyMessage("Rank 10 only! This feature could potentially kill the bot, which is why rank 10 is required", true)
        throw NullPointerException("Manually requested exception from NPECommand")
    }
}
