package io.github.lunarwatcher.chatbot.bot.listener

import io.github.lunarwatcher.chatbot.bot.chat.BMessage
import io.github.lunarwatcher.chatbot.bot.command.CommandCenter.Companion.TRIGGER
import io.github.lunarwatcher.chatbot.bot.commands.ARGUMENT_PATTERN
import io.github.lunarwatcher.chatbot.bot.commands.FLAG_REGEX
import io.github.lunarwatcher.chatbot.bot.commands.User

//TODO add listeners
interface Listener{
    val name: String;
    val description: String;

    fun handleInput(input: String, user: User) : BMessage?;
}

abstract class AbstractListener(override val name: String, override val description: String) : Listener{


    fun isCommand(input: String) : Boolean = input.startsWith(TRIGGER)

    fun splitCommand(input: String) : Map<String, String>{
        val iMap = parseArguments(input);
        val initialSplit = input.split(FLAG_REGEX.toRegex())[0];
        val name = initialSplit.split(" ")[0];
        if(name == input){
            //No arguments, just the name
            return mapOf("name" to name)
        }
        val content = initialSplit.substring(name.length + 1/*avoid the space*/)
        val dMap = mutableMapOf("name" to name, "content" to content)

        if(initialSplit == input){
            return dMap
        }

        //The argument map is empty, meaning the argument parser failed to find any. Just return the name and content
        if(iMap.isEmpty())
            return dMap;

        for(e in iMap)
            dMap.put(e.key, e.value)


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

                retval[g1.substring(1, g1.length)] = g2.substring(1, g2.length)
            }
        }

        return retval;
    }
}

