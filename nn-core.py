# TODO add retrieval-based
import tensorflow as tf
import tensorlayer as tl
from tensorlayer.layers import *
import numpy as np
import os
import time
import datasetUtils as data
from sklearn.utils import shuffle

data.process_data()#Data setup. May require file moving afterward to work with
meta, idx_q, idx_a = data.load_data(PATH="dataset/")# This path
(trainX, trainY), (testX, testY), (validX, validY) = data.split_dataset(idx_q, idx_a)

trainX = tl.prepro.remove_pad_sequences(trainX.tolist())
trainY = tl.prepro.remove_pad_sequences(trainY.tolist())
testX  = tl.prepro.remove_pad_sequences(testX.tolist())
testY  = tl.prepro.remove_pad_sequences(testY.tolist())
validX = tl.prepro.remove_pad_sequences(validX.tolist())
validY = tl.prepro.remove_pad_sequences(validY.tolist())

xLen = len(trainX)
yLen = len(trainY)
assert xLen == yLen

batchSize = 32
step = int(xLen / batchSize)
embedDim = 512

# Since there has to be support for expanding vocabularies, that means the vocab **cannot be re-created**
# every single time the bot trains, as this would wipe anything created from other custom datasets
# from i.e. conversation scrapping (collecting conversations, storing them in a file and later training
# on that data)

# Anyways, the vocab is saved as two files
if os.path.isfile("dataset/vocabidx2w.npy") and os.path.isfile("dataset/vocabw2idx.npy"):
    idx2w, w2idx = data.loadVocab()
    # The w2idx file is loaded as a numpy ndarray, not a dict. Si
    for index, value in np.ndenumerate(w2idx):
        w2idx = value;
        break;
    startId = w2idx["start_id"]
    endId = w2idx["end_id"]
    idx2w = idx2w + ["start_id", "end_id", "name_id"]

    xVocabSize = yVocabSize = len(idx2w)
else:
    w2idx = meta["w2idx"] # word to vector
    idx2w = meta["idx2w"] # vector to word

    xVocabSize = len(idx2w)
    startId = xVocabSize
    endId = xVocabSize + 1
    nameId = xVocabSize + 2
    w2idx.update({"start_id": startId})
    w2idx.update({"end_id": endId})
    w2idx.update({"name_id" : nameId})# The name ID is the one used in replies to be replaced with the bots name

    idx2w = idx2w + ["start_id", "end_id", "name_id"]
    xVocabSize = yVocabSize = xVocabSize + 3

    data.saveVocab(idx2w, w2idx)

def idx(n: str):
    return w2idx[n]

unkId = idx("unk")
padId = idx("_")

def model(encodeSequences, decodeSequences, training = True, reuse = False):
    with tf.variable_scope("model", reuse=reuse):
        with tf.variable_scope("embedding") as emb:
            netEncode = EmbeddingInputlayer(inputs=encodeSequences, vocabulary_size=xVocabSize, embedding_size=embedDim, name="seqEmbedding")
            emb.reuse_variables()
            tl.layers.set_name_reuse(True)
            netDecode = EmbeddingInputlayer(inputs=decodeSequences, vocabulary_size=xVocabSize, embedding_size=embedDim, name="seqEmbedding")
        netRnn = Seq2Seq(netEncode, netDecode,
                         cell_fn=tf.contrib.rnn.BasicLSTMCell,
                         n_hidden=embedDim, initializer=tf.random_uniform_initializer(-.1, .1),
                         encode_sequence_length=retrieve_seq_length_op2(encodeSequences),
                         decode_sequence_length=retrieve_seq_length_op2(decodeSequences),
                         initial_state_encode=None,
                         dropout=(0.5 if training else None), n_layer=3, return_seq_2d=True,
                         name="seq2seq")
        netOut = DenseLayer(netRnn, n_units=xVocabSize, act=tf.identity, name="output")
    return netOut, netRnn

