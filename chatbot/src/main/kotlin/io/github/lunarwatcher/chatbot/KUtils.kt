@file:Suppress("unused")

package io.github.lunarwatcher.chatbot

import io.github.lunarwatcher.chatbot.utils.Utils
import org.jsoup.parser.Parser
import java.io.Reader
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern
import javax.script.Invocable
import javax.script.ScriptEngine

const val DATE_PATTERN = "E, d MMMM HH:mm:ss.SSSS Y z ('GMT' ZZ)"

val formatter = SimpleDateFormat(DATE_PATTERN, Locale.ENGLISH)
const val FLAG_REGEX = """(?i)((?:\s|^)--[a-z\-\d]+)(?:\s*(".+?"))?""";
var FLAG_PATTERN = Pattern.compile(FLAG_REGEX)!!
const val NO_DEFINED_RANK = -1
const val ARGUMENT_REGEX = "(?:\"(.*?)(?<!\\\\)\"| *([a-z0-9]+) *)"
val ARGUMENT_PATTERN = Pattern.compile(ARGUMENT_REGEX)

val mapped = mutableListOf(
        "\\\"" to "\"",
        // Breaking unicode chars
        "\u202E" to "",
        "\u200b" to "",
        "\u200c" to "",
        "\u200a" to ""
)

val mappedRegex = mutableListOf(
        "<a href=\"(.*?)\"( rel=\".*?\")?>(.*?)</a>".toRegex() to "[$3]($1)",
        "^\\n+(.*?)\$".toRegex() to "$1",
        "</?strike>".toRegex() to "---",
        "</?code>".toRegex() to "`",
        "</?b>".toRegex() to "**",
        "</?i>".toRegex() to "*",
        "<br(?:\\s*/)?>".toRegex() to "\n"
)

fun String.prep() = this.trim().remove(" ")

fun String.remove(what: String) = this.replace(what, "")
fun String.remove(what: Regex) = this.replace(what, "")

fun cleanInput(input: String) : String {
    var cleaned = input
    cleaned = Parser.unescapeEntities(cleaned, true)
    for ((o, r) in mapped){
        cleaned = cleaned.replace(o, r)
    }
    for((regex, replacement) in mappedRegex){
        cleaned = cleaned.replace(regex, replacement)
    }
    if(cleaned.startsWith("\n"))
        cleaned = cleaned.replaceFirst("\n", "")
    return cleaned
}

fun <T, O> map(list: List<T>, function: () -> O) : List<O>{
    return list.map { function() }
}

/**
 * Extension function for [cleanInput].
 */
fun String.clean() = cleanInput(this)

fun String.createInvocable(engine: ScriptEngine) : Invocable {
    engine.eval(this)
    return engine as Invocable
}

fun Reader.createInvocable(engine: ScriptEngine) : Invocable {
    engine.eval(this)
    return engine as Invocable
}
const val command = "git rev-parse HEAD"
fun getRevision() : String{

    val process = Runtime.getRuntime().exec(command)
    process.waitFor(5000, TimeUnit.MILLISECONDS)
    val revision = process.inputStream.bufferedReader().readLine()
    return if(revision == null) "Unknown revision" else "Revision $revision"
}

fun <T> Array<T>.safeGet(index: Int) : T? {
    if(index >= size)
        return null
    return this[index]
}

fun <T> List<T>.safeGet(index: Int) : T? {
    if(index >= size)
        return null
    return this[index]
}

fun <T> T.equalsAny(vararg others: T) : Boolean = others.firstOrNull { it == this } != null
fun <A, B> zip(one: Collection<A>, two: Collection<B>) : Map<A, B>
        = one.zip(two).toMap()

fun <K, V> Map<K, V>.getMaxLen() : Int{
    var current = 0
    for (k in this){
        if(k.toString().length > current)
            current = k.toString().length

    }
    return current
}

fun getMaxLen(list: MutableList<String>) : Int{
    val longest = list
            .map { it.length }
            .max()
            ?: 0;

    return longest;
}

fun <T> List<T>.randomItem() : T{
    return get(Utils.random.nextInt(this.size))
}