using System;
using System.Collections.Generic;
using Antlr4.Runtime;

namespace AntlrDenter
{
    public abstract class DenterHelper
    {
        private readonly int _dedentToken;
        private readonly Queue<IToken> _dentsBuffer = new Queue<IToken>();
        private readonly Deque<int> _indentations = new Deque<int>();
        private readonly int _indentToken;
        private readonly int _nlToken;
        private IEofHandler _eofHandler;
        private bool _reachedEof;

        protected DenterHelper(int nlToken, int indentToken, int dedentToken)
        {
            _nlToken = nlToken;
            _indentToken = indentToken;
            _dedentToken = dedentToken;
            _eofHandler = new StandardEofHandler(this);
        }

        public IToken NextToken()
        {
            InitIfFirstRun();
            IToken t = _dentsBuffer.Count == 0
                ? PullToken()
                : _dentsBuffer.Dequeue();
            if (_reachedEof) return t;
            IToken r;
            if (t.Type == _nlToken)
                r = HandleNewlineToken(t);
            else if (t.Type == -1)
                r = _eofHandler.Apply(t);
            else
                r = t;
            return r;
        }

        public IDenterOptions GetOptions()
        {
            return new DenterOptionsImpl(this);
        }

        protected abstract IToken PullToken();

        private void InitIfFirstRun()
        {
            if (_indentations.Count == 0)
            {
                _indentations.AddFront(0);
                // First invocation. Look for the first non-NL. Enqueue it, and possibly an indentation if that non-NL
                // token doesn't start at char 0.
                IToken firstRealToken;
                do
                {
                    firstRealToken = PullToken();
                } while (firstRealToken.Type == _nlToken);

                if (firstRealToken.Column > 0)
                {
                    _indentations.AddFront(firstRealToken.Column);
                    _dentsBuffer.Enqueue(CreateToken(_indentToken, firstRealToken));
                }

                _dentsBuffer.Enqueue(firstRealToken);
            }
        }

        private IToken HandleNewlineToken(IToken t)
        {
            // fast-forward to the next non-NL
            IToken nextNext = PullToken();
            while (nextNext.Type == _nlToken)
            {
                t = nextNext;
                nextNext = PullToken();
            }

            if (nextNext.Type == -1) return _eofHandler.Apply(nextNext);
            // nextNext is now a non-NL token; we'll queue it up after any possible dents

            string nlText = t.Text;
            int indent = nlText.Length - 1; // every NL has one \n char, so shorten the length to account for it
            if (indent > 0 && nlText[0] == '\r')
                --indent; // If the NL also has a \r char, we should account for that as well
            int prevIndent = _indentations.Get(0);
            IToken r;
            if (indent == prevIndent)
            {
                r = t; // just a newline
            }
            else if (indent > prevIndent)
            {
                r = CreateToken(_indentToken, t);
                _indentations.AddFront(indent);
            }
            else
            {
                r = UnwindTo(indent, t);
            }

            _dentsBuffer.Enqueue(nextNext);
            return r;
        }

        private IToken CreateToken(int tokenType, IToken copyFrom)
        {
            string tokenTypeStr;
            if (tokenType == _nlToken)
                tokenTypeStr = "newline";
            else if (tokenType == _indentToken)
                tokenTypeStr = "indent";
            else if (tokenType == _dedentToken)
                tokenTypeStr = "dedent";
            else
                tokenTypeStr = null;
            CommonToken r = new InjectedToken(copyFrom, tokenTypeStr);
            r.Type = tokenType;
            return r;
        }

