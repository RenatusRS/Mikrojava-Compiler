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

"program" { return newSymbol(sym.PROG,   yytext()); }
"print"   { return newSymbol(sym.PRINT,  yytext()); }
"return"  { return newSymbol(sym.RETURN, yytext()); }
"void"    { return newSymbol(sym.VOID,   yytext()); }
"+"       { return newSymbol(sym.PLUS,   yytext()); }
"="       { return newSymbol(sym.EQUAL,  yytext()); }
";"       { return newSymbol(sym.SEMI,   yytext()); }
","       { return newSymbol(sym.COMMA,  yytext()); }
"("       { return newSymbol(sym.LPAREN, yytext()); }
")"       { return newSymbol(sym.RPAREN, yytext()); }
"{"       { return newSymbol(sym.LBRACE, yytext()); }
"}"       { return newSymbol(sym.RBRACE, yytext()); }

"//"             { yybegin(COMMENT);   }
<COMMENT> .      { yybegin(COMMENT);   }
<COMMENT> "\r\n" { yybegin(YYINITIAL); }

[0-9]+                { return newSymbol(sym.NUMBER, new Integer(yytext())); }
[a-zA-Z][a-zA-Z0-9_]* { return newSymbol(sym.IDENT,  yytext());              }

. { System.err.println("Unexpected character: '" + yytext() + "' on line " + (yyline + 1)); }