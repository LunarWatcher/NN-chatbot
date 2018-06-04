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
    private var commandSets = mutableMapOf<List<CommandGroup>, List<Command>>()
    private var commands: MutableList<Command>

    var listeners = mutableListOf<Listener>()
    private lateinit var statusListener: StatusListener
    lateinit var crash: CrashLogs
    lateinit var welcomeListener: WelcomeListener
    lateinit var mentionListener: MentionListener

    init {
        tc = TaughtCommands(db)

        TRIGGER = botProps.getProperty("bot.trigger")
        commands = mutableListOf()

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
        addCommand(DefineCommand())
        addCommand(RegisterWelcome())
        statusListener = StatusListener(db)
        welcomeListener = WelcomeListener(this)

        addCommand(StatusCommand(statusListener))

        listeners = ArrayList()
        listeners.add(WaveListener())
        listeners.add(TestListener())
        listeners.add(MorningListener())
        listeners.add(welcomeListener)

        listeners.add(statusListener)
        /**
         * Pun not intended:
         */
        mentionListener = MentionListener()
        listeners.add(KnockKnock(mentionListener))
        listeners.add(Train(5))
        listeners.add(mentionListener)

        addCommand(JoinTwitch())
        addCommand(LeaveTwitch())
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


    }

    @Throws(IOException::class)
    fun parseMessage(message: String?, user: User, nsfw: Boolean): List<BMessage>? {
        @Suppress("NAME_SHADOWING")
        var message: String = message ?: return null
        message = message.replace("&#8238;", "")
        message = message.replace("\u202E", "")
        message = message.trim()
        //message = message.replace(" +".toRegex(), " ")
        message = cleanInput(message)


        val om = message
        val replies = mutableListOf<BMessage>()
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
                        replies.add(x)
                    }
                }

                val lc = tc.get(name)
                if (lc != null) {
                    //If the command is NSFW but the site doesn't allow it, don't handle the command
                    if (lc.nsfw && !nsfw){}
                    else {

                        val x = lc.handleCommand(message, user)
                        if (x != null)
                            replies.add(x)
                    }
                }
            }

            for (l in listeners) {
                val x = l.handleInput(om, user)
                if (x != null) {
                    replies.add(x)
                }
            }

            mentionListener.done()

            if (replies.size == 0)
                return null
        } catch (e: Exception) {
            crash.crash(e)
            replies.add(BMessage("Something bad happened while processing. Do `" + TRIGGER + "logs` to see the logs", true))
        }

        return replies
    }

    fun isBuiltIn(cmdName: String?, chat: Chat): Boolean {
        return if (cmdName == null) false else get(cmdName, chat) != null
    }

    fun save() {
        statusListener.save()
        welcomeListener.save()
    }

    fun addCommand(c: Command, group: CommandGroup = CommandGroup.COMMON) {
        if(group != CommandGroup.COMMON)
            c.commandGroup = group
        commands.add(c)
    }

    operator fun get(key: String, chat: Chat): Command? = getCommands(chat).firstOrNull{
            it.matchesCommand(key)
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

    fun getCommands(site: Chat) : List<Command>{

        if(commandSets[site.commandGroup] != null)
            return commandSets[site.commandGroup]!!
        val buffer = commands.filter{
            it.commandGroup in site.commandGroup || it.commandGroup == CommandGroup.COMMON
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
                val stripped = input.substring(TRIGGER.length).replace("\n", "").trim()

                return if (stripped.isEmpty()) false else stripped.matches("(?!\\s+)(.*)".toRegex())
            }
            return false
        }

        fun splitCommand(input: String, commandName: String) = arrayOf(commandName, input.replace("$commandName ", ""))

        fun saveTaught() {
            tc.save()

        }
    }

    fun refreshBuckets(){
        commandSets.clear()
    }

    fun postSiteInit(){
        statusListener.initialize()
    }

}
