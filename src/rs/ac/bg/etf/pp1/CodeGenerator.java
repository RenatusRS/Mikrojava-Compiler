package rs.ac.bg.etf.pp1;

import rs.ac.bg.etf.pp1.ast.*;
import rs.ac.bg.etf.pp1.util.Analyzer;
import rs.etf.pp1.mj.runtime.Code;
import rs.etf.pp1.symboltable.concepts.Obj;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Stack;

import static rs.etf.pp1.mj.runtime.Code.eq;


public class CodeGenerator extends VisitorAdaptor {
	
	private final Analyzer analyzer = new Analyzer(MJParser.class);
	
	public void compile(Program program) throws Exception {
		File objFile = new File("test/program.obj");
		if (objFile.exists()) objFile.delete();
		
		program.traverseBottomUp(this);
		Code.write(Files.newOutputStream(objFile.toPath()));
	}
	
	public void visit(ProgName progName) {
		Tab.find("chr").setAdr(Code.pc);
		Tab.find("ord").setAdr(Code.pc);
		Code.put(Code.enter);
		
		Code.put(1);
		Code.put(1);
		
		Code.put(Code.load_n);
		Code.put(Code.exit);
		Code.put(Code.return_);
		
		Tab.find("len").setAdr(0);
		
		Code.put(Code.enter);
		
		Code.put(1);
		Code.put(1);
		
		Code.put(Code.load_n);
		Code.put(Code.arraylength);
		Code.put(Code.exit);
		Code.put(Code.return_);
	}
	
	Stack<Integer> fixUps = new Stack<>();
	
	// ======================================== //
	// PRINT READ
	// ======================================== //
	
	public void visit(PrintStatementOptionalNo printStatementOptional) {
		analyzer.report_info("PRINT NO OPTIONAL", printStatementOptional);
		
		if (printStatementOptional.getExpr().struct == Tab.charType) {
			Code.loadConst(1);
			Code.put(Code.bprint);
		} else {
			Code.loadConst(5);
			Code.put(Code.print);
		}
	}
	
	public void visit(PrintStatementOptionalYes printStatementOptional) {
		analyzer.report_info("PRINT YES OPTIONAL", printStatementOptional);
		
		Code.loadConst(printStatementOptional.getWidth());
		
		int code = printStatementOptional.getExpr().struct == Tab.charType ? Code.bprint : Code.print;
		Code.put(code);
	}
	
	public void visit(ReadStmt readStmt) {
		analyzer.report_info("READ", readStmt);
		
		int code = readStmt.getDesignator().obj.getType() == Tab.charType ? Code.bread : Code.read;
		
		Code.put(code);
		Code.store(readStmt.getDesignator().obj);
	}
	
	// ======================================== //
	// CONST LOAD
	// ======================================== //
	
	public void visit(ConstInt constInt) {
		analyzer.report_info(constInt.getN1().toString(), constInt);
		
		Obj con = Tab.insert(Obj.Con, "$", constInt.struct);
		con.setLevel(0);
		con.setAdr(constInt.getN1());
		
		Code.load(con);
	}
	
	public void visit(ConstChar constChar) {
		analyzer.report_info(constChar.getC1().toString(), constChar);
		
		Obj con = Tab.insert(Obj.Con, "$", constChar.struct);
		con.setLevel(0);
		con.setAdr(constChar.getC1());
		
		Code.load(con);
	}
	
	public void visit(ConstBool constBool) {
		analyzer.report_info(constBool.getB1() ? "True" : "False", constBool);
		
		Obj con = Tab.insert(Obj.Con, "$", constBool.struct);
		con.setLevel(0);
		con.setAdr(constBool.getB1() ? 1 : 0);
		
		Code.load(con);
	}
	
	// ======================================== //
	// METHOD CALL
	// ======================================== //
	
