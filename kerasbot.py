import keras
from keras.layers import *
import keras.backend as K
from keras.models import *
import tensorflow as tf
import tensorlayer as tl

class Bot():

    wordEmbeddingSize = 100
    sentenceEmbeddingSize = 300
    maxInputLen = 50
    maxOutputLen = 50
    subsets = 1
    batchSize = 32
    patience = 0
    dropout = 0.25
    testCount = 100
    def __init__(self, training = False):
        vocabSize = K.variable(50000, dtype="int64", name="vocabSize")
        encodeSequences = tf.placeholder(dtype=tf.int64, shape=[32, None],
                                              name="encodeSequences")
        decodeSequences = tf.placeholder(dtype=tf.int64, shape=[32, None],
                                          name="decodeSequences")
        targetSequences = tf.placeholder(dtype=tf.int64, shape=[32, None],
                                          name="targetSequences")
        targetMask = tf.placeholder(dtype=tf.int64, shape=[32, None], name="targetMask")

        model = Sequential()
        model.add(Embedding(None, 512, input_shape=[32, None], input_length=100))
        model.add(LSTM(100, return_sequences=False))
        model.add(LSTM(100, return_sequences=False))
        model.add(Dropout(0.3))
        model.add(Dense(vocabSize, activation="softmax"))
        model.compile(keras.optimizers.Adam(), loss=tl.cost.cross_entropy_seq_with_mask(logits=model.output.outputs,
                                                                                        target_seqs=targetSequences, input_mask=targetMask,
                                                                                        return_details=False, name="cost"))

#def model(self, encodeSequences, decodeSequences, training=True, reuse=False):
#    with tf.variable_scope("model", reuse=reuse):
#        with tf.variable_scope("embedding") as emb:
#            netEncode = EmbeddingInputlayer(inputs=encodeSequences, vocabulary_size=self.xVocabSize,
#                                            embedding_size=self.embedDim, name="seqEmbedding")
#            emb.reuse_variables()
#            tl.layers.set_name_reuse(True)
#            netDecode = EmbeddingInputlayer(inputs=decodeSequences, vocabulary_size=self.xVocabSize,
#                                            embedding_size=self.embedDim, name="seqEmbedding")
#        netRnn = Seq2Seq(netEncode, netDecode,
#                         cell_fn=tf.contrib.rnn.BasicLSTMCell,
#                         n_hidden=self.embedDim, initializer=tf.random_uniform_initializer(-.1, .1),
#                         encode_sequence_length=retrieve_seq_length_op2(encodeSequences),
#                         decode_sequence_length=retrieve_seq_length_op2(decodeSequences),
#                         initial_state_encode=None,
#                         dropout=(0.5 if training else None), n_layer=3, return_seq_2d=True,
#                         name="seq2seq")
#        netOut = DenseLayer(netRnn, n_units=self.xVocabSize, act=tf.identity, name="output")
#    return netOut, netRnn

if __name__ == "__main__":
    bot = Bot()