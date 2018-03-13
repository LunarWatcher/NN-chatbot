from time import *

def ifOrTuple():
    boolVal = False
    t = time()
    for i in range(10000000):
        "test" if boolVal else "testFalse"

    print("Average: {}".format(time() - t))
    combined = 0.0
    t = time()
    for i in range(10000000):
        ("testFalse", "test")[boolVal]
    print("Average: {}".format(time() - t))

def updateOrManual():

    t = time()
    x = {}
    for i in range(10000000):
        x[i] = i
    print("Average: {}".format(time() - t))

    t = time()
    for i in range(10000000):
        x.update({i: i})
    print("Average: {}".format(time() - t))

if __name__ == "__main__":
    ifOrTuple()
    print("####")
    updateOrManual()
    print("####")