	public void visit(MethodTypeName methodTypeName) {
		if ("main".equalsIgnoreCase(methodTypeName.getName())) Code.mainPc = Code.pc;
		
		methodTypeName.obj.setAdr(Code.pc);
		
		SyntaxNode methodNode = methodTypeName.getParent();
		
		CounterVisitor.VarCounter varCnt = new CounterVisitor.VarCounter();
		methodNode.traverseTopDown(varCnt);
		
		CounterVisitor.FormParamCounter formParCnt = new CounterVisitor.FormParamCounter();
		methodNode.traverseTopDown(formParCnt);
		
		analyzer.report_info("BEGIN " + methodTypeName.getName() + " | PARS: " + formParCnt.getCount() + ", VARS: " + varCnt.getCount(), methodTypeName);
		
		Code.put(Code.enter);
		Code.put(formParCnt.getCount());
		Code.put(formParCnt.getCount() + varCnt.getCount());
	}
	
	public void visit(MethodDecl methodDecl) {
		analyzer.report_info("END " + methodDecl.getMethodTypeName().getName(), methodDecl);
		
		Code.put(Code.exit);
		Code.put(Code.return_);
	}
	
	// ======================================== //
	// DESIGNATOR STATEMENT
	// ======================================== //
	
	public void visit(DesignatorStatementInc designatorStatement) {
		analyzer.report_info("INC", designatorStatement);
		
		Obj designatorObj = designatorStatement.getDesignator().obj;
		
		Code.loadConst(1);
		Code.put(Code.add);
		Code.store(designatorObj);
	}
	
	public void visit(DesignatorStatementDec designatorStatement) {
		analyzer.report_info("DEC", designatorStatement);
		
		Obj designatorObj = designatorStatement.getDesignator().obj;
		
		Code.loadConst(1);
		Code.put(Code.sub);
		Code.store(designatorObj);
	}
	
	public void visit(DesignatorStatementAssign designatorStatement) {
		analyzer.report_info("ASSIGN", designatorStatement);
		
		Obj designatorObj = designatorStatement.getDesignator().obj;
		
		Code.store(designatorObj);
	}
	
	
	public void visit(DesignatorName designatorName) {
		if (designatorName.getParent() instanceof DesignatorVar) return;
		
		analyzer.report_info("DESIGNATOR LOAD", designatorName);
		
		Code.load(designatorName.obj);
	}
	
	public void visit(DesignatorVar designator) {
		SyntaxNode parent = designator.getParent();
		
		if (parent instanceof DesignatorStatementInc) {
			analyzer.report_info("VAR LOAD: Inc", designator);
			Code.load(designator.obj);
		} else if (parent instanceof DesignatorStatementDec) {
			analyzer.report_info("VAR LOAD: Dec", designator);
			Code.load(designator.obj);
		} else if (parent instanceof FactorVar) {
			analyzer.report_info("VAR LOAD: FactorVar", designator);
			Code.load(designator.obj);
		} else analyzer.report_info("VAR LOAD SKIP", designator);
	}
	
	public void visit(DesignatorArray designator) {
		if (designator.getParent() instanceof FactorVar) {
			analyzer.report_info("ARRAY LOAD: FactorVar", designator);
			Code.load(designator.obj);
		} else analyzer.report_info("ARRAY LOAD SKIP", designator);
	}
	
	public void visit(DesignatorStatementFunc designatorStatement) {
	}
	
	// ======================================== //
	// FACTOR
	// ======================================== //
	
	public void visit(FactorFuncCall designatorStatement) {
	}
	
	public void visit(ReturnStmt statement) {
		analyzer.report_info("RETURN", statement);
		
		Code.put(Code.exit);
		Code.put(Code.return_);
	}
	
	public void visit(FactorArray factor) {
		boolean isChar = factor.struct.getElemType() == Tab.charType;
		
		analyzer.report_info("NEW ARRAY: " + (isChar ? "Char" : "Not Char"), factor);
		
		Code.put(Code.newarray);
		Code.put(isChar ? 0 : 1);
	}
	
	// ======================================== //
	// EXPRESSION TERM
	// ======================================== //
	
