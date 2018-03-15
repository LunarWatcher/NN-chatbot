package io.github.lunarwatcher.chatbot.bot.commands

import com.google.common.base.Strings.repeat
import io.github.lunarwatcher.chatbot.Configurations
import io.github.lunarwatcher.chatbot.Constants
import io.github.lunarwatcher.chatbot.bot.ReplyBuilder
import io.github.lunarwatcher.chatbot.bot.chat.BMessage
import io.github.lunarwatcher.chatbot.bot.command.CommandCenter
import io.github.lunarwatcher.chatbot.bot.command.CommandCenter.TRIGGER
import java.awt.SystemColor.info
import java.text.SimpleDateFormat

import java.util.regex.Pattern

val FLAG_REGEX = "( -[a-zA-Z]+)([a-zA-Z ]+(?!-\\w))"
var ARGUMENT_PATTERN = Pattern.compile(FLAG_REGEX)

interface Command{
    val name: String;
    val aliases: List<String>
    val desc: String;
    val help: String;

    /**
     * Check if the input starts with the name or one of the command's aliases
     */
    fun matchesCommand(input: String) : Boolean;
    /**
     * Handle a given command
     */
    fun handleCommand(input: String, user: User) : BMessage?;

}

/**
 * Info about a user.
 */
class User(var site: String, var userID: Long, var userName: String, var roomID: Int, var nsfwSite: Boolean = false);

/**
 * Utility implementation of [Command]
 */
abstract class AbstractCommand(override val name: String, override val aliases: List<String>,
                               override val desc: String = Constants.NO_DESCRIPTION,
                               override val help: String = Constants.NO_HELP) : Command{

    override fun matchesCommand(input: String): Boolean{
        val input = input.toLowerCase();
        val split = input.split(" ");
        if(split[0] == name.toLowerCase()){
            return true;
        }

        return aliases.any{split[0] == it.toLowerCase()}
    }

    fun splitCommand(input: String) : Map<String, String>{
        val iMap = parseArguments(input);
        val initialSplit = input.split(FLAG_REGEX.toRegex())[0];
        val name = initialSplit.split(" ")[0];
        if(name == input){
            //No arguments, just the name
            return mapOf("name" to name.trim())
        }
        val content = initialSplit.substring(name.length + 1/*avoid the space*/)
        val dMap = mutableMapOf("name" to name.trim(), "content" to content.trim())

        if(initialSplit == input){
            return dMap
        }

        //The argument map is empty, meaning the argument parser failed to find any. Just return the name and content
        if(iMap.isEmpty())
            return dMap;

        for(e in iMap)
            dMap.put(e.key.trim(), e.value.trim())


        return dMap;

    }

    fun parseArguments(input: String) : Map<String, String>{
        if(input.substring(name.length).isEmpty()){
            //no arguments passed
            return mapOf();
        }

        val retval: MutableMap<String, String> = mutableMapOf()
        val matcher = ARGUMENT_PATTERN.matcher(input);
        while(matcher.find()){
            val groups = matcher.groupCount()
            if(groups == 2){
                val g1 = matcher.group(1)
                val g2 = matcher.group(2);

                retval.put(g1.substring(1, g1.length), g2.substring(1, g2.length))
            }
        }

        return retval;
    }

    fun removeName(input: String) : String{
        if(input.substring(name.length).startsWith(" ")){
            //If the name is removed successfully, the string should start with a space
            return input.substring(name.length);
        }

        //otherwise it's an alias

        aliases.forEach { alias->
            if(input.substring(alias.length).startsWith(" ")){
                return input.substring(alias.length);
            }
        }
        //If it can't find in either the name or the alias, that means the command doesn't match.
        //Since this is checked with the matchesCommand check, this last line here should never
        //be called.
        return input;
    }

}

