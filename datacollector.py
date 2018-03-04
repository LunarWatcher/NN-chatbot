import os

class DataCollector():
    qna = dict()

    def __init__(self, targetFile: str):
        self.file = targetFile

    def saveData(self):
        type = "w"
        if os.path.isfile(self.file):
            type = "a"# if the file exists, append instead
        approveAll = input("Do you want to automatically deny(n) or approve(y) all the sentences or manually pick(p) for each?")
        if(approveAll == "n"):
            return

        with open(self.file, type) as f:
            for q, a in self.qna:
                print("q: [{}] a: [{}]".format(q, a))
                if(approveAll == "p"):
                    res = input("Append to file? y/n")
                    if(res == "n"):
                        improved = input("Apply better answer? n/[your sentence without brackets]")
                        if(improved == "n"):
                            continue
                        else:
                            a = improved
                if(type == "a"):
                    f.write(q + "\n" + a + "\n")
                else:
                    f.write(q + a)
