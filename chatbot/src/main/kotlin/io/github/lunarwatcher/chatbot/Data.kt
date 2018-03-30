package io.github.lunarwatcher.chatbot

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
