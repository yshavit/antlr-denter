import unittest
from enum import Enum
from antlr4.Token import CommonToken
from antlr4.Token import Token
from antlr_denter.DenterHelper import DenterHelper


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
    def set_ignore_eof_true(self):
        pass

    def dented(self, expected: []):
        pass


class LineBuilder:
    def __init__(self, line_no, builder: []):
        self.lineNo = line_no
        self.builder = builder
        self.pos = 0

    def addToken(self, prefix: str, s: str, token_type: TT):
        token = CommonToken(type=token_type.value)
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
    def of(first_line: str):
        tb = TokenChecker()
        line_builder = LineBuilder(0, tb.tokens)
        leading = leading_spaces_of(first_line)
        line_builder.pos = leading
        first_line = first_line[leading:]
        if first_line:
            line_builder.addToken("", first_line, TT.NORMAL)
        return tb

    def nl(self, line: str):
        self.tokenize("\n", line)
        return self

    def rf(self, line: str):
        self.tokenize("\r\n", line)
        return self

    def set_ignore_eof_true(self):
        self.ignoreEOF = True
        return self

    def dented(self, expected: []):
        dented = self.dent(self.tokens)
        dented_types = self.tokens_to_types(dented)
        self.assertEqual(expected, dented_types, "dented tokens")

    def tokens_to_types(self, tokens: []):
        types = []
        for t in tokens:
            if t.type == Token.EOF:
                token_type = TT.EOF_TOKEN
            else:
                token_type = TT(t.type)
            types.append(token_type)
        return types

    def tokenize(self, nl_chars: str, line: str):
        self.lineNo += 1
        line_builder = LineBuilder(self.lineNo, self.tokens)
        leading = leading_spaces_of(line)
        line_builder.addToken(nl_chars, line[0:leading], TT.NL)
        line = line[leading:]
        if line:
            line_builder.addToken("", line, TT.NORMAL)

    def dent(self, tokens: []):
        denter = InterableBasedDenterHelper(TT.NL.value, TT.INDENT.value, TT.DEDENT.value, tokens)
        if self.ignoreEOF:
            denter.should_ignore_eof = True
        dented = []
        while True:
            token = denter.next_token()
            dented.append(token)
            if token.type == Token.EOF:
                return dented

    def raw(self, expected: []):
        token = CommonToken(type=Token.EOF)
        token.text = "<eof-token>"
        self.tokens.append(token)
        raw_types = self.tokens_to_types(self.tokens)
        self.assertEqual(expected, raw_types, "raw tokens")
        return self

    def reset(self):
        self.tokens = []
        self.lineNo = 0
        self.ignoreEOF = False

def leading_spaces_of(s: str):
    for i in range(len(s)):
        if not s[i].isspace():
            return i
    return len(s)


class InterableBasedDenterHelper(DenterHelper):
  
    def __init__(self, nl_token, indent_token, dedent_token, tokens: []):
        super(InterableBasedDenterHelper, self).__init__(nl_token, indent_token, dedent_token, False)
        self.tokens = tokens
        self.currentIndex = -1

    def pull_token(self):
        self.currentIndex += 1
        return self.tokens[self.currentIndex]


