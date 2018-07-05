package io.github.lunarwatcher.chatbot.bot.command


import io.github.lunarwatcher.chatbot.Constants
import io.github.lunarwatcher.chatbot.Constants.RELOCATION_VOTES
import io.github.lunarwatcher.chatbot.CrashLogs
import io.github.lunarwatcher.chatbot.Database
import io.github.lunarwatcher.chatbot.bot.Bot
import io.github.lunarwatcher.chatbot.bot.chat.Message
import io.github.lunarwatcher.chatbot.bot.chat.ReplyMessage
import io.github.lunarwatcher.chatbot.bot.commands.ICommand
import io.github.lunarwatcher.chatbot.bot.commands.`fun`.*
import io.github.lunarwatcher.chatbot.bot.commands.admin.*
import io.github.lunarwatcher.chatbot.bot.commands.basic.*
import io.github.lunarwatcher.chatbot.bot.commands.discord.DiscordSummon
import io.github.lunarwatcher.chatbot.bot.commands.discord.NSFWState
import io.github.lunarwatcher.chatbot.bot.commands.learn.LearnCommand
import io.github.lunarwatcher.chatbot.bot.commands.learn.TaughtCommands
import io.github.lunarwatcher.chatbot.bot.commands.learn.UnlearnCommand
import io.github.lunarwatcher.chatbot.bot.commands.meta.*
import io.github.lunarwatcher.chatbot.bot.commands.stackexchange.JoinCommand
import io.github.lunarwatcher.chatbot.bot.commands.stackexchange.LeaveCommand
import io.github.lunarwatcher.chatbot.bot.commands.stackexchange.SERooms
import io.github.lunarwatcher.chatbot.bot.commands.twitch.JoinTwitch
import io.github.lunarwatcher.chatbot.bot.commands.twitch.LeaveTwitch
import io.github.lunarwatcher.chatbot.bot.listener.*
import io.github.lunarwatcher.chatbot.bot.sites.Chat
import org.slf4j.LoggerFactory
import java.io.IOException
import java.util.*

enum class CommandGroup{
    COMMON, STACKEXCHANGE, DISCORD, TWITCH, NSFW;

}


class CommandCenter private constructor(botProps: Properties, val db: Database) {
    private var logger = LoggerFactory.getLogger(this::class.java)
    private var commandSets = mutableMapOf<List<CommandGroup>, List<ICommand>>()
    private var commands: MutableList<ICommand>

    var listeners = mutableListOf<Listener>()
    private var statusListener: StatusListener
    var crash: CrashLogs
    var welcomeListener: WelcomeListener
    var mentionListener: MentionListener

    init {
        logger.info("Alive!");
        tc = TaughtCommands(db)

        TRIGGER = botProps.getProperty("bot.trigger")
        commands = mutableListOf()

        val location = LocationCommand()
        val alive = Alive()

        addCommand(HelpCommand())
        addCommand(ShrugCommand())
        addCommand(AboutCommand())
        addCommand(LearnCommand(tc, this))
        addCommand(UnlearnCommand(tc, this))
        addCommand(GetRankCommand())
        addCommand(BanCommand())
        addCommand(UnbanCommand())
        addCommand(SaveCommand())
        addCommand(alive)
        addCommand(WhoMade())
        addCommand(ChangeCommandStatus(this))
        addCommand(RandomNumber())
        addCommand(LMGTFY())
        addCommand(SetRankCommand())
        addCommand(DebugRanks())
        addCommand(Kill())
        addCommand(LickCommand())
        addCommand(GiveCommand())
        addCommand(PingCommand())
        addCommand(BasicPrintCommand("I LUV APPHULS!", true, "apples", listOf("apphuls"), "Apples!"))
        addCommand(BasicPrintCommand("(╯°□°）╯︵ ┻━┻", false, "tableflip", listOf(), "The tables have turned..."))
        addCommand(BasicPrintCommand("┬─┬ ノ( ゜-゜ノ)", false,"unflip", listOf(), "The tables have turned..."))
        addCommand(TimeCommand())
        addCommand(ShutdownCommand())
        val netStat = NetStat()
        addCommand(netStat)
        addCommand(location)
        crash = CrashLogs()
        addCommand(crash)
        addCommand(DogeCommand())
        addCommand(RepeatCommand())
        addCommand(BlameCommand())
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
        addCommand(TestCommand());
        addCommand(MultiMessageTest())

        statusListener = StatusListener(db)
        welcomeListener = WelcomeListener(this)

        addCommand(StatusCommand(statusListener))

        listeners = ArrayList()
        listeners.add(WaveListener())
        listeners.add(TestListener())
        listeners.add(MorningListener())
        listeners.add(welcomeListener)

        listeners.add(statusListener)
        mentionListener = MentionListener(netStat)
        listeners.add(KnockKnock(mentionListener))
        listeners.add(mentionListener)

        addCommand(JoinTwitch())
        addCommand(LeaveTwitch())
        ///////////////////////////////////////////////
        addCommand(NSFWState(), CommandGroup.DISCORD)
        addCommand(DiscordSummon(), CommandGroup.DISCORD)

        //////////////////////////////////////////////
        addCommand(JoinCommand(RELOCATION_VOTES), CommandGroup.STACKEXCHANGE)
        addCommand(LeaveCommand(RELOCATION_VOTES), CommandGroup.STACKEXCHANGE)
        addCommand(AddHome(), CommandGroup.STACKEXCHANGE)
        addCommand(RemoveHome(), CommandGroup.STACKEXCHANGE)
        addCommand(SERooms(), CommandGroup.STACKEXCHANGE)


        //////////////////////////////////////////////
    }

