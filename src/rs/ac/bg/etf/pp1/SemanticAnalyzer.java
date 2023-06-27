package rs.ac.bg.etf.pp1;

import rs.ac.bg.etf.pp1.ast.*;
import rs.etf.pp1.mj.runtime.Code;
import rs.etf.pp1.symboltable.concepts.Obj;

import org.apache.log4j.Logger;
import rs.etf.pp1.symboltable.concepts.Struct;

import java.util.*;

public class SemanticAnalyzer extends VisitorAdaptor {
	
	private boolean errorDetected;
	
	private final Logger log = Logger.getLogger(MJParser.class);
	private Obj currentMethod = Tab.noObj;
	private Struct requiredType = null;
	private int whileDepth = 0;
	
	private final Stack<List<Struct>> methodCalls = new Stack<>();
	private final HashMap<String, List<Struct>> methods = new HashMap<>();
	
	public SemanticAnalyzer() {
		Tab.init();
	}
	
	public void analyze(Program program) throws Exception {
		errorDetected = false;
		program.traverseBottomUp(this);
		
		if (errorDetected) throw new Exception("Semantic analysis failed");
	}
	
	private void report_error(String message, SyntaxNode info) {
		errorDetected = true;
		
		if (info != null) message = "Line " + info.getLine() + ": " + message;
		
		log.error(message);
	}
	
	private void report_info(String message, SyntaxNode info) {
		if (info != null) message = "Line " + info.getLine() + ": " + message;
		
		log.info(message);
	}

	// ========================================================================
	// PROGRAM
	// ========================================================================
	public void visit(ProgName progName) {
		progName.obj = Tab.insert(Obj.Prog, progName.getName(), Tab.noType);
		Tab.openScope();
	}
	
	public void visit(Program program) {
		Code.dataSize = Tab.currentScope.getnVars();
		
		Tab.chainLocalSymbols(program.getProgName().obj);
		Tab.closeScope();
	}
	
	// ========================================================================
	// TYPE
	// ========================================================================
	public void visit(Type type) {
		Obj typeNode = Tab.find(type.getTypeName());
		type.struct = Tab.noType;
		
		if (typeNode == Tab.noObj) {
			report_error("Type: Type '" + type.getTypeName() + "' not found in symbol table", null);
			return;
		}
		
		if (Obj.Type != typeNode.getKind()) {
			report_error("Type: Name '" + type.getTypeName() + "' is not a type", type);
			return;
		}
		
		type.struct = typeNode.getType();
		requiredType = type.struct;
	}
	
	public void visit(ConstDecl constDecl) {
		if (Tab.find(constDecl.getName()) != Tab.noObj) {
			report_error("ConstDecl: Name '" + constDecl.getName() + "' already in use", constDecl);
			return;
		}
		
		if (!constDecl.getConstValue().struct.assignableTo(requiredType)) {
			String message = "ConstDecl: Type of constant '" + constDecl.getName() + "' does not match required type (required: '" + structToString(requiredType) + "', got: '" + structToString(constDecl.getConstValue().struct) + "')";
			report_error(message, constDecl);
			return;
		}
		
		Obj constNode = Tab.insert(Obj.Con, constDecl.getName(), requiredType);
		constNode.setLevel(0);
		
		report_info("ConstDecl: Inserted constant '" + constDecl.getName() + "'", constDecl);
	}
	
	public void visit(VarDeclName varDecl) {
		if (Tab.find(varDecl.getName()) != Tab.noObj) {
			report_error("VarDeclName: Name '" + varDecl.getName() + "' already in use", varDecl);
			return;
		}
		
		Obj varNode = Tab.insert(Obj.Var, varDecl.getName(), requiredType);
		varNode.setLevel(0);
		
		report_info("VarDeclName: Inserted variable '" + varDecl.getName() + "'", varDecl);
	}
	
