

class Memory:
    """Class for managing the bot's memories (not conversation memory,
    but remembering stuff like name and whatever else the bot
    possibly could need to know."""

    def __init__(self, netconfig):
        self.name = netconfig.botName