encodeSequences = tf.placeholder(dtype=tf.int64, shape=[batchSize, None], name="encodeSequences")
decodeSequences = tf.placeholder(dtype=tf.int64, shape=[batchSize, None], name="decodeSequences")
targetSequences = tf.placeholder(dtype=tf.int64, shape=[batchSize, None], name="targetSequences")
targetMask      = tf.placeholder(dtype=tf.int64, shape=[batchSize, None], name="targetMask")
outputLayer, _ = model(encodeSequences, decodeSequences, True, False)

encodeSequences2 = tf.placeholder(dtype=tf.int64, shape=[1, None], name="encodeSequences")
decodeSequences2 = tf.placeholder(dtype=tf.int64, shape=[1, None], name="decodeSequences")

net, netRnn = model(encodeSequences2, decodeSequences2, False, True)
sm = tf.nn.softmax(net.outputs)

loss = tl.cost.cross_entropy_seq_with_mask(logits=outputLayer.outputs, target_seqs=targetSequences, input_mask=targetMask, return_details=False, name="cost")
outputLayer.print_params(False)
learningRate = 0.0001
optimizer = tf.train.AdamOptimizer(learning_rate=learningRate).minimize(loss)

sess = tf.Session(config = tf.ConfigProto(allow_soft_placement=True, log_device_placement=False))
tl.layers.initialize_global_variables(sess)
tl.files.load_and_assign_npz(sess=sess, name="n.npz", network=net)

epochs = 50

def clean(string: str):
    string = string.replace("!", "").replace(".", "").replace(",", "").replace("?", "")
    string = string.lower()
    return string

for epoch in range(epochs):
    epochTime = time.time()
    trainX, trainY = shuffle(trainX, trainY)# Passing random_state with a value != None gives a seed

    avgLoss, iterations = 0,0

    for X, Y in tl.iterate.minibatches(inputs=trainX, targets=trainY, batch_size=batchSize, shuffle = False):
        stepTime = time.time()

        X = tl.prepro.pad_sequences(X)
        targetSeqs = tl.prepro.sequences_add_end_id(Y, end_id=endId)
        targetSeqs = tl.prepro.pad_sequences(targetSeqs)
        decodeSeqs = tl.prepro.sequences_add_start_id(Y, start_id=startId, remove_last=False)
        decodeSeqs = tl.prepro.pad_sequences(decodeSeqs)
        tMask = tl.prepro.sequences_get_mask(targetSeqs)

        _, iLoss = sess.run([optimizer, loss], {encodeSequences: X, decodeSequences: decodeSeqs, targetSequences: targetSeqs, targetMask: tMask})

        if iterations % 200 == 0:
            print("Epoch[%d/%d] step:[%d/%d] loss=%f took=%.5fs" %(epoch, epochs, iterations, step, iLoss, time.time() - stepTime))

        avgLoss += iLoss
        iterations += 1

        # Inference
        if iterations % 1000 == 0:
            seeds = [
                "Great!",
                "Trump won in the polls last night",
                "Im a programmer. What about you?",
                "Hi!",
                "nice to meet you",
            ]
            for seed in seeds:
                seed = clean(seed)
                print ("Input: ", seed)
                try:
                    seedId = [w2idx[w] for w in seed.split(" ")]
                except KeyError:
                    continue
                for _ in range (5):
                    state = sess.run(netRnn.final_state_encode, {encodeSequences2: [seedId]})
                    o, state = sess.run([sm, netRnn.final_state_decode], {netRnn.initial_state_decode: state, decodeSequences2: [[startId]]})
                    wId = tl.nlp.sample_top(o[0], top_k=3)
                    w = idx2w[wId]
                    sentence = [w]
                    for __ in range(30):
                        o, state = sess.run([sm, netRnn.final_state_decode], {netRnn.initial_state_decode: state, decodeSequences2:[[wId]]})
                        wId = tl.nlp.sample_top(o[0], top_k=2)
                        w = idx2w[wId]
                        if wId == endId:
                            break;
                        sentence = sentence + [w]

                    print("> ", ' '.join(sentence))
    print("Epoch[%d/%d] with average loss:%f took:%.5fs" % (epoch, epochs, avgLoss/iterations, time.time()-epochTime))

    tl.files.save_npz(net.all_params, name='n.npz', sess=sess)
