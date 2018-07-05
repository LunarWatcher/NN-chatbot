package io.github.lunarwatcher.chatbot.bot.commands.basic

import com.google.common.base.Strings
import io.github.lunarwatcher.chatbot.NO_DEFINED_RANK
import io.github.lunarwatcher.chatbot.bot.ReplyBuilder
import io.github.lunarwatcher.chatbot.bot.chat.Message
import io.github.lunarwatcher.chatbot.bot.chat.ReplyMessage
import io.github.lunarwatcher.chatbot.bot.command.CommandCenter
import io.github.lunarwatcher.chatbot.bot.commands.AbstractCommand
import io.github.lunarwatcher.chatbot.bot.commands.ICommand
import io.github.lunarwatcher.chatbot.bot.commands.learn.LearnedCommand
import io.github.lunarwatcher.chatbot.getMaxLen
import io.github.lunarwatcher.chatbot.utils.Utils


class HelpCommand : AbstractCommand("help", listOf("halp", "hilfen", "help"),
        "Lists all the commands the bot has",
        "Use `" + CommandCenter.TRIGGER + "help` to list all the commands and `" + CommandCenter.TRIGGER + "help [command name]` to get more info about a specifc command\n" +
                "Call `${CommandCenter.TRIGGER}help trucated` for a small version of the help command, or `${CommandCenter.TRIGGER}help full` for the full version. " +
                "Note that not passing full or trucated leads to it defaulting to the site specific settings", rankRequirement = 1){

    override fun handleCommand(message: Message): List<ReplyMessage>? {

        val center = message.chat.commands

        val `in` = splitCommand(message.content)
        if(`in`.size == 1 ||
                (`in`.size == 2 && `in`.containsKey("content")
                        && (`in`["content"] == "truncated" || `in`["content"] == "full")
                        )){
            val content = `in`["content"]
            val truncated = if(content == null) message.chat.truncated
            else content == "truncated"

            var commands: MutableMap<String, String> = mutableMapOf()
            val learnedCommands: MutableList<String> = mutableListOf()
            var listeners: MutableMap<String, String> = mutableMapOf();

            if (!message.chat.commands.getCommands(message.chat).isEmpty()) {

                for (command: ICommand in center.getCommands(message.chat)) {
                    commands[command.name] = command.desc;
                }
            }

            if (!CommandCenter.tc.commands.isEmpty()) {
                for (cmd: LearnedCommand in CommandCenter.tc.commands) {
                    learnedCommands.add(cmd.name)
                }
            }

            if (!center.listeners.isEmpty()) {
                for (listener in center.listeners) {
                    listeners[listener.name] = listener.description;
                }
            }

            listeners = listeners.toSortedMap()
            commands = commands.toSortedMap()

            if(!truncated) {

                val reply = ReplyBuilder(message.chat.name == "discord");
                reply.fixedInput().append("###################### Help ######################")
                        .nl().fixedInput().nl();
                val names: MutableList<String> = mutableListOf()

                names.addAll(commands.keys);
                names.addAll(learnedCommands.toSet())
                names.addAll(listeners.keys);

                val maxLen = getMaxLen(names);

                if (!commands.isEmpty()) {
                    reply.fixedInput().append("==================== Commands").nl()
                    for (command in commands) {
                        reply.fixedInput().append(CommandCenter.TRIGGER + command.key);
                        reply.append(Strings.repeat(" ", maxLen - command.key.length + 2) + "| ")
                                .append(command.value).nl();
                    }
                }

                if (!learnedCommands.isEmpty()) {
                    reply.fixedInput().append("==================== Learned Commands").nl()
                    reply.fixedInput();
                    for (i in 0 until CommandCenter.tc.commands.size) {
                        val command = CommandCenter.tc.commands[i]
                        if (command.nsfw && !message.nsfwSite) {
                            continue
                        }

                        reply.append(command.name);
                        if (command.nsfw)
                            reply.append(" - NSFW");

                        if (i < CommandCenter.tc.commands.size - 1) reply.append(", ")
                    }

                }
                reply.fixedInput().newLine()
                if (!listeners.isEmpty()) {
                    reply.fixedInput().append("==================== Listeners").newLine()

                    for (listener in listeners) {
                        reply.fixedInput().append(listener.key);
                        reply.append(Strings.repeat(" ", maxLen - listener.key.length + 2) + "| ")
                                .append(listener.value).nl();
                    }
                }
                return listOf(ReplyMessage(reply.toString(), false));
            }else{
                val builder = ReplyBuilder()
                builder.append("**Commands**: ")
                builder.append(commands.keys.joinToString(", "))
                        .append(". **User taught commands:** ")
                        .append(learnedCommands.toSortedSet().joinToString(", "))
                        .append(". **Listeners**: " + listeners.keys.joinToString(", "))
                return listOf(ReplyMessage(builder.toString(), false));
            }
        }else{
            val cmd = (`in`["content"] ?: return listOf(ReplyMessage("in[content] == null. /cc @Zoe", false))).toLowerCase();
            val desc: String
            val help: String
            val name: String
            val aliases: String
            //No clue what to call this thing
            val d: String;
            val rank: Int
            val nsfw: Boolean
            val chat = message.chat
            when {
                center.isBuiltIn(cmd, chat) -> {
                    desc = center[cmd, chat]!!.desc;
                    help = center[cmd, chat]!!.help;
                    name = center[cmd, chat]!!.name;
                    val aliasBuffer = center[cmd, message.chat]!!.aliases
                    aliases = if(aliasBuffer.isEmpty())
                        "None"
                    else
                        aliasBuffer.joinToString(", ")
                    rank = center[cmd, chat]!!.rankRequirement

                    d = "Built in command. "
                    nsfw = center[cmd, chat]!!.nsfw
                }
                CommandCenter.tc.doesCommandExist(cmd) -> {
                    desc = CommandCenter.tc.get(cmd)!!.desc;
                    help = CommandCenter.tc.get(cmd)!!.help;
                    name = CommandCenter.tc.get(cmd)!!.name;
                    d = "Taught command. (Taught to the bot by user " + CommandCenter.tc.get(cmd)?.creator + " on " + CommandCenter.tc.get(cmd)?.site + "). "
                    aliases = "None. "
                    rank = 1
                    nsfw = CommandCenter.tc.get(cmd)!!.nsfw
                }

                else -> return listOf(ReplyMessage("The command you tried finding help for (`$cmd`) does not exist. Make sure you've got the name right", true))
            }

            val reply = ReplyBuilder(message.chat.name == "discord");

            if(nsfw && !message.nsfwSite){
                return listOf(ReplyMessage("The command is not available here", true))
            }

            reply.fixedInput().append(d).append("`${CommandCenter.TRIGGER}")
                    .append(name)
                    .append(if(nsfw) "` (NSFW)" else "`")
                    .append(": $desc")
                    .nl().fixedInput().append(help)
                    .nl().fixedInput().append("Known aliases: $aliases")
                    .nl().fixedInput().append("Rank required: " +
                            "${if(rank == NO_DEFINED_RANK)
                                "1 (WARNING: Undefined in code. Actual rank may differ from listed)"
                            else rank.toString()} " +
                            "(your rank: ${Utils.getRank(message.user.userID, message.chat.config)})")


            return listOf(ReplyMessage(reply.toString(), true));
        }
    }

}