	public void visit(VarDeclArray varDecl) {
		if (Tab.find(varDecl.getName()) != Tab.noObj) {
			report_error("VarDeclArray: Name '" + varDecl.getName() + "' already in use", varDecl);
			return;
		}
		
		Obj varNode = Tab.insert(Obj.Var, varDecl.getName(), new Struct(Struct.Array, requiredType));
		varNode.setLevel(0);
		
		report_info("VarDeclArray: Inserted array '" + varDecl.getName() + "'", varDecl);
	}
	
	public void visit(VarDeclMatrix varDecl) {
		if (Tab.find(varDecl.getName()) != Tab.noObj) {
			report_error("VarDeclMatrix: Name '" + varDecl.getName() + "' already in use", varDecl);
			return;
		}
		
		Obj varNode = Tab.insert(Obj.Var, varDecl.getName(), new Struct(Struct.Array, new Struct(Struct.Array, requiredType)));
		varNode.setLevel(0);
		
		report_info("VarDeclMatrix: Inserted matrix '" + varDecl.getName() + "'", varDecl);
	}
	
	
	// ========================================================================
	// CONST VALUE
	// ========================================================================
	public void visit(ConstInt constInt) {
		constInt.struct = Tab.intType;
	}
	
	public void visit(ConstChar constChar) {
		constChar.struct = Tab.charType;
	}
	
	public void visit(ConstBool constBool) {
		constBool.struct = Tab.boolType;
	}
	
	// ========================================================================
	// FACTOR
	// ========================================================================
	public void visit(FactorConst factor) {
		factor.struct = factor.getConstValue().struct;
	}
	
	public void visit(FactorVar factor) {
		factor.struct = factor.getDesignator().obj.getType();
	}
	
	public void visit(FactorExpression factor) {
		factor.struct = factor.getExpr().struct;
	}
	
	public void visit(FactorArray factor) {
		factor.struct = new Struct(Struct.Array, factor.getType().struct);
		
		if (factor.getExpr().struct != Tab.intType) {
			report_error("FactorArray: Array index is not of type int", factor);
			return;
		}
	}
	
	public void visit(FactorMatrix factor) {
		factor.struct = new Struct(Struct.Array, new Struct(Struct.Array, factor.getType().struct));
		
		if (factor.getExpr().struct != Tab.intType) {
			report_error("FactorMatrix: Matrix index is not of type int", factor);
			return;
		}
		
		if (factor.getExpr1().struct != Tab.intType) {
			report_error("FactorMatrix: Matrix array index is not of type int", factor);
			return;
		}
	}
	
	public void visit(FactorFuncCall factor) {
		Obj designatorObj = factor.getDesignator().obj;
		
		factor.struct = designatorObj.getType();
		
		if (designatorObj == Tab.noObj) {
			report_error("FactorFuncCall: Name '" + factor.getDesignator().obj.getName() + "' not found in symbol table", factor);
			return;
		}
		
		if (designatorObj.getKind() != Obj.Meth) {
			report_error("FactorFuncCall: Name '" + factor.getDesignator().obj.getName() + "' is not a function", factor);
			return;
		}
		
		System.out.println(designatorObj.getName());
		System.out.println(methods);
		List<Struct> methodParams = methods.get(designatorObj.getName());
		List<Struct> callParams = methodCalls.pop();
		
		if (methodParams.size() != callParams.size()) {
			report_error("FactorFuncCall: Number of parameters does not match", factor);
			return;
		}
		
		for (int i = 0; i < methodParams.size(); i++) {
			if (!compareStructs(methodParams.get(i), callParams.get(i))) {
				report_error("FactorFuncCall: Parameter types do not match", factor);
				return;
			}
		}
	}
	
	// ========================================================================
	// EXPR
	// ========================================================================
	public void visit(ExprTerm expr) {
		expr.struct = expr.getTerm().struct;
	}
	
	public void visit(ExprMinus expr) {
		expr.struct = Tab.intType;
		
		if (expr.getTerm().struct != Tab.intType) {
			report_error("ExprMinus: Expression is not of type int", expr);
			return;
		}
	}
	
