using System.Collections.Generic;
using System.Collections.ObjectModel;
using System.Linq;
using Antlr4.Runtime;
using AntlrDenter;
using NUnit.Framework;
using static AntlrDenterTest.DenterHelperTests.TokenType;

namespace AntlrDenterTest
{
    public class DenterHelperTests
    {
        public enum TokenType
        {
            NL,
            INDENT,
            DEDENT,
            NORMAL,
            EOF_TOKEN
        }

        [SetUp]
        public void Setup()
        {
        }

        [Test]
        public void Simple()
        {
            TokenChecker
                .Of("hello")
                .Nl("  bar")
                .Raw(NORMAL, NL, NORMAL, EOF_TOKEN)
                .Dented(NORMAL, INDENT, NORMAL, NL, DEDENT, EOF_TOKEN);
        }

        [Test]
        public void SimpleWithNLs()
        {
            TokenChecker
                .Of("hello")
                .Nl("world")
                .Nl("  tab1")
                .Nl("  tab2")
                .Raw(NORMAL, NL, NORMAL, NL, NORMAL, NL, NORMAL, EOF_TOKEN)
                .Dented(NORMAL, NL, NORMAL, INDENT, NORMAL, NL, NORMAL, NL, DEDENT, EOF_TOKEN);
        }

        [Test]
        public void MultipleDedents()
        {
            TokenChecker
                .Of("hello")
                .Nl("  line2")
                .Nl("    line3")
                .Nl("world")
                .Raw(NORMAL, NL, NORMAL, NL, NORMAL, NL, NORMAL, EOF_TOKEN)
                .Dented(NORMAL, INDENT, NORMAL, INDENT, NORMAL, NL, DEDENT, DEDENT, NORMAL, NL, EOF_TOKEN);
        }

        [Test]
        public void MultipleDedentsToEof()
        {
            TokenChecker
                .Of("hello")
                .Nl("  line2")
                .Nl("    line3")
                .Raw(NORMAL, NL, NORMAL, NL, NORMAL, EOF_TOKEN)
                .Dented(NORMAL, INDENT, NORMAL, INDENT, NORMAL, NL, DEDENT, DEDENT, EOF_TOKEN);
        }

        [Test]
        public void IgnoreBlankLines()
        {
            TokenChecker
                .Of("hello")
                .Nl("     ")
                .Nl("")
                .Nl("  dolly")
                .Nl("        ")
                .Nl("    ")
                .Nl("")
                .Nl("world")
                .Raw(NORMAL, NL, NL, NL, NORMAL, NL, NL, NL, NL, NORMAL, EOF_TOKEN)
                .Dented(NORMAL, INDENT, NORMAL, NL, DEDENT, NORMAL, NL, EOF_TOKEN);
        }

        [Test]
        public void AllIndented()
        {
            TokenChecker
                .Of("    hello")
                .Nl("    line2")
                .Nl("       line3")
                .Nl("    ")
                .Raw(NORMAL, NL, NORMAL, NL, NORMAL, NL, EOF_TOKEN)
                .Dented(INDENT, NORMAL, NL, NORMAL, INDENT, NORMAL, NL, DEDENT, DEDENT, EOF_TOKEN);
        }

        [Test]
        public void StartIndentedThenEmptyLines()
        {
            TokenChecker
                .Of("    hello")
                .Nl("    line2")
                .Nl("")
                .Raw(NORMAL, NL, NORMAL, NL, EOF_TOKEN)
                .Dented(INDENT, NORMAL, NL, NORMAL, NL, DEDENT, EOF_TOKEN);
        }

        [Test]
        public void DedentToNegative()
        {
            // this shouldn't explode, it should just result in an extra dedent
            TokenChecker
                .Of("    hello")
                .Nl("    world")
                .Nl("boom")
                .Raw(NORMAL, NL, NORMAL, NL, NORMAL, EOF_TOKEN)
                .Dented(INDENT, NORMAL, NL, NORMAL, NL, DEDENT, NORMAL, NL, EOF_TOKEN);
        }

        [Test]
        public void HalfDent()
        {
            TokenChecker
                .Of("hello")
                .Nl("     world")
                .Nl("  boom")
                .Raw(NORMAL, NL, NORMAL, NL, NORMAL, EOF_TOKEN)
                .Dented(NORMAL, INDENT, NORMAL, NL, DEDENT, INDENT, NORMAL, NL, DEDENT, EOF_TOKEN);
        }

        [Test]
        public void HalfDentFromTwo()
        {
            TokenChecker
                .Of("hello")
                .Nl("     world")
                .Nl("         universe")
                .Nl("  boom")
                .Raw(NORMAL, NL, NORMAL, NL, NORMAL, NL, NORMAL, EOF_TOKEN)
                .Dented(NORMAL, INDENT, NORMAL, INDENT, NORMAL, NL, DEDENT, DEDENT, INDENT, NORMAL, NL, DEDENT,
                    EOF_TOKEN);
        }

        [Test]
        public void WithReturn()
        {
            TokenChecker
                .Of("hello")
                .Nl("world")
                .Rf("dolly")
                .Raw(NORMAL, NL, NORMAL, NL, NORMAL, EOF_TOKEN)
                .Dented(NORMAL, NL, NORMAL, NL, NORMAL, NL, EOF_TOKEN);
        }

