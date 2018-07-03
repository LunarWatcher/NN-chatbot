@file:Suppress("UNCHECKED_CAST")

package io.github.lunarwatcher.chatbot.bot.listener

import io.github.lunarwatcher.chatbot.Database
import io.github.lunarwatcher.chatbot.WelcomeMessages
import io.github.lunarwatcher.chatbot.bot.chat.Message
import io.github.lunarwatcher.chatbot.bot.chat.ReplyMessage
import io.github.lunarwatcher.chatbot.bot.command.CommandCenter
import io.github.lunarwatcher.chatbot.bot.command.CommandGroup
import io.github.lunarwatcher.chatbot.bot.sites.discord.DiscordChat
import io.github.lunarwatcher.chatbot.safeGet

@Suppress("NAME_SHADOWING")
class KnockKnock(val mention: MentionListener) : AbstractListener("Knock knock", "The name says it all"){
    var context: Context? = null

    override fun handleInput(message: Message): ReplyMessage? {
        var input = message.content
        if(!mention.isMentioned(input, message.chat) && context == null){
            if (input.contains(KNOCK_REGEX)) {
                context = Context(0, message.user.userID)
            }else
                return null
        }else if(mention.isMentioned(input, message.chat) && context == null) {
            input = input.split(" ", limit = 2).safeGet(1) ?: ""
            if (input.contains(KNOCK_REGEX)) {
                context = Context(0, message.user.userID)
            }else
                return null
        }

        if(context?.user != message.user.userID){
            return null;
        }

        mention.ignoreNext();

        when(context?.index){
            0 ->{
                context?.next()
                return ReplyMessage("Who's there?", true);
            }
            1->{
                if(mention.isMentionedStart(input, message.chat)){
                    input = input.split(" ", limit=2).safeGet(1) ?: ""
                }
                val who = "$input who?"
                context?.next()
                return ReplyMessage(who, true);
            }
            2->{
                context = null;
                return ReplyMessage("Hahaha!", true)
            }
            else->return null;
        }
    }

    companion object {
        val KNOCK_REGEX = "(?i)knock\\W*?knock".toRegex();
    }
}

class Context(var index: Int, var user: Long) {
    fun next(){
        index++;
    }
}

class TestListener : AbstractListener("Test", description="Is this thing on??"){
    val poked = mutableListOf<Long>();

    override fun handleInput(message: Message): ReplyMessage? {
        if (message.content.toLowerCase().contains(TEST_REGEX)){
            if (poked.contains(message.user.userID))
                return null;
            poked.add(message.user.userID);
            return ReplyMessage("You passed! Congratulations!", false)
        }
        return null
    }

    companion object {
        val TEST_REGEX = """^['"*]*test['"*]*$""".toRegex()
    }
}

class WaveListener : AbstractListener("wave", "Waves back when a wave is detected"){
    val pause = 30000;
    var lastWave: Long = 0;
    override fun handleInput(message: Message): ReplyMessage? {
        if(System.currentTimeMillis() - lastWave >= pause) {

            if (message.content == "o/") {
                lastWave = System.currentTimeMillis();
                return ReplyMessage("\\o", false);
            }else if (message.content == "\\o") {
                lastWave = System.currentTimeMillis();
                return ReplyMessage("o/", false)
            }

        }

        return null;
    }
}

class StatusListener(val database: Database) : AbstractListener("status", "tracks messages for statuses. See also the status command"){
    /**
     * <Site, <Server, <UID, Messages>>>
     */
    var users: MutableMap<String, MutableMap<Long, MutableMap<Long, Long>>>

    init{
        users = mutableMapOf()
    }

    fun initialize(){
        for(site in CommandCenter.bot.chats) {
            val b = database.getMap("status-" + site.name) as MutableMap<String, MutableMap<String, Long>>?
            if (b != null) {
                try {
                    users[site.name] = b.map{
                        it.key.toLong() to it.value.map{
                            it.key.toLong() to it.value}.toMap().toMutableMap()
                    }.toMap().toMutableMap()
                } catch (e: ClassCastException) {
                    users[site.name] = mutableMapOf()
                    e.printStackTrace()
                }
            } else {
                users[site.name] = mutableMapOf()
            }
        }
    }

