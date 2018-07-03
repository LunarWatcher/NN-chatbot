package io.github.lunarwatcher.chatbot.bot.commands

import io.github.lunarwatcher.chatbot.*
import io.github.lunarwatcher.chatbot.bot.chat.ReplyMessage
import io.github.lunarwatcher.chatbot.bot.command.CommandGroup
import io.github.lunarwatcher.chatbot.bot.sites.Chat
import io.github.lunarwatcher.chatbot.utils.Utils
import me.philippheuer.twitch4j.message.commands.Command

/**
 * Utility implementation of [Command]
 */
abstract class AbstractCommand(override val name: String, override val aliases: List<String>,
                               override val desc: String = Constants.NO_DESCRIPTION,
                               override val help: String = Constants.NO_HELP,
                               override val rankRequirement: Int = NO_DEFINED_RANK,
                               override val nsfw: Boolean = false,
                               override var commandGroup: CommandGroup = CommandGroup.COMMON) : ICommand {

    init {
        @Suppress("LeakingThis")
        if (rankRequirement != NO_DEFINED_RANK) {
            if (rankRequirement < 0 || rankRequirement > 10)
                throw IllegalArgumentException("The rank requirement must be between 0")
        }
    }

    override fun matchesCommand(input: String): Boolean{
        val input = input.toLowerCase();
        val split = input.split(" ");
        if(split[0].toLowerCase() == name.toLowerCase()){
            return true;
        }

        return aliases.any{split[0].toLowerCase() == it.toLowerCase()}
    }

    /**
     * This would be considerably easier to integrate into a centralized dispatch system (where commands are refered through
     * method instances instead of declaring entire classes for them), but rewriting would take a lot of time. So this
     * method exists to enable classes where ranks appy to easily check for validity vs [rankRequirement]
     */
    override fun canUserRun(user: User, chat: Chat) : Boolean {
        if(rankRequirement == NO_DEFINED_RANK && !Utils.isBanned(user.userID, chat.config)){
            // rank not defined (req == 1) && user !banned - can run
            return true;
        }else if(rankRequirement != NO_DEFINED_RANK){
            return Utils.getRank(user.userID, chat.config) >= rankRequirement
        }
        return false
    }
    fun splitCommand(input: String) : Map<String, String>{
        val iMap = parseArguments(input);

        val initialSplit = input.split(FLAG_REGEX.toRegex()).joinToString(" ").trim();
        val name = initialSplit.split(" ")[0];

        if(name == input){
            //No arguments, just the name
            return mapOf("name" to name.trim())
        }

        val dMap = try{
            val content = initialSplit.substring(name.length + 1/*avoid the space*/).replace(FLAG_REGEX, "")
            mutableMapOf("name" to name.trim(), "content" to content.trim())
        }catch(e: StringIndexOutOfBoundsException){
            mutableMapOf("name" to name.trim())
        }

        if(initialSplit == input){
            return dMap
        }

        //The argument map is empty, meaning the argument parser failed to find any. Just return the name and content
        if(iMap.isEmpty())
            return dMap;

        for(e in iMap)
            dMap[e.key.trim()] = e.value.trim()


        return dMap;

    }

    fun parseArguments(input: String) : Map<String, String>{
        if (input.isEmpty())
            return mapOf()
        val used = input.split(" ")[0]
        if(input.substring(used.length).trim().isEmpty()){
            //no arguments passed
            return mapOf();
        }

        val retval: MutableMap<String, String> = mutableMapOf()
        val matcher = ARGUMENT_PATTERN.matcher(input);
        while(matcher.find()){
            val groups = matcher.groupCount()
            val g1 = matcher.group(1)
            if(groups == 2){
                val g2 = matcher.group(2);
                retval[g1.substring(1, g1.length).prep()] = g2?.substring(1, g2.length)?.trim() ?: "true"
            }else if(groups == 1){
                retval[g1.substring(1, g1.length).prep()] = "true"
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

    fun lowRank() : ReplyMessage
            = ReplyMessage("You need rank $rankRequirement or higher ot use this command.", true)

    fun lowRank(reason: String) : ReplyMessage
            = ReplyMessage("You need rank $rankRequirement or higher ot use this command. Supplied reason: $reason", true)
}
