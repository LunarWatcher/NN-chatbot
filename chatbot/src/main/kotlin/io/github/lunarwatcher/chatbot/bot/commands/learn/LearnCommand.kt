package io.github.lunarwatcher.chatbot.bot.commands.learn

import io.github.lunarwatcher.chatbot.bot.chat.Message
import io.github.lunarwatcher.chatbot.bot.chat.ReplyMessage
import io.github.lunarwatcher.chatbot.bot.command.CommandCenter
import io.github.lunarwatcher.chatbot.bot.commands.AbstractCommand
import io.github.lunarwatcher.chatbot.utils.Utils

class LearnCommand(val commands: TaughtCommands, val center: CommandCenter) : AbstractCommand("learn", listOf("teach"), "Teaches the bot a new command. ",
        help = "Syntax: ${CommandCenter.TRIGGER}help commandName commandOutput -d, -desc, or -description (optional) description -nsfw (optional) whether the command is NSFW or not (boolean). -reply (optional) whether or not the message should reply to the person who uses it. -noArgs (optional) whether or not to ignore input even if the command input items (%s or {id}) are present.\n" +
                "Symbols:\n" +
                "* %s - requires input for the command to be used. Alternatively {number}, where \"number\" is replaced with the ID of the supplied item. It should increment from 0 (the number corresponds with the item in the supplied arguments)\n" +
                "* \\un - adds the username for whoever uses the command\n" +
                "* \\ping - pings the user running the command" +
                "* \\uid - adds the user ID for whoever uses the command", rankRequirement = 1){

    override fun handleCommand(message: Message): List<ReplyMessage>? {
        var nsfw = false;

        //on Discord there may appear commands that one would consider unwanted on sites like the Stack Exchange network.
        //These are just called NSFW because I have nothing better to call them.
        //If they are manually added as SFW they will appear everywhere. Otherwise, only some sites will have access
        if(message.chat.name == "discord") {
            nsfw = true
        }
        val args = splitCommand(message.content);

        println(args)
        if(args.size < 2)
            listOf(ReplyMessage("You have to supply at least two arguments!", true));

        val split = (args["content"] ?: return listOf(ReplyMessage("Supply valid arguments -_-", true)))
                .split(" ".toRegex(), limit = 2);

        if(split.size != 2){
            listOf(ReplyMessage("Not enough arguments", true));
        }

        var name = "undefined";
        var desc = "No description was supplied";
        val creator = message.user.userID;
        var output = "undefined";
        var reply = false;
        var a = false

        for(i in 0 until args.size){
            val key = args.keys.elementAt(i);
            when (key) {
                "content" -> { //The name is the name of the command used, not the one that's attempted learned.
                    name = args["content"]!!.split(" ", limit = 2)[0]
                    output = args["content"]!!.split(" ", limit = 2)[1]
                }
                "--d" -> desc = args["--d"] ?: "No description supplied"
                "--desc" -> desc = args["--desc"] ?: "No description supplied"
                "--description" -> desc = args["--description"] ?: "No description supplied"
                "--nsfw" -> {
                    nsfw = try {
                        (args["--nsfw"] ?: (message.chat.name == "discord")).toString().toBoolean()
                    }catch(e: Exception){
                        message.chat.name == "discord";
                    }
                }
                "--reply" ->{
                    reply = try{
                        args["--reply"]?.toBoolean() ?: false
                    }catch(e: Exception){
                        false
                    }
                }
                "--noArgs" ->{
                    a = try{
                        (args["--noArgs"]?.toBoolean() ?: false)
                    }catch(e: Exception){
                        false
                    }
                }
            }
        }

        if(name == "undefined" || output == "undefined")
            listOf(ReplyMessage("Something went wrong. ICommand not added", true))

        if(commands.doesCommandExist(name) || center.isBuiltIn(name, message.chat)){
            listOf(ReplyMessage("That command already exists", true));
        }

        if(output.contains(percentageArgs) && output.contains(bracketArgs) && !a){
            listOf(ReplyMessage("Warning: ambiguous arguments detected (%s and {[0-9]+} formats were detected). This is not compatible with the system. Append --noArgs after the command output (with a space between) to disable input.", true));
        }

        if(!a){
            if(output.contains(invalidArguments))
                listOf(ReplyMessage("Your message contains input arguments in the % format that are invalid. Use %s for all input types, use {itemId} for all formatting (where itemId is a number starting at 0), or add --noArgs if you don't want the message to take input.", true));
        }

        commands.addCommand(LearnedCommand(name, desc, output, reply, creator, nsfw, message.chat.name, a));
        center.refreshBuckets();
        return listOf(ReplyMessage(Utils.getRandomLearnedMessage(), true));
    }

    companion object {
        val invalidArguments = "(?i)%[^s]".toRegex();
        val bracketArgs = "\\{[0-9]+}".toRegex();
        val percentageArgs = "%s".toRegex()
    }
}
