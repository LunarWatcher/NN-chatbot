package io.github.lunarwatcher.chatbot.bot.commands

import io.github.lunarwatcher.chatbot.Constants
import io.github.lunarwatcher.chatbot.bot.chat.BMessage
import io.github.lunarwatcher.chatbot.bot.command.CommandCenter
import io.github.lunarwatcher.chatbot.bot.command.CommandCenter.TRIGGER
import io.github.lunarwatcher.chatbot.bot.sites.Chat
import io.github.lunarwatcher.chatbot.utils.Utils

class CheckCommand(var site: Chat) : AbstractCommand("getRank", listOf(), "Checks as user's role",
        "Supported roles: `admin`, `normal`, `privileged`(/`priv`) and `banned`. Unknown roles defaults to a normal check." +
                " Example usage: `" + TRIGGER + "check admin 6296561`"){

    override fun handleCommand(input: String, user: User): BMessage? {
        if(!matchesCommand(input)){
            return null;
        }

        val split = splitCommand(input);
        if(split["content"] == null)
            return BMessage("You have to specify a user to check", true);
        val id = try{
            split["content"]!!.toLong()
        }catch(e: ClassCastException){
            return BMessage("Please specify a valid user ID", true);
        }catch(e: NumberFormatException){
            val usr = site.config.ranks.entries.firstOrNull{
                it.value.username?.replace(" ", "")?.toLowerCase()?.trim() ==
                        (split["content"] ?: return BMessage("Please specify a valid user ID", true))
                                .replace(" ", "").toLowerCase().trim()
            }
            usr?.value?.uid ?: return BMessage("You have to supply a valid user ID/indexed username", true);
        }catch(e: NullPointerException){
            site.commands.crash.crash(e);
            return BMessage("Something bad happened. Type `${CommandCenter.TRIGGER}logs` to see the stacktrace", true);
        }

        val rank: Int = Utils.getRank(id, site.config)
        val username: String? = site.config.ranks[id]?.username
        if(rank == 0)
            return BMessage((username ?: "The user $id").toString() + " is ${Constants.Ranks.getRank(rank).toLowerCase()}", true)

        return BMessage((username ?: "The user $id").toString() + "'s rank is ${Constants.Ranks.getRank(rank).toLowerCase()}", true)
    }

}
