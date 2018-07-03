package io.github.lunarwatcher.chatbot.bot.commands.learn

import io.github.lunarwatcher.chatbot.Constants
import io.github.lunarwatcher.chatbot.Database
import io.github.lunarwatcher.chatbot.bot.commands.ICommand

@Suppress("UNCHECKED_CAST", "UNUSED")
class TaughtCommands(val db: Database){

    val commands: MutableList<LearnedCommand>

    init{
        commands = mutableListOf()
        load()
    }

    fun doesCommandExist(name: String) : Boolean = commands.any{
        it.matchesCommand(name)
    }

    fun save(){

        /*
        The map has a key of the command name, and a map of the attributes contained in the LearnedCommand class
         */
        val map: MutableList<Map<String, Any?>> = mutableListOf()

        commands.forEach{it ->
            val cmdMap = mutableMapOf<String, Any?>()
            val lc: LearnedCommand = it

            cmdMap["name"] = lc.name;
            cmdMap["desc"] = lc.desc;
            cmdMap["output"] = lc.output;
            cmdMap["creator"] = lc.creator;
            cmdMap["reply"] = lc.reply;
            cmdMap["site"] = lc.site;
            cmdMap["nsfw"] = lc.nsfw
            cmdMap["disableInput"] = lc.disableInput
            map.add(cmdMap)

        }

        db.put("learned", map);
    }

    fun load(){
        val loaded: MutableList<Any>? = db.getList(Constants.LEARNED_COMMANDS);

        if(loaded == null){
            println("No learned commands found")
            return;
        }

        loaded.forEach{
            val map: MutableMap<String, Any?> = it as MutableMap<String, Any?>

            val name: String = map["name"] as String;
            val desc: String = map["desc"] as String;
            val output: String = map["output"] as String;
            val site: String = map["site"] as String? ?: "Unknown";
            val nsfw: Boolean = map["nsfw"] as Boolean? ?: true;
            val disableInput = map["disableInput"] as Boolean? ?: false
            //Since at this time there are some commands that have been created before this system was added, allow for the site
            //to be null and instead loaded to "unknown". This is not going to happen often so it isn't going to be a big problem
            //when the bot is actively used

            //Keep these in case a user-implemented database doesn't work as it is supposed to.
            val creator: Long = try{
                map["creator"] as Long;
            }catch(e: ClassCastException){
                (map["creator"] as Int).toLong();
            }
            val reply: Boolean = try{
                map["reply"] as Boolean
            } catch(e: ClassCastException){
                (map["reply"] as String).toBoolean();
            }

            addCommand(LearnedCommand(name, desc, output, reply, creator, nsfw, site, disableInput))
        }

        fun addCommand(c: ICommand) {
            if(c != LearnedCommand::class){
                return
            }
            commands.add(c as LearnedCommand)
        }
    }


    fun addCommand(command: LearnedCommand) = commands.add(command)
    fun removeCommand(command: LearnedCommand){
        removeCommand(command.name);
    }
    fun removeCommand(name: String){
        commands.remove(commands.firstOrNull{ it.name == name} ?: return)
    }
    fun removeCommands(creator: Long) {
        commands.removeIf{it.creator == creator}
    }

    fun commandExists(cmdName: String) : Boolean{
        return commands.any{it.name == cmdName}
    }

    fun get(cmdName: String) : LearnedCommand? {
        return commands.firstOrNull { it.name.toLowerCase() == cmdName.toLowerCase() } ?: commands.firstOrNull{ cmdName.toLowerCase() in it.aliases.map{ it.toLowerCase() } }
    }
}
