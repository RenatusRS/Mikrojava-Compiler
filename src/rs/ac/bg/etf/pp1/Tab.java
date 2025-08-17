package rs.ac.bg.etf.pp1;

import rs.ac.bg.etf.pp1.ast.SyntaxNode;
import rs.ac.bg.etf.pp1.util.Analyzer;
import rs.etf.pp1.symboltable.concepts.Obj;
import rs.etf.pp1.symboltable.concepts.Struct;

public class Tab extends rs.etf.pp1.symboltable.Tab {
	public static final Struct boolType = new Struct(Struct.Bool);
	public static final Struct setType = new Struct(Struct.Enum);
	private static final Analyzer analyzer = Analyzer.getInstance(MJParser.class);
	
	public static void init() {
		if (currentScope != null) return;
		
		rs.etf.pp1.symboltable.Tab.init();
		currentScope.addToLocals(new Obj(Obj.Type, "bool", boolType));
		currentScope.addToLocals(new Obj(Obj.Type, "set", setType));
	}
	
	public static void closeScope(SyntaxNode node) {
		rs.etf.pp1.symboltable.Tab.closeScope();
		analyzer.report_info(node, "Scope closed");
	}
	
	public static void openScope(SyntaxNode node) {
		rs.etf.pp1.symboltable.Tab.openScope();
		analyzer.report_info(node, "Scope opened");
	}
}
