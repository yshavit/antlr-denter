package com.yuvalshavit.effes.parser;

import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableList;
import org.antlr.v4.runtime.CommonToken;
import org.antlr.v4.runtime.Token;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import static org.testng.AssertJUnit.assertEquals;

public final class DenterHelperTest {
  private enum TokenType {
    NL, INDENT, OUTDENT, NORMAL, EOF_TOKEN
  }

  @Test
  public void simple() {
     TokenChecker
      .of("hello")
      .nl("  ", "bar")
      .check(TokenType.NORMAL, TokenType.INDENT, TokenType.NORMAL, TokenType.OUTDENT, TokenType.EOF_TOKEN);
  }

  @Test
  public void todo() {
    // data-driven. File should should be just spaces, newlines, and N or R for the line end.
    // N means just \n, R means \r\n.
    // Output should be something like: "INDENT INDENT OUTDENT INDENT"
    // Don't forget to test files that start off with an indentation! Also files that start with several newlines,
    // possibly with indentations
    // Also test blank lines (not ending in N or R).
    throw new AssertionError();
  }

  private static class TokenChecker {
    private int lineNo;
    private ImmutableList.Builder<Token> tokens = ImmutableList.builder();

    private TokenChecker() {}

    public static TokenChecker of(String... firstLine) {
      TokenChecker tb = new TokenChecker();
      LineBuilder lineBuilder = new LineBuilder(0, tb.tokens);
      for (String s : firstLine) {
        lineBuilder.addToken("", s, TokenType.NORMAL);
      }
      return tb;
    }

    public TokenChecker nl(String... line) {
      tokenize("\n", line);
      return this;
    }

    public void check(TokenType... expected) {
      tokens.add(new CommonToken(Token.EOF, "<eof-token>"));
      ImmutableList<Token> raw = tokens.build();
      List<Token> dented = dent(raw);
      List<TokenType> dentedTypes = tokensToTypes(dented);
      assertEquals(Arrays.asList(expected), dentedTypes);
    }

    private List<TokenType> tokensToTypes(List<Token> tokens) {
      ImmutableList.Builder<TokenType> types = ImmutableList.builder();
      for (Token t : tokens) {
        int type = t.getType();
        TokenType tokenType = type == Token.EOF
          ? TokenType.EOF_TOKEN
          : TokenType.values()[type];
        types.add(tokenType);
      }
      return types.build();
    }

    private void tokenize(String nlChars, String[] line) {
      LineBuilder lineBuilder = new LineBuilder(++lineNo, tokens);
      int i = 0;
      if (line.length > 0 && line[0].startsWith(" ")) {
        lineBuilder.addToken(nlChars, line[0], TokenType.NL);
        ++i;
      } else {
        lineBuilder.addToken(nlChars, "", TokenType.NL);
      }
      for (; i < line.length; ++i) {
        lineBuilder.addToken("", line[1], TokenType.NORMAL);
      }
    }

    private List<Token> dent(List<Token> tokens) {
      final Iterator<Token> tokenIter = tokens.iterator();
      Supplier<Token> tokenSupplier = new Supplier<Token>() {
        @Override
        public Token get() {
          return tokenIter.next();
        }
      };
      DenterHelper denter = new DenterHelper(
        tokenSupplier, TokenType.NL.ordinal(), TokenType.INDENT.ordinal(), TokenType.OUTDENT.ordinal());
      ImmutableList.Builder<Token> dented = ImmutableList.builder();
      while(true) {
        Token token = denter.nextToken();
        dented.add(token);
        if (token.getType() == Token.EOF) {
          return dented.build();
        }
      }
    }

    private static class LineBuilder {
      private final int lineNo;
      private int pos = 0;
      private final ImmutableList.Builder<Token> builder;

      private LineBuilder(int lineNo, ImmutableList.Builder<Token> builder) {
        this.lineNo = lineNo;
        this.builder = builder;
      }

      private void addToken(String prefix, String s, TokenType tokenType) {
        CommonToken token = new CommonToken(tokenType.ordinal(), prefix + s);
        token.setCharPositionInLine(pos);
        token.setLine(lineNo);
        pos += s.length();
        builder.add(token);
      }
    }
  }

}
