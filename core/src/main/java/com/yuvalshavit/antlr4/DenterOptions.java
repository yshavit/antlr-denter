package com.yuvalshavit.antlr4;

public interface DenterOptions {
  /**
   * Don't do any special handling for EOFs; they'll just be passed through normally. That is, we won't unwind indents
   * or add an extra NL.
   *
   * This is useful when the lexer will be used to parse rules that are within a line, such as expressions. One use
   * case for that might be unit tests that want to exercise these sort of "line fragments".
   */
  void ignoreEOF();
}
