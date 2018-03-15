import Config
from discord.ext import commands as dCommands

import Commands


class SDiscord:
    discord = None

class Discord():

    def __init__(self):
        SDiscord.discord = self
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

            if message.content.startswith(Config.trigger + "whois"):
                try:
                    await self.bot.send_message(message.channel, await self.bot.get_user_info(message.split(" ")[1]).name)
                except:
                    await self.bot.send_message(message.channel, "Something went wrong.")
                return

            if message.content.startswith("<@{}>".format(self.bot.user.id)):

                content = message.content.replace("<@{}>".format(self.bot.user.id), "")
                await self.bot.send_message(message.channel, nnFun(content))
            elif message.content.startswith(Config.trigger):
                if message.content[len(Config.trigger):].startswith("summon"):
                    await self.bot.send_message(message.channel, "To add me to a server, you need the appropriate privileges to do so. In order to add me, you have to authorize me, which can be done at this link: https://discordapp.com/oauth2/authorize?client_id={}&scope=bot&permissions=0".format(self.bot.user.id))
                else:
                    await Commands.delegateDiscord(message, self.bot, message.author.id, nnFun)

        self.bot.run(Config.discordToken)


if __name__ == "__main__":
    bot = Discord()
    bot.start(None)

