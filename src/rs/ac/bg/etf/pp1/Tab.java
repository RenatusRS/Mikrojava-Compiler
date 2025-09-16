package rs.ac.bg.etf.pp1;

import rs.ac.bg.etf.pp1.ast.SyntaxNode;
import rs.ac.bg.etf.pp1.util.Analyzer;
import rs.etf.pp1.symboltable.concepts.Obj;
import rs.etf.pp1.symboltable.concepts.Struct;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Tab extends rs.etf.pp1.symboltable.Tab {
	public static final Struct boolType = new Struct(Struct.Bool);
	public static final Struct setType = new Struct(Struct.Enum);
	private static final Analyzer analyzer = Analyzer.getInstance();
	private static int tempAdr = 100;
	
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
	
	private static final Map<Struct, List<Obj>> objCache = new HashMap<>();
	
	public static Obj insertTemp(Struct type) {
		List<Obj> cached = objCache.get(type);
		Obj temp;
		
		if (cached != null && !cached.isEmpty()) {
			temp = cached.remove(cached.size() - 1);
			analyzer.report_info(null, "Reused temporary variable: " + temp.getName() + " at address " + temp.getAdr());
		} else {
			int adr = tempAdr;
			tempAdr += 2;
			temp = Tab.insert(Obj.Var, "$" + adr, type);
			temp.setLevel(1);
			temp.setAdr(adr);
			analyzer.report_info(null, "New temporary variable inserted: " + temp.getName() + " at address " + temp.getAdr());
		}
		
		return temp;
	}
	
	public static void free(Obj obj) {
		objCache.computeIfAbsent(obj.getType(), k -> new ArrayList<>()).add(obj);
		analyzer.report_info(null, "Temporary variable freed: " + obj.getName());
	}
}