	public void visit(ExprAdd expr) {
		expr.struct = Tab.intType;
		
		if (expr.getTerm().struct != Tab.intType) {
			report_error("ExprAdd: Expression [term] is not of type int", expr);
			return;
		}
		
		if (expr.getExpr().struct != Tab.intType) {
			report_error("ExprAdd: Expression [expr] is not of type int", expr);
			return;
		}
	}
	
	public void visit(ExprMap expr) {
		expr.struct = Tab.noType;
		
		Obj designator = expr.getDesignator().obj;
		if (designator.getKind() != Obj.Var) {
			report_error("ExprMap: Designator is not a variable", expr);
			return;
		}
		
		if (designator.getType().getKind() != Struct.Array) {
			report_error("ExprMap: Designator is not an array (required: '" + structToString(Struct.Array) + "', got: '" + structToString(designator.getType().getKind()) + "')", expr);
			return;
		}
		
		if (designator.getType().getElemType().getKind() == Struct.Array) {
			report_error("ExprMap: Designator shouldn't be a matrix", expr);
			return;
		}
		
		Obj iterator = Tab.find(expr.getIterator());
		
		if (iterator == Tab.noObj) {
			report_error("ExprMap: Iterator '" + expr.getIterator() + "' not found in symbol table", expr);
			return;
		}
		
		if (iterator.getType() != designator.getType().getElemType()) {
			report_error("ExprMap: Iterator '" + expr.getIterator() + "' is not of type " + designator.getType().getElemType().getKind(), expr);
			return;
		}
		
		expr.struct = new Struct(Struct.Array, designator.getType().getElemType());
	}
	
	// ========================================================================
	// TERM
	// ========================================================================
	public void visit(TermMul term) {
		term.struct = term.getFactor().struct;
		
		if (term.getFactor().struct != Tab.intType) {
			report_error("TermMul: Term [factor] is not of type int", term);
			return;
		}
		
		if (term.getTerm().struct != Tab.intType) {
			report_error("TermMul: Term [term] is not of type int", term);
			return;
		}
	}
	
	public void visit(TermFactor term) {
		term.struct = term.getFactor().struct;
	}
	
	// ========================================================================
	// CONDITION
	// ========================================================================
	public void visit(CondFactRel condFact) {
		condFact.struct = Tab.boolType;
		
		if (!condFact.getExpr().struct.compatibleWith(condFact.getExpr1().struct)) {
			report_error("CondFactRel: Condition expression types do not match (left: '" + structToString(condFact.getExpr().struct) + "', right: '" + structToString(condFact.getExpr1().struct) + "')", condFact);
			return;
		}
	}
	
	public void visit(CondFactExpr condFact) {
		condFact.struct = condFact.getExpr().struct;
		
		if (condFact.struct != Tab.boolType) {
			report_error("CondFactExpr: Condition expression is not of type bool", condFact);
			return;
		}
	}
	
	
	// ========================================================================
	// DESIGNATOR
	// ========================================================================
	public void visit(DesignatorVar designator) {
		designator.obj = Tab.find(designator.getName());
		
		if (designator.obj == Tab.noObj) {
			report_error("DesignatorVar: Name '" + designator.getName() + "' not found in symbol table", designator);
			return;
		}
		
		SyntaxNode parent = designator.getParent();
		
		if (parent instanceof DesignatorStatementFunc || parent instanceof FactorFuncCall) {
			methodCalls.push(new ArrayList<>());
		}
	}
	
	public void visit(DesignatorArray designator) {
		designator.obj = Tab.find(designator.getName());
		
		if (designator.obj == Tab.noObj) {
			report_error("DesignatorArray: Name '" + designator.getName() + "' not found in symbol table", designator);
			return;
		}
		
		if (designator.obj.getType().getKind() != Struct.Array) {
			report_error("DesignatorArray: Designator is not an array (required: '" + structToString(Struct.Array) + "', got: '" + structToString(designator.obj.getType().getKind()) + "')", designator);
			return;
		}
		
		if (designator.getExpr().struct != Tab.intType) {
			report_error("DesignatorArray: Array index is not of type int", designator);
			return;
		}
		
		designator.obj = new Obj(Obj.Elem, designator.obj.getName(), designator.obj.getType().getElemType());
	}
	