        /**
         * Returns a DEDENT token, and also queues up additional DEDENTS as necessary.
         * @param targetIndent the "size" of the indentation (number of spaces) by the end
         * @param copyFrom the triggering token
         * @return a DEDENT token
         */
        private IToken UnwindTo(int targetIndent, IToken copyFrom)
        {
            //assert _dentsBuffer.isEmpty() : _dentsBuffer;
            _dentsBuffer.Enqueue(CreateToken(_nlToken, copyFrom));
            // To make things easier, we'll queue up ALL of the dedents, and then pop off the first one.
            // For example, here's how some text is analyzed:
            //
            //  Text          :  Indentation  :  Action         : Indents Deque
            //  [ baseline ]  :  0            :  nothing        : [0]
            //  [   foo    ]  :  2            :  INDENT         : [0, 2]
            //  [    bar   ]  :  3            :  INDENT         : [0, 2, 3]
            //  [ baz      ]  :  0            :  DEDENT x2      : [0]

            while (true)
            {
                int prevIndent = _indentations.RemoveFront();
                if (prevIndent == targetIndent) break;
                if (targetIndent > prevIndent)
                {
                    // "weird" condition above
                    _indentations.AddFront(prevIndent); // restore previous indentation, since we've indented from it
                    _dentsBuffer.Enqueue(CreateToken(_indentToken, copyFrom));
                    break;
                }

                _dentsBuffer.Enqueue(CreateToken(_dedentToken, copyFrom));
            }

            _indentations.AddFront(targetIndent);
            return _dentsBuffer.Dequeue();
        }

        public static IBuilder0 Builder()
        {
            return new BuilderImpl();
        }

        private class StandardEofHandler : IEofHandler
        {
            private readonly DenterHelper _helper;

            public StandardEofHandler(DenterHelper helper)
            {
                _helper = helper;
            }

            public IToken Apply(IToken t)
            {
                IToken r;
                // when we reach EOF, unwind all indentations. If there aren't any, insert a NL. This lets the grammar treat
                // un-indented expressions as just being NL-terminated, rather than NL|EOF.
                if (_helper._indentations.Count == 0)
                {
                    r = _helper.CreateToken(_helper._nlToken, t);
                    _helper._dentsBuffer.Enqueue(t);
                }
                else
                {
                    r = _helper.UnwindTo(0, t);
                    _helper._dentsBuffer.Enqueue(t);
                }

                _helper._reachedEof = true;
                return r;
            }
        }

        private interface IEofHandler
        {
            IToken Apply(IToken t);
        }

        private class DenterOptionsImpl : IDenterOptions
        {
            private readonly DenterHelper _helper;

            public DenterOptionsImpl(DenterHelper helper)
            {
                _helper = helper;
            }

            public void IgnoreEof()
            {
                _helper._eofHandler = new EofHandler(_helper);
            }

            private class EofHandler : IEofHandler
            {
                private readonly DenterHelper _helper;

                public EofHandler(DenterHelper helper)
                {
                    _helper = helper;
                }

                public IToken Apply(IToken t)
                {
                    _helper._reachedEof = true;
                    return t;
                }
            }
        }

        private class InjectedToken : CommonToken
        {
            private readonly string _type;

            public InjectedToken(IToken oldToken, string type) : base(oldToken)
            {
                _type = type;
            }

            public string GetText()
            {
                if (_type != null) Text = _type;
                return Text;
            }
        }

        public interface IBuilder0
        {
            IBuilder1 Nl(int nl);
        }

        public interface IBuilder1
        {
            IBuilder2 Indent(int indent);
        }

        public interface IBuilder2
        {
            IBuilder3 Dedent(int dedent);
        }

        public interface IBuilder3
        {
            DenterHelper PullToken(Func<IToken> puller);
        }

        private class BuilderImpl : IBuilder0, IBuilder1, IBuilder2, IBuilder3
        {
            private int _dedent;
            private int _indent;
            private int _nl;

            public IBuilder1 Nl(int nl)
            {
                _nl = nl;
                return this;
            }

            public IBuilder2 Indent(int indent)
            {
                _indent = indent;
                return this;
            }

            public IBuilder3 Dedent(int dedent)
            {
                _dedent = dedent;
                return this;
            }

            public DenterHelper PullToken(Func<IToken> puller)
            {
                return new DenterHelperImpl(_nl, _indent, _dedent, puller);
            }

            private class DenterHelperImpl : DenterHelper
            {
                private readonly Func<IToken> _puller;

                public DenterHelperImpl(int nlToken, int indentToken, int dedentToken, Func<IToken> puller) : base(
                    nlToken, indentToken, dedentToken)
                {
                    _puller = puller;
                }

                protected override IToken PullToken()
                {
                    return _puller();
                }
            }
        }
    }
}