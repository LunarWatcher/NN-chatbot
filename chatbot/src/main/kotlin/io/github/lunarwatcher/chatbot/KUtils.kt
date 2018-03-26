package io.github.lunarwatcher.chatbot

val mapped = mutableListOf(
        "&lt;" to "<",
        "&gt;" to ">",
        "&amp;" to "&",
        "&qout;" to "\""

)

fun cleanInput(input: String) : String {
    var cleaned = input
    for ((o, r) in mapped){
        cleaned = cleaned.replace(o, r)
    }
    return cleaned
}
