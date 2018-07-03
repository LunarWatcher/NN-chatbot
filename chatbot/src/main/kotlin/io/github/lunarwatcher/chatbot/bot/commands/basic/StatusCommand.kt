package io.github.lunarwatcher.chatbot.bot.commands.basic

import com.google.common.base.Strings
import io.github.lunarwatcher.chatbot.BotCore
import io.github.lunarwatcher.chatbot.bot.ReplyBuilder
import io.github.lunarwatcher.chatbot.bot.chat.Message
import io.github.lunarwatcher.chatbot.bot.chat.ReplyMessage
import io.github.lunarwatcher.chatbot.bot.commands.AbstractCommand
import io.github.lunarwatcher.chatbot.bot.listener.StatusListener
import io.github.lunarwatcher.chatbot.bot.sites.discord.DiscordChat
import io.github.lunarwatcher.chatbot.formatter
import io.github.lunarwatcher.chatbot.getMaxLen
import io.github.lunarwatcher.chatbot.utils.Utils
import org.joda.time.Instant
import org.joda.time.Period

class StatusCommand(val statusListener: StatusListener) : AbstractCommand("status", listOf("leaderboard"), desc="Shows the leading chatters"){

    override fun handleCommand(message: Message): ReplyMessage? {
        val siteS = message.chat.name
        val site = message.chat

        val room = if(message.chat is DiscordChat)
            message.user.args.firstOrNull { it.first == "guildID" }?.second?.toLong() ?: (message.chat as DiscordChat).getChannel(message.roomID)?.guild?.longID ?: return null
        else message.roomID

        if(statusListener.users[siteS] == null)
            statusListener.users[siteS] = mutableMapOf()

        if (statusListener.users.isEmpty() || statusListener.users[siteS]!![room] == null)
            return ReplyMessage("No users registered yet. Try again later", true)
        if(statusListener.users[siteS]!![room]!!.isEmpty())
            return ReplyMessage("No users registered yet. Try again later", true)

        if(!statusListener.users[siteS]!!.keys.contains(room))
            statusListener.users[siteS]!![room] = mutableMapOf()

        if(message.content.contains("--clear")){
            if(Utils.getRank(message.user.userID, site.config) < 9)
                return ReplyMessage("You need rank 9 or higher to clear the status", true);
            if(message.content.contains("--confirm")){
                statusListener.users.clear()
                return ReplyMessage("Erased like your browser history.", true)
            }
            return ReplyMessage("Confirm with --confirm", true)
        }

        val buffer = statusListener.users[siteS]!![room]!!
        val localCopy = mutableMapOf<String, Long>()

        for((uid, messages) in buffer){
            val username = site.getUsername(uid)
            localCopy["$username ($uid)"] = messages

        }
        val longFirst = localCopy.map { it.value to it.key }.associateBy ({it.first}, {it.second}).toSortedMap(compareBy{-it})

        val reply = ReplyBuilder()
        reply.discord = site.name == "discord"

        val now = Instant()
        val duration = Period(BotCore.STARTED_AT, now)

        val days = duration.days
        val hours = duration.hours
        val minutes = duration.minutes
        val seconds = duration.seconds

        reply.fixedInput().append("Started ${formatter.format(BotCore.STARTED_AT.toDate().time)} (running for $days days, $hours hours, $minutes minutes, and $seconds seconds.)").nl()
        reply.fixedInput().append("Message status").nl()

        val maxLen = localCopy.getMaxLen()
        val x = "Username:"

        reply.fixedInput().append("$x${Strings.repeat(" ", maxLen - x.length + 2)}- Message count").nl()

        for((count, who) in longFirst){
            reply.fixedInput().append("$who${Strings.repeat(" ", maxLen - who.length + 2)}- $count").nl()
        }

        return ReplyMessage(reply.toString(), false)
    }


}