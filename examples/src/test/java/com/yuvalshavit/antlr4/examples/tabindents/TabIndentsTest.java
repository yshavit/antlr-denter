package com.yuvalshavit.antlr4.examples.tabindents;

import com.beust.jcommander.internal.Lists;
import com.google.common.primitives.Ints;
import com.yuvalshavit.antlr4.examples.util.ParserUtils;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.List;

import static org.testng.Assert.assertEquals;

public final class TabIndentsTest {

  @Test
  public void tabbedGrammar() {
    String content = "{\n\tline1\n\tline2\n}";
    List<String> expected = Arrays.asList("line1", "line2");

    TabIndentsParser parser = ParserUtils.getParser(TabIndentsLexer.class, TabIndentsParser.class, content);
    TabIndentsParser.ExprContext expr = parser.expr();
    List<String> actual = Lists.newArrayList();
    for (TerminalNode numNode : expr.WORD()) {
      actual.add(numNode.getText());
    }
    assertEquals(actual, expected);
  }

  @Test(expectedExceptions = ParserUtils.AntlrParseException.class,
        expectedExceptionsMessageRegExp = "at line 1 column 2: mismatched input '\\\\n' expecting INDENT")
  public void tabbedGrammarInputUsesSpaces() {
    String content = "{\n    line1\n    line2\n}";
    TabIndentsParser parser = ParserUtils.getParser(TabIndentsLexer.class, TabIndentsParser.class, content);
    parser.expr();
  }
}
