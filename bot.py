import tensorlayer as tl
from tensorlayer.layers import *
import datasetUtils as data
import Config
import tensorflow as tf
from multiprocessing.dummy import Pool
from sklearn.utils import shuffle
import os
import Commands as cmd
import sys
import grammarRules as grammar
import argparse

# noinspection PyShadowingNames,PyAttributeOutsideInit
class Bot():
    batchSize = 32
    embedDim = 512
    # This is true by default to make it possible to run on 2 gig VRAM systems.
    embeddingCpu = True
    def __init__(self, training):
        self.training = training
        self.initialized = False
        print("Using CPU for embedding? " + str(self.embeddingCpu))
        print("Change the variable in the Bot class to use either the GPU exclusively")


    def createInference(self):
        self.encodeSequences2 = tf.placeholder(dtype=tf.int64, shape=[1, None], name="encodeSequences")
        self.decodeSequences2 = tf.placeholder(dtype=tf.int64, shape=[1, None], name="decodeSequences")

        self.net, self.netRnn = self.model(self.encodeSequences2, self.decodeSequences2, False, True)
        self.sm = tf.nn.softmax(self.net.outputs)

    def createPlaceholders(self):
        self.encodeSequences = tf.placeholder(dtype=tf.int64, shape=[self.batchSize, None],
                                              name="encodeSequences")
        self.decodeSequences = tf.placeholder(dtype=tf.int64, shape=[self.batchSize, None],
                                              name="decodeSequences")
        self.targetSequences = tf.placeholder(dtype=tf.int64, shape=[self.batchSize, None],
                                              name="targetSequences")
        self.targetMask = tf.placeholder(dtype=tf.int64, shape=[self.batchSize, None], name="targetMask")

    def idx(self, n: str):
        return self.w2idx[n]
    @staticmethod
    def getEpochs():
        while True:
            try:
                epochs = int(input("How many epochs do you want to train for?: "))
                break
            except ValueError:
                print("Invalid int")
        return epochs
    @staticmethod
    def cleanInput(string: str):
        return data.filterLine(string, data.EN_WHITELIST)

    def train(self, epochs: int):
        print("Training started: " + str(epochs) + " epochs")
        for epoch in range(epochs):
            epochTime = time.time()
            print("Time taken")
            trainX, trainY = shuffle(self.trainX, self.trainY)# Passing random_state with a value != None gives a seed
            print("Data shuffled")
            avgLoss, iterations = 0,0

            for X, Y in tl.iterate.minibatches(inputs=trainX, targets=trainY, batch_size=self.batchSize, shuffle = False):

                X = tl.prepro.pad_sequences(X)
                targetSeqs = tl.prepro.sequences_add_end_id(Y, end_id=self.endId)
                targetSeqs = tl.prepro.pad_sequences(targetSeqs)
                decodeSeqs = tl.prepro.sequences_add_start_id(Y, start_id=self.startId, remove_last=False)
                decodeSeqs = tl.prepro.pad_sequences(decodeSeqs)
                tMask = tl.prepro.sequences_get_mask(targetSeqs)

                _, iLoss = self.sess.run([self.optimizer, self.loss], {self.encodeSequences: X,
                                                                       self.decodeSequences: decodeSeqs,
                                                                       self.targetSequences: targetSeqs,
                                                                       self.targetMask: tMask})

                if iterations % 100 == 0:
                    print("Epoch[%d/%d] step:[%d/%d] loss=%f" %(epoch + 1, epochs, iterations,
                                                                           self.iterationsPerEpoch, iLoss))

                avgLoss += iLoss
                iterations += 1

            print("Epoch[%d/%d] with average loss:%f took:%.5fs" % (epoch + 1, epochs, avgLoss/iterations, time.time()-epochTime))

            tl.files.save_npz(self.net.all_params, name='n.npz', sess=self.sess)

        # TODO tensorboard

    def predict(self, input: str):
        if not self.training and not self.initialized:
            return "Sorry, I boot asyncronously. The neural net backend boots later than the bot itself. It should be up shortly :)"

        if input.strip() == "":
            return "I don't know what to say."
        input = self.cleanInput(input)
        inputTokens = list()
        for w in input.split(" "):
            try:
                inputTokens.append(self.w2idx[w])
            except KeyError:
                inputTokens.append(self.unkId)

        state = self.sess.run(self.netRnn.final_state_encode, {self.encodeSequences2: [inputTokens]})
        o, state = self.sess.run([self.sm, self.netRnn.final_state_decode],
                            {self.netRnn.initial_state_decode: state, self.decodeSequences2: [[self.startId]]})
        wId = tl.nlp.sample_top(o[0], top_k=10)
        w = self.idx2w[wId]
        sentence = [w]
        for __ in range(30):
            o, state = self.sess.run([self.sm, self.netRnn.final_state_decode],
                                {self.netRnn.initial_state_decode: state, self.decodeSequences2: [[wId]]})
            wId = tl.nlp.sample_top(o[0], top_k=3)
            w = self.idx2w[wId]
            if wId == self.endId:
                break;
            elif wId == self.padId:
                # Skip if it's padding. unk is not skipped as it is still necessary in some cases, and
                # some messages even end up as nothing if it's ignored.
                continue

            sentence = sentence + [w]

        # Parse "unk"-only messages to "I don't understand" in some or another form.
        answer = ' '.join(sentence)
        print(answer)
        return grammar.fix(answer)


    def model(self, encodeSequences, decodeSequences, training=True, reuse=False):
        with tf.variable_scope("model", reuse=reuse):
            with tf.variable_scope("embedding") as emb:
                if self.embeddingCpu:
                    with tf.device("/cpu:0"):
                        netEncode, netDecode = self.createEncDec(emb, encodeSequences, decodeSequences)
                else:
                    netEncode, netDecode = self.createEncDec(emb, encodeSequences, decodeSequences)
            netRnn = Seq2Seq(netEncode, netDecode,
                             cell_fn=tf.contrib.rnn.BasicLSTMCell,
                             n_hidden=self.embedDim, initializer=tf.random_uniform_initializer(-.1, .1),
                             encode_sequence_length=retrieve_seq_length_op2(encodeSequences),
                             decode_sequence_length=retrieve_seq_length_op2(decodeSequences),
                             initial_state_encode=None,
                             dropout=(0.5 if training else None), n_layer=3, return_seq_2d=True,
                             name="seq2seq")
            netOut = DenseLayer(netRnn, n_units=data.VOCAB_SIZE, act=tf.identity, name="output")
        return netOut, netRnn

    def createEncDec(self, emb, encodeSequences, decodeSequences):
        netEncode = EmbeddingInputlayer(inputs=encodeSequences, vocabulary_size=data.VOCAB_SIZE,
                                        embedding_size=self.embedDim, name="seqEmbedding")
        emb.reuse_variables()
        tl.layers.set_name_reuse(True)
        netDecode = EmbeddingInputlayer(inputs=decodeSequences, vocabulary_size=data.VOCAB_SIZE,
                                        embedding_size=self.embedDim, name="seqEmbedding")
        return netEncode, netDecode

    def bootElsewhere(self):

        cmd.PermissionManager.assumeAllAndInject()# Inject all the permissions before site boot
        if not Config.isAnyEnabled(list(Config.enabledSites.keys())):
            print("No sites are enabled! Shutting off...")
            exit(-1)

        if Config.isSiteEnabled("discord"):
            import discordBot
            discord = discordBot.Discord()
            pool = Pool(processes=1)
            pool.apply_async(discord.start, args=[self.predict])
        if Config.isAnyEnabled(["stackoverflow.com", "stackexchange.com", "meta.stackexchange.com"]):
            import stackexchange
            stackexchange.Stackexchange.nnFun = self.predict
            stackexchange.start()

        while True:
            message = input(">> ")
            if message == "~!save":
                cmd.PermissionManager.saveAll()
            elif(message == "~!stop" or message == "~!break"):
                cmd.PermissionManager.saveAll()
                break
            else:
                print("Unknown command.")


        if Config.isAnyEnabled(["stackoverflow.com", "stackexchange.com", "meta.stackexchange.com"]):
            if "stackexchange" in sys.modules:
                for site in stackexchange.Stackexchange.sites:
                    site.stop()
            else:
                print("Site (stackexchange) enabled, but not imported!")

        if Config.isSiteEnabled("discord"):
            if "discord" in sys.modules:
                discord.bot.logout()
            else:
                print("Site (discord) enabled, but not imported!")

    def startChat(self, mode):

        if mode == 0:
            while True:
                message = input("You >> ")
                if message == "~!exit":
                    self.sess.close()

                    exit()
                print("Bot >> " + self.predict(message))
            pass
        elif mode == 1:
            self.bootElsewhere()
        else:
            print("Unknown mode")
            exit()

    def startNet(self):
        if self.training:
            # TODO not implemented: custom datasets
            # y if forced else input
            dataset = "y" #input("Use the default dataset (y) or a custom one (filename, dataset/ is appended automatically)")

            if dataset == "y":
                pass
            else:
                pass
            self.meta, self.idx_q, self.idx_a = data.loadData(PATH="dataset/")# This path
            (trainX, trainY), (testX, testY), (validX, validY) = data.splitDataset(self.idx_q, self.idx_a)

            self.trainX = tl.prepro.remove_pad_sequences(trainX.tolist())
            self.trainY = tl.prepro.remove_pad_sequences(trainY.tolist())
            self.testX  = tl.prepro.remove_pad_sequences(testX.tolist())
            self.testY  = tl.prepro.remove_pad_sequences(testY.tolist())
            self.validX = tl.prepro.remove_pad_sequences(validX.tolist())
            self.validY = tl.prepro.remove_pad_sequences(validY.tolist())

            xLen = len(trainX)
            yLen = len(trainY)
            assert xLen == yLen

            self.iterationsPerEpoch = int(xLen / self.batchSize)
        # Since there has to be support for expanding vocabularies, that means the vocab **cannot be re-created**
        # every single time the bot trains, as this would wipe anything created from other custom datasets
        # from i.e. conversation scrapping (collecting conversations, storing them in a file and later training
        # on that data)

        # Anyways, the vocab is saved as two files
        if os.path.isfile("dataset/vocabidx2w.npy") and os.path.isfile("dataset/vocabw2idx.npy"):
            self.idx2w, self.w2idx = data.loadVocab()
            if isinstance(self.idx2w, np.ndarray):
                self.idx2w = self.idx2w.tolist()

            for k, v in np.ndenumerate(self.w2idx):
                self.w2idx = v
                break

            self.startId = self.w2idx["start_id"]
            self.endId = self.w2idx["end_id"]
            if not "name_id" in self.idx2w:
                # Since some earlier versions of the program itself created the dict without adding the
                # name ID, this check is here to make sure it's present
                print("Name ID not found. Appending and saving the dictionary...")
                self.nameId = len(self.idx2w) + 1
                self.w2idx["name_id"] = self.nameId
                self.idx2w = self.idx2w + ["name_id"]
                data.saveVocab(self.idx2w, self.w2idx)
            else:
                self.nameId = self.w2idx["name_id"]

            self.xVocabSize = self.yVocabSize = len(self.idx2w)
        else:
            if not self.training:
                print("The vocab files were not found in the dataset directory. (if changes were made to the directories, please change the code as well)")
                print("Since training is set to false, none of the data required for this operation is present. Please train the bot before attempting to use")
                print("It, or at the very least make sure the vocabs are present. The program will not exit. To avoid seeing this warning in the future, please")
                print("run the program in training mode")
                exit()
            self.w2idx = self.meta["w2idx"] # word to vector
            self.idx2w = self.meta["idx2w"] # vector to word
            print(self.w2idx)
            self.xVocabSize = len(self.idx2w)
            self.startId = self.xVocabSize
            self.endId = self.xVocabSize + 1
            self.nameId = self.xVocabSize + 2
            self.w2idx["start_id"] = self.startId
            self.w2idx["end_id"] = self.endId
            self.w2idx["name_id"] = self.nameId# The name ID is the one used in replies to be replaced with the bots name

            self.idx2w = self.idx2w + ["start_id", "end_id", "name_id"]
            self.xVocabSize = self.yVocabSize = self.xVocabSize + 3

            data.saveVocab(self.idx2w, self.w2idx)


        self.unkId = self.idx("unk")
        self.padId = self.idx("_")
        self.startId = self.idx("start_id")
        self.endId = self.idx("end_id")
        self.nameId = self.idx("name_id")
        self.xVocabSize = len(self.idx2w)
        if self.embeddingCpu:
            with tf.device("/cpu:0"):
                self.createPlaceholders()
        else:
            self.createPlaceholders()

        self.outputLayer, _ = self.model(self.encodeSequences, self.decodeSequences, False, False)

        if self.embeddingCpu:
            with tf.device("/cpu:0"):
                self.createInference()
        else:
            self.createInference()

        self.loss = tl.cost.cross_entropy_seq_with_mask(logits=self.outputLayer.outputs,
                                                        target_seqs=self.targetSequences, input_mask=self.targetMask,
                                                        return_details=False, name="cost")

        self.outputLayer.print_params(False)

        learningRate = 0.0001

        self.optimizer = tf.train.AdamOptimizer(learning_rate=learningRate).minimize(self.loss)

        self.sess = tf.Session(config=tf.ConfigProto(gpu_options=tf.GPUOptions(allow_growth=True), log_device_placement=False))
        tl.layers.initialize_global_variables(self.sess)
        tl.files.load_and_assign_npz(sess=self.sess, name="n.npz", network=self.net)
        self.initialized = True
        if self.training:

            epochs = self.getEpochs()

            self.train(epochs)

