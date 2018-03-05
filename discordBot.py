import discord
from discord.ext import commands as dCommands

import Config
import Commands

class Discord():

    def __init__(self):
        self.bot = dCommands.Bot(command_prefix=Config.trigger)
        print("Discord created")

    def start(self, nnFun):

        print("Booting Discord!")
        @self.bot.event
        async def on_ready():
            print("Logged in!")

        @self.bot.event
        async def on_message(message):

            print("!>>" + message.content)
            if message.author == self.bot.user:
                return;

            if message.content.startswith("<@!{}>".format(self.bot.user.id)):

                content = message.content.replace("<@!{}>".format(self.bot.user.id), "")
                await self.bot.send_message(message.channel, nnFun(content))
            elif message.content.startswith(Config.trigger):
                await Commands.delegateDiscord(message, self.bot, message.author.id, nnFun)

        self.bot.run(Config.discordToken)





