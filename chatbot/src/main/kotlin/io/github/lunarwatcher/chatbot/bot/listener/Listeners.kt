@file:Suppress("UNCHECKED_CAST")

package io.github.lunarwatcher.chatbot.bot.listener

import io.github.lunarwatcher.chatbot.Database
import io.github.lunarwatcher.chatbot.bot.chat.BMessage
import io.github.lunarwatcher.chatbot.bot.commands.User
import io.github.lunarwatcher.chatbot.bot.sites.Chat

@Suppress("NAME_SHADOWING")
class KnockKnock(val mention: MentionListener) : AbstractListener("Knock knock", "The name says it all"){
    var context: Context? = null

    override fun handleInput(input: String, user: User): BMessage? {
        val input = input.toLowerCase();
        if(!mention.isMentioned(input) && context == null){
            return null;

        }else if(mention.isMentioned(input) && context == null) {

            if (input.contains("knock knock")) {
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

class StatusListener(val site: Chat, val database: Database) : AbstractListener("status", "tracks messages for statuses. See also the status command"){
    var users: MutableMap<Int, MutableMap<Long, Long>>

    init{
        val b = database.getMap("status-" + site.name) as MutableMap<String, MutableMap<String, Long>>?
        if (b != null){
            try {
                users = mutableMapOf()
                for (user in b){
                    try {
                        users[user.key.toInt()] = user.value.map{it.key.toLong() to it.value}.associateBy({it.first}, {it.second}).toMutableMap()

                    }catch(e: Exception){
                        e.printStackTrace()
                    }
                }
            }catch(e: ClassCastException){
                users = mutableMapOf()
                e.printStackTrace()
            }
        }else{
            users = mutableMapOf()
        }
    }

    override fun handleInput(input: String, user: User): BMessage? {
        val uid = user.userID
        if(!users.keys.contains(user.roomID))
            users[user.roomID] = mutableMapOf()
        if(users[user.roomID]!![uid] == null)
            users[user.roomID]!![uid] = 0

        users[user.roomID]!![uid] = users[user.roomID]!!.getNonNull(uid) + 1
        return null
    }

    fun save(){
        database.put("status-" + site.name, users.toMap())
    }
}

class MorningListener : AbstractListener("Morning", "GOOOOOOD MORNING!"){
    private var lastMessage = 0L
    override fun handleInput(input: String, user: User): BMessage? {
        if(input.toLowerCase() == "morning" && System.currentTimeMillis() - lastMessage > 30000) {
            lastMessage = System.currentTimeMillis()
            return BMessage("morning", false)
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


