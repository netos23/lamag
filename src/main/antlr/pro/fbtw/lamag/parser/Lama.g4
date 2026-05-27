grammar Lama;

program
    : importDecl* scopeBody EOF
    ;

importDecl
    : IMPORT UIDENT (COMMA UIDENT)* SEMI
    ;

scopeBody
    : declaration* sequence?
    ;

declaration
    : varDecl
    | funDecl
    | infixDecl
    ;

modifier
    : PUBLIC EXTERNAL?
    | EXTERNAL
    ;

varDecl
    : modifier? VAR varInit (COMMA varInit)* SEMI
    ;

varInit
    : LIDENT (EQ expression)?
    ;

funDecl
    : modifier? FUN LIDENT LPAREN patternList? RPAREN (block | SEMI)
    ;

infixDecl
    : PUBLIC? (INFIX_KW | INFIXL | INFIXR) INFIX position? LPAREN patternList? RPAREN block
    ;

position
    : (AT | BEFORE | AFTER) INFIX
    ;

block
    : LBRACE scopeBody RBRACE
    ;

sequence
    : expression (SEMI expression?)*
    ;

expression
    : letExpr
    | assignment
    ;

letExpr
    : LET pattern EQ expression IN expression
    ;

assignment
    : cons (ASSIGN assignment)?
    ;

cons
    : logicOr (COLON cons)?
    ;

logicOr
    : logicAnd (OR logicAnd)*
    ;

logicAnd
    : equality (AND equality)*
    ;

equality
    : comparison ((EQ | EQEQ | NE) comparison)*
    ;

comparison
    : additive ((LE | LT | GE | GT) additive)*
    ;

additive
    : multiplicative ((PLUS | MINUS) multiplicative)*
    ;

multiplicative
    : unary ((MUL | DIV | MOD) unary)*
    ;

unary
    : MINUS unary
    | postfix
    ;

postfix
    : primary postfixPart*
    ;

postfixPart
    : DOT LIDENT (LPAREN argList? RPAREN)?
    | LBRACK expression RBRACK
    | LPAREN argList? RPAREN
    ;

primary
    : DECIMAL
    | STRING
    | CHAR
    | TRUE
    | FALSE
    | LIDENT
    | UIDENT (LPAREN argList? RPAREN)?
    | lambdaExpr
    | arrayLiteral
    | listLiteral
    | ifExpr
    | whileExpr
    | forExpr
    | doWhileExpr
    | caseExpr
    | SKIP_KW
    | LPAREN scopeBody RPAREN
    ;

lambdaExpr
    : FUN LPAREN patternList? RPAREN block
    ;

arrayLiteral
    : LBRACK argList? RBRACK
    ;

listLiteral
    : LBRACE argList? RBRACE
    ;

ifExpr
    : IF expression THEN thenBranch=scopeBody elifBranch* (ELSE elseBranch=scopeBody)? FI
    ;

elifBranch
    : ELIF expression THEN scopeBody
    ;

whileExpr
    : WHILE expression DO scopeBody OD
    ;

forExpr
    : FOR init=scopeBody COMMA condition=expression COMMA step=expression DO body=scopeBody OD
    ;

doWhileExpr
    : DO scopeBody WHILE expression OD
    ;

caseExpr
    : CASE expression OF caseBranch (BAR caseBranch)* ESAC
    ;

caseBranch
    : pattern ARROW scopeBody
    ;

argList
    : expression (COMMA expression)*
    ;

patternList
    : pattern (COMMA pattern)*
    ;

pattern
    : patternPrimary (COLON pattern)?
    ;

patternPrimary
    : UNDERSCORE
    | UIDENT (LPAREN patternList? RPAREN)?
    | LBRACK patternList? RBRACK
    | LBRACE patternList? RBRACE
    | LIDENT (AT_SIGN pattern)?
    | MINUS? DECIMAL
    | STRING
    | CHAR
    | TRUE
    | FALSE
    | HASH (BOX | VAL | STR | SEXP | ARRAY | FUN)
    | LPAREN pattern RPAREN
    ;

IMPORT: 'import';
PUBLIC: 'public';
EXTERNAL: 'external';
VAR: 'var';
FUN: 'fun';
INFIX_KW: 'infix';
INFIXL: 'infixl';
INFIXR: 'infixr';
AT: 'at';
BEFORE: 'before';
AFTER: 'after';
LET: 'let';
IN: 'in';
IF: 'if';
THEN: 'then';
ELIF: 'elif';
ELSE: 'else';
FI: 'fi';
WHILE: 'while';
DO: 'do';
OD: 'od';
FOR: 'for';
CASE: 'case';
OF: 'of';
ESAC: 'esac';
SKIP_KW: 'skip';
TRUE: 'true';
FALSE: 'false';
BOX: 'box';
VAL: 'val';
STR: 'str';
SEXP: 'sexp';
ARRAY: 'array';

ASSIGN: ':=';
ARROW: '->';
OR: '!!';
AND: '&&';
EQEQ: '==';
NE: '!=';
LE: '<=';
GE: '>=';
EQ: '=';
LT: '<';
GT: '>';
PLUS: '+';
MINUS: '-';
MUL: '*';
DIV: '/';
MOD: '%';
COLON: ':';
DOT: '.';
COMMA: ',';
SEMI: ';';
BAR: '|';
LPAREN: '(';
RPAREN: ')';
LBRACE: '{';
RBRACE: '}';
LBRACK: '[';
RBRACK: ']';
HASH: '#';
AT_SIGN: '@';
UNDERSCORE: '_';

DECIMAL: [0-9]+;
UIDENT: [A-Z] [a-zA-Z0-9_']*;
LIDENT: [a-z] [a-zA-Z0-9_']*;
INFIX: [!$%&*+./:<=>?@\\^|~-]+;

STRING
    : '"' (ESC | ~["\\\r\n])* '"'
    ;

CHAR
    : '\'' (ESC | ~['\\\r\n]) '\''
    ;

fragment ESC
    : '\\' (["'\\nrtbf] | 'u' HEX HEX HEX HEX)
    ;

fragment HEX
    : [0-9a-fA-F]
    ;

LINE_COMMENT
    : '--' ~[\r\n]* -> skip
    ;

BLOCK_COMMENT
    : '(*' .*? '*)' -> skip
    ;

WS
    : [ \t\r\n]+ -> skip
    ;
