@file:Suppress("unused")

package io.github.lunarwatcher.chatbot

import java.io.Reader
import java.util.concurrent.TimeUnit
import javax.script.Invocable
import javax.script.ScriptEngine

val mapped = mutableListOf(
        "&lt;" to "<",
        "&gt;" to ">",
        "&amp;" to "&",
        "&quot;" to "\"",
        "&#39;" to "'",
        "<i>" to "*",
        "</i>" to "*",
        "<b>" to "**",
        "</b>" to "**",
        "<code>" to "`",
        "</code>" to "`",
        "<strike>" to "---",
        "</strike>" to "---"

)

fun cleanInput(input: String) : String {
    var cleaned = input
    for ((o, r) in mapped){
        cleaned = cleaned.replace(o, r)
    }
    return cleaned
}

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
