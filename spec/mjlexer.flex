package rs.ac.bg.etf.pp1;

import java_cup.runtime.Symbol;

%%

%{
    // ukljucivanje informacija o poziciji tokena na osnovu tipa
    private Symbol newSymbol(int type) {
        return new Symbol(type, yyline + 1, yycolumn);
    }

    // ukljucivanje informacija o poziciji tokena na osnovu tipa i vrednosti
    private Symbol newSymbol(int type, Object value) {
        return new Symbol(type, yyline + 1, yycolumn, value);
    }
%}

%cup
%line
%column

%xstate COMMENT

%eofval{
    return newSymbol(sym.EOF);
%eofval}

%%

" "    { }
"\b"   { }
"\t"   { }
"\r\n" { }
"\f"   { }

"program"        { return newSymbol(sym.PROG,          yytext()); }
"break"          { return newSymbol(sym.BREAK,         yytext()); }
"class"          { return newSymbol(sym.CLASS,         yytext()); }
"else"           { return newSymbol(sym.ELSE,          yytext()); }
"const"          { return newSymbol(sym.CONST,         yytext()); }
"if"             { return newSymbol(sym.IF,            yytext()); }
"while"          { return newSymbol(sym.WHILE,         yytext()); }
"new"            { return newSymbol(sym.NEW,           yytext()); }
"print"          { return newSymbol(sym.PRINT,         yytext()); }
"read"           { return newSymbol(sym.READ,          yytext()); }
"return"         { return newSymbol(sym.RETURN,        yytext()); }
"void"           { return newSymbol(sym.VOID,          yytext()); }
"extends"        { return newSymbol(sym.EXTENDS,       yytext()); }
"continue"       { return newSymbol(sym.CONTINUE,      yytext()); }

"true"           { return newSymbol(sym.BOOL,              true); }
"false"          { return newSymbol(sym.BOOL,             false); }

"+"              { return newSymbol(sym.PLUS,          yytext()); }
"-"              { return newSymbol(sym.MINUS,         yytext()); }
"*"              { return newSymbol(sym.MUL,           yytext()); }
"/"              { return newSymbol(sym.DIV,           yytext()); }
"%"              { return newSymbol(sym.MOD,           yytext()); }
"=="             { return newSymbol(sym.EQUAL,         yytext()); }
"!="             { return newSymbol(sym.NOT_EQUAL,     yytext()); }
">"              { return newSymbol(sym.GREATER,       yytext()); }
">="             { return newSymbol(sym.GREATER_EQUAL, yytext()); }
"<"              { return newSymbol(sym.LESS,          yytext()); }
"<="             { return newSymbol(sym.LESS_EQUAL,    yytext()); }
"&&"             { return newSymbol(sym.AND,           yytext()); }
"||"             { return newSymbol(sym.OR,            yytext()); }
"="              { return newSymbol(sym.ASSIGN,        yytext()); }
"++"             { return newSymbol(sym.INC,           yytext()); }
"--"             { return newSymbol(sym.DEC,           yytext()); }
";"              { return newSymbol(sym.SEMI,          yytext()); }
":"              { return newSymbol(sym.COLON,         yytext()); }
","              { return newSymbol(sym.COMMA,         yytext()); }
"."              { return newSymbol(sym.DOT,           yytext()); }
"("              { return newSymbol(sym.LPAREN,        yytext()); }
")"              { return newSymbol(sym.RPAREN,        yytext()); }
"["              { return newSymbol(sym.LBRACKET,      yytext()); }
"]"              { return newSymbol(sym.RBRACKET,      yytext()); }
"{"              { return newSymbol(sym.LBRACE,        yytext()); }
"}"              { return newSymbol(sym.RBRACE,        yytext()); }
"=>"             { return newSymbol(sym.LAMBDA,        yytext()); }

"union"         { return newSymbol(sym.UNION,         yytext()); }
"do" 		    { return newSymbol(sym.DO,            yytext()); }



"//"             { yybegin(COMMENT);   }
<COMMENT> .      { yybegin(COMMENT);   }
<COMMENT> "\r\n" { yybegin(YYINITIAL); }

'.'                   { return newSymbol(sym.CHAR,   yytext().charAt(1));    }
[0-9]+                { return newSymbol(sym.NUMBER, new Integer(yytext())); }
[a-zA-Z][a-zA-Z0-9_]* { return newSymbol(sym.IDENT,  yytext());              }

. { System.err.println("Unexpected character: '" + yytext() + "' on line " + (yyline + 1)); }