package io.github.lunarwatcher.chatbot.bot.commands.meta

import io.github.lunarwatcher.chatbot.bot.chat.Message
import io.github.lunarwatcher.chatbot.bot.chat.ReplyMessage
import io.github.lunarwatcher.chatbot.bot.command.CommandCenter
import io.github.lunarwatcher.chatbot.bot.commands.AbstractCommand
import io.github.lunarwatcher.chatbot.utils.Utils

class ChangeCommandStatus(val center: CommandCenter) : AbstractCommand("declare", listOf(), "Changes a commands status. Only commands available on the site can be edited"){
    override fun handleCommand(message: Message): ReplyMessage? {
        if(Utils.getRank(message.user.userID, message.chat.config) < 7){
            return ReplyMessage("I'm afraid I can't let you do that, User", true);
        }
        try {
            val content = splitCommand(message.content)["content"] ?: return ReplyMessage("Which command do you want to change, and what's the new state?", true)

            val args = content.split(" ");
            if(args.size != 2){
                return ReplyMessage("Usage: ${CommandCenter.TRIGGER}declare commandname (n)sfw", true)
            }
            val command = args[0];

            val newState: String = args[1];
            val actual: Boolean
            actual = when (newState) {
                "sfw" -> false
                "nsfw" -> true
                else -> newState.toBoolean()
            };

            if (center.isBuiltIn(command, message.chat)) {
                System.out.println(command);
                return ReplyMessage("You can't change the status of included commands.", true);
            }
            if (CommandCenter.tc.doesCommandExist(command)) {
                CommandCenter.tc.commands.forEach{
                    if(it.name == command) {
                        if(it.nsfw == actual){
                            return ReplyMessage("The status was already set to " + (if (actual) "NSFW" else "SFW"), true);
                        }
                        it.nsfw = actual;

                        return ReplyMessage("ICommand status changed to " + (if (actual) "NSFW" else "SFW"), true);
                    }
                }
            } else {
                return ReplyMessage("The command doesn't exist.", true);
            }
        }catch(e:ClassCastException){
            return ReplyMessage("Something just went terribly wrong. Sorry 'bout that", true);
        }catch(e: IndexOutOfBoundsException){
            return ReplyMessage("Not enough arguments. I need the command name and new state", true);
        }
        return ReplyMessage("This is in theory unreachable code. If you read this message something bad happened", true);
    }
}
