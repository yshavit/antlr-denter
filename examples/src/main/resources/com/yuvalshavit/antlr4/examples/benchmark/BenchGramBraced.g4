grammar BenchGramBraced;


block: INDENT stat (NL stat)* DEDENT;

stat: assign
    | ifBlock
    | whileBlock
    | anonBlock
    ;

expr: ID
    | INT
    | STRING
    | funcCall
    | expr ('*'|'/') expr
    | expr ('+'|'-') expr
    | '(' expr ')'
    ;

funcCall: ID '(' argsList? ')';

argsList: expr (',' expr)*;

assign: ID '==' expr NL;

ifBlock: 'if' expr 'then' block elseIfBlock?;
elseIfBlock: 'else if' expr 'then' block elseBlock?;
elseBlock: 'else' block;

whileBlock: 'while' expr block;

anonBlock: 'scoped' block;

INDENT: '▶';
DEDENT: '◀';
NL:     '\r'? '\n';


INT: [0-9]+;
STRING: '"' CHAR*? '"';
fragment CHAR: '\\\\' | '\\\"' | ~["];
ID: [a-zA-Z]+;
WS: [ \t] -> skip;
