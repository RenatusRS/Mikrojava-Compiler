package rs.ac.bg.etf.pp1;

import rs.ac.bg.etf.pp1.ast.*;

public class CounterVisitor extends VisitorAdaptor {
	
	protected int count;
	
	public int getCount() {
		return count;
	}
	
	public static class FormParamCounter extends CounterVisitor {
		
		public void visit(FormParVar formPar) {
			count++;
		}
		
		public void visit(FormParArray formPar) {
			count++;
		}
		
		public void visit(FormParMatrix formPar) {
			count++;
		}
		
		
	}
	
	public static class VarCounter extends CounterVisitor {
		
		public void visit(VarDecl varDecl) {
			count++;
		}
		
	}
	
	public static class ConstCounter extends CounterVisitor {
		
		public void visit(ConstDecl constDecl) {
			count++;
		}
		
	}
	
}