class DenterHelperTest(unittest.TestCase):

    def test_simple(self):
        TokenChecker()\
            .of("hello")\
            .nl("  bar")\
            .raw([TT.NORMAL, TT.NL, TT.NORMAL, TT.EOF_TOKEN])\
            .dented([TT.NORMAL, TT.INDENT, TT.NORMAL, TT.NL, TT.DEDENT, TT.EOF_TOKEN])

    def test_simple_with_newlines(self):
        TokenChecker()\
            .of("hello")\
            .nl("world")\
            .nl("  tab1")\
            .nl("  tab2")\
            .raw([TT.NORMAL, TT.NL, TT.NORMAL, TT.NL, TT.NORMAL, TT.NL, TT.NORMAL, TT.EOF_TOKEN])\
            .dented([TT.NORMAL, TT.NL, TT.NORMAL, TT.INDENT, TT.NORMAL, TT.NL, TT.NORMAL, TT.NL, TT.DEDENT, TT.EOF_TOKEN])

    def test_multiple_dedents(self):
        TokenChecker()\
            .of("hello")\
            .nl("  line2")\
            .nl("    line3")\
            .nl("world")\
            .raw([TT.NORMAL, TT.NL, TT.NORMAL, TT.NL, TT.NORMAL, TT.NL, TT.NORMAL, TT.EOF_TOKEN])\
            .dented([TT.NORMAL, TT.INDENT, TT.NORMAL, TT.INDENT, TT.NORMAL, TT.NL, TT.DEDENT, TT.DEDENT, TT.NORMAL, TT.NL, TT.EOF_TOKEN])

    def test_multiple_dedents_with_same_line(self):
        TokenChecker() \
            .of("hello") \
            .nl("  line2") \
            .nl("  line3")\
            .nl("    line4") \
            .nl("world") \
            .raw([TT.NORMAL, TT.NL, TT.NORMAL, TT.NL, TT.NORMAL, TT.NL, TT.NORMAL, TT.NL, TT.NORMAL, TT.EOF_TOKEN]) \
            .dented([TT.NORMAL, TT.INDENT, TT.NORMAL, TT.NL, TT.NORMAL, TT.INDENT, TT.NORMAL, TT.NL, TT.DEDENT, TT.DEDENT, TT.NORMAL, TT.NL, TT.EOF_TOKEN])

    def test_multiple_dedents_to_eof(self):
        TokenChecker()\
            .of("hello")\
            .nl("  line2")\
            .nl("    line3")\
            .raw([TT.NORMAL, TT.NL, TT.NORMAL, TT.NL, TT.NORMAL, TT.EOF_TOKEN])\
            .dented([TT.NORMAL, TT.INDENT, TT.NORMAL, TT.INDENT, TT.NORMAL, TT.NL, TT.DEDENT, TT.DEDENT, TT.EOF_TOKEN])

    def test_ignore_blank_lines(self):
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

    def test_all_indented(self):
        TokenChecker()\
            .of("    hello")\
            .nl("    line2")\
            .nl("       line3")\
            .nl("    ")\
            .raw([TT.NORMAL, TT.NL, TT.NORMAL, TT.NL, TT.NORMAL, TT.NL, TT.EOF_TOKEN])\
            .dented([TT.INDENT, TT.NORMAL, TT.NL, TT.NORMAL, TT.INDENT, TT.NORMAL, TT.NL, TT.DEDENT, TT.DEDENT, TT.EOF_TOKEN])

    def test_start_indented_then_empty_lines(self):
        TokenChecker()\
            .of("    hello")\
            .nl("    line2")\
            .nl("")\
            .raw([TT.NORMAL, TT.NL, TT.NORMAL, TT.NL, TT.EOF_TOKEN])\
            .dented([TT.INDENT, TT.NORMAL, TT.NL, TT.NORMAL, TT.NL, TT.DEDENT, TT.EOF_TOKEN])

    def test_detent_to_negative(self):
        #this shouldn't explode, it should just result in an extra dedent
        TokenChecker()\
            .of("    hello")\
            .nl("    world")\
            .nl("boom")\
            .raw([TT.NORMAL, TT.NL, TT.NORMAL, TT.NL, TT.NORMAL, TT.EOF_TOKEN])\
            .dented([TT.INDENT, TT.NORMAL, TT.NL, TT.NORMAL, TT.NL, TT.DEDENT, TT.NORMAL, TT.NL, TT.EOF_TOKEN])

    def test_half_dent(self):
        TokenChecker()\
            .of("hello")\
            .nl("     world")\
            .nl("  boom")\
            .raw([TT.NORMAL, TT.NL, TT.NORMAL, TT.NL, TT.NORMAL, TT.EOF_TOKEN])\
            .dented([TT.NORMAL, TT.INDENT, TT.NORMAL, TT.NL, TT.DEDENT, TT.INDENT, TT.NORMAL, TT.NL, TT.DEDENT, TT.EOF_TOKEN])

    def test_half_dent_from_two(self):
        TokenChecker()\
            .of("hello")\
            .nl("     world")\
            .nl("         universe")\
            .nl("  boom")\
            .raw([TT.NORMAL, TT.NL, TT.NORMAL, TT.NL, TT.NORMAL, TT.NL, TT.NORMAL, TT.EOF_TOKEN])\
            .dented([TT.NORMAL, TT.INDENT, TT.NORMAL, TT.INDENT, TT.NORMAL, TT.NL, TT.DEDENT, TT.DEDENT, TT.INDENT, TT.NORMAL, TT.NL, TT.DEDENT, TT.EOF_TOKEN])

    def test_with_return(self):
        TokenChecker()\
            .of("hello")\
            .nl("world")\
            .rf("dolly")\
            .raw([TT.NORMAL, TT.NL, TT.NORMAL, TT.NL, TT.NORMAL, TT.EOF_TOKEN])\
            .dented([TT.NORMAL, TT.NL, TT.NORMAL, TT.NL, TT.NORMAL, TT.NL, TT.EOF_TOKEN])

    def test_ignore_eof_no_dedents(self):
        TokenChecker()\
            .of("hello")\
            .raw([TT.NORMAL, TT.EOF_TOKEN])\
            .set_ignore_eof_true()\
            .dented([TT.NORMAL, TT.EOF_TOKEN])

    def test_ignore_eof_with_dedents(self):
        TokenChecker()\
            .of("hello")\
            .nl("  world")\
            .raw([TT.NORMAL, TT.NL, TT.NORMAL, TT.EOF_TOKEN])\
            .set_ignore_eof_true()\
            .dented([TT.NORMAL, TT.INDENT, TT.NORMAL, TT.EOF_TOKEN])

    def test_tab_indents(self):
        TokenChecker()\
            .of("{")\
            .nl("\t\tline1")\
            .nl("\t\tline2")\
            .nl("}")\
            .raw([TT.NORMAL, TT.NL, TT.NORMAL, TT.NL, TT.NORMAL, TT.NL, TT.NORMAL, TT.EOF_TOKEN])\
            .dented([TT.NORMAL, TT.INDENT, TT.NORMAL, TT.NL, TT.NORMAL, TT.NL, TT.DEDENT, TT.NORMAL, TT.NL, TT.EOF_TOKEN])

    def test_back_and_forth(self):
        TokenChecker()\
            .of("hello")\
            .nl("  world")\
            .nl("    this")\
            .nl("test")\
            .nl("  is")\
            .nl("    great")\
            .raw([TT.NORMAL, TT.NL, TT.NORMAL, TT.NL, TT.NORMAL, TT.NL, TT.NORMAL, TT.NL, TT.NORMAL, TT.NL, TT.NORMAL, TT.EOF_TOKEN])\
            .dented([TT.NORMAL, TT.INDENT, TT.NORMAL, TT.INDENT, TT.NORMAL, TT.NL, TT.DEDENT, TT.DEDENT, TT.NORMAL, TT.INDENT, TT.NORMAL, TT.INDENT, TT.NORMAL, TT.NL, TT.DEDENT, TT.DEDENT,TT.EOF_TOKEN])

    if __name__ == '__main__':
        unittest.main()
