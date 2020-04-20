import types

from antlr4.Token import CommonToken
from antlr4.Token import Token


class DenterHelper(object):
    def __init__(self, nlToken, indentToken, dedentToken, ignoreEOF):
        self.dentsBuffer = []
        self.indentations = []
        self.nlToken = nlToken
        self.indentToken = indentToken
        self.dedentToken = dedentToken
        self.reachedEof = False
        self.shouldIgnoreEOF = ignoreEOF

    def nextToken(self):
        self.initIfFirstRun()
        if not self.dentsBuffer:
            t = self.pullToken()
        else:
            t = self.dentsBuffer.pop(0)
        if self.reachedEof:
            return t
        if t.type == self.nlToken:
            r = self.handleNewLineToken(t)
        elif t.type == Token.EOF:
            r = self.apply(t)
        else:
            r = t
        return r

    def pullToken(self):
        """

        :rtype: CommonToken
        """
        pass

    def initIfFirstRun(self):
        if not self.indentations:
            self.indentations.insert(0, 0)
            while True:
                firstRealToken = self.pullToken()
                if firstRealToken.type != self.nlToken:
                    break
            if firstRealToken.column > 0:
                self.indentations.insert(0, firstRealToken.column)
                self.dentsBuffer.append(self.createToken(self.indentToken, firstRealToken))
            self.dentsBuffer.append(firstRealToken)

    def handleNewLineToken(self, t: Token):
        nextNext = self.pullToken()
        while nextNext.type == self.nlToken:
            t = nextNext
            nextNext = self.pullToken()
        if nextNext.type == Token.EOF:
            return self.apply(nextNext)
        nlText = t.text
        indent = len(nlText) - 1
        if indent > 0 and nlText[0] == '\r':
            indent -= 1
        prevIndent = self.indentations[0]
        if indent == prevIndent:
            r = t
        elif indent > prevIndent:
            r = self.createToken(self.indentToken, t)
            self.indentations.insert(0, indent)
        else:
            r = self.unwindTo(indent, t)
        self.dentsBuffer.append(nextNext)
        return r

    def createToken(self, tokenType, copyFrom: CommonToken):
        if tokenType == self.nlToken:
            tokenTypeStr = 'newLine'
        elif tokenType == self.indentToken:
            tokenTypeStr = 'indent'
        elif tokenType == self.dedentToken:
            tokenTypeStr = 'dedent'
        else:
            tokenTypeStr = None
        r = self.getInjectedToken(copyFrom, tokenTypeStr)
        r.type = tokenType
        return r

    def getInjectedToken(self, copyfrom: CommonToken, tokenTypeStr):
        newToken = copyfrom.clone()
        newToken.text = tokenTypeStr
        return newToken

    def unwindTo(self, targetIndent, copyFrom : CommonToken):
        self.dentsBuffer.append(self.createToken(self.nlToken, copyFrom))
        while True:
            prevIndent = self.indentations.pop(0)
            if prevIndent == targetIndent:
                break
            if targetIndent > prevIndent:
                self.indentations.insert(0, prevIndent)
                self.dentsBuffer.append(self.createToken(self.indentToken, copyFrom))
                break
            self.dentsBuffer.append(self.createToken(self.dedentToken, copyFrom))
        self.indentations.insert(0, targetIndent)
        return self.dentsBuffer.pop(0)

    def apply(self, t: Token):
        if self.shouldIgnoreEOF:
            self.reachedEof = True
            return t
        else:
            # when we reach EOF, unwind all indentations. If there aren't any, insert a NL. This lets the grammar treat
            # un-indented expressions as just being NL-terminated, rather than NL|EOF.
            if not self.indentations:
                r = self.createToken(self.nlToken, t)
                self.dentsBuffer.append(t)
            else:
                r = self.unwindTo(0, t)
                self.dentsBuffer.append(t)
            self.reachedEof = True
            return r
