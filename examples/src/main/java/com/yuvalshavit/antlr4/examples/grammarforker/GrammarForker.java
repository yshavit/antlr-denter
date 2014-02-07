package com.yuvalshavit.antlr4.examples.grammarforker;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import com.yuvalshavit.antlr4.examples.util.ResourcesReader;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

public final class GrammarForker {
  private static final ResourcesReader resources = new ResourcesReader(GrammarForker.class);

  public static void main(String[] ignored) throws Exception {
    GrammarForker forker = new GrammarForker();
    BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

    String templateFilePath = null;
    String destPath = null;
    while (true) {
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
    String grammarNameBase = templateFile.getName();

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
      this.tokens = resources.readFileToString(tokensFile);
    }

    public String fork(String grammarName, String template) {
      String h = header.replace("${GRAMMAR_NAME}", grammarName);
      String templateWithTokens = template.replace("// ${FORKER_TOKENS}", tokens);
      return h + "\n" + templateWithTokens;
    }
  }
}
