package io.github.lunarwatcher.chatbot.bot.command


import io.github.lunarwatcher.chatbot.*
import io.github.lunarwatcher.chatbot.Constants.RELOCATION_VOTES
import io.github.lunarwatcher.chatbot.bot.Bot
import io.github.lunarwatcher.chatbot.bot.chat.BMessage
import io.github.lunarwatcher.chatbot.bot.commands.*
import io.github.lunarwatcher.chatbot.bot.listener.*
import io.github.lunarwatcher.chatbot.bot.sites.Chat
import io.github.lunarwatcher.chatbot.bot.sites.discord.DiscordChat
import io.github.lunarwatcher.chatbot.bot.sites.se.SEChat
import io.github.lunarwatcher.chatbot.bot.sites.twitch.TwitchChat
import java.io.IOException
import java.util.*
import kotlin.reflect.KClass

enum class CommandGroup{
    COMMON, STACKEXCHANGE, DISCORD, TWITCH, NSFW;

}


class CommandCenter private constructor(botProps: Properties, val db: Database) {
    private var commandSets = mutableMapOf<List<CommandGroup>, MutableMap<CmdInfo, Command>>()
    private var commands: MutableMap<CmdInfo, Command>

    var listeners = mutableListOf<Listener>()
    private var statusListener: StatusListener
    var crash: CrashLogs

    init {
        tc = TaughtCommands(db)

        TRIGGER = botProps.getProperty("bot.trigger")
        commands = HashMap()

        val location = LocationCommand()
        val alive = Alive()

        addCommand(HelpCommand())
        addCommand(ShrugCommand())
        addCommand(AboutCommand())
        addCommand(Learn(tc, this))
        addCommand(UnLearn(tc, this))
        addCommand(UpdateRank())
        addCommand(CheckCommand())
        addCommand(BanUser())
        addCommand(Unban())
        addCommand(SaveCommand())
        addCommand(alive)
        addCommand(WhoMade())
        addCommand(ChangeCommandStatus(this))
        addCommand(RandomNumber())
        addCommand(LMGTFY())
        addCommand(UpdateRank())
        addCommand(DebugRanks())
        addCommand(Kill())
        addCommand(Lick())
        addCommand(Give())
        addCommand(Ping())
        addCommand(Appul())
        addCommand(BasicPrintCommand("(╯°□°）╯︵ ┻━┻", "tableflip", ArrayList(), "The tables have turned..."))
        addCommand(BasicPrintCommand("┬─┬ ノ( ゜-゜ノ)", "unflip", ArrayList(), "The tables have turned..."))
        addCommand(TimeCommand())
        addCommand(KillBot())
        addCommand(NetStat())
        addCommand(location)
        crash = CrashLogs()
        addCommand(crash)
        addCommand(DogeCommand())
        addCommand(RepeatCommand())
        addCommand(Blame())
        addCommand(WakeCommand())
        addCommand(WhoIs())
        addCommand(BlacklistRoom())
        addCommand(UnblacklistRoom())
        addCommand(TellCommand())
        addCommand(NPECommand())
        addCommand(RevisionCommand())
        addCommand(NetIpCommand())
        addCommand(GitHubCommand())
        addCommand(WikiCommand())
        addCommand(CatCommand())
        addCommand(DogCommand())
        statusListener = StatusListener(db)
        addCommand(StatusCommand(statusListener))

        listeners = ArrayList()
        listeners.add(WaveListener())
        listeners.add(TestListener())
        listeners.add(MorningListener())


        listeners.add(statusListener)
        /**
         * Pun not intended:
         */
        val ml = MentionListener()
        listeners.add(KnockKnock(ml))
        listeners.add(Train(5))
        listeners.add(ml)

        ///////////////////////////////////////////////
        addCommand(NSFWState(), CommandGroup.DISCORD)
        addCommand(DiscordSummon(), CommandGroup.DISCORD)

        //////////////////////////////////////////////
        addCommand(Summon(RELOCATION_VOTES), CommandGroup.STACKEXCHANGE)
        addCommand(UnSummon(RELOCATION_VOTES), CommandGroup.STACKEXCHANGE)
        addCommand(AddHome(), CommandGroup.STACKEXCHANGE)
        addCommand(RemoveHome(), CommandGroup.STACKEXCHANGE)
        addCommand(SERooms(), CommandGroup.STACKEXCHANGE)

        //////////////////////////////////////////////
        addCommand(JoinTwitch(), CommandGroup.TWITCH)
        addCommand(LeaveTwitch(), CommandGroup.TWITCH)

    }