	public void visit(ExprAdd expr) {
		SyntaxNode operation = expr.getAddOp();
		
		boolean isPlus = operation instanceof PlusAO;
		
		analyzer.report_info("ADD: " + (isPlus ? "Plus" : "Minus"), expr);
		
		int code = isPlus ? Code.add : Code.sub;
		Code.put(code);
	}
	
	public void visit(ExprMinus expr) {
		analyzer.report_info("UNARY MINUS", expr);
		
		Code.put(Code.neg);
	}
	
	public void visit(TermMul term) {
		SyntaxNode operation = term.getMulOp();
		
		int code = operation instanceof MulMO ? Code.mul : operation instanceof DivMO ? Code.div : Code.rem;
		
		String codeText = operation instanceof MulMO ? "MUL" : operation instanceof DivMO ? "DIV" : "MOD";
		
		analyzer.report_info("MUL: " + codeText, term);
		
		Code.put(code);
	}
	
	// ======================================== //
	// CONDITION
	// ======================================== //
	
	public void visit(CondFactRel condFact) {
		SyntaxNode operation = condFact.getRelOp();
		
		int code;
		
		if (operation instanceof EqualRO) code = eq;
		else if (operation instanceof NotEqualRO) code = Code.ne;
		else if (operation instanceof GreaterRO) code = Code.gt;
		else if (operation instanceof GreaterEqualRO) code = Code.ge;
		else if (operation instanceof LessRO) code = Code.lt;
		else code = Code.le;
		
		analyzer.report_info("REL: " + operation.getClass().getSimpleName(), condFact);
		
		Code.putFalseJump(code, 0);
	}
	
	// ======================================== //
	// JUMPS
	// ======================================== //
	
	public void visit(ConditionOr condition) {
		analyzer.report_info("OR", condition);
	}
	
	public void visit(IfStmt statement) {
		analyzer.report_info("IF", statement);
		
		Code.fixup(fixUps.pop());
	}
	
	public void visit(ElseStmt statement) {
		analyzer.report_info("ELSE", statement);
		
		Code.fixup(fixUps.pop());
	}
	
	public void visit(IfBase ifBase) {
		analyzer.report_info("IF BASE", ifBase);
		
		Code.loadConst(0);
		Code.putFalseJump(Code.gt, 0);
		fixUps.push(Code.pc - 2);
	}
	
	public void visit(ElseBase elseBase) {
		analyzer.report_info("ELSE BASE", elseBase);
		
		Code.putJump(0);
		Code.fixup(fixUps.pop());
		fixUps.push(Code.pc - 2);
	}
	
	// ======================================== //
	// CUSTOM FUNCTIONS
	// ======================================== //
	
	public void visit(DesignatorStatementFindAny designatorStatement) {
		analyzer.report_info("FIND ANY", designatorStatement);
		
		Obj bool = designatorStatement.getDesignator().obj;
		Obj arr = designatorStatement.getDesignator1().obj;
		
		Code.loadConst(0);
		
		int START = Code.pc;
		
		Code.put(Code.dup_x1);
		
		Code.load(arr);
		
		Code.put(Code.dup_x1);
		Code.put(Code.pop);
		
		Code.put(Code.aload);
		
		Code.put(Code.dup2);
		
		Code.putFalseJump(Code.ne, 0);
		int FOUND = Code.pc - 2;
		
		Code.put(Code.pop);
		
		Code.put(Code.dup_x1);
		
		Code.put(Code.pop);
		
		Code.loadConst(1);
		Code.put(Code.add);
		
		Code.put(Code.dup);
		
		Code.load(arr);
		Code.put(Code.arraylength);
		
		Code.putFalseJump(Code.ge, START);
		
		// fail found
		Code.put(Code.pop);
		Code.put(Code.pop);
		
		Code.loadConst(0);
		
		Code.putJump(0);
		
		int END = Code.pc - 2;
		
		// found
		Code.fixup(FOUND);
		
		Code.put(Code.pop);
		Code.put(Code.pop);
		Code.put(Code.pop);
		
		Code.loadConst(1);
		
		Code.fixup(END);
		Code.store(bool);
	}
}
