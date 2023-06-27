package rs.ac.bg.etf.pp1;

import rs.ac.bg.etf.pp1.ast.*;
import rs.etf.pp1.mj.runtime.Code;
import rs.etf.pp1.symboltable.concepts.Obj;

import java.io.File;
import java.nio.file.Files;
import java.util.List;
import java.util.Stack;


public class CodeGenerator extends VisitorAdaptor {
	
	public void compile(Program program) throws Exception {
		File objFile = new File("test/program.obj");
		if (objFile.exists()) objFile.delete();
		
		program.traverseBottomUp(this);
		Code.write(Files.newOutputStream(objFile.toPath()));
	}
	
	// ======================================== //
	// PRINT READ
	// ======================================== //
	
	public void visit(PrintStatementOptionalNo printStatementOptional) {
		if (printStatementOptional.getExpr().struct == Tab.charType) {
			Code.loadConst(1);
			Code.put(Code.bprint);
		} else {
			Code.loadConst(5);
			Code.put(Code.print);
		}
	}
	
	public void visit(PrintStatementOptionalYes printStatementOptional) {
		Code.loadConst(printStatementOptional.getWidth());
		
		int code = printStatementOptional.getExpr().struct == Tab.charType ? Code.bprint : Code.print;
		Code.put(code);
	}
	
	public void visit(ReadStmt readStmt) {
		int code = readStmt.getDesignator().obj.getType() == Tab.charType ? Code.bread : Code.read;
		
		Code.put(code);
		Code.store(readStmt.getDesignator().obj);
	}
	
	// ======================================== //
	// CONST LOAD
	// ======================================== //
	
	public void visit(ConstInt constInt) {
		System.out.println("ConstInt");
		
		Obj con = Tab.insert(Obj.Con, "$", constInt.struct);
		con.setLevel(0);
		con.setAdr(constInt.getN1());
		Code.load(con);
	}
	
	public void visit(ConstChar constChar) {
		System.out.println("ConstChar");
		
		Obj con = Tab.insert(Obj.Con, "$", constChar.struct);
		con.setLevel(0);
		con.setAdr(constChar.getC1());
		Code.load(con);
	}
	
	public void visit(ConstBool constBool) {
		System.out.println("ConstBool");
		
		Obj con = Tab.insert(Obj.Con, "$", constBool.struct);
		con.setLevel(0);
		con.setAdr(constBool.getB1() ? 1 : 0);
		Code.load(con);
	}
	
	// ======================================== //
	// METHOD CALL
	// ======================================== //
	
	public void visit(MethodTypeName methodTypeName) {
		System.out.println("MethodTypeName");
		
		if ("main".equalsIgnoreCase(methodTypeName.getName())) Code.mainPc = Code.pc;
		
		methodTypeName.obj.setAdr(Code.pc);
		
		SyntaxNode methodNode = methodTypeName.getParent();
		
		CounterVisitor.VarCounter varCnt = new CounterVisitor.VarCounter();
		methodNode.traverseTopDown(varCnt);
		
		CounterVisitor.FormParamCounter formParCnt = new CounterVisitor.FormParamCounter();
		methodNode.traverseTopDown(formParCnt);
		
		System.out.println(methodTypeName.getName() + " " + formParCnt.getCount() + " " + varCnt.getCount());
		
		Code.put(Code.enter);
		Code.put(formParCnt.getCount());
		Code.put(formParCnt.getCount() + varCnt.getCount());
	}
	
	public void visit(MethodDecl methodDecl) {
		System.out.println("MethodDecl");
		
		Code.put(Code.exit);
		Code.put(Code.return_);
	}
	
	// ======================================== //
	// DESIGNATOR STATEMENT
	// ======================================== //
	
	public void visit(DesignatorStatementInc designatorStatement) {
		System.out.println("DesignatorStatementInc");
		
		Obj designatorObj = designatorStatement.getDesignator().obj;
		
		if (designatorObj.getKind() == Obj.Elem) Code.put(Code.dup2);
		
		Code.load(designatorObj);
		Code.loadConst(1);
		Code.put(Code.add);
		Code.store(designatorObj);
	}
	