    @Throws(IOException::class)
    fun parseMessage(message: String?, user: User, nsfw: Boolean): List<BMessage>? {
        @Suppress("NAME_SHADOWING")
        var message: String = message ?: return null
        message = message.replace("&#8238;", "")
        message = message.replace("\u202E", "")
        message = message.trim { it <= ' ' }
        message = message.replace(" +".toRegex(), " ")
        message = cleanInput(message)


        val om = message
        var replies: MutableList<BMessage>? = ArrayList()
        try {
            if (isCommand(message)) {
                message = message.substring(TRIGGER.length)

                //Get rid of white space to avoid problems down the line
                message = message.trim()

                val name = message.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[0]

                val c = get(name, user.chat)
                if (c != null) {
                    val x = c.handleCommand(message, user)
                    if (x != null) {
                        //There are still some commands that could return null here
                        replies!!.add(x)
                    }
                }

                val lc = tc.get(name)
                if (lc != null) {
                    //If the command is NSFW but the site doesn't allow it, don't handle the command
                    if (lc.nsfw && !nsfw){}
                    else {

                        val x = lc.handleCommand(message, user)
                        if (x != null)
                            replies!!.add(x)
                    }
                }
            }

            for (l in listeners) {
                val x = l.handleInput(om, user)
                if (x != null) {
                    replies!!.add(x)
                }
            }

            if (replies!!.size == 0)
                replies = null
        } catch (e: Exception) {
            crash.crash(e)
            if (replies == null)
                replies = ArrayList()
            replies.add(BMessage("Something bad happened while processing. Do `" + TRIGGER + "logs` to see the logs", true))
        }

        return replies
    }

    /**
     * Manual command injection into the public storage
     */
    fun manualCommandInjection(c: Command?) {
        if (c == null)
            return

        addCommand(c)
    }

    fun isBuiltIn(cmdName: String?, chat: Chat): Boolean {
        return if (cmdName == null) false else get(cmdName, chat) != null
    }

    fun save() {
        statusListener.save()
    }

    fun addCommand(c: Command, group: CommandGroup = CommandGroup.COMMON) {
        val name = c.name
        val aliases = c.aliases
        commands.putIfAbsent(CmdInfo(name, aliases, group), c)
    }

    operator fun get(key: String, chat: Chat): Command? {
        return MapUtils.get(key, getCommands(chat)) as Command?
    }

    operator fun get(key: CmdInfo, chat: Chat): Command? {
        return MapUtils.get(key, getCommands(chat)) as Command?
    }

    fun hookupToRanks(user: Long, username: String, site: Chat) {
        if (site.config.getRank(user) == null) {
            //This code exists in an attempt to map every. Single. User. who uses the bot or even talk around it
            //This will build up a fairly big database, but that's why there is (going to be) a purge method
            //for the database
            site.config.addRank(user, Constants.DEFAULT_RANK, username)
        } else {
            if (site.config.getRank(user)!!.username == null || site.config.getRank(user)!!.username != username) {
                site.config.addRank(user, site.config.getRank(user)!!.rank, username)
            }
        }
    }

    fun getCommands(site: Chat) : MutableMap<CmdInfo, Command>{
        val buffer = mutableMapOf<CmdInfo, Command>()
        if(commandSets[site.commandGroup] != null)
            return commandSets[site.commandGroup]!!

        commands.forEach {(info, command) ->
            if(info.group in site.commandGroup || info.group == CommandGroup.COMMON) {

                buffer[info] = command
            }

        }
        commandSets[site.commandGroup] = buffer
        return buffer
    }

    companion object {
        lateinit var INSTANCE: CommandCenter

        fun initialize(botProps: Properties, db: Database){
            INSTANCE = CommandCenter(botProps, db)
        }

        lateinit var TRIGGER: String
        lateinit var tc: TaughtCommands
        lateinit var bot: Bot

        fun isCommand(input: String): Boolean {
            if (input.startsWith(TRIGGER)) {
                val stripped = input.substring(TRIGGER.length).replace("\n", "")

                return if (stripped.isEmpty() || stripped.trim { it <= ' ' }.isEmpty()) false else stripped.matches("(?!\\s+)(.*)".toRegex())
            }
            return false
        }

        fun splitCommand(input: String, commandName: String) = arrayOf(commandName, input.replace("$commandName ", ""))

        fun saveTaught() {
            tc.save()

        }
    }

}
