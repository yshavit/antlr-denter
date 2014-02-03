package com.yuvalshavit.effes.parser;

import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableList;
import org.antlr.v4.runtime.CommonToken;
import org.antlr.v4.runtime.Token;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import static com.yuvalshavit.effes.parser.DenterHelperTest.TokenType.EOF_TOKEN;
import static com.yuvalshavit.effes.parser.DenterHelperTest.TokenType.INDENT;
import static com.yuvalshavit.effes.parser.DenterHelperTest.TokenType.NL;
import static com.yuvalshavit.effes.parser.DenterHelperTest.TokenType.NORMAL;
import static com.yuvalshavit.effes.parser.DenterHelperTest.TokenType.DEDENT;
import static org.testng.AssertJUnit.assertEquals;

public final class DenterHelperTest {
  public enum TokenType {
    NL, INDENT, DEDENT, NORMAL, EOF_TOKEN
  }

  @Test
  public void simple() {
     TokenChecker
      .of("hello")
      .nl("  ", "bar")
      .check(NORMAL, INDENT, NORMAL, DEDENT, EOF_TOKEN);
  }

  @Test
  public void simpleWithNLs() {
    TokenChecker
      .of("hello")
      .nl("world")
      .nl("  ", "tab1")
      .nl("  ", "tab2")
      .check(NORMAL, NL, NORMAL, INDENT, NORMAL, NL, NORMAL, DEDENT, EOF_TOKEN);
  }

  @Test
  public void multipleDedents() {
    TokenChecker
      .of("hello")
      .nl("  ", "line2")
      .nl("    ", "line3")
      .nl("world")
      .check(NORMAL, INDENT, NORMAL, INDENT, NORMAL, DEDENT, DEDENT, NORMAL, EOF_TOKEN);
  }

  @Test
  public void multipleDedentsToEof() {
    TokenChecker
      .of("hello")
      .nl("  ", "line2")
      .nl("    ", "line3")
      .check(NORMAL, INDENT, NORMAL, INDENT, NORMAL, DEDENT, DEDENT, EOF_TOKEN);
  }

  @Test
  public void ignoreBlankLines() {
    TokenChecker
      .of("hello")
      .nl("     ")
      .nl("")
      .nl("  ", "dolly")
      .nl("        ")
      .nl("    ")
      .nl("")
      .nl("world")
      .check(NORMAL, INDENT, NORMAL, DEDENT, NORMAL, EOF_TOKEN);
  }

  @Test
  public void allIndented() {
    TokenChecker
      .of("    ", "hello")
      .nl("    ", "line2")
      .nl("       ", "line3")
      .nl("    ")
      .check(NORMAL, NL, NORMAL, INDENT, NORMAL, DEDENT, EOF_TOKEN);
  }

  @Test
  public void startIndentedThenEmptyLines() {
    TokenChecker
      .of("    ", "hello")
      .nl("    ", "line2")
      .nl("");
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
        if (s.trim().isEmpty()) {
          lineBuilder.pos = s.length();
          continue;
        }
        lineBuilder.addToken("", s, NORMAL);
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
          ? EOF_TOKEN
          : TokenType.values()[type];
        types.add(tokenType);
      }
      return types.build();
    }

    private void tokenize(String nlChars, String[] line) {
      LineBuilder lineBuilder = new LineBuilder(++lineNo, tokens);
      int i = 0;
      if (line.length > 0 && (line[0].startsWith(" ") || line[0].isEmpty())) {
        lineBuilder.addToken(nlChars, line[0], NL);
        ++i;
      } else {
        lineBuilder.addToken(nlChars, "", NL);
      }
      for (; i < line.length; ++i) {
        lineBuilder.addToken("", line[i], NORMAL);
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
        tokenSupplier, NL.ordinal(), INDENT.ordinal(), DEDENT.ordinal());
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
