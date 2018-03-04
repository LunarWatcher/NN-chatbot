
# Google command
# Kill command
# Lick command
# Give command

# Ban, unban, promote, demote
# wikipedia
# xkcd

import discord
import Config
import random as r
import os
import time

r.seed = time.time()

class Command():
    sites = []
    def __init__(self, name: str, aliases: [], help: str, desc: str, handlerMethod, rankReq: int = 1):
        self.name = name;
        self.aliases = aliases
        self.help = help
        self.desc = desc
        self.rankReq = rankReq
        self.handlerMethod = handlerMethod

    def onMessage(self, specificCommand, message, userId: int, indentBased=True):
        # TODO implement rank system
        userRank = 1
        reply, response = self.handlerMethod(specificCommand, message, indentBased, userRank)
        return reply, response

class StaticResponses:
    licks = ["Tastes horrible!", "Tastes like a wet cat!", "Tastes like a wet dog!", "Tastes like pizza!",
             "Tastes like sewer! *shivers*", "*dies*", "*shivers*", "*passes out*",
             "Tastes AWFUL! *goes back in time to avoid licking it in the first place*"]
    kills = ["*shoots {}*. ***HEADSHOT!***", "{} has been disposed of.",
             "{} was in an airplane \"accident\"", "It was in the news. {} didn't watch enough MLP!",
             "{} was beamed into space", "PIRATES! {} was ejected along with the rest of the crew",
             "{} didn't watch their step and fell off a cliff", "Unfortunately, {} hasn't been heard from in a few days",
             "I can't dispose of {}. They're already disposed of", "It's all over the news today, {} was killed by an angry mob!",
             "*sending poisoned dinner to {}...*"]

async def delegateDiscord(message: discord.Message, dClient: discord.Client, uid: int, nnFun):
    # The discord part checks for the presence of the trigger before callig this method
    triggerless = str(message.content)[len(Config.trigger):]
    cmdName = triggerless.split()[0]
    messageContent = triggerless[len(cmdName):].strip()
    try:
        _, replyContent = Commands.commands[Commands.getCommandName(cmdName)].onMessage(cmdName, messageContent, uid, False)
        await dClient.send_message(message.channel, replyContent)
    except KeyError:
        await dClient.send_message(message.channel, "Sorry, that's not a command I know");

def delegate(message, uid: int, client, nnFun):
    if message.content.startswith(Config.netTrigger):
        message.message.reply(nnFun(Commands.cleanMessage(message.content.replace(Config.netTrigger, "").strip())))
        return
    # TODO listeners
    if not message.content.startswith(Config.trigger):

        return
    cleaned = Commands.cleanMessage(message.content)
    triggerless = str(cleaned)[len(Config.trigger):]
    cmdName = triggerless.split()[0]
    messageContent = triggerless[len(cmdName) + 1:]

    try:
        reply, replyContent = Commands.commands[Commands.getCommandName(cmdName)].onMessage(cmdName, messageContent, uid)
        if(reply):
            message.message.reply(replyContent)
        else:
            message.room.send_message(replyContent)
    except KeyError:
        message.message.reply("Sorry, that's not a command I know");

def helpCommand(specificCommand, message, indentBased, userRank: int):
    commandNames = list(Commands.commands.keys())
    commandDescriptions = [cmd.desc for name, cmd in Commands.commands.items()]
    res = ""
    maxLen = 0
    for n in commandNames:
        maxLen = max(maxLen, len(n))
    for i in range(len(commandNames)):
        adjustedLen: int = (maxLen) - len(commandNames[i])
        res = res + commandNames[i] + "".join([" " for i in range(adjustedLen)]) + " | " + commandDescriptions[i] + "\n"

    return False, fixedFormat(res, not indentBased)

def lickCommand(specificCommand, message, discord: bool, userRank: int):
    message = message.strip()
    if message == "":
        return "You have to tell me who to lick"
    return True, "*licks " + message + "*. " + StaticResponses.licks[r.randint(0, len(StaticResponses.licks) - 1)]

def killCommand(specificCommand, message, discord: bool, userRank: int):
    message = message.strip()
    print(">>:" + message)
    if message == "":
        return "You have to tell me who to kill"
    return True, StaticResponses.kills[r.randint(0, len(StaticResponses.kills) - 1)].format(message)

# Utils

def fixedFormat(stringToFormat: str, discord: bool):
    result = ""
    if not discord:
        for line in stringToFormat.split("\n"):
            result += ''.join([" " for i in range(4)]) + line + "\n"
    else:
        result += "```"
        result += stringToFormat
        result += "```"
    return result

class UserInfo:

    def __init__(self, uid: int, initialRank = 1):
        self.uid = uid;
        self.rank = initialRank

    def ban(self):
        self.rank = 0
    def unban(self):
        self.rank = 1
    def setRank(self, rank: int):
        if(rank < 0):
            rank = 0;
        if rank > 10:
            rank = 10

        self.rank = rank

class Site:

    def __init__(self, name: str):
        self.name = name
        self.users = dict()
        if os.path.isfile(Config.storageDir + "privs_" + name.replace(".", "_") + ".dat"):
            try:
                with open(Config.storageDir + "privs_" + name.replace(".", "_") + ".dat", "r") as f:
                    unr = [s.strip() for s in f.readline().split("+++$+++")]
                    if(len(unr) != 2):
                        print(unr)
                    else:
                        self.users.update({str(unr[0]) : int(unr[1])})
            except:
                # Something IO-related went to hell, most likely that the file is empty
                # Ignore it, it doesn't matter
                pass

    def save(self):
        fileName = "privs_" + self.name.replace(".", "_") + ".dat"
        with open(Config.storageDir + fileName, "w") as f:
            for username, idx in self.users.items():
                f.write(username + " +++$+++ " + idx)



class Commands:
    commands = {
        "help" : Command("help", ["halp", "hilfe", "help"], "", "Lists the bots commands", handlerMethod=helpCommand, rankReq=1),
        "lick" : Command("lick", [], "", "Licks someone", handlerMethod=lickCommand, rankReq=1),
        "kill" : Command("kill", ["assassinate"], "", "Disposes of someone", handlerMethod=killCommand, rankReq=1),
    }

    @staticmethod
    def getCommandName(foundName: str):
        try:
            cmd = Commands.commands[foundName.strip()]#This throws an exception if not found
            if cmd is None:
                raise KeyError()
            return foundName# Meaning if it gets here, it is found and the command name is this name
        except KeyError:
            # This is where things gets complicated.
            pass
        # Since the key isn't the obvious one (the name itself), it is most likely an alias
        # So iterate the commands dict...
        for cmdN, command in Commands.commands.items():
            for alias in command.aliases:#... iterate the aliases ...
                if foundName == alias:# ... and if the alias matches...
                    return cmdN #... return the name of the command.
        return None# Otherwise, all options are exhausted. Return None

    cleaning = {"&quot;": "\"", "&#39;" : '\'',
                "&gt;": ">", "&lt;": "<"}
    @staticmethod
    def cleanMessage(raw: str):
        fixed = raw
        for k, v in Commands.cleaning.items():
            fixed = fixed.replace(k, v)
        return fixed

