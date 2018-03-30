@file:Suppress("unused")

package io.github.lunarwatcher.chatbot

import java.io.Reader
import javax.script.Invocable
import javax.script.ScriptEngine

val mapped = mutableListOf(
        "&lt;" to "<",
        "&gt;" to ">",
        "&amp;" to "&",
        "&quot;" to "\"",
        "&#39;" to "'"

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
