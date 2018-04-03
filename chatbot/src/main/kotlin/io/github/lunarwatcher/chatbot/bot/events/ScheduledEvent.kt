package io.github.lunarwatcher.chatbot.bot.events

import io.github.lunarwatcher.chatbot.bot.sites.Chat

interface ScheduledEvent {
    val chat: Chat

    fun run()
    fun planNext(): Long
}