    @Throws(IOException::class)
    fun parseMessage(message: Message): List<ReplyMessage>? {
        logger.info("Message @ " + message.chat.name + " : \"${message.content}\" - ${message.user.userName}")

        val replies = mutableListOf<ReplyMessage>()
        try {
            if (isCommand(message.content)) {
                replies.addAll(handleCommands(message));
            }
            replies.addAll(handleListeners(message));
            mentionListener.done()
        } catch (e: Exception) {
            crash.crash(e)
            replies.add(ReplyMessage("Something bad happened while processing. Do `" + TRIGGER + "logs` to see the logs", true))
        }

        return replies
    }

    fun handleCommands(message: Message) : List<ReplyMessage> {
        val localMessage = message.clone()
        val replies = mutableListOf<ReplyMessage>()
        localMessage.substring(TRIGGER.length)

        val name = localMessage.content.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[0]

        val c = get(name, localMessage.chat)
        if (c != null) {
            val x = c.handleCommand(localMessage)
            if (x != null) {
                //There are still some commands that could return null here
                if(x.isEmpty()){
                    logger.warn("[MEMORY] Command " + c.name + " returned an empty list. Should return null instead")
                    return replies;
                }
                replies.addAll(x)
                return replies;
            }
        }

        val lc = tc.get(name)
        if (lc != null) {
            //If the command is NSFW but the site doesn't allow it, don't handle the command
            if (lc.nsfw && !localMessage.nsfwSite){}
            else {

                val x = lc.handleCommand(localMessage)
                if (x != null) {
                    if(x.isEmpty()){
                        logger.warn("[MEMORY] Taught command returned an empty list. Should return null instead")
                        return replies;
                    }
                    replies.addAll(x)
                    return replies;
                }

            }
        }
        return replies;
    }

    fun handleListeners(message: Message) : List<ReplyMessage>{
        val replies = mutableListOf<ReplyMessage>()
        for (l in listeners) {
            val x = l.handleInput(message)
            if (x != null) {
                replies.addAll(x)
            }
        }
        return replies;
    }

    fun isBuiltIn(cmdName: String?, chat: Chat): Boolean {
        return if (cmdName == null) false else get(cmdName, chat) != null
    }

    fun save() {
        statusListener.save()
        welcomeListener.save()
    }

    fun addCommand(c: ICommand, group: CommandGroup = CommandGroup.COMMON) {
        if(group != CommandGroup.COMMON)
            c.commandGroup = group
        commands.add(c)
    }

    operator fun get(key: String, chat: Chat): ICommand? = getCommands(chat).firstOrNull{
            it.matchesCommand(key)
    }


    fun hookupToRanks(user: Long,site: Chat) {
        if (site.config.getRank(user) == null) {
            //This code exists in an attempt to map every. Single. User. who uses the bot or even talk around it
            //This will build up a fairly big database, but that's why there is (going to be) a purge method
            //for the database
            site.config.addRank(user, Constants.DEFAULT_RANK)
        }
    }

    fun getCommands(site: Chat) : List<ICommand>{

        if(commandSets[site.commandGroup] != null)
            return commandSets[site.commandGroup]!!
        val buffer = commands.filter{
            it.commandGroup in site.commandGroup || it.commandGroup == CommandGroup.COMMON
        }
        commandSets[site.commandGroup] = buffer
        return buffer
    }


    companion object {
        /**
         * Ignored when sending messages. Useful to avoid output.
         */
        val NO_MESSAGE = ReplyMessage("", false)


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