def getBooleanInput(prompt):
    while True:
        try:
            return {"true":True,"false":False, "t": True, "f": False, "1": True, "0": False,
                    "True": True, "False" : False, "y" : True, "n": False, "yes" : True,
                    "no" : False}[input(prompt).lower()]
        except KeyError:
            print("Invalid input please enter True or False!")


def parseBoolean(str):
    try:
        return {"true":True,"false":False, "t": True, "f": False, "1": True, "0": False,
               "True": True, "False" : False, "y" : True, "n": False, "yes" : True,
               "no" : False}[str]
    except:
        print("Invalid boolean: " + str)
        return False

def getIntInput(prompt, validChoices = []):

    while True:
        try:
            inp = input(prompt).lower()
            try:
                intVal = int(inp)
                if len(validChoices) != 0:
                    if intVal not in validChoices:
                        print("Invalid choice")
                        continue
                return intVal
            except ValueError:
                pass

            return {"console": 0, "c": 0, "o": 1}[inp]

        except KeyError:
            print("Invalid int")


if __name__ == '__main__':
    print("#########################################")
    print("Booting...")

    parser = argparse.ArgumentParser()
    parser.add_argument("--training", type=str, help="Am I training? (boolean)")
    parser.add_argument("--mode", type=int, help="(When training = false only) Where to boot the bot. 0 is the console, 1 is online. More modes may come eventually")
    args = parser.parse_args()
    trainingArg = parseBoolean(args.training)
    modeArg = args.mode

    if os.path.isdir("dataset/"):
        print("Dataset directory found!")
        training = getBooleanInput("Am I training?: ") if trainingArg is None else trainingArg

    else:
        print("Dataset directory not found!")
        os.mkdir("dataset/")
        data.processData()
        print("Dataset prepared")
        print("Training is forced, in order to get the necessary files for the net")
        training = True

    print("I'm gonna be " + ("training!" if training else "chatting"))
    mode = 0
    if not training:
        mode = getIntInput("Mode: ", [0, 1]) if modeArg is None else modeArg



    tf.reset_default_graph()

    bot = Bot(training)

    if not training:
        pool = Pool(processes=1)
        pool.apply_async(bot.startNet )
        bot.startChat(mode)
    else:
        bot.startNet()
