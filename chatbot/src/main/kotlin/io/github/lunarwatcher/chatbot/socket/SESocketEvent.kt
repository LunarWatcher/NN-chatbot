package io.github.lunarwatcher.chatbot.socket

import com.fasterxml.jackson.databind.JsonNode


//1: message
//2: edited
//3: join
//4: leave
//5: room name/description changed
//6: star
//7: Debug message (?)
//8: ping - if called, ensure that the content does not contain a ping to the bot name if 1 is called
//        - WARNING: Using event 8 will trigger in every single active room.
//9:
//10: deleted
//11:
//12:
//13:
//14:
//15: Access level changed (kicks, RO added, read/write status changed, etc)
//16:
//17: Invite
//18: reply
//19: message moved out
//20: message moved in

//34: Username/profile picture changed

open class SESocketEvent(val eventID: Int, private var function: JsonNode.() -> Unit){
    open fun invoke(event: JsonNode){
        function.invoke(event)
    }
}

class JoinEvent(function: JsonNode.() -> Unit) : SESocketEvent(3, function){
    override fun invoke(event: JsonNode){

    }
}