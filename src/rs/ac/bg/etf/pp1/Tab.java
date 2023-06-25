package rs.ac.bg.etf.pp1;

import rs.etf.pp1.symboltable.concepts.Obj;
import rs.etf.pp1.symboltable.concepts.Struct;

public class Tab extends rs.etf.pp1.symboltable.Tab {
	public static final Struct boolType = new Struct(Struct.Bool);
	
	public static void init() {
		if (currentScope != null) return;
		
		rs.etf.pp1.symboltable.Tab.init();
		currentScope.addToLocals(new Obj(Obj.Type, "bool", boolType));
	}
}
