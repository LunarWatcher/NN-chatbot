package io.github.lunarwatcher.chatbot.bot.commands.stackexchange

import io.github.lunarwatcher.chatbot.Configurations
import io.github.lunarwatcher.chatbot.bot.ReplyBuilder
import io.github.lunarwatcher.chatbot.bot.chat.Message
import io.github.lunarwatcher.chatbot.bot.chat.ReplyMessage
import io.github.lunarwatcher.chatbot.bot.command.CommandCenter
import io.github.lunarwatcher.chatbot.bot.commands.AbstractCommand
import io.github.lunarwatcher.chatbot.bot.sites.se.SEChat

@Suppress("UNCHECKED_CAST")
class SERooms : AbstractCommand("inRooms", listOf()){
    override fun handleCommand(message: Message): List<ReplyMessage>? {
         if(message.chat !is SEChat)
            return listOf(ReplyMessage("Invalid site. BlameCommand ${Configurations.CREATOR}", true))

        val sechats: List<SEChat> = CommandCenter.bot.chats.filter { it is SEChat } as List<SEChat>

        val (ids, sites) =
                Pair(sechats.map { it.rooms.map {it.id}.toMutableList()},
                        sechats.map {it.rooms.map { it.parent.name}.toMutableList()})
        val so = mutableListOf<Int>()
        val se = mutableListOf<Int>()
        val mse = mutableListOf<Int>()

        for(j in 0 until ids.size) {
            for (i in 0 until ids[j].size) {
                when (sites[j][i]) {
                    "stackoverflow" -> so.add(ids[j][i])
                    "stackexchange" -> se.add(ids[j][i])
                    "metastackexchange" -> mse.add(ids[j][i])
                }
            }
        }

        val res = StringBuilder()
        res.append("[")
        so.forEachIndexed{ k,v-> res.append("$v" + if(k == so.size - 2) ", and " else if (k < so.size - 2) ", " else "] on SO, ") }
        res.append("[")
        se.forEachIndexed{ k,v-> res.append("$v" + if(k == se.size - 2) ", and " else if (k < se.size - 2) ", " else "] on SE, and ") }
        res.append("[")
        mse.forEachIndexed{ k,v-> res.append("$v" + if(k == mse.size - 2) ", and " else if (k < mse.size - 2) ", " else "] on MSE") }

        return listOf(ReplyMessage(
                ReplyBuilder(false)
                        .append("""I am currently in these rooms: $res""")
                        .build(),
                true));
    }
}