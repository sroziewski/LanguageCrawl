import argparse

from gensim.models import Word2Vec

from cassandra.cluster import Cluster

cluster = Cluster(['localhost'],
                  port=9042,
                  connect_timeout=10000)
session = cluster.connect('test',
                          wait_for_all_pools=True)
session.execute('USE ngramspace')
session.default_fetch_size = 1000


def query_db(query):
    return session.execute(query, )


def get_args():
    parser = argparse.ArgumentParser(description='Training word2vec model')
    parser.add_argument('-n', metavar='n', type=int, help='#documents to be processed')
    return parser.parse_args()


document_number = get_args().n

i = 0
filename = 'model.dat'

for _row in query_db('SELECT * FROM document;'):
    sentences_in = list(_row.content.split("|"))
    sentences = []
    for sentence in sentences_in:
        sentences.append(list(sentence.split()))
    if i == 0:
        model = Word2Vec(sentences, min_count=1, size=300, workers=3, window=5, sg=1)
    else:
        model.build_vocab(sentences, update=True)
        model.train(sentences, total_examples=sentences.__len__(), epochs=model.epochs)
    i += 1
    if document_number == i:
        model.wv.save_word2vec_format(filename, binary=True)
        print(f"Model saved in file {filename}")
        exit(0)
