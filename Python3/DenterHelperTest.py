import unittest
from enum import Enum
from antlr4.Token import CommonToken
from antlr4.Token import Token

from DenterHelper import DenterHelper


class TT(Enum):
    NL = 0
    INDENT = 1
    DEDENT = 2
    NORMAL = 3
    EOF_TOKEN = 4

class TokenBuilder:
    def nl(self, line: str):
        pass

    def rf(self, line: str):
        pass

    def raw(self, expected: []):
        pass

class DentChecker:
    def ignoreEof(self):
        pass

    def dented(self, expected: []):
        pass

class LineBuilder:
    def __init__(self, lineNo, builder: []):
        self.lineNo = lineNo
        self.builder = builder
        self.pos = 0

    def addToken(self, prefix: str, s: str, tokenType: TT):
        token = CommonToken(type=tokenType.value)
        token.text = prefix + s
        token.column = self.pos
        token.line = self.lineNo
        self.pos += len(s)
        self.builder.append(token)

class TokenChecker(TokenBuilder, DentChecker, unittest.TestCase):

    def __init__(self):
        super().__init__()
        self.lineNo = 0
        self.tokens = []
        self.ignoreEOF = False

    @staticmethod
    def of(firstLine: str):
        tb = TokenChecker()
        lineBuilder = LineBuilder(0, tb.tokens)
        leading = leadingSpacesOf(firstLine)
        lineBuilder.pos = leading
        firstLine = firstLine[leading:]
        if firstLine:
            lineBuilder.addToken("", firstLine, TT.NORMAL)
        return tb

    def nl(self, line: str):
        self.tokenize("\n", line)
        return self

    def rf(self, line: str):
        self.tokenize("\r\n", line)
        return self

    def ignoreEof(self):
        self.ignoreEOF = True
        return self

    def dented(self, expected: []):
        dented = self.dent(self.tokens)
        dentedTypes = self.tokensToTypes(dented)
        self.assertEqual(expected, dentedTypes, "dented tokens")

    def tokensToTypes(self, tokens: []):
        types = []
        for t in tokens:
            if t.type == Token.EOF:
                tokenType = TT.EOF_TOKEN
            else:
                tokenType = TT(t.type)
            types.append(tokenType)
        return types

    def tokenize(self, nlChars: str, line: str):
        self.lineNo += 1
        lineBuilder = LineBuilder(self.lineNo, self.tokens)
        leading = leadingSpacesOf(line)
        lineBuilder.addToken(nlChars, line[0:leading], TT.NL)
        line = line[leading:]
        if line:
            lineBuilder.addToken("", line, TT.NORMAL)

    def dent(self, tokens: []):
        denter = InterableBasedDenterHelper(TT.NL.value, TT.INDENT.value, TT.DEDENT.value, tokens)
        if self.ignoreEOF:
            denter.shouldIgnoreEOF = True
        dented = []
        while True:
            token = denter.nextToken()
            dented.append(token)
            if token.type == Token.EOF:
                return dented

    def raw(self, expected: []):
        token = CommonToken(type=Token.EOF)
        token.text = "<eof-token>"
        self.tokens.append(token)
        rawTypes = self.tokensToTypes(self.tokens)
        self.assertEqual(expected, rawTypes, "raw tokens")
        return self

    def reset(self):
        self.tokens = []
        self.lineNo = 0
        self.ignoreEOF = False

def leadingSpacesOf(s: str):
    for i in range(len(s)):
        if not s[i].isspace():
            return i
    return len(s)


class InterableBasedDenterHelper(DenterHelper):

    def __init__(self, nlToken, indentToken, dedentToken, tokens: []):
        super(InterableBasedDenterHelper, self).__init__(nlToken, indentToken, dedentToken, False)
        self.tokens = tokens
        self.currentIndex = -1

    def pullToken(self):
        self.currentIndex += 1
        return self.tokens[self.currentIndex]


