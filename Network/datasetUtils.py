import itertools
import os
import pickle
import re

import nltk
import numpy as np
from random import sample

EN_WHITELIST = '0123456789abcdefghijklmnopqrstuvwxyz \'\"+.,!?*-^_'
EN_BLACKLIST = '$`()/<=>@[\\]{|}~'

limit = {
    'maxq': 25,
    'minq': 2,
    'maxa': 25,
    'mina': 2
}

UNK = 'unk'
VOCAB_SIZE = 45000


def getId2line():
    lines = open('raw_data/movie_lines.txt', encoding='utf-8', errors='ignore').read().split('\n')
    id2line = {}
    for line in lines:
        _line = line.split(' +++$+++ ')
        if len(_line) == 5:
            id2line[_line[0]] = _line[4]
    return id2line


def getConversations():
    conv_lines = open('raw_data/movie_conversations.txt', encoding='utf-8', errors='ignore').read().split('\n')
    convs = []
    for line in conv_lines[:-1]:
        _line = line.split(' +++$+++ ')[-1][1:-1].replace("'", "").replace(" ", "")
        convs.append(_line.split(','))

    return convs


def extractConversations(convs, id2line, path=''):
    idx = 0
    for conv in convs:
        f_conv = open(path + str(idx) + '.txt', 'w')
        for line_id in conv:
            f_conv.write(id2line[line_id])
            f_conv.write('\n')
        f_conv.close()
        idx += 1


def gatherDataset(convs, id2line):
    questions = [];
    answers = []
    for conv in convs:
        if len(conv) % 2 != 0:
            conv = conv[:-1]
        for i in range(len(conv)):
            if i % 2 == 0:
                questions.append(id2line[conv[i]])
            else:
                answers.append(id2line[conv[i]])

    return questions, answers


def filterLine(line, whitelist=EN_WHITELIST):
    reformatted = re.sub(r'(?P<group>[\'\"])', r" \g<group> ", line.lower())
    reformatted = re.sub(r'(?P<group>[?!.,^:;\-+_])', r" \g<group> ", reformatted)
    reformatted = re.sub(r'( - - )', r' -- ', reformatted)
    reformatted = " ".join(reformatted.split())
    return ''.join([ch for ch in reformatted.strip() if ch in whitelist])


def filterData(qseq, aseq):
    filtered_q, filtered_a = [], []
    raw_data_len = len(qseq)

    assert len(qseq) == len(aseq)

    for i in range(raw_data_len):
        qlen, alen = len(qseq[i].split(' ')), len(aseq[i].split(' '))
        if qlen >= limit['minq'] and qlen <= limit['maxq']:
            if alen >= limit['mina'] and alen <= limit['maxa']:
                filtered_q.append(qseq[i])
                filtered_a.append(aseq[i])

    # print the fraction of the original data, filtered
    filt_data_len = len(filtered_q)
    filtered = int((raw_data_len - filt_data_len) * 100 / raw_data_len)
    print(str(filtered) + '% filtered from original data')

    return filtered_q, filtered_a


'''
 read list of words, create index to word,
  word to index dictionaries
    return tuple( vocab->(word, count), idx2w, w2idx )
'''


def index_(tokenized_sentences, vocab_size):
    # get frequency distribution
    freq_dist = nltk.FreqDist(itertools.chain(*tokenized_sentences))
    # get vocabulary of 'vocab_size' most used words
    vocab = freq_dist.most_common(vocab_size)
    # index2word
    index2word = ['_'] + [UNK] + [x[0] for x in vocab]
    # word2index
    word2index = dict([(w, i) for i, w in enumerate(index2word)])
    return index2word, word2index, freq_dist


'''
 filter based on number of unknowns (words not in vocabulary)
  filter out the worst sentences
'''


def filterUnk(qtokenized, atokenized, w2idx):
    data_len = len(qtokenized)

    filtered_q, filtered_a = [], []

    for qline, aline in zip(qtokenized, atokenized):
        unk_count_q = len([w for w in qline if w not in w2idx])
        unk_count_a = len([w for w in aline if w not in w2idx])
        if unk_count_a <= 2:
            if unk_count_q > 0:
                if unk_count_q / len(qline) > 0.2:
                    pass
            filtered_q.append(qline)
            filtered_a.append(aline)

    # print the fraction of the original data, filtered
    filt_data_len = len(filtered_q)
    filtered = int((data_len - filt_data_len) * 100 / data_len)
    print(str(filtered) + '% filtered from original data')

    return filtered_q, filtered_a


'''
 create the final raw_data :
  - convert list of items to arrays of indices
  - add zero padding
      return ( [array_en([indices]), array_ta([indices]) )
'''


def zeroPad(qtokenized, atokenized, w2idx):
    # num of rows
    data_len = len(qtokenized)

    # numpy arrays to store indices
    idx_q = np.zeros([data_len, limit['maxq']], dtype=np.int32)
    idx_a = np.zeros([data_len, limit['maxa']], dtype=np.int32)

    for i in range(data_len):
        q_indices = padSeq(qtokenized[i], w2idx, limit['maxq'])
        a_indices = padSeq(atokenized[i], w2idx, limit['maxa'])

        # print(len(idx_q[i]), len(q_indices))
        # print(len(idx_a[i]), len(a_indices))
        idx_q[i] = np.array(q_indices)
        idx_a[i] = np.array(a_indices)

    return idx_q, idx_a


