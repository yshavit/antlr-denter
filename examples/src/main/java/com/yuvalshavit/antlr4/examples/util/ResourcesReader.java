package com.yuvalshavit.antlr4.examples.util;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;

import java.io.IOException;
import java.net.URL;
import java.util.List;

public class ResourcesReader {
  private final String pathBase;

  public ResourcesReader(Class<?> owningClass) {
    pathBase = owningClass.getPackage().getName().replace('.', '/');
  }

  public URL url(String fileName) {
    return Resources.getResource(pathBase + "/" + fileName);
  }

  public List<String> readFile(String relativePath) {
    try {
      return Resources.readLines(url(relativePath), Charsets.UTF_8);
    } catch (IOException e) {
      throw new AssertionError(e);
    }
  }

  public String readFileToString(String relativePath) {
    try {
      return Resources.toString(url(relativePath), Charsets.UTF_8);
    } catch (IOException e) {
      throw new AssertionError(e);
    }
  }
}
