import logging.handlers
import os
import traceback

import Config
from ChatExchange.chatexchange.client import Client
from ChatExchange.chatexchange.events import MessagePosted, MessageEdited

import Commands

logger = logging.getLogger(__name__)

class Stackexchange():
    sites = []
    nnFun = None

    @staticmethod
    def getSite(target):
        return [site for site in Stackexchange.sites if site.name == target]

def start():
    password = Config.sePassword
    startSites(["stackoverflow.com", "stackexchange.com", "meta.stackexchange.com"], password)

def startSites(sites: [], password):
    for site in sites:
        if Config.isSiteEnabled(site):
            Stackexchange.sites.append(SESite(site, password))
def onMessage(message, client):

    if not isinstance(message, MessagePosted) and not isinstance(message, MessageEdited):
        logger.debug("Ignored event: %r", message)
        return;

    print("")
    print("!>> (%s) %s" % (message.user.name, Commands.Commands.cleanMessage(message.content)))
    Commands.delegate(message, message.user.id, client, Stackexchange.nnFun)

class SESite(object):

    def __init__(self, siteName: str, password: str):
        self.name = siteName
        self.client = Client(siteName)
        self.client.login(Config.email, password)
        self.rooms = []
        for room in Config.homes[siteName]:
            self.join(room)
        if os.path.isfile(Config.storageDir + self.name + ".rooms"):
            with open(Config.storageDir + self.name + ".rooms", "r") as f:
                lines = f.readlines()
                for line in lines:
                    try:
                        self.join(int(line))
                    except ValueError:
                        print("Invalid room ID: " + line)

    def stop(self):
        with open(Config.storageDir + self.name + ".rooms", "w") as f:
            for room in self.rooms:
                f.write(str(room) + "\n")

        for room in self.rooms:
            self.leave(room)
        self.client.logout()


    def join(self, room: int):
        if room in self.rooms:
            return
        r = self.client.get_room(room)
        r.join()
        if not Config.startQuiet:
            r.send_message("Howdy folks!")
        r.watch(onMessage)

        self.rooms.append(room)

    def leave(self, room: int):
        if room not in self.rooms:
            return

        rm = self.client.get_room(room)
        if not Config.leaveQuiet:
            rm.send_message("Adios! I'm off to the tavern")

        rm.leave()
        try:
            self.rooms.remove(room)
        except:
            print(traceback.format_exc())


    def getClient(self):
        return self.client

    def getRoom(self, room: int):
        return self.client.get_room(room)

logging.basicConfig(level=logging.INFO)
logger.setLevel(logging.DEBUG)
wrapper_logger = logging.getLogger('chatexchange.client')
wrapper_handler = logging.handlers.TimedRotatingFileHandler(
    filename='client.log',
    when='midnight', delay=True, utc=True, backupCount=7,
)
wrapper_handler.setFormatter(logging.Formatter(
    "%(asctime)s: %(levelname)s: %(threadName)s: %(message)s"
))
wrapper_logger.addHandler(wrapper_handler)
