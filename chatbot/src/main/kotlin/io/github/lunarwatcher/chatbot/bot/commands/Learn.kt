@file:Suppress("NAME_SHADOWING")

package io.github.lunarwatcher.chatbot.bot.commands

import io.github.lunarwatcher.chatbot.Constants.LEARNED_COMMANDS
import io.github.lunarwatcher.chatbot.Database
import io.github.lunarwatcher.chatbot.bot.chat.BMessage
import io.github.lunarwatcher.chatbot.bot.command.CommandCenter
import io.github.lunarwatcher.chatbot.utils.Utils
import java.text.MessageFormat

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

        fun addCommand(c: Command) {
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

class LearnedCommand(cmdName: String, cmdDesc: String = "No description supplied",
                     val output: String, val reply: Boolean, val creator: Long,
                     override var nsfw: Boolean = false, val site: String, val disableInput: Boolean = false)
    : AbstractCommand(cmdName, listOf(), cmdDesc, "This is a learned command and does not have help"){

    override fun handleCommand(input: String, user: User): BMessage? {
        if(!matchesCommand(input)){
            return null;
        }
        var output = this.output;

        output = output.replace("""(?i)\\un""".toRegex(), user.userName).replace("""(?i)\\uid""".toRegex(), user.userID.toString())

        if(!disableInput) {
            if (output.contains("\\{[0-9]+}".toRegex())) {
                println("Group is TRIGGERED AND SUPER OFFENDED SHØ!! :p");
                val items = (splitCommand(input)["content"]?.split(",")?.map { it.trim() }?.toTypedArray()
                        ?: arrayOf());
                println(items);
                output = MessageFormat.format(output, *items);
            } else if (output.contains("%s")) {
                println(output)
                val arguments = output.split("%s").size - 1
                val given = (splitCommand(input)["content"] ?: return BMessage(
                        "You need $arguments ${if (arguments == 1) "argument" else "arguments"} to run this command",
                        true))

                        .split(",");
                if (given.size != arguments) {
                    return BMessage("Not enough arguments. Found ${given.size}, requires $arguments", true)
                }
                output = output.format(*given.toTypedArray())

            }
        }


        if(user.chat.site.name == "discord"){
            output = output.replace("\\\\", "\\")
        }
        return BMessage(output, reply);
    }

}

class Learn(val commands: TaughtCommands, val center: CommandCenter) : AbstractCommand("learn", listOf("teach"), "Teaches the bot a new command. ",
        help = "Syntax: ${CommandCenter.TRIGGER}help commandName commandOutput -d, -desc, or -description (optional) description -nsfw (optional) whether the command is NSFW or not (boolean). -reply (optional) whether or not the message should reply to the person who uses it. -noArgs (optional) whether or not to ignore input even if the command input items (%s or {id}) are present.\n" +
                "Symbols:\n" +
                "* %s - requires input for the command to be used. Alternatively {number}, where \"number\" is replaced with the ID of the supplied item. It should increment from 0 (the number corresponds with the item in the supplied arguments)\n" +
                "* \\un - adds the username for whoever uses the command\n" +
                "* \\ping - pings the user running the command" +
                "* \\uid - adds the user ID for whoever uses the command", rankRequirement = 1){

    override fun handleCommand(input: String, user: User): BMessage? {
        val input = input;

        if(!matchesCommand(input)) return null;

        var nsfw = false;

        //on Discord there may appear commands that one would consider unwanted on sites like the Stack Exchange network.
        //These are just called NSFW because I have nothing better to call them.
        //If they are manually added as SFW they will appear everywhere. Otherwise, only some sites will have access
        if(user.chat.name == "discord") {
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
                        (args["--nsfw"] ?: (user.chat.name == "discord")).toString().toBoolean()
                    }catch(e: Exception){
                        user.chat.name == "discord";
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
            return BMessage("Something went wrong. Command not added", true)

        if(commands.doesCommandExist(name) || center.isBuiltIn(name, user.chat)){
            return BMessage("That command already exists", true);
        }

        if(output.contains(percentageArgs) && output.contains(bracketArgs) && !a){
            return BMessage("Warning: ambiguous arguments detected (%s and {[0-9]+} formats were detected). This is not compatible with the system. Append --noArgs after the command output (with a space between) to disable input.", true);
        }

        if(!a){
            if(output.contains(invalidArguments))
                return BMessage("Your message contains input arguments in the % format that are invalid. Use %s for all input types, use {itemId} for all formatting (where itemId is a number starting at 0), or add --noArgs if you don't want the message to take input.", true);
        }

        commands.addCommand(LearnedCommand(name, desc, output, reply, creator, nsfw, user.chat.name, a));
        center.refreshBuckets();
        return BMessage(Utils.getRandomLearnedMessage(), true);
    }

    companion object {
        val invalidArguments = "(?i)%[^s]".toRegex();
        val bracketArgs = "\\{[0-9]+}".toRegex();
        val percentageArgs = "%s".toRegex()
    }
}

class UnLearn(val commands: TaughtCommands, val center: CommandCenter) : AbstractCommand("unlearn", listOf("forget"), "Forgets a taught command", rankRequirement = 1){

    override fun handleCommand(input: String, user: User): BMessage? {
        if(!matchesCommand(input)) return null;

        val `in` = splitCommand(input)

        val name = `in`["content"] ?: return BMessage("I need to know what to forget", true);

        if(center.isBuiltIn(name, user.chat)){
            return BMessage("You can't make me forget something that's hard-coded. :>", true);
        }

        if(!commands.doesCommandExist(name)){
            return BMessage("I can't forget what I never knew", true);
        }

        if(commands.get(name)!!.nsfw && !user.nsfwSite)
            return BMessage("Can't forget a command that's not available to this site", true)

        commands.removeCommand(name);
        center.refreshBuckets()

        return BMessage(Utils.getRandomForgottenMessage(), true);
    }
}