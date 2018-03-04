import logging.handlers

from ChatExchange.chatexchange.client import Client
from ChatExchange.chatexchange.browser import LoginError
from ChatExchange.chatexchange.events import MessagePosted, MessageEdited, MessageStarred
from ChatExchange.chatexchange.messages import Message
import Config
import Commands
import traceback


logger = logging.getLogger(__name__)

class Stackexchange():
    sites = []
    nnFun = None

def start():
    email = Config.email
    password = Config.sePassword

    Stackexchange.sites.append(SESite("meta.stackexchange.com", password))
    Stackexchange.sites.append(SESite("stackexchange.com", password))
    Stackexchange.sites.append(SESite("stackoverflow.com", password))
    Commands.Command.sites.append("meta.stackexchange.com")
    Commands.Command.sites.append("stackexchange.com")
    Commands.Command.sites.append("stackoverflow.com")
    while True:
        message = input(">> ")
        if(message == "~!stop" or message == "~!break"):
            break


    for site in Stackexchange.sites:
        site.stop()

def onMessage(message, client):
    if not isinstance(message, MessagePosted) and not isinstance(message, MessageEdited):
        logger.debug("Ignored event: %r", message)
        return;

    print("")
    print("!>> (%s) %s" % (message.user.name, Commands.Commands.cleanMessage(message.content)))
    Commands.delegate(message, client, message.user.id, Stackexchange.nnFun)

class SESite(object):

    def __init__(self, siteName: str, password: str):
        self.client = Client(siteName)
        self.client.login(Config.email, password)
        self.rooms = []
        for room in Config.homes[siteName]:
            self.join(room)

    def stop(self):
        for room in self.rooms:
            self.leave(room)
        self.client.logout()


    def join(self, room: int):
        r = self.client.get_room(room)
        r.join()
        if not Config.startQuiet:
            r.send_message("Howdy folks!")
        r.watch(onMessage)

        self.rooms.append(room)

    def leave(self, room: int):
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
