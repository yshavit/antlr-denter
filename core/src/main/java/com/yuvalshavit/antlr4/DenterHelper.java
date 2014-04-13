package com.yuvalshavit.antlr4;

import org.antlr.v4.runtime.CommonToken;
import org.antlr.v4.runtime.Token;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Queue;

public abstract class DenterHelper {
  private final Queue<Token> dentsBuffer = new ArrayDeque<>();
  private final Deque<Integer> indentations = new ArrayDeque<>();
  private final int nlToken;
  private final int indentToken;
  private final int dedentToken;
  private boolean reachedEof;
  private EofHandler eofHandler = new StandardEofHandler();

  protected DenterHelper(int nlToken, int indentToken, int dedentToken) {
    this.nlToken = nlToken;
    this.indentToken = indentToken;
    this.dedentToken = dedentToken;
  }

  public Token nextToken() {
    initIfFirstRun();
    Token t = dentsBuffer.isEmpty()
      ? pullToken()
      : dentsBuffer.remove();
    if (reachedEof) {
      return t;
    }
    final Token r;
    if (t.getType() == nlToken) {
      r = handleNewlineToken(t);
    } else if (t.getType() == Token.EOF) {
      r = eofHandler.apply(t);

    } else {
      r = t;
    }
    return r;
  }

  public DenterOptions getOptions() {
    return new DenterOptionsImpl();
  }

  protected abstract Token pullToken();

  private void initIfFirstRun() {
    if (indentations.isEmpty()) {
      indentations.push(0);
      // First invocation. Look for the first non-NL. Enqueue it, and possibly an indentation if that non-NL
      // token doesn't start at char 0.
      Token firstRealToken;
      do {
        firstRealToken = pullToken();
      }
      while(firstRealToken.getType() == nlToken);

      if (firstRealToken.getCharPositionInLine() > 0) {
        indentations.push(firstRealToken.getCharPositionInLine());
        dentsBuffer.add(createToken(indentToken, firstRealToken));
      }
      dentsBuffer.add(firstRealToken);
    }
  }

  private Token handleNewlineToken(Token t) {
    // fast-forward to the next non-NL
    Token nextNext = pullToken();
    while (nextNext.getType() == nlToken) {
      t = nextNext;
      nextNext = pullToken();
    }
    if (nextNext.getType() == Token.EOF) {
      return eofHandler.apply(nextNext);
    }
    // nextNext is now a non-NL token; we'll queue it up after any possible dents

    String nlText = t.getText();
    int indent = nlText.length() - 1; // every NL has one \n char, so shorten the length to account for it
    if (indent > 0 && nlText.charAt(0) == '\r') {
      --indent; // If the NL also has a \r char, we should account for that as well
    }
    int prevIndent = indentations.peek();
    final Token r;
    if (indent == prevIndent) {
      r = t; // just a newline
    } else if (indent > prevIndent) {
      r = createToken(indentToken, t);
      indentations.push(indent);
    } else {
      r = unwindTo(indent, t);
    }
    dentsBuffer.add(nextNext);
    return r;
  }

  private final class StandardEofHandler implements EofHandler {
    @Override
    public Token apply(Token t) {
      Token r;
      // when we reach EOF, unwind all indentations. If there aren't any, insert a NL. This lets the grammar treat
      // un-indented expressions as just being NL-terminated, rather than NL|EOF.
      if (indentations.isEmpty()) {
        r = createToken(nlToken, t);
        dentsBuffer.add(t);
      } else {
        r = unwindTo(0, t);
        dentsBuffer.add(t);
      }
      reachedEof = true;
      return r;
    }
  }

  private Token createToken(int tokenType, Token copyFrom) {
    String tokenTypeStr;
    if (tokenType == nlToken) {
      tokenTypeStr = "newline";
    } else if (tokenType == indentToken) {
      tokenTypeStr = "indent";
    } else if (tokenType == dedentToken) {
      tokenTypeStr = "dedent";
    } else {
      tokenTypeStr = null;
    }
    CommonToken r = new InjectedToken(copyFrom, tokenTypeStr);
    r.setType(tokenType);
    return r;
  }

  /**
   * Returns a DEDENT token, and also queues up additional DEDENTS as necessary.
   * @param targetIndent the "size" of the indentation (number of spaces) by the end
   * @param copyFrom the triggering token
   * @return a DEDENT token
   */
  private Token unwindTo(int targetIndent, Token copyFrom) {
    assert dentsBuffer.isEmpty() : dentsBuffer;
    dentsBuffer.add(createToken(nlToken, copyFrom));
    // To make things easier, we'll queue up ALL of the dedents, and then pop off the first one.
    // For example, here's how some text is analyzed:
    //
    //  Text          :  Indentation  :  Action         : Indents Deque
    //  [ baseline ]  :  0            :  nothing        : [0]
    //  [   foo    ]  :  2            :  INDENT         : [0, 2]
    //  [    bar   ]  :  3            :  INDENT         : [0, 2, 3]
    //  [ baz      ]  :  0            :  DEDENT x2      : [0]

    while (true) {
      int prevIndent = indentations.pop();
      if (prevIndent == targetIndent) {
        break;
      }
      if (targetIndent > prevIndent) {
        // "weird" condition above
        indentations.push(prevIndent); // restore previous indentation, since we've indented from it
        dentsBuffer.add(createToken(indentToken, copyFrom));
        break;
      }
      dentsBuffer.add(createToken(dedentToken, copyFrom));
    }
    indentations.push(targetIndent);
    return dentsBuffer.remove();
  }

  private interface EofHandler {
    Token apply(Token t);
  }

  private class DenterOptionsImpl implements DenterOptions {
    @Override
    public void ignoreEOF() {
      eofHandler = new EofHandler() {
        @Override
        public Token apply(Token t) {
          reachedEof = true;
          return t;
        }
      };
    }

  }

  private static class InjectedToken extends CommonToken {
    private String type;

    private InjectedToken(Token oldToken, String type) {
      super(oldToken);
      this.type = type;
    }

    @Override
    public String getText() {
      if (type != null) {
        setText(type);
      }
      return super.getText();
    }
  }

  public interface Builder0 {
    Builder1 nl(int nl);
  }

  public interface Builder1 {
    Builder2 indent(int indent);
  }

  public interface Builder2 {
    Builder3 dedent(int dedent);
  }

  public interface Builder3 {
    DenterHelper pullToken(TokenPuller puller);
  }

  public interface TokenPuller {
    Token pullToken();
  }

  public static Builder0 builder() {
    return new BuilderImpl();
  }

  private static class BuilderImpl implements Builder0, Builder1, Builder2, Builder3 {
    private int nl;
    private int indent;
    private int dedent;

    @Override
    public Builder1 nl(int nl) {
      this.nl = nl;
      return this;
    }

    @Override
    public Builder2 indent(int indent) {
      this.indent = indent;
      return this;
    }

    @Override
    public Builder3 dedent(int dedent) {
      this.dedent = dedent;
      return this;
    }

    @Override
    public DenterHelper pullToken(TokenPuller puller) {
      final TokenPuller p = puller;
      return new DenterHelper(nl, indent, dedent) {
        @Override
        protected Token pullToken() {
          return p.pullToken();
        }
      };
    }
  }
}
