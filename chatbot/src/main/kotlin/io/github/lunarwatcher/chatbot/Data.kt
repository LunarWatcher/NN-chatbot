package io.github.lunarwatcher.chatbot

import io.github.lunarwatcher.chatbot.bot.command.CommandCenter
import java.io.Reader
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*
import java.util.concurrent.Callable
import java.util.concurrent.TimeUnit
import java.util.function.BiConsumer
import javax.script.Invocable
import javax.script.ScriptEngine
import javax.script.ScriptEngineManager

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

fun main(args: Array<String>){

    val process = Runtime.getRuntime()
            .exec(arrayOf("python", "Network/bot.py", "--training=false", "--mode=2"))

    println("Start")
    process.waitFor()
    println("End")
    println(process.exitValue())

    println(process.inputStream.bufferedReader().use { it.readText() })
    println(process.errorStream.bufferedReader().use { it.readText() })
}