    override fun handleInput(message: Message): ReplyMessage? {
        val site = message.chat.name
        if(!users.keys.contains(site))
            users[site] = mutableMapOf()

        val room = if(message.chat is DiscordChat)
            message.user.args.firstOrNull { it.first == "guildID" }?.second?.toLong() ?: (message.chat as DiscordChat).getChannel(message.roomID)?.guild?.longID ?: return null
        else message.roomID

        val users = this.users[site]!!

        val uid = message.user.userID
        if(!users.keys.contains(room))
            users[room] = mutableMapOf()
        if(users[room]!![uid] == null)
            users[room]!![uid] = 0

        users[room]!![uid] = users[room]!!.getNonNull(uid) + 1
        return null
    }

    fun save(){
        for((site, status) in users) {
            database.put("status-$site", status)
        }
    }
}

class MorningListener : AbstractListener("Morning", "GOOOOOOD MORNING!"){
    private var lastMessages = mutableMapOf<Long, Long>()

    override fun handleInput(message: Message): ReplyMessage? {
        if(lastMessages[message.roomID] == null || System.currentTimeMillis() - lastMessages[message.roomID]!! > (WAIT * 1000)) {

            if(message.content.replace("[.,!\\-~]*".toRegex(), "").toLowerCase()
                            .matches(regex)) {
                lastMessages[message.roomID] = System.currentTimeMillis()
                return ReplyMessage(message.content, false)
            }
        }
        return null
    }

    companion object {
        /**
         * Wait time in seconds
         */
        const val WAIT = 10 * 60 //10 minutes * 60 seconds

        val matches = listOf(
                "(?:good\\W*)?morn(?:ing|o)?",
                "gm", "g(?:uten|od) morgen"
        )

        val regex = """(?i)^(${matches.joinToString("|")})$""".toRegex()
        
    }
}

class BasicListener(val output: String, val pattern: Regex, name: String, description: String, val reply: Boolean = false) : AbstractListener(name, description){
    constructor(output: String, pattern: String, name: String, description: String) : this(output, pattern.toRegex(), name, description)

    override fun handleInput(message: Message): ReplyMessage? {
        if(message.content.matches(pattern)){
            return ReplyMessage(output, reply)
        }
        return null
    }
}

class WelcomeListener(center: CommandCenter) : AbstractListener("Welcome", "Sends welcome messages to new users if there's a welcome message registered", CommandGroup.STACKEXCHANGE){
    val mappedUsers: MutableMap<String, MutableMap<Long, MutableList<Long>>>
    init{
        val mappedUsers = center.db.getMap("welcomed") as Map<String, Map<String, MutableList<Long>>>?
        this.mappedUsers = mappedUsers?.map{ it.key to it.value.map{
            it.key.toLong() to it.value.map{
                it.toLong()
            }.toMutableList()
        }.toMap().toMutableMap()
        }?.toMap()?.toMutableMap() ?: mutableMapOf()
    }

    override fun handleInput(message: Message): ReplyMessage? {

        val room = if(message.chat is DiscordChat)
            message.user.args.firstOrNull { it.first == "guildID" }?.second?.toLong() ?: (message.chat as DiscordChat).getChannel(message.roomID)?.guild?.longID ?: return null
        else message.roomID

        if (mappedUsers[message.chat.name] == null)
            mappedUsers[message.chat.name] = mutableMapOf()
        if(mappedUsers[message.chat.name]!![room] == null)
            mappedUsers[message.chat.name]!![room] = mutableListOf()

        if (message.user.userID !in mappedUsers[message.chat.name]!![room]!!) {

            mappedUsers[message.chat.name]!![room]!!.add(message.user.userID)
            if (WelcomeMessages.INSTANCE!!.hasMessage(message.chat.name, room))
                return ReplyMessage(WelcomeMessages.INSTANCE!!.messages[message.chat.name]!![room]!!, true)

        }
        return null
    }

    fun save(){
        CommandCenter.INSTANCE.db.put("welcomed", mappedUsers)
    }
}

/**
 * Saves some time in casting; but it will throw a NPE if Map\[what] == null
 */
fun <K, V> Map<K, V>.getNonNull(what: K) : V{
    return get(what) as V
}

fun <K, V> MutableMap<K, V>.getNonNullI(what: K, defaultValue: V) : V{
    return try {
        get(what) as V
    }catch(e: Exception){
        this[what] = defaultValue
        defaultValue
    }
}