class HelpCommand(var center: CommandCenter) : AbstractCommand("help", listOf(),
        "Lists all the commands the bot has",
        "Use `" + CommandCenter.TRIGGER + "help` to list all the commands and `" + CommandCenter.TRIGGER + "help [command name]` to get more info about a specifc command"){

    override fun handleCommand(input: String, user: User): BMessage? {
        if(!matchesCommand(input)){
            return null;
        }

        val `in` = splitCommand(input)
        if(`in`.size == 1) {

            //No arguments supplied
            val reply = ReplyBuilder(center.site.name == "discord");
            reply.fixedInput().append("###################### Help ######################")
                    .nl().fixedInput().nl();
            val commands: MutableMap<String, String> = mutableMapOf()
            val learnedCommands: MutableMap<String, String> = mutableMapOf()
            val listeners: MutableMap<String, String> = mutableMapOf();

            val names: MutableList<String> = mutableListOf()

            if (!center.commands.isEmpty()) {

                for (command: Command in center.commands.values) {
                    commands.put(command.name, command.desc);
                }
            }

            if (!CommandCenter.tc.commands.isEmpty()) {
                for (cmd: LearnedCommand in CommandCenter.tc.commands.values) {
                    learnedCommands.put(cmd.name, cmd.desc)
                }
            }

            if(!center.listeners.isEmpty()){
                for(listener in center.listeners){
                    listeners.put(listener.name, listener.description);
                }
            }

            names.addAll(commands.keys);
            names.addAll(learnedCommands.keys)
            names.addAll(listeners.keys);

            val maxLen = getMaxLen(names);

            if (!commands.isEmpty()) {
                reply.fixedInput().append("==================== Commands").nl()
                for (command in commands) {
                    reply.fixedInput().append(TRIGGER + command.key);
                    reply.append(repeat(" ", maxLen - command.key.length + 2) + "| ")
                            .append(command.value).nl();
                }
            }

            if (!learnedCommands.isEmpty()) {
                reply.fixedInput().append("==================== Learned Commands").nl()
                for (cmd in CommandCenter.tc.commands.entries) {
                    val command: LearnedCommand = cmd.value;

                    if (command.nsfw && !user.nsfwSite) {
                        continue;
                    }

                    reply.fixedInput().append(TRIGGER + command.name);
                    reply.append(repeat(" ", maxLen - command.name.length + 2) + "| ")
                            .append(command.desc);
                    if (command.nsfw)
                        reply.append(" - NSFW");
                    reply.nl();
                }
            }

            if(!listeners.isEmpty()){
                reply.fixedInput().append("==================== Listeners").newLine()

                for(listener in listeners){
                    reply.fixedInput().append(listener.key);
                    reply.append(repeat(" ", maxLen - listener.key.length + 2) + "| ")
                            .append(listener.value).nl();
                }
            }
            return BMessage(reply.toString(), false);
        }else{
            val cmd = (`in`["content"] ?: return null).toLowerCase();
            var desc: String
            var help: String
            var name: String
            //No clue what to call this thing
            var d: String;

            if(center.isBuiltIn(cmd)){
                desc = center.get(cmd)?.desc ?: return null;
                help = center.get(cmd)?.help ?: return null;
                name = center.get(cmd)?.name ?: return null;
                d = "Built in command. "
            }else if(CommandCenter.tc.doesCommandExist(cmd)){
                desc = CommandCenter.tc.get(cmd)?.desc ?: return null;
                help = CommandCenter.tc.get(cmd)?.help ?: return null;
                name = CommandCenter.tc.get(cmd)?.name ?: return null;
                d = "Taught command. (Learned to the bot by user " + CommandCenter.tc.get(cmd)?.creator + " on " + CommandCenter.tc.get(cmd)?.site + "). "
            }else{
                return BMessage("The command you tried finding help for (`$cmd`) does not exist. Make sure you've got the name right", true)
            }

            val reply: ReplyBuilder = ReplyBuilder(center.site.name == "discord");

            reply.fixedInput().append(d).append("`" + TRIGGER).append(name).append("`: " + desc)
                    .nl().fixedInput().append(help)


            return BMessage(reply.toString(), true);
        }
    }

}

fun getMaxLen(list: MutableList<String>) : Int{
    var longest = 0;

    for(item in list){
        val len = item.length;
        if(len > longest)
            longest = len;
    }
    return longest;
}

class ShrugCommand(val shrug: String): AbstractCommand("shrug", listOf("dunno", "what"), "Shrugs", "Use `" + TRIGGER + "shrug` to use the command"){
    override fun handleCommand(input: String, user: User): BMessage? {
        if(!matchesCommand(input)){
            return null;
        }
        return BMessage(shrug, false);
    }
}

class AboutCommand : AbstractCommand("about", listOf("whoareyou"), "Let me tell you a little about myself...", "Use `" + TRIGGER + "about` to show the info"){

    override fun handleCommand(input: String, user: User): BMessage? {
        if(!matchesCommand(input)){
            return null;
        }

        val reply = ReplyBuilder();

        reply.append("Hiya! I'm Alisha, a chatbot designed by [${Configurations.CREATOR}](${Configurations.CREATOR_GITHUB}). ")
                .append("I'm open-source and the code is available on [Github](${Configurations.GITHUB}).")

        return BMessage(reply.toString(), true)
    }
}

class BasicPrintCommand(val print: String, name: String, aliases: List<String>, desc: String) : AbstractCommand(name, aliases, desc){
    override fun handleCommand(input: String, user: User): BMessage? {
        if(!matchesCommand(input))
            return null;
        return BMessage(print, false)
    }
}

class Alive : AbstractCommand("alive", listOf(), "Used to check if the bot is working"){
    override fun handleCommand(input: String, user: User): BMessage? {
        if(!matchesCommand(input))
            return null;

        return BMessage("I'm pretty sure I am.", true);
    }
}

class TimeCommand : AbstractCommand("time", listOf(), "What time is it?"){
    val formatter = SimpleDateFormat("a, d MMMM HH:mm:ss.SSSS Y X z (Z)")
    override fun handleCommand(input: String, user: User): BMessage? {
        return BMessage(formatter.format(System.currentTimeMillis()), true)
    }
}