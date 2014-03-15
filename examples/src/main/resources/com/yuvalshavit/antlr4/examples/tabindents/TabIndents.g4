grammar TabIndents;

tokens { INDENT, DEDENT }
@lexer::header {
  import com.yuvalshavit.antlr4.DenterHelper;
}
@lexer::members {
  private final DenterHelper denter = new DenterHelper(NL, TabIndentsParser.INDENT, TabIndentsParser.DEDENT) {
    @Override
    public Token pullToken() {
      return TabIndentsLexer.super.nextToken();
    }
  };

  @Override
  public Token nextToken() {
    return denter.nextToken();
  }
}

expr: OPEN INDENT (WORD NL)+ DEDENT CLOSE
    ;

NL: ('\r'?'\n''\t'*); // note the '\t'*
WS: ' '+ -> skip;

WORD: [a-zA-Z0-9]+;
OPEN: '{';
CLOSE: '}';