'''
 replace words with indices in a sequence
  replace with unknown if word not in lookup
    return [list of indices]
'''


def padSeq(seq, lookup, maxlen):
    indices = []
    for word in seq:
        if word in lookup:
            indices.append(lookup[word])
        else:
            indices.append(lookup[UNK])
    return indices + [0] * (maxlen - len(seq))


def processData():
    id2line = getId2line()
    print('>> gathered id2line dictionary.\n')
    convs = getConversations()
    print(convs[121:125])
    print('>> gathered conversations.\n')
    questions, answers = gatherDataset(convs, id2line)



    # change to lower case (just for en)
    questions = [line.lower() for line in questions]
    answers = [line.lower() for line in answers]

    # filter out unnecessary characters
    print('\n>> Filter lines')
    questions = [filterLine(line, EN_WHITELIST) for line in questions]
    answers = [filterLine(line, EN_WHITELIST) for line in answers]

    # filter out too long or too short sequences
    print('\n>> 2nd layer of filtering')
    qlines, alines = filterData(questions, answers)

    for q, a in zip(qlines[141:145], alines[141:145]):
        print('q : [{0}]; a : [{1}]'.format(q, a))

    # convert list of [lines of text] into list of [list of words ]
    print('\n>> Segment lines into words')
    qtokenized = [[w.strip() for w in wordlist.split(' ') if w] for wordlist in qlines]
    atokenized = [[w.strip() for w in wordlist.split(' ') if w] for wordlist in alines]
    print('\n:: Sample from segmented list of words')

    for q, a in zip(qtokenized[141:145], atokenized[141:145]):
        print('q : [{0}]; a : [{1}]'.format(q, a))

    # indexing -> idx2w, w2idx
    print('\n >> Index words')
    idx2w, w2idx, freq_dist = index_(qtokenized + atokenized, vocab_size=VOCAB_SIZE)

    # filter out sentences with too many unknowns
    print('\n >> Filter Unknowns')
    qtokenized, atokenized = filterUnk(qtokenized, atokenized, w2idx)
    print('\n Final raw_data len : ' + str(len(qtokenized)))

    print('\n >> Zero Padding')
    idx_q, idx_a = zeroPad(qtokenized, atokenized, w2idx)

    print('\n >> Save numpy arrays to disk')
    # save them
    np.save('dataset/idx_q.npy', idx_q)
    np.save('dataset/idx_a.npy', idx_a)

    # let us now save the necessary dictionaries
    metadata = {
        'w2idx': w2idx,
        'idx2w': idx2w,
        'limit': limit,
        'freq_dist': freq_dist
    }

    # write to disk : data control dictionaries
    with open('dataset/metadata.pkl', 'wb') as f:
        pickle.dump(metadata, f)

    # count of unknowns
    unk_count = (idx_q == 1).sum() + (idx_a == 1).sum()
    # count of words
    word_count = (idx_q > 1).sum() + (idx_a > 1).sum()

    print('% unknown : {0}'.format(100 * (unk_count / word_count)))
    print('Dataset count : ' + str(idx_q.shape[0]))





# noinspection PyDefaultArgument
def splitDataset(x, y, ratio=[0.7, 0.15, 0.15]):
    # number of examples
    data_len = len(x)
    lens = [int(data_len * item) for item in ratio]

    trainX, trainY = x[:lens[0]], y[:lens[0]]
    testX, testY = x[lens[0]:lens[0] + lens[1]], y[lens[0]:lens[0] + lens[1]]
    validX, validY = x[-lens[-1]:], y[-lens[-1]:]

    return (trainX, trainY), (testX, testY), (validX, validY)


def batchGen(x, y, batch_size):
    # infinite while
    while True:
        for i in range(0, len(x), batch_size):
            if (i + 1) * batch_size < len(x):
                yield x[i: (i + 1) * batch_size].T, y[i: (i + 1) * batch_size].T


def randBatchGen(x, y, batch_size):
    while True:
        sample_idx = sample(list(np.arange(len(x))), batch_size)
        yield x[sample_idx].T, y[sample_idx].T


def decode(sequence, lookup, separator=''):  # 0 used for padding, is ignored
    return separator.join([lookup[element] for element in sequence if element])


def loadData(PATH=''):
    # read data control dictionaries
    with open(PATH + 'metadata.pkl', 'rb') as f:
        metadata = pickle.load(f)
    # read numpy arrays
    idx_q = np.load(PATH + 'idx_q.npy')
    idx_a = np.load(PATH + 'idx_a.npy')
    return metadata, idx_q, idx_a


def saveVocab(idx2w, w2idx, path='dataset/', filename="vocab{}.npy"):
    np.save(path + filename.format("idx2w"), arr=idx2w)
    np.save(path + filename.format("w2idx"), arr=w2idx)


def loadVocab(path="dataset/", filename="vocab{}.npy"):
    if os.path.isfile(path + filename.format("idx2w")) and os.path.isfile(path + filename.format("w2idx")):
        idx2w = np.load(path + filename.format("idx2w"))
        w2idx = np.load(path + filename.format("w2idx"))
        return idx2w, w2idx
    return None, None
