grammar SimpleCalc;

tokens { INDENT, DEDENT }
@lexer::header {
  import com.yuvalshavit.antlr4.DenterHelper;
}
@lexer::members {
  private final DenterHelper denter = new DenterHelper(NL, SimpleCalcParser.INDENT, SimpleCalcParser.DEDENT) {
    @Override
    public Token pullToken() {
      return SimpleCalcLexer.super.nextToken(); // must be to super.nextToken, or we'll recurse forever!
    }
  };

  @Override
  public Token nextToken() {
    return denter.nextToken();
  }
}

expr: OP INDENT expr expr DEDENT # operation
    | INT nl                     # intLiteral
    ;

nl: NL | DEDENT;

NL: ('\r'? '\n' ' '*); // note the ' '*
WS: [ \t]+ -> skip;
LINE_COMMENT: '//' ~[\r\n]* -> skip;

INT: [0-9]+;
OP: [a-zA-Z]+;
