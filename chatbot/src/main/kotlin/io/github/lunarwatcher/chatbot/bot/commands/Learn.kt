@file:Suppress("NAME_SHADOWING")

package io.github.lunarwatcher.chatbot.bot.commands

import io.github.lunarwatcher.chatbot.Constants.LEARNED_COMMANDS
import io.github.lunarwatcher.chatbot.Database
import io.github.lunarwatcher.chatbot.MapUtils
import io.github.lunarwatcher.chatbot.bot.chat.BMessage
import io.github.lunarwatcher.chatbot.bot.command.CmdInfo
import io.github.lunarwatcher.chatbot.bot.command.CommandCenter
import io.github.lunarwatcher.chatbot.utils.Utils

@Suppress("UNCHECKED_CAST", "UNUSED")
class TaughtCommands(val db: Database){

    val commands: MutableMap<CmdInfo, LearnedCommand>

    init{
        commands = mutableMapOf()
        load()
    }

    fun doesCommandExist(name: String) : Boolean{
        return MapUtils.get(name, commands) != null;
    }

    fun save(){

        /*
        The map has a key of the command name, and a map of the attributes contained in the LearnedCommand class
         */
        val map: MutableList<Map<String, Any?>> = mutableListOf()

        commands.forEach{
            val cmdMap = mutableMapOf<String, Any?>()
            val lc: LearnedCommand = it.value;

            cmdMap["name"] = lc.name;
            cmdMap["desc"] = lc.desc;
            cmdMap["output"] = lc.output;
            cmdMap["creator"] = lc.creator;
            cmdMap["reply"] = lc.reply;
            cmdMap["site"] = lc.site;
            cmdMap["nsfw"] = lc.nsfw
            map.add(cmdMap)

        }

        db.put("learned", map);
    }

    fun load(){
        val loaded: MutableList<Any>? = db.getList(LEARNED_COMMANDS);

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

            addCommand(LearnedCommand(name, desc, output, reply, creator, nsfw, site))
        }

        fun addCommand(c: Command) {
            if(c != LearnedCommand::class){
                return
            }
            val name = c.name
            val aliases = c.aliases
            commands.putIfAbsent(CmdInfo(name, aliases), c as LearnedCommand)
        }
    }

    fun addCommand(command: LearnedCommand) = addCommand(CmdInfo(command.name, command.aliases), command)

    fun addCommand(cmdInfo: CmdInfo, command: LearnedCommand) = commands.put(cmdInfo, command)
    fun removeCommand(command: LearnedCommand){
        removeCommand(command.name);
    }
    fun removeCommand(name: String){
        commands.remove((commands.entries.firstOrNull{it.value.name == name} ?: return).key)
    }
    fun removeCommands(creator: Long) = commands.entries.removeIf{it.value.creator == creator}

    fun commandExists(cmdName: String) : Boolean{
        return commands.entries.firstOrNull{it.value.name == cmdName} != null
    }

    fun get(cmdName: String) : LearnedCommand? {
        return commands.entries.firstOrNull{it.value.name == cmdName}?.value
    }
}

class LearnedCommand(cmdName: String, cmdDesc: String = "No description supplied",
                     val output: String, val reply: Boolean, val creator: Long, var nsfw: Boolean = false, val site: String)
    : AbstractCommand(cmdName, listOf(), cmdDesc, "This is a learned command and does not have help"){

    override fun handleCommand(input: String, user: User): BMessage? {
        if(!matchesCommand(input)){
            return null;
        }
        var output = this.output;

        output = output.replace("""(?i)\\un""".toRegex(), user.userName).replace("""(?i)\\uid""".toRegex(), user.userID.toString())

        if(output.contains("%s")){
            val arguments = output.split("%s").size - 1
            val given = (splitCommand(input)["content"]?: return BMessage(
                    "You need $arguments ${if (arguments == 1) "argument" else "arguments"} to run this command",
                    true))

                    .split(",");
            if(given.size != arguments){
                return BMessage("Not enough arguments. Found ${given.size}, requires $arguments", true)
            }
            output = output.format(*given.toTypedArray())

        }


        if(user.site == "discord"){
            output = output.replace("\\\\", "\\")
        }
        return BMessage(output, reply);
    }

}

class Learn(val commands: TaughtCommands, val center: CommandCenter) : AbstractCommand("learn", listOf("teach"), "Teaches the bot a new command. ",
        help = "Syntax: ${CommandCenter.TRIGGER}help commandName commandOutput -d (optional) description -nsfw (optional) whether the command is NSFW or not (boolean)\n" +
                "Symbols:\n" +
                "* %s - requires input for the command to be used\n" +
                "* \\un - adds the username for whoever uses the command\n" +
                "* \\uid - adds the user ID for whoever uses the command" ){

    override fun handleCommand(input: String, user: User): BMessage? {
        val input = input;

        if(!matchesCommand(input)) return null;

        var nsfw = false;

        //on Discord there may appear commands that one would consider unwanted on sites like the Stack Exchange network.
        //These are just called NSFW because I have nothing better to call them.
        //If they are manually added as SFW they will appear everywhere. Otherwise, only some sites will have access
        if(center.site.name == "discord") {
            nsfw = true
        }
        val args = splitCommand(input);

        println(args)
        if(args.size < 2)
            return BMessage("You have to supply at least two arguments!", true);

        val split = (args["content"] ?: return BMessage("Supply valid arguments -_-", true))
                .split(" ".toRegex(), limit = 2);

        if(split.size != 2){
            return BMessage("Not enough arguments", true);
        }

        var name = "undefined";
        var desc = "No description was supplied";
        val creator = user.userID;
        var output = "undefined";
        val reply = false;

        for(i in 0 until args.size){
            val key = args.keys.elementAt(i);
            when (key) {
                "content" -> { //The name is the name of the command used, not the one that's attempted learned.
                    name = args["content"]!!.split(" ", limit = 2)[0]
                    output = args["content"]!!.split(" ", limit = 2)[1]
                }
                "-d" -> desc = args["-d"] ?: "No description supplied"
                "-nsfw" -> {
                    nsfw = try {
                        (args["-nsfw"] ?: (center.site.name == "discord")).toString().toBoolean()
                    }catch(e: Exception){
                        center.site.name == "discord";
                    }
                }
            }
        }

        if(name == "undefined" || output == "undefined")
            return BMessage("Something went wrong. Command not added", true)

        if(commands.doesCommandExist(name) || center.isBuiltIn(name)){
            return BMessage("That command already exists", true);
        }

        commands.addCommand(LearnedCommand(name, desc, output, reply, creator, nsfw, center.site.name))

        return BMessage(Utils.getRandomLearnedMessage(), true);
    }
}

class UnLearn(val commands: TaughtCommands, val center: CommandCenter) : AbstractCommand("unlearn", listOf("forget"), "Forgets a taught command"){

    override fun handleCommand(input: String, user: User): BMessage? {
        if(!matchesCommand(input)) return null;

        val `in` = splitCommand(input)

        val name = `in`["content"] ?: return BMessage("I need to know what to forget", true);

        if(center.isBuiltIn(name)){
            return BMessage("You can't make me forget something that's hard-coded :>", true);
        }

        if(!commands.doesCommandExist(name)){
            return BMessage("I can't forget what I never knew", true);
        }

        commands.removeCommand(name);

        return BMessage(Utils.getRandomForgottenMessage(), true);
    }
}