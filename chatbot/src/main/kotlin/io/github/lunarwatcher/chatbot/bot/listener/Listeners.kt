@file:Suppress("UNCHECKED_CAST")

package io.github.lunarwatcher.chatbot.bot.listener

import io.github.lunarwatcher.chatbot.Database
import io.github.lunarwatcher.chatbot.bot.chat.BMessage
import io.github.lunarwatcher.chatbot.bot.command.CommandCenter
import io.github.lunarwatcher.chatbot.bot.commands.User

@Suppress("NAME_SHADOWING")
class KnockKnock(val mention: MentionListener) : AbstractListener("Knock knock", "The name says it all"){
    var context: Context? = null

    override fun handleInput(input: String, user: User): BMessage? {
        val input = input.toLowerCase();
        if(!mention.isMentioned(input, user.chat) && context == null){
            return null;

        }else if(mention.isMentioned(input, user.chat) && context == null) {

            if (input.contains("knock\\W*?knock".toRegex())) {
                context = Context(0, user.userID)
            }else
                return null

        }

        if(context?.user != user.userID){
            return null;
        }

        mention.ignoreNext();
        when(context?.index){
            0 ->{
                context?.next()
                return BMessage("Who's there?", true);
            }
            1->{
                val who = "$input who?"
                context?.next()
                return BMessage(who, true);
            }
            2->{
                context = null;
                return BMessage("Hahaha!", true)
            }
            else->return null;
        }
    }
}

class Context(var index: Int, var user: Long) {
    fun next(){
        index++;
    }
}

class Train(val count: Int) : AbstractListener("Train", "Finds message trains and joins in"){

    init{
        if(count < 2)
            throw IllegalArgumentException("The count cannot be < 2!");
    }

    var previous: String? = null;
    var pCount: Int = 0;
    var preUser: Long = 0;
    override fun handleInput(input: String, user: User): BMessage? {
        if(previous == null) {
            pCount = 1;
            previous = input;
            preUser = user.userID;
            return null;
        }else{
            if(previous?.toLowerCase() == input.toLowerCase()){
                if(preUser == user.userID)
                    return null;
                preUser = user.userID;
                pCount++;
            }else{
                previous = input;
                pCount = 1;
                preUser = user.userID;
                return null;
            }

            if(pCount >= count){
                pCount = 0;
                previous = null;
                return BMessage(previous, false);
            }
        }

        return null;
    }
}

class TestListener : AbstractListener("Test", description="Is this thing on??"){
    val poked = mutableListOf<Long>();

    override fun handleInput(input: String, user: User): BMessage? {
        if (input.toLowerCase().contains("^test$".toRegex())){
            if (poked.contains(user.userID))
                return null;
            poked.add(user.userID);
            return BMessage("You passed! Congratulations!", false)
        }
        return null
    }
}

class WaveListener : AbstractListener("wave", "Waves back when a wave is detected"){
    val pause = 30000;
    var lastWave: Long = 0;
    override fun handleInput(input: String, user: User): BMessage? {
        //using isCommand is optional in listeners, but some listeners want to ignore it if it is a command
        if(isCommand(input))
            return null;
        if(System.currentTimeMillis() - lastWave >= pause) {

            if (input == "o/") {
                lastWave = System.currentTimeMillis();
                return BMessage("\\o", false);
            }else if (input == "\\o") {
                lastWave = System.currentTimeMillis();
                return BMessage("o/", false)
            }

        }

        return null;
    }
}

class StatusListener(val database: Database) : AbstractListener("status", "tracks messages for statuses. See also the status command"){
    var users: MutableMap<String, MutableMap<Long, MutableMap<Long, Long>>>

    init{
        users = mutableMapOf()
        for(site in CommandCenter.bot.chats) {
            val b = database.getMap("status-" + site.name) as MutableMap<String, MutableMap<String, Long>>?
            if (b != null) {
                try {
                    users[site.name] = mutableMapOf()
                    for (user in b) {
                        try {
                            users[site.name]!![user.key.toLong()] =
                                    user.value.map {
                                        it.key.toLong() to it.value
                                    }
                                    .associateBy({ it.first }, { it.second })
                                    .toMutableMap()

                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                } catch (e: ClassCastException) {
                    users[site.name] = mutableMapOf()
                    e.printStackTrace()
                }
            } else {
                users[site.name] = mutableMapOf()
            }
        }
    }

    override fun handleInput(input: String, user: User): BMessage? {
        val site = user.chat.site.name
        if(!users.keys.contains(site))
            users[site] = mutableMapOf()

        val users = this.users[site]!!

        val uid = user.userID
        if(!users.keys.contains(user.roomID))
            users[user.roomID] = mutableMapOf()
        if(users[user.roomID]!![uid] == null)
            users[user.roomID]!![uid] = 0

        users[user.roomID]!![uid] = users[user.roomID]!!.getNonNull(uid) + 1
        return null
    }

    fun save(){
        for((site, status) in users) {
            database.put("status-$site", status)
        }
    }
}

class MorningListener : AbstractListener("Morning", "GOOOOOOD MORNING!"){
    private var lastMessage = 0L
    override fun handleInput(input: String, user: User): BMessage? {
        if(System.currentTimeMillis() - lastMessage > (WAIT * 1000)) {
            for(match in matches) {
                if(input.replace("[^a-zA-Z]".toRegex(), "")
                                .matches("(?i)^$match$".toRegex())) {
                    lastMessage = System.currentTimeMillis()
                    return BMessage(input, false)
                }
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
                "morn",
                "morning",
                "morno",
                "gm",
                "good morning"
        )
        
    }
}

class BasicListener(val output: String, val pattern: Regex, name: String, description: String, val reply: Boolean = false) : AbstractListener(name, description){
    constructor(output: String, pattern: String, name: String, description: String) : this(output, pattern.toRegex(), name, description)

    override fun handleInput(input: String, user: User): BMessage? {
        if(input.matches(pattern)){
            return BMessage(output, reply)
        }
        return null
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


