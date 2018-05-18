package io.github.lunarwatcher.chatbot

import io.github.lunarwatcher.chatbot.bot.command.CommandCenter

/**
 * Site specific config
 */
data class Site(var name: String, val url: String, val config: SiteConfig){

    fun `is`(name: String) : Boolean {
        return this.name == name;
    }
}

/**
 * The account details for the site
 */
data class SiteConfig(var username: String, var password: String, var email: String, var userID: Long, var messageOnLeave: Boolean = true);

class WelcomeMessages private constructor(var messages: MutableMap<String, MutableMap<Long, String>>){
    companion object {
        var INSTANCE: WelcomeMessages? = null

        fun initialize(db: Database) : WelcomeMessages{
            if(INSTANCE != null)
                return INSTANCE!!

            @Suppress("UNCHECKED_CAST")
            val cache = db.getMap("welcome-messages") as Map<String, Map<String, String>>?
            INSTANCE = if(cache != null && cache.isNotEmpty()){
                WelcomeMessages(cache.map { it.key to it.value.map{ it.key.toLong() to it.value}.toMap().toMutableMap() }
                        .toMap().toMutableMap())
            }else{
                WelcomeMessages(mutableMapOf())
            }
            return INSTANCE!!
        }
    }

    fun removeMessage(site: String, channel: Long){
        messages.get(site)?.remove(channel)
    }

    fun addMessage(site: String, channel: Long, message: String){
        if(messages[site] == null)
            messages[site] = mutableMapOf()
        messages[site]!![channel] = message
    }

    fun getMessage(site: String, channel: Long) : String?{
        return messages[site]?.get(channel)
    }

    fun hasMessage(site: String, channel: Long) : Boolean = messages[site]?.get(channel) != null

    fun save(){
        CommandCenter.INSTANCE.db.put("welcome-messages", messages)
    }
}

class MapUtils{
    companion object {

        /**
         * Again attempting to use the equals method to get a correct match
         */
        fun get(key: Any, map: Map<*, *>) : Any? {
            //firstOrNull isn't the first element in the list, it's the first available element with
            //a given key, or null if not found
            return map.entries
                    .firstOrNull {
                            it.key == key }
                    ?.value
        }
    }
}