	public void visit(DesignatorMatrix designator) {
		designator.obj = Tab.find(designator.getName());
		
		if (designator.obj == Tab.noObj) {
			report_error("DesignatorMatrix: Name '" + designator.getName() + "' not found in symbol table", designator);
			return;
		}
		
		if (designator.obj.getType().getKind() != Struct.Array || designator.obj.getType().getElemType().getKind() != Struct.Array) {
			report_error("DesignatorMatrix: Name '" + designator.getName() + "' is not a matrix 1", designator);
			return;
		}
		
		designator.obj = new Obj(Obj.Elem, designator.obj.getName(), designator.obj.getType().getElemType());
		
		if (designator.obj.getType().getKind() != Struct.Array) {
			report_error("DesignatorMatrix: Name '" + designator.getName() + "' is not a matrix 2", designator);
			return;
		}
		
		designator.obj = new Obj(Obj.Elem, designator.obj.getName(), designator.obj.getType().getElemType());
		
		if (designator.getExpr().struct != Tab.intType) {
			report_error("DesignatorMatrix: Matrix index is not of type int", designator);
			return;
		}
		
		if (designator.getExpr1().struct != Tab.intType) {
			report_error("DesignatorMatrix: Matrix array index is not of type int", designator);
			return;
		}
	}
	
	// ========================================================================
	// DESIGNATOR STATEMENT
	// ========================================================================
	
	public void visit(DesignatorStatementInc designatorStatement) {
		Obj designatorObj = designatorStatement.getDesignator().obj;
		
		if (designatorObj.getKind() != Obj.Var) {
			report_error("DesignatorStatementInc: Designator is not a variable", designatorStatement);
			return;
		}
		
		if (designatorObj.getType() != Tab.intType) {
			report_error("DesignatorStatementInc: Designator is not of type int", designatorStatement);
			return;
		}
	}
	
	public void visit(DesignatorStatementDec designatorStatement) {
		Obj designatorObj = designatorStatement.getDesignator().obj;
		
		if (designatorObj.getKind() != Obj.Var) {
			report_error("DesignatorStatementDec: Designator is not a variable", designatorStatement);
			return;
		}
		
		if (designatorObj.getType() != Tab.intType) {
			report_error("DesignatorStatementDec: Designator is not of type int", designatorStatement);
			return;
		}
	}
	
	public void visit(DesignatorStatementAssign designatorStatement) {
		Obj designatorStatementObj = designatorStatement.getDesignator().obj;
		
		if (designatorStatementObj.getKind() != Obj.Var) {
			report_error("DesignatorStatementAssign: Designator is not a variable", designatorStatement);
			return;
		}
		
		if (!designatorStatement.getExpr().struct.assignableTo(designatorStatementObj.getType())) {
			report_error("DesignatorStatementAssign: Designator is not of the same type as expression", designatorStatement);
			return;
		}
	}
	
	public void visit(DesignatorStatementFunc designatorStatement) {
		Obj designatorObj = designatorStatement.getDesignator().obj;
		
		if (designatorObj.getKind() != Obj.Meth) {
			report_error("DesignatorStatementFunc: Designator is not a function", designatorStatement);
			return;
		}
		
		List<Struct> methodParams = methods.get(designatorObj.getName());
		List<Struct> callParams = methodCalls.pop();
		
		if (methodParams.size() != callParams.size()) {
			report_error("DesignatorStatementFunc: Number of parameters does not match", designatorStatement);
			return;
		}
		
		for (int i = 0; i < methodParams.size(); i++) {
			if (!compareStructs(methodParams.get(i), callParams.get(i))) {
				report_error("DesignatorStatementFunc: Parameter types do not match", designatorStatement);
				return;
			}
		}
	}
	
