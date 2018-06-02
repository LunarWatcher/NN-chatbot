@file:Suppress("unused")

package io.github.lunarwatcher.chatbot

import org.jsoup.parser.Parser
import java.io.Reader
import java.util.concurrent.TimeUnit
import java.util.function.Predicate
import javax.script.Invocable
import javax.script.ScriptEngine

val mapped = mutableListOf(
        "\\\"" to "\"",//This has to come after the previous one; otherwise it breaks
        "<i>" to "*",
        "</i>" to "*",
        "<b>" to "**",
        "</b>" to "**",
        "<code>" to "`",
        "</code>" to "`",
        "<strike>" to "---",
        "</strike>" to "---",
        "<br>" to "\n"
)

val mappedRegex = mutableListOf(
        "\\B<a href=\"(.*?)\" (rel=\".*?\")?>(.*?)</a>\\B".toRegex() to "[$3]($1)",
        "^\\n+(.*?)\$".toRegex() to "$1"
)

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
