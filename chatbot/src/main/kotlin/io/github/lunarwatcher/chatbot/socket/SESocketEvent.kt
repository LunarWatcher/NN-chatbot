package io.github.lunarwatcher.chatbot.socket

import com.fasterxml.jackson.databind.JsonNode
import io.github.lunarwatcher.chatbot.bot.sites.se.SERoom

// ##### Event ID reference ##### //
//1: message
//2: edited
//3: join
//4: leave
//5: room name/description changed
//6: star
//7: Debug message (?)
//8: ping
//10: deleted
//15: Access level changed (kicks, RO added, read/write status changed, etc)
//17: Invite
//18: reply
//19: message moved out
//20: message moved in
//34: Username/profile picture changed

/**
 * EventHandler interface; the primary handler for SE socket events.
 */
interface EventHandler{
    fun onEventReceived(origin: SERoom, eventId: Int, rawEvent: JsonNode, eventNode: JsonNode)
}