	// ========================================================================
	// PRINT READ
	// ========================================================================

	public void visit(PrintStmt printStmt) {
		Struct exprType = printStmt.getPrintStatementOptional().struct;
		if (!Arrays.asList(Tab.intType, Tab.charType, Tab.boolType).contains(exprType)) {
			report_error("PrintStmt: Print statement expression is not of type int, char or bool", printStmt);
			return;
		}
	}
	
	public void visit(PrintStatementOptionalYes printStatement) {
		 printStatement.struct = printStatement.getExpr().struct;
	}
	
	public void visit(PrintStatementOptionalNo printStatement) {
		printStatement.struct = printStatement.getExpr().struct;
	}
	
	public void visit(ReadStmt statement) {
		if (statement.getDesignator().obj.getKind() != Obj.Var) {
			report_error("ReadStmt: Read statement designator is not a variable", statement);
			return;
		}
		
		if (!Arrays.asList(Tab.intType, Tab.charType, Tab.boolType).contains(statement.getDesignator().obj.getType())) {
			report_error("ReadStmt: Read statement designator is not of type int, char or bool", statement);
			return;
		}
	}
	
	// ========================================================================
	// RETURN
	// ========================================================================
	
	public void visit(ReturnItemExpr returnItem) {
		if (currentMethod == Tab.noObj) {
			report_error("ReturnItemExpr: Return expression outside of method", returnItem);
			return;
		}
		
		if (currentMethod.getType() == Tab.noType) {
			report_error("ReturnItemExpr: Return expression in void method", returnItem);
			return;
		}
		
		if (!currentMethod.getType().assignableTo(returnItem.getExpr().struct)) {
			report_error("ReturnItemExpr: Return expression is not of the same type as method", returnItem);
			return;
		}
	}
	
	public void visit(ReturnItemVoid returnItem) {
		if (currentMethod == Tab.noObj) {
			report_error("ReturnItemVoid: Return expression outside of method", returnItem);
			return;
		}
		
		if (currentMethod.getType() != Tab.noType) {
			report_error("ReturnItemVoid: Return expression in non-void method", returnItem);
			return;
		}
	}
	
	// ========================================================================
	// WHILE
	// ========================================================================
	
	public void visit(WhileTrigger whileTrigger) {
		whileDepth++;
	}
	
	public void visit(WhileStatement whileStatement) {
		whileDepth--;
	}
	
	public void visit(BreakStmt breakStmt) {
		if (whileDepth == 0) {
			report_error("BreakStmt: Break statement outside of while loop", breakStmt);
			return;
		}
	}
	
	public void visit(ContinueStmt continueStmt) {
		if (whileDepth == 0) {
			report_error("ContinueStmt: Continue statement outside of while loop", continueStmt);
			return;
		}
	}
	
	// ========================================================================
	// METHODS
	// ========================================================================
	
	public void visit(MethodTypeName methodTypeName) {
		currentMethod = methodTypeName.obj = Tab.insert(Obj.Meth, methodTypeName.getName(), requiredType);
		methods.put(currentMethod.getName(), new ArrayList<>());
		Tab.openScope();
	}
	
	public void visit(MethodDecl methodDecl) {
		Tab.chainLocalSymbols(currentMethod);
		Tab.closeScope();
		currentMethod = Tab.noObj;
	}
	
	public void visit(ReturnVoid returnVoid) {
		requiredType = Tab.noType;
	}
	
	public void visit(FormParVar formPar) {
		if (Tab.find(formPar.getName()) != Tab.noObj) {
			report_error("FormParVar: Formal parameter '" + formPar.getName() + "' already declared", formPar);
			return;
		}
		
		Struct type = formPar.getType().struct;
		methods.get(currentMethod.getName()).add(type);
		Tab.insert(Obj.Var, formPar.getName(), type);
	}
	
