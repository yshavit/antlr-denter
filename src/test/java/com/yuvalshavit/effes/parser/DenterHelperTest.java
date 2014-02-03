package com.yuvalshavit.effes.parser;

import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableList;
import org.antlr.v4.runtime.CommonToken;
import org.antlr.v4.runtime.Token;
import org.testng.annotations.Test;

import java.util.ArrayList;
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
      .nl("  bar")
      .raw(NORMAL, NL, NORMAL, EOF_TOKEN)
      .dented(NORMAL, INDENT, NORMAL, DEDENT, EOF_TOKEN);
  }

  @Test
  public void simpleWithNLs() {
    TokenChecker
      .of("hello")
      .nl("world")
      .nl("  tab1")
      .nl("  tab2")
      .raw(NORMAL, NL, NORMAL, NL, NORMAL, NL, NORMAL, EOF_TOKEN)
      .dented(NORMAL, NL, NORMAL, INDENT, NORMAL, NL, NORMAL, DEDENT, EOF_TOKEN);
  }

  @Test
  public void multipleDedents() {
    TokenChecker
      .of("hello")
      .nl("  line2")
      .nl("    line3")
      .nl("world")
      .raw(NORMAL, NL, NORMAL, NL, NORMAL, NL, NORMAL, EOF_TOKEN)
      .dented(NORMAL, INDENT, NORMAL, INDENT, NORMAL, DEDENT, DEDENT, NORMAL, EOF_TOKEN);
  }

  @Test
  public void multipleDedentsToEof() {
    TokenChecker
      .of("hello")
      .nl("  line2")
      .nl("    line3")
      .raw(NORMAL, NL, NORMAL, NL, NORMAL, EOF_TOKEN)
      .dented(NORMAL, INDENT, NORMAL, INDENT, NORMAL, DEDENT, DEDENT, EOF_TOKEN);
  }

  @Test
  public void ignoreBlankLines() {
    TokenChecker
      .of("hello")
      .nl("     ")
      .nl("")
      .nl("  dolly")
      .nl("        ")
      .nl("    ")
      .nl("")
      .nl("world")
      .raw(NORMAL, NL, NL, NL, NORMAL, NL, NL, NL, NL, NORMAL, EOF_TOKEN)
      .dented(NORMAL, INDENT, NORMAL, DEDENT, NORMAL, EOF_TOKEN);
  }

  @Test
  public void allIndented() {
    TokenChecker
      .of("    hello")
      .nl("    line2")
      .nl("       line3")
      .nl("    ")
      .raw(NORMAL, NL, NORMAL, NL, NORMAL, NL, EOF_TOKEN)
      .dented(INDENT, NORMAL, NL, NORMAL, INDENT, NORMAL, DEDENT, DEDENT, EOF_TOKEN);
  }

  @Test
  public void startIndentedThenEmptyLines() {
    TokenChecker
      .of("    hello")
      .nl("    line2")
      .nl("")
      .raw(NORMAL, NL, NORMAL, NL, EOF_TOKEN)
      .dented(INDENT, NORMAL, NL, NORMAL, DEDENT, EOF_TOKEN);
  }

  @Test
  public void dedentToNegative() {
    // this shouldn't explode, it should just result in an extra dedent
    TokenChecker
      .of("    hello")
      .nl("    world")
      .nl("boom")
      .raw(NORMAL, NL, NORMAL, NL, NORMAL, EOF_TOKEN)
      .dented(INDENT, NORMAL, NL, NORMAL, DEDENT, NORMAL, EOF_TOKEN);
  }

  @Test
  public void halfDent() {
    TokenChecker
      .of("hello")
      .nl("     world")
      .nl("  boom")
      .raw(NORMAL, NL, NORMAL, NL, NORMAL, EOF_TOKEN)
      .dented(NORMAL, INDENT, NORMAL, DEDENT, INDENT, NORMAL, DEDENT, EOF_TOKEN);
  }

  @Test
  public void withReturn() {
    TokenChecker
      .of("hello")
      .nl("world")
      .rf("dolly")
      .raw(NORMAL, NL, NORMAL, NL, NORMAL, EOF_TOKEN)
      .dented(NORMAL, NL, NORMAL, NL, NORMAL, EOF_TOKEN);
  }

  private interface TokenBuilder {
    TokenBuilder nl(String line);
    TokenBuilder rf(String line);
    DentChecker raw(TokenType... expected);
  }

  private interface DentChecker {
    void dented(TokenType... expected);
  }

  private static class TokenChecker implements TokenBuilder, DentChecker {
    private int lineNo;
    private final List<Token> tokens = new ArrayList<>();

    private TokenChecker() {}

    public static TokenChecker of(String firstLine) {
      TokenChecker tb = new TokenChecker();
      LineBuilder lineBuilder = new LineBuilder(0, tb.tokens);
      int leading = leadingSpacesOf(firstLine);
      lineBuilder.pos = leading;
      firstLine = firstLine.substring(leading);
      if (!firstLine.isEmpty()) {
        lineBuilder.addToken("", firstLine, NORMAL);
      }
      return tb;
    }

    @Override
    public TokenBuilder nl(String line) {
      tokenize("\n", line);
      return this;
    }

    @Override
    public TokenBuilder rf(String line) {
      tokenize("\r\n", line);
      return this;
    }

    @Override
    public void dented(TokenType... expected) {
      List<Token> dented = dent(tokens);
      List<TokenType> dentedTypes = tokensToTypes(dented);
      assertEquals("dented tokens", Arrays.asList(expected), dentedTypes);
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

    private void tokenize(String nlChars, String line) {
      LineBuilder lineBuilder = new LineBuilder(++lineNo, tokens);
      int leading = leadingSpacesOf(line);
      lineBuilder.addToken(nlChars, line.substring(0, leading), NL);
      line = line.substring(leading);
      if (!line.isEmpty()) {
        lineBuilder.addToken("", line, NORMAL);
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

    @Override
    public DentChecker raw(TokenType... expected) {
      tokens.add(new CommonToken(Token.EOF, "<eof-token>"));
      List<TokenType> rawTypes = tokensToTypes(tokens);
      assertEquals("raw tokens", Arrays.asList(expected), rawTypes);
      return this;
    }

    private static class LineBuilder {
      private final int lineNo;
      private int pos = 0;
      private final List<Token> builder;

      private LineBuilder(int lineNo, List<Token> builder) {
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

  private static int leadingSpacesOf(String s) {
    for (int i = 0, len = s.length(); i < len; ++i) {
      if (s.charAt(i) != ' ') {
        return i;
      }
    }
    // no spaces in the string (including blank string)
    return s.length();
  }

}