	public void visit(DesignatorStatementDec designatorStatement) {
		System.out.println("DesignatorStatementDec");
		
		Obj designatorObj = designatorStatement.getDesignator().obj;
		
		if (designatorObj.getKind() == Obj.Elem) Code.put(Code.dup2);
		
		Code.load(designatorObj);
		Code.loadConst(1);
		Code.put(Code.sub);
		Code.store(designatorObj);
	}
	
	public void visit(DesignatorStatementAssign designatorStatement) {
		System.out.println("DesignatorAssign");
		
		Obj designatorObj = designatorStatement.getDesignator().obj;
		
		if (designatorObj.getKind() == Obj.Elem) Code.put(Code.dup2);
		
		Code.store(designatorObj);
	}
	
	public void visit(DesignatorVar designator) {
		System.out.println("DesignatorVar");
		
		SyntaxNode parent = designator.getParent();
		
		if (parent instanceof DesignatorStatementInc
				|| parent instanceof DesignatorStatementDec
				|| parent instanceof DesignatorStatementAssign
				|| parent instanceof DesignatorStatementFunc) {
			return;
		}
		
		Code.load(designator.obj);
	}
	
	public void visit(DesignatorArray designator) {
		System.out.println("DesignatorArray");
		
		SyntaxNode parent = designator.getParent();
		
		if (parent instanceof DesignatorStatementInc
				|| parent instanceof DesignatorStatementDec
				|| parent instanceof DesignatorStatementAssign
				|| parent instanceof DesignatorStatementFunc) {
			return;
		}
		
		Code.load(designator.obj);
	}
	
	public void visit(DesignatorStatementFunc designatorStatement) { // TODO vrv ne radi
		System.out.println("DesignatorFunc");
		
		Obj functionObj = designatorStatement.getDesignator().obj;
		
		int offset = functionObj.getAdr() - Code.pc;
		Code.put(Code.call);
		Code.put2(offset);
		
		if (functionObj.getType() != Tab.noType) Code.put(Code.pop);
	}
	
	// ======================================== //
	// FACTOR
	// ======================================== //
	
	public void visit(FactorFuncCall designatorStatement) {
		System.out.println("FactorFunc");
		
		Obj functionObj = designatorStatement.getDesignator().obj;
		
		int offset = functionObj.getAdr() - Code.pc;
		Code.put(Code.call);
		Code.put2(offset);
	}
	
	public void visit(ReturnStmt statement) {
		System.out.println("ReturnStmt");
		
		Code.put(Code.exit);
		Code.put(Code.return_);
	}
	
	public void visit(FactorArray factor) {
		System.out.println("FactorArray");
		
		Code.put(Code.newarray);
		Code.put(factor.getExpr().struct.getElemType() == Tab.charType ? 0 : 1);
	}
	
	// ======================================== //
	// EXPRESSION TERM
	// ======================================== //
	
	public void visit(ExprAdd expr) {
		System.out.println("ExprAdd");
		
		SyntaxNode operation = expr.getAddOp();
		
		int code = operation instanceof PlusAO ? Code.add : Code.sub;
		Code.put(code);
	}
	
	public void visit(ExprMinus expr) {
		System.out.println("ExprMinus");
		
		Code.put(Code.neg);
	}
	
	public void visit(TermMul term) {
		System.out.println("TermMul");
		
		SyntaxNode operation = term.getMulOp();
		
		int code = operation instanceof MulMO ? Code.mul : operation instanceof DivMO ? Code.div : Code.rem;
		Code.put(code);
	}
	
	// ======================================== //
	// CONDITION
	// ======================================== //
	
	public void visit(CondFactRel condFact) {
		System.out.println("CondFactRel");
		
		SyntaxNode operation = condFact.getRelOp();
		
		int code;
		
		if (operation instanceof EqualRO) code = Code.eq;
		else if (operation instanceof NotEqualRO) code = Code.ne;
		else if (operation instanceof GreaterRO) code = Code.gt;
		else if (operation instanceof GreaterEqualRO) code = Code.ge;
		else if (operation instanceof LessRO) code = Code.lt;
		else code = Code.le;
		
		Code.putFalseJump(code, 0);
	}
	
	// ======================================== //
	// IF STATEMENT
	// ======================================== //
	
	Stack<List<Integer>> OrStack = new Stack<>();
	Stack<List<Integer>> AndStack = new Stack<>();
	Stack<List<Integer>> CondStack = new Stack<>();
	
	
	
}
