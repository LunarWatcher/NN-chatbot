package io.github.lunarwatcher.chatbot.bot.commands

import io.github.lunarwatcher.chatbot.BotCore
import io.github.lunarwatcher.chatbot.Configurations
import io.github.lunarwatcher.chatbot.Constants
import io.github.lunarwatcher.chatbot.Database
import io.github.lunarwatcher.chatbot.bot.ReplyBuilder
import io.github.lunarwatcher.chatbot.bot.chat.BMessage
import io.github.lunarwatcher.chatbot.bot.command.CommandCenter
import io.github.lunarwatcher.chatbot.bot.sites.Chat
import io.github.lunarwatcher.chatbot.bot.sites.se.SEChat
import io.github.lunarwatcher.chatbot.utils.Utils

@Suppress("NAME_SHADOWING")
class BotConfig{
    val site: Chat;
    val ranks: MutableMap<Long, RankInfo>;
    val homes: MutableList<Int>;

    constructor(site: Chat){
        this.site = site;

        ranks = mutableMapOf();
        homes = mutableListOf()
    }

    fun addHomeRoom(newRoom: Int) : Boolean{
        homes.filter { it == newRoom }
                .forEach { return false }

        homes.add(newRoom);
        return true;
    }

    fun removeHomeRoom(rr: Int) : Boolean{
        for(i in (homes.size - 1)downTo 0){
            if(homes[i] == rr){
                homes.removeAt(i)
                return true;
            }
        }

        return false;
    }

    fun set(homes: List<Int>?, ranked: Map<Long, RankInfo>?){
        if(homes != null) {
            this.homes.addAll(homes)
        }
        ranked?.forEach{
            this.ranks[it.key] = it.value
        }
    }

    fun addRank(user: Long, rank: Int, username: String?){
        var username = username

        if(username == null){
            val rank = ranks[user];
            if(rank?.username != null){
                username = rank.username;
            }
        }
        ranks[user] = RankInfo(user, rank, username);
    }

    fun getRank(user: Long) : RankInfo? = ranks[user];
}

class RankInfo(val uid: Long, val rank: Int, var /*usernames can change*/username: String?/*Nullable because it isn't always this can be passed*/)

class ChangeCommandStatus(val center: CommandCenter) : AbstractCommand("declare", listOf(), "Changes a commands status. Only commands available on the site can be edited"){
    override fun handleCommand(input: String, user: User): BMessage? {


        if(Utils.getRank(user.userID, center.site.config) < 7){
            return BMessage("I'm afraid I can't let you do that, User", true);
        }
        try {
            val args = input.split(" ");
            val command = args[0];

            val newState: String = args[1];
            val actual: Boolean
            actual = when (newState) {
                "sfw" -> false
                "nsfw" -> true
                else -> newState.toBoolean()
            };

            if (center.isBuiltIn(command)) {
                System.out.println(command);
                return BMessage("You can't change the status of included commands.", true);
            }
            if (CommandCenter.tc.doesCommandExist(command)) {
                CommandCenter.tc.commands.forEach{
                    if(it.value.name == command) {
                        if(it.value.nsfw == actual){
                            return BMessage("The status was already set to " + (if (actual) "NSFW" else "SFW"), true);
                        }
                        it.value.nsfw = actual;

                        return BMessage("Command status changed to " + (if (actual) "NSFW" else "SFW"), true);
                    }
                }
            } else {
                return BMessage("The command doesn't exist.", true);
            }
        }catch(e:ClassCastException){
            return BMessage("Something just went terribly wrong. Sorry 'bout that", true);
        }catch(e: IndexOutOfBoundsException){
            return BMessage("Not enough arguments. I need the command name and new state", true);
        }
        return BMessage("This is in theory unreachable code. If you read this message something bad happened", true);
    }
}

