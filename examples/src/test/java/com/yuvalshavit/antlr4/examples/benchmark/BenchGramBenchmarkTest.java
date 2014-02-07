package com.yuvalshavit.antlr4.examples.benchmark;

import com.beust.jcommander.internal.Lists;
import com.google.common.collect.Sets;
import com.yuvalshavit.antlr4.examples.grammarforker.GrammarForker;
import com.yuvalshavit.antlr4.examples.util.ParserUtils;
import com.yuvalshavit.antlr4.examples.util.ResourcesReader;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Set;

public class BenchGramBenchmarkTest {
  private static final ResourcesReader resources = new ResourcesReader(BenchGramBenchmarkTest.class);
  private static final String TEST_PROVIDER = "test-provider-0";

  @DataProvider(name = TEST_PROVIDER)
  public Object[][] readParseFiles() {
    Set<String> files = Sets.newHashSet(resources.readFile("."));
    List<Object[]> parseTests = Lists.newArrayList();
    for (String fileName : files) {
      if (fileName.endsWith(".benchdent")) {
        parseTests.add(new Object[] { fileName });
      }
    }
    return parseTests.toArray(new Object[parseTests.size()][]);
  }

  @Test(dataProvider = TEST_PROVIDER)
  public void checkExpr(String fileName) {
    String dentedProgram = resources.readFileToString(fileName);
    BenchGramDentingLexer lexer = ParserUtils.getLexer(BenchGramDentingLexer.class, dentedProgram);
    String braced = GrammarForker.dentedToBraced(
      lexer,
      BenchGramDentingParser.INDENT,
      BenchGramDentingParser.DEDENT,
      BenchGramDentingParser.NL,
      ";");
    System.out.println(braced);
  }
}
