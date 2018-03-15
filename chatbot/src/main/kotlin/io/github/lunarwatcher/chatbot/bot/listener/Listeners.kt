package io.github.lunarwatcher.chatbot.bot.listener

import io.github.lunarwatcher.chatbot.bot.chat.BMessage
import io.github.lunarwatcher.chatbot.bot.commands.User

class KnockKnock(val mention: MentionListener) : AbstractListener("Knock knock", "The name says it all"){
    var context: Context? = null

    override fun handleInput(input: String, user: User): BMessage? {
        val input = input.toLowerCase();
        if(!mention.isMentioned(input) && context == null){
            return null;

        }else if(mention.isMentioned(input) && context == null) {

            if (input.contains("knock knock")) {
                context = Context(0, user.userID)
            }else
                return null

        }

        if(context?.user != user.userID){
            return null;
        }

        mention.ignoreNext();
        val inp =
        when(context?.index){
            0 ->{
                context?.next()
                return BMessage("Who's there?", true);
            }
            1->{
                val who = input + " who?"
                context?.next()
                return BMessage(who, true);
            }
            2->{
                context = null;
                return BMessage("Hahaha!", true)
            }
            else->return null;
        }
    }
}

class Context(var index: Int, var user: Long) {
    fun next(){
        index++;
    }
}

class Train(val count:Int) : AbstractListener("Train", "Finds message trains and joins in"){

    init{
        if(count < 2)
            throw IllegalArgumentException("The count cannot be < 2!");
    }

    var previous: String? = null;
    var pCount: Int = 0;
    var preUser: Long = 0;
    override fun handleInput(input: String, user: User): BMessage? {
        if(previous == null) {
            pCount = 1;
            previous = input;
            preUser = user.userID;
            return null;
        }else{
            if(previous?.toLowerCase() == input.toLowerCase()){
                if(preUser == user.userID)
                    return null;
                preUser = user.userID;
                pCount++;
            }else{
                previous = input;
                pCount = 1;
                preUser = user.userID;
                return null;
            }

            if(pCount >= count){
                pCount = 0;
                previous = null;
                return BMessage(previous, false);
            }
        }

        return null;
    }
}