@Suppress("UNCHECKED_CAST")
class SERooms(val chat: SEChat) : AbstractCommand("inRooms", listOf()){
    override fun handleCommand(input: String, user: User): BMessage? {
        val sechats: List<SEChat> = CommandCenter.bot.chats.filter { it is SEChat } as List<SEChat>

        val (ids, sites) =
                Pair(sechats.map { it.rooms.map {it.id}.toMutableList()},
                sechats.map {it.rooms.map { it.parent.site.name}.toMutableList()})
        val so = mutableListOf<Int>()
        val se = mutableListOf<Int>()
        val mse = mutableListOf<Int>()

        for(j in 0 until ids.size) {
            for (i in 0 until ids[j].size) {
                when (sites[j][i]) {
                    "stackoverflow" -> so.add(ids[j][i])
                    "stackexchange" -> se.add(ids[j][i])
                    "metastackexchange" -> mse.add(ids[j][i])
                }
            }
        }

        val res = StringBuilder()
        res.append("[")
        so.forEachIndexed{ k,v-> res.append("$v" + if(k == so.size - 2) ", and " else if (k < so.size - 2) ", " else "] on SO, ") }
        res.append("[")
        se.forEachIndexed{ k,v-> res.append("$v" + if(k == se.size - 2) ", and " else if (k < se.size - 2) ", " else "] on SE, and ") }
        res.append("[")
        mse.forEachIndexed{ k,v-> res.append("$v" + if(k == mse.size - 2) ", and " else if (k < mse.size - 2) ", " else "] on MSE") }

        return BMessage(
                    ReplyBuilder(false)
                            .append("""I am currently in these rooms: $res""")
                            .build(),
                    true)
    }
}

class LocationCommand : AbstractCommand("location", listOf(), help="Shows the current bot location"){
    override fun handleCommand(input: String, user: User): BMessage? {
        return BMessage("${Configurations.INSTANCE_LOCATION} (${BotCore.LOCATION})", true)
    }
}


class CentralBlacklistStorage private constructor(var database: Database){
    var list: MutableMap<String, MutableList<Int>>
    init{
        println("Creating new Central Blacklist Storage")
        val existing = database.getMap("blacklisted-rooms")
        list = mutableMapOf()
        if(existing != null) {
            for ((site, roomList) in existing) {
                if (roomList is List<*>) {

                    @Suppress("UNCHECKED_CAST")
                    list[site] = (roomList.map { it.toString().toIntOrNull() }.filter { it != null } as List<Int>).toMutableList()
                }
            }
        }
    }

    fun blacklist(where: String, which: Int) : Boolean{
        if(!list.containsKey(where))
            list[where] = mutableListOf()
        if(list[where]!!.contains(which))
            return false
        list[where]!!.add(which)
        println(list)
        return true
    }

    fun unblacklist(where: String, which: Int) : Boolean{
        if(!list.containsKey(where)) {
            list[where] = mutableListOf()
            return false
        }
        if(!list[where]!!.contains(which))
            return false
        list[where]!!.remove(which)
        return true
    }

    fun save(){
        database.put("blacklisted-rooms", list)
    }

    fun isBlacklisted(where: String, which: Int) : Boolean{
        if(!list.keys.contains(where))
            list[where] = mutableListOf()
        return list[where]!!.contains(which)
    }

    companion object {
        private var instance: CentralBlacklistStorage? = null

        fun getInstance(database: Database) : CentralBlacklistStorage{
            return if(instance == null){
                instance = CentralBlacklistStorage(database);
                instance as CentralBlacklistStorage
            }else{
                instance as CentralBlacklistStorage
            }
        }

        fun save(){
            instance?.save();
        }
    }
}

class BlacklistRoom(val site: Chat) : AbstractCommand("ban-room", listOf(), "Blacklists a room"){
    override fun handleCommand(input: String, user: User): BMessage? {
        val content = splitCommand(input)["content"] ?: return BMessage("You have to tell me which room", true);
        val where = site.site.name
        val which = content.toIntOrNull() ?: return BMessage("You have to tell me which room to blacklist", true)
        val rank = Utils.getRank(user.userID, site.config)

        if(rank < 8)
            return BMessage("You need rank ${Constants.Ranks.getRank(rank)} (numeric: $rank) or higher to use this feature", true)

        val result = CentralBlacklistStorage.getInstance(site.database).blacklist(where, which)
        if(!result)
            return BMessage("Room already blacklisted", true)
        site.leaveServer(which);
        return BMessage("Room blacklisted", true);
    }
}

class UnblacklistRoom(val site: Chat) : AbstractCommand("unban-room", listOf(), "Removes the blacklisting of a room."){
    override fun handleCommand(input: String, user: User): BMessage? {
        val content = splitCommand(input)["content"] ?: return BMessage("You have to tell me which room", true);
        val where = site.site.name
        val which = content.toIntOrNull() ?: return BMessage("You have to tell me which room to unblock", true)
        val rank = Utils.getRank(user.userID, site.config)

        if(rank < 8)
            return BMessage("You need rank ${Constants.Ranks.getRank(rank)} (numeric: $rank) or higher to use this feature", true)

        val result = CentralBlacklistStorage.getInstance(site.database).unblacklist(where, which)
        if(!result)
            return BMessage("The room isn't blacklisted", true)

        return BMessage("Room unblocked", true);
    }
}

