package com.yuvalshavit.antlr4.examples.simplecalc;

import com.beust.jcommander.internal.Lists;
import com.google.common.base.Joiner;
import com.google.common.collect.Sets;
import com.yuvalshavit.antlr4.examples.util.ParserUtils;
import com.yuvalshavit.antlr4.examples.util.ResourcesReader;
import org.antlr.v4.runtime.tree.ParseTree;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Set;

import static org.testng.Assert.assertEquals;

public final class SimpleCalcRunnerTest {
  private static final ResourcesReader resources = new ResourcesReader(SimpleCalcRunnerTest.class);
  private static final String TEST_PROVIDER = "test-provider-0";

  @DataProvider(name = TEST_PROVIDER)
  public Object[][] readParseFiles() {
    Set<String> files = Sets.newHashSet(resources.readFile("."));
    List<Object[]> parseTests = Lists.newArrayList();
    for (String fileName : files) {
      if (fileName.endsWith(".simplecalc")) {
        parseTests.add(new Object[] { fileName });
      }
    }
    return parseTests.toArray(new Object[parseTests.size()][]);
  }

  @Test(dataProvider = TEST_PROVIDER)
  public void checkExpr(String fileName) {
    Case testCase = readCase(fileName);

    SimpleCalcParser parser = ParserUtils.getParser(SimpleCalcLexer.class, SimpleCalcParser.class, testCase.source);

    ParseTree tree = parser.expr();

    Integer actual = new SimpleCalcRunner().visit(tree);
    assertEquals(actual, Integer.valueOf(testCase.expectedResult));
  }

  private Case readCase(String fileName) {
    List<String> lines = resources.readFile(fileName);
    int expected = Integer.parseInt(lines.get(0));
    String rest = Joiner.on('\n').join(lines.subList(1, lines.size()));
    return new Case(expected, rest);
  }

  private static class Case {
    private final int expectedResult;
    private final String source;

    private Case(int expectedResult, String source) {
      this.expectedResult = expectedResult;
      this.source = source;
    }
  }
}
