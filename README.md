# NN-chatbot

This is a chatbot both designed for neural network interraction in addition to the default command-based system. Note that it's still a work in progress, so there are bound to be bugs.

# Install

## Dependencies

* discord.py
* numpy
* tensorlayer
* tensorflow (**`tensorflow-gpu` is recommended** - CPU is extremely slow)
* sklearn
* tensorboard (will be added layer)
* asyncio
* nltk
* Python 3.6 (anything under 3.5 requires code edits because of the async keyword. 3.6 is the only tested version)

### Please note:

In some cases, the dependencies for the depencencies of this project doesn't get installed. I.e. Tensorlayer requires Scipy, which for some reason doesn't get installed. Should this happen, `pip install` the missing packages. Using `pip` on the package installs should be enough to avoid missing dependencies. However, should there still be missing dependencies, manually install them. 

## Dataset

The [Corell Movie Dialog Corpus](http://www.cs.cornell.edu/~cristian/Cornell_Movie-Dialogs_Corpus.html) is the dataset the bot is designed for at the moment. There will be support for custom dataset (and conversation scrapping for personalized conversations), but that's an issue for later.

## Setup

The text files in the corell movie dialog corpus goes in a directory called `raw_data`. 

Rename `Config.py_example` to `Config.py` and fill in the necessary values. The bot is designed to run on all three sites, in addition to inthe console, so there is currently no system to handle missing sites. You can of course run it in the console and not deal with websites and API's.

And finally, when all the data is added, run `bot.py`. It'll set up the necessary files and start training once it's done. Checkpoints are saved every epoch (and it overrides the past save).

## System

The code is based on a bag of words, in order to be able to vectorize the words properly. 

# Known issues

* Changing the vocab size prevents loading of previous save

Unfortunately, by design. The Embedding layer takes a fixed input which is saved in the checkpoint, meaning changes in dimensions will prevent loading of the previous model. Figuring out a way to get an expanding vocab is a top priority.

* TensorFlow throws OutOfMemoryErrors

The easy way: Reduce the input dim or the vocab size. 
The hard way: Get a better GPU with more vram, or get more GPUs. If you use the CPU, get more RAM.

* Nonsense replies

Train more. 

* No memory

[Image showing the differences between some of the core chatbot types](https://www.marutitech.com/wp-content/uploads/2017/04/Chatbot-conversation-framework.png) (not embedded because it didn't load properly)

General chatbots are incredibly hard to make. Creating knowledge-based generative models isn't exactly something that's documented in the tensorflow documentation (or keras for that matter). For now, getting sensible replies is the top priority, along with expandable vocabulary. Adding generative memory (not retrieval-based) with context is a task for later

* Training is slow

Use a GPU if possible. If you already are, use more. Or decrease the vocab size, it speeds it up a little.


# Licensing

Licensed under either of

* Apache License, Version 2.0, ([LICENSE-APACHE](https://github.com/LunarWatcher/NN-chatbot/blob/master/LICENSE-APACHE) or http://www.apache.org/licenses/LICENSE-2.0)
* MIT license ([LICENSE-MIT](https://github.com/LunarWatcher/NN-chatbot/blob/master/LICENSE-MIT) or http://opensource.org/licenses/MIT)

at your option.

## Contribution Licensing

Unless you explicitly state otherwise, any contribution intentionally submitted for inclusion in the work by you, as defined in the Apache-2.0 license, shall be dual licensed as above, without any additional terms or conditions.