class DenterHelperTest(unittest.TestCase):


    def test_simple(self):
        TokenChecker()\
            .of("hello")\
            .nl("  bar")\
            .raw([TT.NORMAL, TT.NL, TT.NORMAL, TT.EOF_TOKEN])\
            .dented([TT.NORMAL, TT.INDENT, TT.NORMAL, TT.NL, TT.DEDENT, TT.EOF_TOKEN])


    def test_simpleWithNLs(self):
        TokenChecker()\
            .of("hello")\
            .nl("world")\
            .nl("  tab1")\
            .nl("  tab2")\
            .raw([TT.NORMAL, TT.NL, TT.NORMAL, TT.NL, TT.NORMAL, TT.NL, TT.NORMAL, TT.EOF_TOKEN])\
            .dented([TT.NORMAL, TT.NL, TT.NORMAL, TT.INDENT, TT.NORMAL, TT.NL, TT.NORMAL, TT.NL, TT.DEDENT, TT.EOF_TOKEN])

    def test_multipleDedents(self):
        TokenChecker()\
            .of("hello")\
            .nl("  line2")\
            .nl("    line3")\
            .nl("world")\
            .raw([TT.NORMAL, TT.NL, TT.NORMAL, TT.NL, TT.NORMAL, TT.NL, TT.NORMAL, TT.EOF_TOKEN])\
            .dented([TT.NORMAL, TT.INDENT, TT.NORMAL, TT.INDENT, TT.NORMAL, TT.NL, TT.DEDENT, TT.DEDENT, TT.NORMAL, TT.NL, TT.EOF_TOKEN])

    def test_multipleDedentsWithSameLine(self):
        TokenChecker() \
            .of("hello") \
            .nl("  line2") \
            .nl("  line3")\
            .nl("    line4") \
            .nl("world") \
            .raw([TT.NORMAL, TT.NL, TT.NORMAL, TT.NL, TT.NORMAL, TT.NL, TT.NORMAL, TT.NL, TT.NORMAL, TT.EOF_TOKEN]) \
            .dented([TT.NORMAL, TT.INDENT, TT.NORMAL, TT.NL, TT.NORMAL, TT.INDENT, TT.NORMAL, TT.NL, TT.DEDENT, TT.DEDENT, TT.NORMAL, TT.NL, TT.EOF_TOKEN])

    def test_multipleDedentsToEof(self):
        TokenChecker()\
            .of("hello")\
            .nl("  line2")\
            .nl("    line3")\
            .raw([TT.NORMAL, TT.NL, TT.NORMAL, TT.NL, TT.NORMAL, TT.EOF_TOKEN])\
            .dented([TT.NORMAL, TT.INDENT, TT.NORMAL, TT.INDENT, TT.NORMAL, TT.NL, TT.DEDENT, TT.DEDENT, TT.EOF_TOKEN])

    def test_ignoreBlankLines(self):
        TokenChecker()\
            .of("hello")\
            .nl("     ")\
            .nl("")\
            .nl("  dolly")\
            .nl("        ")\
            .nl("    ")\
            .nl("")\
            .nl("world")\
            .raw([TT.NORMAL, TT.NL, TT.NL, TT.NL, TT.NORMAL, TT.NL, TT.NL, TT.NL, TT.NL, TT.NORMAL, TT.EOF_TOKEN])\
            .dented([TT.NORMAL, TT.INDENT, TT.NORMAL, TT.NL, TT.DEDENT, TT.NORMAL, TT.NL, TT.EOF_TOKEN])

    def test_allIndented(self):
        TokenChecker()\
            .of("    hello")\
            .nl("    line2")\
            .nl("       line3")\
            .nl("    ")\
            .raw([TT.NORMAL, TT.NL, TT.NORMAL, TT.NL, TT.NORMAL, TT.NL, TT.EOF_TOKEN])\
            .dented([TT.INDENT, TT.NORMAL, TT.NL, TT.NORMAL, TT.INDENT, TT.NORMAL, TT.NL, TT.DEDENT, TT.DEDENT, TT.EOF_TOKEN])

    def test_startIndentedThenEmptyLines(self):
        TokenChecker()\
            .of("    hello")\
            .nl("    line2")\
            .nl("")\
            .raw([TT.NORMAL, TT.NL, TT.NORMAL, TT.NL, TT.EOF_TOKEN])\
            .dented([TT.INDENT, TT.NORMAL, TT.NL, TT.NORMAL, TT.NL, TT.DEDENT, TT.EOF_TOKEN])

    def test_detentToNegative(self):
        #this shouldn't explode, it should just result in an extra dedent
        TokenChecker()\
            .of("    hello")\
            .nl("    world")\
            .nl("boom")\
            .raw([TT.NORMAL, TT.NL, TT.NORMAL, TT.NL, TT.NORMAL, TT.EOF_TOKEN])\
            .dented([TT.INDENT, TT.NORMAL, TT.NL, TT.NORMAL, TT.NL, TT.DEDENT, TT.NORMAL, TT.NL, TT.EOF_TOKEN])

    def test_halfDent(self):
        TokenChecker()\
            .of("hello")\
            .nl("     world")\
            .nl("  boom")\
            .raw([TT.NORMAL, TT.NL, TT.NORMAL, TT.NL, TT.NORMAL, TT.EOF_TOKEN])\
            .dented([TT.NORMAL, TT.INDENT, TT.NORMAL, TT.NL, TT.DEDENT, TT.INDENT, TT.NORMAL, TT.NL, TT.DEDENT, TT.EOF_TOKEN])

    def test_halfDentFromTwo(self):
        TokenChecker()\
            .of("hello")\
            .nl("     world")\
            .nl("         universe")\
            .nl("  boom")\
            .raw([TT.NORMAL, TT.NL, TT.NORMAL, TT.NL, TT.NORMAL, TT.NL, TT.NORMAL, TT.EOF_TOKEN])\
            .dented([TT.NORMAL, TT.INDENT, TT.NORMAL, TT.INDENT, TT.NORMAL, TT.NL, TT.DEDENT, TT.DEDENT, TT.INDENT, TT.NORMAL, TT.NL, TT.DEDENT, TT.EOF_TOKEN])

    def test_withReturn(self):
        TokenChecker()\
            .of("hello")\
            .nl("world")\
            .rf("dolly")\
            .raw([TT.NORMAL, TT.NL, TT.NORMAL, TT.NL, TT.NORMAL, TT.EOF_TOKEN])\
            .dented([TT.NORMAL, TT.NL, TT.NORMAL, TT.NL, TT.NORMAL, TT.NL, TT.EOF_TOKEN])

    def test_ignoreEofNoDedents(self):
        TokenChecker()\
            .of("hello")\
            .raw([TT.NORMAL, TT.EOF_TOKEN])\
            .ignoreEof()\
            .dented([TT.NORMAL, TT.EOF_TOKEN])

    def test_ignoreEofWithDedents(self):
        TokenChecker()\
            .of("hello")\
            .nl("  world")\
            .raw([TT.NORMAL, TT.NL, TT.NORMAL, TT.EOF_TOKEN])\
            .ignoreEof()\
            .dented([TT.NORMAL, TT.INDENT, TT.NORMAL, TT.EOF_TOKEN])

    def test_tabIndents(self):
        TokenChecker()\
            .of("{")\
            .nl("\t\tline1")\
            .nl("\t\tline2")\
            .nl("}")\
            .raw([TT.NORMAL, TT.NL, TT.NORMAL, TT.NL, TT.NORMAL, TT.NL, TT.NORMAL, TT.EOF_TOKEN])\
            .dented([TT.NORMAL, TT.INDENT, TT.NORMAL, TT.NL, TT.NORMAL, TT.NL, TT.DEDENT, TT.NORMAL, TT.NL, TT.EOF_TOKEN])

    if __name__ == '__main__':
        unittest.main()