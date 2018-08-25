package io.github.lunarwatcher.chatbot.bot.commands.discord

import io.github.lunarwatcher.chatbot.bot.chat.Message
import io.github.lunarwatcher.chatbot.bot.chat.ReplyMessage
import io.github.lunarwatcher.chatbot.bot.command.CommandGroup
import io.github.lunarwatcher.chatbot.bot.commands.AbstractCommand
import io.github.lunarwatcher.chatbot.bot.sites.discord.DiscordChat
import sx.blah.discord.handle.obj.IGuild

class ListGuildsCommand : AbstractCommand("listGuilds", listOf(), "Lists joined guilds", rankRequirement = 6, nsfw = false, commandGroup = CommandGroup.DISCORD) {
    override fun handleCommand(message: Message): List<ReplyMessage>? {
        if(!canUserRun(message.user, message.chat)){
            return listOf(ReplyMessage("You can't run this command"));
        }
        if(message.chat is DiscordChat){
            val client = (message.chat as? DiscordChat)?.client ?: return listOf(ReplyMessage("Failed to infer arguments.", true));
            val messages = mutableListOf<ReplyMessage>()
            var builder = StringBuilder()
            client.guilds.forEach {
                if(builder.length + it.name.length > 2000){

                    var msg = builder.toString().trim()
                    if(msg.endsWith(",")){
                        msg = msg.substring(0, msg.length - 1)
                    }
                    messages.add(ReplyMessage(msg, false));

                    builder = StringBuilder();
                }
                builder.append("${it.name} (UID ${it.stringID}), ")
            }

            return messages;
        }else{
            return listOf(ReplyMessage("Invalid site for this command", false));
        }
    }

}

class LeaveGuildCommand : AbstractCommand("leaveGuild", listOf(), "Leaves a guild", rankRequirement = 8, nsfw = false, commandGroup = CommandGroup.DISCORD) {
    override fun handleCommand(message: Message): List<ReplyMessage>? {
        if(!canUserRun(message.user, message.chat)){
            return listOf(ReplyMessage("You can't run this command"));
        }
        if(message.chat is DiscordChat){
            val client = (message.chat as? DiscordChat)?.client ?: return listOf(ReplyMessage("Failed to infer arguments.", true));
            val id = message.content.toLongOrNull() ?: return listOf(ReplyMessage("Failed to infer arguments: could not convert server ID to long"));
            val guild: IGuild = client.getGuildByID(id) ?: return listOf(ReplyMessage("Failed to find guild."));
            guild.leave()
            return listOf(ReplyMessage("Sucecssfully left guild"))
        }else{
            return listOf(ReplyMessage("Invalid site for this command", false));
        }
    }

}

