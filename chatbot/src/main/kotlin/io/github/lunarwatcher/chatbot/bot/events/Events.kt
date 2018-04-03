package io.github.lunarwatcher.chatbot.bot.events

import io.github.lunarwatcher.chatbot.bot.exceptions.RoomNotFoundException
import io.github.lunarwatcher.chatbot.bot.sites.Chat
import io.github.lunarwatcher.chatbot.bot.sites.se.SEChat
import java.util.*

class SilentRoomEvent(override val chat: Chat) : ScheduledEvent{

    override fun planNext(): Long {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun run() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}
