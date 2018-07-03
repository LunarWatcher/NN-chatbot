package io.github.lunarwatcher.chatbot.data

import io.github.lunarwatcher.chatbot.RankInfo
import io.github.lunarwatcher.chatbot.bot.sites.Chat

@Suppress("NAME_SHADOWING")
class BotConfig(val site: Chat){

    val ranks: MutableMap<Long, RankInfo> = mutableMapOf();
    val homes: MutableList<Long> = mutableListOf();

    fun addHomeRoom(newRoom: Long) : Boolean{
        homes.filter { it == newRoom }
                .forEach { return false }

        homes.add(newRoom);
        return true;
    }

    fun removeHomeRoom(rr: Long) : Boolean{
        for(i in (homes.size - 1)downTo 0){
            if(homes[i] == rr){
                homes.removeAt(i)
                return true;
            }
        }

        return false;
    }

    fun set(homes: List<Long>?, ranked: Map<Long, RankInfo>?){
        if(homes != null) {
            homes.forEach { this.homes.add(it.toLong() )}
        }
        ranked?.forEach{
            this.ranks[it.key] = it.value
        }
    }

    fun addRank(user: Long, rank: Int){

        ranks[user] = RankInfo(user, rank);
    }

    fun getRank(user: Long) : RankInfo? = ranks[user];
}