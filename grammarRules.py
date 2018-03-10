# Since everything is put to lower case, the capitalization has to be handled externally. If capitalization
# was included in the dataset, there'd be too many unique words (i.e. "this" would have the combinations
# "This", "THis", "THIs", "THIS", "tHis", "tHIs", and so on. You get the idea. And since doing this would
# require a lot more memory to deal with, this file handles regexes and replacement for grammar rules
# based on rules

import re

def capitalization(rule, string):
    found = re.finditer(rule, string)
    recompiled = []
    for matchId, match in enumerate(found):
        fullMatch = match.group(0).split()
        fullMatch[0] = fullMatch[0].title()
        recompiled.append(' '.join(fullMatch))

    return ' '.join(recompiled)

# TODO acronym capitalization (lol, omg, rofl)

# Basic rules - those that area easy to replace
basicRules = {
    r"\bi('|\b)(?!.e.)" : r"I\1",

}

complexRules = {
    r"(([A-Za-z]|\d(?!\d*\. )|[.$_]\w+)(\S*))((?:(?:etc\.|i\.e\.|e\.g\.|vs\.|\.\.\.|\w*\.(?![\s\")])|[*-]+|\n(?![ \t]*\n| *(?:[*-]|\d+\.))|[^.?!\n]?))+(?:([.?!]+)(?=[\s\")]|$)|\n\n|\n(?= *[*-])|\n(?= *\d+\.)|$))": capitalization,

}

eosSymbols = r"\s+(?P<match>[?!.,])"
doubleDash = r"( - - )"
completeRemoval = r"(\s+(?P<match>['-])\s+)"


def fix(input: str):
    input = clean(input)

    for rule, replacement in complexRules.items():
        input = replacement(rule, input)
    for rule, replacement in basicRules.items():
        input = re.sub(rule, replacement, input)

    return input

def clean(input: str):
    cleaned = re.sub(eosSymbols, r"\g<match>", input)
    cleaned = re.sub(doubleDash, " -- ", cleaned)
    cleaned = re.sub(completeRemoval, r"\g<match>", cleaned)
    return cleaned

# Debug testing
if __name__ == "__main__":
    testSentences = [
        "hi , i ' m Olivia. i don ' t have a life .",
        "omg, this is sooooo fun! i hope we can do this again!",
        "lol, I agree"
    ]
    for sentence in testSentences:
        print(fix(sentence))
