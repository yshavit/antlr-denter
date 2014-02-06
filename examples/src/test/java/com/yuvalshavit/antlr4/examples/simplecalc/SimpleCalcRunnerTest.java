package com.yuvalshavit.antlr4.examples.simplecalc;

import com.beust.jcommander.internal.Lists;
import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.collect.Sets;
import com.google.common.io.Resources;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.DiagnosticErrorListener;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.atn.PredictionMode;
import org.antlr.v4.runtime.misc.Nullable;
import org.antlr.v4.runtime.tree.ParseTree;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Set;

import static org.testng.Assert.assertEquals;

public final class SimpleCalcRunnerTest {
  private static final String pathBase = SimpleCalcRunnerTest.class.getPackage().getName().replace('.', '/');
  private static final String TEST_PROVIDER = "test-provider-0";

  @DataProvider(name = TEST_PROVIDER)
  public Object[][] readParseFiles() {
    Set<String> files = Sets.newHashSet(readFile("."));
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

    CharStream input = new ANTLRInputStream(testCase.source);
    Lexer lexer = new SimpleCalcLexer(input);
    lexer.addErrorListener(new AntlrFailureListener());
    TokenStream tokens = new CommonTokenStream(lexer);

    SimpleCalcParser parser = new SimpleCalcParser(tokens);
    parser.getInterpreter().setPredictionMode(PredictionMode.LL_EXACT_AMBIG_DETECTION);
    parser.removeErrorListeners(); // don't spit to stderr
    parser.addErrorListener(new DiagnosticErrorListener());
    parser.addErrorListener(new AntlrFailureListener());

    ParseTree tree = parser.expr();

    Integer actual = new SimpleCalcRunner().visit(tree);
    assertEquals(actual, Integer.valueOf(testCase.expectedResult));
  }

  private List<String> readFile(String relativePath) {
    try {
      return Resources.readLines(url(relativePath), Charsets.UTF_8);
    } catch (IOException e) {
      throw new AssertionError(e);
    }
  }

  private Case readCase(String fileName) {
    List<String> lines = readFile(fileName);
    int expected = Integer.parseInt(lines.get(0));
    String rest = Joiner.on('\n').join(lines.subList(1, lines.size()));
    return new Case(expected, rest);
  }

  private URL url(String fileName) {
    return Resources.getResource(pathBase + "/" + fileName);
  }

  private static class Case {
    private final int expectedResult;
    private final String source;

    private Case(int expectedResult, String source) {
      this.expectedResult = expectedResult;
      this.source = source;
    }
  }

  private static class AntlrFailureListener extends BaseErrorListener {
    @Override
    public void syntaxError(Recognizer<?, ?> recognizer, @Nullable Object offendingSymbol, int line,
                            int charPositionInLine, String msg, @Nullable RecognitionException e) {
      throw new AntlrParseException(line, charPositionInLine, msg, e);
    }
  }

  private static class AntlrParseException extends RuntimeException {
    public AntlrParseException(int line, int posInLine, String msg, Throwable cause) {
      // posInLine comes in 0-indexed, but we want to 1-index it so it lines up with what editors say (they
      // tend to 1-index)
      super(String.format("at line %d column %d: %s", line, posInLine+1, msg), cause);
    }
  }
}
