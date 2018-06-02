package io.github.lunarwatcher.chatbot;

import io.github.lunarwatcher.chatbot.bot.Bot;

import java.util.TimerTask;

public class SaveTask extends TimerTask {
    Bot bot;

    public SaveTask(Bot bot) {
        this.bot = bot;
    }

    public void run(){
        System.out.println("Scheduled saving...");
        bot.save();
        System.out.println("Saved!");
    }
}