	public void visit(FormParArray formPar) {
		if (Tab.find(formPar.getName()) != Tab.noObj) {
			report_error("FormParArray: Formal parameter '" + formPar.getName() + "' already declared", formPar);
			return;
		}
		
		Struct type = new Struct(Struct.Array, formPar.getType().struct);
		methods.get(currentMethod.getName()).add(type);
		Tab.insert(Obj.Var, formPar.getName(), type);
	}
	
	public void visit(FormParMatrix formPar) {
		if (Tab.find(formPar.getName()) != Tab.noObj) {
			report_error("FormParMatrix: Formal parameter '" + formPar.getName() + "' already declared", formPar);
			return;
		}
		
		Struct type = new Struct(Struct.Array, new Struct(Struct.Array, formPar.getType().struct));
		methods.get(currentMethod.getName()).add(type);
		Tab.insert(Obj.Var, formPar.getName(), type);
	}
	
	public void visit(MethodVarName methodVar) {
		if (Tab.find(methodVar.getName()) != Tab.noObj) {
			report_error("MethodVarName: Method variable '" + methodVar.getName() + "' already declared", methodVar);
			return;
		}
		
		Tab.insert(Obj.Var, methodVar.getName(), requiredType);
	}
	
	public void visit(MethodVarArray methodVar) {
		if (Tab.find(methodVar.getName()) != Tab.noObj) {
			report_error("MethodVarArray: Method variable '" + methodVar.getName() + "' already declared", methodVar);
			return;
		}

		Tab.insert(Obj.Var, methodVar.getName(), new Struct(Struct.Array, requiredType));
	}
	
	public void visit(MethodVarMatrix methodVar) {
		if (Tab.find(methodVar.getName()) != Tab.noObj) {
			report_error("MethodVarMatrix: Method variable '" + methodVar.getName() + "' already declared", methodVar);
			return;
		}
		
		Tab.insert(Obj.Var, methodVar.getName(), new Struct(Struct.Array, new Struct(Struct.Array, requiredType)));
	}
	
	// ========================================================================
	// ACTUAL PARAMETERS
	// ========================================================================
	
	public void visit(ActParsListComma actParsList) {
		methodCalls.peek().add(actParsList.getExpr().struct);
	}
	
	public void visit(ActParsListSingle actParsList) {
		methodCalls.peek().add(actParsList.getExpr().struct);
	}
	
	// ========================================================================
	// HELPER FUNCTIONS
	// ========================================================================
	
	private String structToString(int struct) {
		switch (struct) {
			case Struct.None:
				return "None";
			case Struct.Int:
				return "Int";
			case Struct.Char:
				return "Char";
			case Struct.Array:
				return "Array";
			case Struct.Class:
				return "Class";
			case Struct.Bool:
				return "Bool";
			case Struct.Enum:
				return "Enum";
			case Struct.Interface:
				return "Interface";
			
			default:
				return "Unknown";
		}
	}
	
	private String structToString(Struct struct) {
		return structToString(struct.getKind());
	}
	
	private String objToString(int obj) {
		switch (obj) {
			case Obj.Con:
				return "Con";
			case Obj.Var:
				return "Var";
			case Obj.Type:
				return "Type";
			case Obj.Meth:
				return "Meth";
			case Obj.Fld:
				return "Fld";
			case Obj.Prog:
				return "Prog";
			case Obj.Elem:
				return "Elem";
			case Obj.NO_VALUE:
				return "NO_VALUE";
			
			default:
				return "Unknown";
		}
	}
	
	private String objToString(Obj obj) {
		return objToString(obj.getKind());
	}
	
	private boolean compareStructs(Struct s1, Struct s2) {
		if (s1.getKind() == s2.getKind()) {
			if (s1.getKind() == Struct.Array) {
				return compareStructs(s1.getElemType(), s2.getElemType());
			}
			
			return true;
		}
		
		return false;
	}
}
