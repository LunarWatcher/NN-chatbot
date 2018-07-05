package io.github.lunarwatcher.chatbot.bot.commands.learn

import io.github.lunarwatcher.chatbot.bot.chat.Message
import io.github.lunarwatcher.chatbot.bot.chat.ReplyMessage
import io.github.lunarwatcher.chatbot.bot.commands.AbstractCommand
import java.text.MessageFormat

class LearnedCommand(cmdName: String, cmdDesc: String = "No description supplied",
                     val output: String, val reply: Boolean, val creator: Long,
                     override var nsfw: Boolean = false, val site: String, val disableInput: Boolean = false)
    : AbstractCommand(cmdName, listOf(), cmdDesc, "This is a learned command and does not have help"){

    override fun handleCommand(message: Message): List<ReplyMessage>? {
        var output = this.output;

        output = output.replace(USERNAME_REGEX, message.user.userName).replace(USERID_REGEX, message.user.userID.toString())

        if(!disableInput) {
            if (output.contains("\\{[0-9]+}".toRegex())) {
                println("Group is TRIGGERED AND SUPER OFFENDED SHØ!! :p");
                val items = (splitCommand(message.content)["content"]?.split(",")?.map { it.trim() }?.toTypedArray()
                        ?: arrayOf());
                println(items);
                output = MessageFormat.format(output, *items);
            } else if (output.contains("%s")) {
                println(output)
                val arguments = output.split("%s").size - 1
                val given = (splitCommand(message.content)["content"] ?: return listOf(ReplyMessage(
                        "You need $arguments ${if (arguments == 1) "argument" else "arguments"} to run this command",
                        true)))
                        .split(",");
                if (given.size != arguments) {
                    listOf(ReplyMessage("Not enough arguments. Found ${given.size}, requires $arguments", true))
                }
                output = output.format(*given.toTypedArray())

            }
        }


        if(message.chat.name == "discord"){
            output = output.replace("\\\\", "\\")
        }
        return listOf(ReplyMessage(output, reply));
    }

    companion object {
        val USERNAME_REGEX = """(?i)\\un""".toRegex();
        val USERID_REGEX = """(?i)\\uid""".toRegex();
    }

}