        [Test]
        public void IgnoreEofNoDedent()
        {
            TokenChecker
                .Of("hello")
                .Raw(NORMAL, EOF_TOKEN)
                .IgnoreEof()
                .Dented(NORMAL, EOF_TOKEN);
        }

        [Test]
        public void IgnoreEofWithDedent()
        {
            TokenChecker
                .Of("hello")
                .Nl("  world")
                .Raw(NORMAL, NL, NORMAL, EOF_TOKEN)
                .IgnoreEof()
                .Dented(NORMAL, INDENT, NORMAL, EOF_TOKEN);
        }

        [Test]
        public void TabIndents()
        {
            TokenChecker
                .Of("{")
                .Nl("\t\tline1")
                .Nl("\t\tline2")
                .Nl("}")
                .Raw(NORMAL, NL, NORMAL, NL, NORMAL, NL, NORMAL, EOF_TOKEN)
                .Dented(NORMAL, INDENT, NORMAL, NL, NORMAL, NL, DEDENT, NORMAL, NL, EOF_TOKEN);
        }

        private static int LeadingSpacesOf(string s)
        {
            for (int i = 0, len = s.Length; i < len; ++i)
                if (!char.IsWhiteSpace(s[i]))
                    return i;
            // no spaces in the string (including blank string)
            return s.Length;
        }

        private interface ITokenBuilder
        {
            ITokenBuilder Nl(string line);
            ITokenBuilder Rf(string line);
            IDentChecker Raw(params TokenType[] expected);
        }

        private interface IDentChecker
        {
            IDentChecker IgnoreEof();
            void Dented(params TokenType[] expected);
        }

        private class TokenChecker : ITokenBuilder, IDentChecker
        {
            private readonly List<IToken> _tokens = new List<IToken>();
            private bool _ignoreEof;
            private int _lineNo;

            private TokenChecker()
            {
            }

            public IDentChecker IgnoreEof()
            {
                _ignoreEof = true;
                return this;
            }

            public void Dented(params TokenType[] expected)
            {
                var dented = Dent(_tokens);
                var dentedTypes = TokensToTypes(dented);
                Assert.AreEqual(expected.ToList(), dentedTypes);
            }

            public ITokenBuilder Nl(string line)
            {
                Tokenize("\n", line);
                return this;
            }

            public ITokenBuilder Rf(string line)
            {
                Tokenize("\r\n", line);
                return this;
            }

            public IDentChecker Raw(params TokenType[] expected)
            {
                _tokens.Add(new CommonToken(-1, "<eof-token>"));
                var rawTypes = TokensToTypes(_tokens);
                Assert.AreEqual(expected.ToList(), rawTypes);
                return this;
            }

            public static TokenChecker Of(string firstLine)
            {
                var tb = new TokenChecker();
                var lineBuilder = new LineBuilder(0, tb._tokens);
                int leading = LeadingSpacesOf(firstLine);
                lineBuilder.Pos = leading;
                firstLine = firstLine.Substring(leading);
                if (firstLine.Length != 0) lineBuilder.AddToken("", firstLine, NORMAL);
                return tb;
            }

            private static ReadOnlyCollection<TokenType> TokensToTypes(IEnumerable<IToken> tokens)
            {
                var types = new List<TokenType>();
                foreach (IToken t in tokens)
                {
                    int type = t.Type;
                    TokenType tokenType = type == -1
                        ? EOF_TOKEN
                        : (TokenType) type;
                    types.Add(tokenType);
                }

                return new ReadOnlyCollection<TokenType>(types);
            }

            private void Tokenize(string nlChars, string line)
            {
                var lineBuilder = new LineBuilder(++_lineNo, _tokens);
                int leading = LeadingSpacesOf(line);
                lineBuilder.AddToken(nlChars, line.Substring(0, leading), NL);
                line = line.Substring(leading);
                if (line.Length != 0) lineBuilder.AddToken("", line, NORMAL);
            }

            private ReadOnlyCollection<IToken> Dent(List<IToken> tokens)
            {
                IEnumerator<IToken> tokenIter = tokens.GetEnumerator();
                DenterHelper denter = new IterableBasedDenterHelper((int) NL, (int) INDENT, (int) DEDENT, tokenIter);
                if (_ignoreEof) denter.GetOptions().IgnoreEof();

                var dented = new List<IToken>();
                while (true)
                {
                    IToken token = denter.NextToken();
                    dented.Add(token);
                    if (token.Type == -1) return new ReadOnlyCollection<IToken>(dented);
                }
            }

            private class LineBuilder
            {
                private readonly List<IToken> _builder;
                private readonly int _lineNo;
                internal int Pos;

                internal LineBuilder(int lineNo, List<IToken> builder)
                {
                    _lineNo = lineNo;
                    _builder = builder;
                }

                internal void AddToken(string prefix, string s, TokenType tokenType)
                {
                    var token = new CommonToken((int) tokenType, prefix + s);
                    token.Column = Pos;
                    token.Line = _lineNo;
                    Pos += s.Length;
                    _builder.Add(token);
                }
            }
        }

        private class IterableBasedDenterHelper : DenterHelper
        {
            private readonly IEnumerator<IToken> _tokens;

            internal IterableBasedDenterHelper(int nlToken, int indentToken, int dedentToken,
                IEnumerator<IToken> tokens)
                : base(nlToken, indentToken, dedentToken)
            {
                _tokens = tokens;
            }

            protected override IToken PullToken()
            {
                _tokens.MoveNext();
                return _tokens.Current;
            }
        }
    }
}