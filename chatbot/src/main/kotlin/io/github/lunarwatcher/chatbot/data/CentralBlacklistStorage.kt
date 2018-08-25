package io.github.lunarwatcher.chatbot.data

import io.github.lunarwatcher.chatbot.Database

class CentralBlacklistStorage private constructor(var database: Database){
    var list: MutableMap<String, MutableList<Long>>
    init{
        println("Creating new Central Blacklist Storage")
        val existing = database.getMap("blacklisted-rooms")
        list = mutableMapOf()
        if(existing != null) {
            for ((site, roomList) in existing) {
                if (roomList is List<*>) {

                    @Suppress("UNCHECKED_CAST")
                    list[site] = (roomList.map { it.toString().toLongOrNull() }.filter { it != null } as List<Long>).toMutableList()
                }
            }
        }
    }

    fun blacklist(where: String, which: Long) : Boolean{
        if(!list.containsKey(where))
            list[where] = mutableListOf()
        if(list[where]!!.contains(which))
            return false
        list[where]!!.add(which)
        println(list)
        return true
    }

    fun unblacklist(where: String, which: Long) : Boolean{
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

    fun isBlacklisted(where: String, which: Long) : Boolean{
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