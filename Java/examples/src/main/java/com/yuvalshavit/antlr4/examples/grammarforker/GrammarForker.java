package com.yuvalshavit.antlr4.examples.grammarforker;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import com.yuvalshavit.antlr4.examples.util.ResourcesReader;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.Token;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

public final class GrammarForker {
  public static final String INDENT_BRACE = " {";
  public static final String DEDENT_BRACE = "}";
  private static final ResourcesReader resources = new ResourcesReader(GrammarForker.class);

  public static String dentedToBraced(Lexer lexer, int indent, int dedent, int nl, String nlReplacement) {
    StringBuilder sb = new StringBuilder();
    int indentation = 0;
    int charsWrittenInLine = 0;
    for (Token t = lexer.nextToken(); t.getType() != Lexer.EOF; t = lexer.nextToken()) {
      int expectCharsWritten = t.getCharPositionInLine();
      int tokenType = t.getType();
      if (tokenType == indent) {
        sb.append(INDENT_BRACE).append('\n');
        ++indentation;
        charsWrittenInLine = 0;
      } else if (tokenType == dedent) {
        --indentation;
        addSpaces(sb, indentation * 2);
        sb.append(DEDENT_BRACE).append('\n');
        charsWrittenInLine = 0;
      } else if (tokenType == nl) {
        sb.append(nlReplacement);
        charsWrittenInLine = 0;
      } else {
        String text = t.getText();
        if (charsWrittenInLine < expectCharsWritten) {
          if ("else".equals(text)) {
            // Instead of a newline (which we previously added), just add a space. Also, note that the
            // charsWrittenInLine happens unconditionally. This is intentional, to trick the indent-inserter into
            // thinking it's already done its job. The net effect is to put else's on the same line as the if's
            // closing brace, which is a standard style.
            chomp(sb, '\n');
            sb.append(' ');
          } else {
            addSpaces(sb, expectCharsWritten - charsWrittenInLine);
          }
          charsWrittenInLine = expectCharsWritten;
        }
        sb.append(text);
        charsWrittenInLine += text.length();
      }
    }
    return sb.toString();
  }

  private static void chomp(StringBuilder sb, char chompWhat) {
    if (sb.charAt(sb.length() - 1) == chompWhat) {
      sb.setLength(sb.length() - 1);
    }
  }

  private static void addSpaces(StringBuilder sb, int indentation) {
    for (int i = 0; i < indentation; ++i) {
      sb.append(' ');
    }
  }

  public static void main(String[] ignored) throws Exception {
    GrammarForker forker = new GrammarForker();
    BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

    String templateFilePath = null;
    String destPath = null;
    while (true) {
      System.out.printf("pwd: %s%n", new File(".").getAbsoluteFile().getParent());
      templateFilePath = prompt(reader, "template file", templateFilePath);
      if (templateFilePath == null) {
        break;
      }
      if (destPath == null) {
        destPath = new File(templateFilePath).getParent();
      }
      destPath = prompt(reader, "destination dir", destPath);
      if (destPath == null) {
        break;
      }
      try {
        forker.fork(templateFilePath, destPath);
      } catch (Exception e) {
        e.printStackTrace();
        Thread.sleep(10); // hacky, but it works!
      }
      System.out.println();
    }
  }

  private static String prompt(BufferedReader reader, String prompt, String defaultValue) throws IOException {
    System.out.print(prompt);
    if (defaultValue != null) {
      System.out.printf(" [%s]", defaultValue);
    }
    System.out.print(": ");
    String result = reader.readLine();
    if (result == null) {
      return null;
    }
    result = result.trim();
    if (result.isEmpty()) {
      if (defaultValue != null) {
        return defaultValue;
      } else {
        System.out.println("Need a non-empty value.");
        return prompt(reader, prompt, defaultValue);
      }
    } else {
      return result;
    }
  }

  private static final Forker bracedForker = new Forker(
    "grammar-forker-braced-header.txt",
    "grammar-forker-braced-tokens.txt");
  private static final Forker dentingForker = new Forker(
    "grammar-forker-denting-header.txt",
    "grammar-forker-denting-tokens.txt");

  void fork(String templateFilePath, String destPath) throws IOException {
    File templateFile = new File(templateFilePath);
    File destDir = new File(destPath);
    if (!templateFile.isFile()) {
      throw new IllegalArgumentException("not a file: " + templateFilePath);
    }
    if (!destDir.isDirectory()) {
      throw new IllegalArgumentException("not a directory: " + destPath);
    }

    String template = Files.toString(templateFile, Charsets.UTF_8);
    String grammarNameBase = templateFile.getName()
      .replaceFirst("[^-]+-", "") // "foo-Bar" becomes "Bar"
      .replaceFirst("\\.g4$", ""); // remove '.g4' extension


    writeGrammarFile(template, dentingForker, grammarNameBase + "Denting", destPath);
    writeGrammarFile(template, bracedForker, grammarNameBase + "Braced", destPath);
  }

  private void writeGrammarFile(String template, Forker forker, String grammarName,
                                String destPath) throws IOException {
    String denting = forker.fork(grammarName, template);
    File to = new File(destPath, grammarName + ".g4");
    System.out.printf("Writing to file %s...", to);
    Files.write(denting, to, Charsets.UTF_8);
    System.out.println("OK.");
  }

  private static class Forker {
    private final String header;
    private final String tokens;

    private Forker(String headerFile, String tokensFile) {
      this.header = resources.readFileToString(headerFile);
      this.tokens = resources.readFileToString(tokensFile)
        .replace("${INDENT_BRACE}", INDENT_BRACE)
        .replace("${DEDENT_BRACE}", DEDENT_BRACE);
    }

    public String fork(String grammarName, String template) {
      String h = header.replace("${GRAMMAR_NAME}", grammarName);
      String templateWithTokens = template.replace("// ${FORKER_TOKENS}", tokens);
      return h + "\n" + templateWithTokens;
    }
  }
}
