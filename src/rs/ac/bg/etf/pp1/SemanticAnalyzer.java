package rs.ac.bg.etf.pp1;

import rs.ac.bg.etf.pp1.ast.*;
import rs.ac.bg.etf.pp1.util.Analyzer;
import rs.etf.pp1.mj.runtime.Code;
import rs.etf.pp1.symboltable.concepts.Obj;
import rs.etf.pp1.symboltable.concepts.Struct;

import java.util.*;

public class SemanticAnalyzer extends VisitorAdaptor {
	
	private final Analyzer analyzer = Analyzer.getInstance(MJParser.class);
	private final Stack<List<Struct>> methodCalls = new Stack<>();
	private final HashMap<Obj, List<Struct>> methods = new HashMap<>();
	private Obj currentMethod = Tab.noObj;
	private Struct requiredType = null;
	private int whileDepth = 0;
	
	public SemanticAnalyzer() {
	}
	
	public void analyze(Program program) throws Exception {
		Tab.init();
		
		addMethod("add", Tab.noType, Arrays.asList(
				new AbstractMap.SimpleEntry<>("aa", Tab.setType),
				new AbstractMap.SimpleEntry<>("bb", Tab.intType)
		));
		
		addMethod("addAll", Tab.noType, Arrays.asList(
				new AbstractMap.SimpleEntry<>("aa", Tab.setType),
				new AbstractMap.SimpleEntry<>("bb", new Struct(Struct.Array, Tab.intType))
		));
		
		Obj ordMethod = Tab.find("ord");
		if (ordMethod == Tab.noObj) {
			analyzer.report_error(null, "Method 'ord' not found in symbol table");
		} else {
			methods.put(ordMethod, Arrays.asList(Tab.charType));
			analyzer.report_info(null, "Method 'ord' found in symbol table with return type '" + structToString(ordMethod.getType()) + "'");
		}
		
		Obj chrMethod = Tab.find("chr");
		if (chrMethod == Tab.noObj) {
			analyzer.report_error(null, "Method 'chr' not found in symbol table");
		} else {
			methods.put(chrMethod, Arrays.asList(Tab.intType));
			analyzer.report_info(null, "Method 'chr' found in symbol table with return type '" + structToString(chrMethod.getType()) + "'");
		}
		
		Obj lenMethod = Tab.find("len");
		if (lenMethod == Tab.noObj) {
			analyzer.report_error(null, "Method 'len' not found in symbol table");
		} else {
			methods.put(lenMethod, Arrays.asList(new Struct(Struct.Array, Tab.noType)));
			analyzer.report_info(null, "Method 'len' found in symbol table with return type '" + structToString(lenMethod.getType()) + "'");
		}
		
		program.traverseBottomUp(this);
		
		if (analyzer.isErrorDetected()) throw new Exception("Semantic analysis failed");
	}
	
	
	// ========================================================================
	// PROGRAM
	// ========================================================================
	public void visit(ProgName progName) {
		analyzer.report_info(progName, "Program name: " + progName.getName());
		progName.obj = analyzer.infoInsert(progName, Obj.Prog, progName.getName(), Tab.noType);
		Tab.openScope(progName);
	}
	
	public void visit(Program program) {
		Code.dataSize = Tab.currentScope.getnVars();
		
		Tab.chainLocalSymbols(program.getProgName().obj);
		Tab.closeScope(program);
	}
	
	// ========================================================================
	// TYPE
	// ========================================================================
	public void visit(Type type) {
		Obj typeNode = Tab.find(type.getTypeName());
		type.struct = Tab.noType; // just for error handling
		
		if (analyzer.ifErrorNotExists(type, type.getTypeName()) || analyzer.errorObjWrongKind(type, type.getTypeName(), Obj.Type)) return;
		
		type.struct = typeNode.getType();
		requiredType = type.struct;
		analyzer.report_info(type, "Required type set to '" + structToString(requiredType) + "'");
	}
	
	public void visit(ConstDecl constDecl) {
		if (analyzer.isErrorNotAssignable(constDecl, constDecl.getConstValue().struct, requiredType)) return;
		
		Obj obj = analyzer.infoInsert(constDecl, Obj.Con, constDecl.getName(), requiredType);
		obj.setLevel(0);
		
		if (constDecl.getConstValue() instanceof ConstBool) {
			obj.setAdr(((ConstBool) constDecl.getConstValue()).getB1() ? 1 : 0);
			analyzer.report_info(constDecl, "Constant '" + constDecl.getName() + "' value set to '" + (obj.getAdr() == 1 ? "true" : "false") + "'");
		} else if (constDecl.getConstValue() instanceof ConstChar) {
			obj.setAdr(((ConstChar) constDecl.getConstValue()).getC1());
			analyzer.report_info(constDecl, "Constant char '" + constDecl.getName() + "' value set to '" + (char) obj.getAdr() + "'");
		} else if (constDecl.getConstValue() instanceof ConstInt) {
			obj.setAdr(((ConstInt) constDecl.getConstValue()).getN1());
			analyzer.report_info(constDecl, "Constant int '" + constDecl.getName() + "' value set to '" + obj.getAdr() + "'");
		}
	}
	
	public void visit(VarDeclName varDecl) {
		analyzer.infoInsert(varDecl, Obj.Var, varDecl.getName(), requiredType);
	}
	
	public void visit(VarDeclArray varDecl) {
		analyzer.infoInsert(varDecl, Obj.Var, varDecl.getName(), new Struct(Struct.Array, requiredType));
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
	
	public void visit(FactorNewArray factor) {
		if (factor.getType().struct == Tab.setType) {
			analyzer.report_info(factor, "Factor is a set, setting struct to 'Set'");
			factor.struct = Tab.setType;
		} else {
			analyzer.report_info(factor, "Factor is an array of type '" + structToString(factor.getExpr().struct) + "', setting struct to 'Array(" + structToString(factor.getExpr().struct) + ")'");
			factor.struct = new Struct(Struct.Array, factor.getType().struct);
		}
		
		// Check if [] is used with int type
		analyzer.isErrorStructWrongKind(factor, factor.getExpr().struct, Tab.intType);
	}
	
	public void visit(FactorFuncCall factor) {
		Obj designatorObj = factor.getDesignator().obj;
		
		factor.struct = designatorObj.getType();
		
		if (
				analyzer.ifErrorNotExists(factor, designatorObj.getName()) ||
						analyzer.errorObjWrongKind(factor, designatorObj.getName(), Obj.Meth)
		) return;
		
		List<Struct> methodParams = methods.get(designatorObj);
		List<Struct> callParams = methodCalls.pop();
		
		analyzer.errorParameterNotMatch(factor, methodParams, callParams);
	}
	
	// ========================================================================
	// EXPR
	// ========================================================================
	public void visit(ExprTerm expr) {
		expr.struct = expr.getTerm().struct;
	}
	
	public void visit(ExprMinus expr) {
		expr.struct = Tab.intType;
		
		analyzer.isErrorNotAssignable(expr, expr.getTerm().struct, Tab.intType);
	}
	
	public void visit(ExprAdd expr) {
		expr.struct = Tab.intType;
		
		analyzer.isErrorNotAssignable(expr, expr.getTerm().struct, Tab.intType);
		analyzer.isErrorNotAssignable(expr, expr.getExpr().struct, Tab.intType);
	}
	
	public void visit(ExprSet expr) {
		expr.struct = Tab.setType;
		
		analyzer.isErrorNotAssignable(expr, expr.getTerm().struct, Tab.setType);
		analyzer.isErrorNotAssignable(expr, expr.getExpr().struct, Tab.setType);
	}
	
	public void visit(ExprMap expr) {
		expr.struct = Tab.noType;
		
		Obj designator = expr.getDesignator().obj;
		
		
		if (designator.getKind() != Obj.Var) {
			analyzer.report_error(expr, "Designator is not a variable");
			return;
		}
		
		if (designator.getType().getKind() != Struct.Array) {
			analyzer.report_error(expr, "Designator is not an array (required: '" + structToString(Struct.Array) + "', got: '" + structToString(designator.getType().getKind()) + "')");
			return;
		}
		
		Obj iterator = Tab.find(expr.getIterator());
		
		if (iterator == Tab.noObj) {
			analyzer.report_error(expr, "Iterator '" + expr.getIterator() + "' not found in symbol table");
			return;
		}
		
		if (iterator.getType() != designator.getType().getElemType()) {
			analyzer.report_error(expr, "Iterator '" + expr.getIterator() + "' is not of type " + designator.getType().getElemType().getKind());
			return;
		}
		
		expr.struct = new Struct(Struct.Array, designator.getType().getElemType());
	}
	
	// ========================================================================
	// TERM
	// ========================================================================
	public void visit(TermMul term) {
		term.struct = term.getFactor().struct;
		
		analyzer.isErrorNotAssignable(term, term.getFactor().struct, Tab.intType);
		analyzer.isErrorNotAssignable(term, term.getTerm().struct, Tab.intType);
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
			analyzer.report_error(condFact, "Condition expression types do not match (left: '" + structToString(condFact.getExpr().struct) + "', right: '" + structToString(condFact.getExpr1().struct) + "')");
		}
	}
	
	public void visit(CondFactExpr condFact) {
		condFact.struct = condFact.getExpr().struct;
		
		analyzer.isErrorNotAssignable(condFact, condFact.getExpr().struct, Tab.boolType);
	}
	
	
	// ========================================================================
	// DESIGNATOR
	// ========================================================================
	public void visit(DesignatorName designatorName) {
		designatorName.obj = Tab.find(designatorName.getName());
		
		analyzer.ifErrorNotExists(designatorName, designatorName.getName());
	}
	
	public void visit(DesignatorVar designator) {
		designator.obj = Tab.find(designator.getDesignatorName().getName());
		
		// if (analyzer.ifErrorNotExists(designator, designator.getDesignatorName().getName())) return;
		
		SyntaxNode parent = designator.getParent();
		
		if (parent instanceof DesignatorStatementFunc || parent instanceof FactorFuncCall) {
			methodCalls.push(new ArrayList<>());
		}
	}
	
	public void visit(DesignatorArray designator) {
		designator.obj = Tab.find(designator.getDesignatorName().getName());
		
		if (
				analyzer.ifErrorNotExists(designator, designator.getDesignatorName().getName())
		) return;
		
		if (designator.obj.getType().getKind() != Struct.Array) {
			analyzer.report_error(designator, "Designator is not an array (required: '" + structToString(Struct.Array) + "', got: '" + structToString(designator.obj.getType().getKind()) + "')");
			return;
		}
		
		if (designator.getExpr().struct != Tab.intType) {
			analyzer.report_error(designator, "Array index is not of type int");
			return;
		}
		
		designator.obj = new Obj(Obj.Elem, designator.obj.getName(), designator.obj.getType().getElemType());
	}
	
	// ========================================================================
	// DESIGNATOR STATEMENT
	// ========================================================================
	public void visit(DesignatorStatementInc designatorStatement) {
		Obj designatorObj = designatorStatement.getDesignator().obj;
		
		if (designatorObj.getKind() != Obj.Var) {
			analyzer.report_error(designatorStatement, "Designator is not a variable");
			return;
		}
		
		if (designatorObj.getType() != Tab.intType) {
			analyzer.report_error(designatorStatement, "Designator is not of type int");
		}
	}
	
	public void visit(DesignatorStatementDec designatorStatement) {
		Obj designatorObj = designatorStatement.getDesignator().obj;
		
		if (designatorObj.getKind() != Obj.Var) {
			analyzer.report_error(designatorStatement, "Designator is not a variable");
			return;
		}
		
		if (designatorObj.getType() != Tab.intType) {
			analyzer.report_error(designatorStatement, "Designator is not of type int");
		}
	}
	
	public void visit(DesignatorStatementAssign designatorStatement) {
		Obj designatorStatementObj = designatorStatement.getDesignator().obj;
		
		if (designatorStatementObj.getKind() != Obj.Var && designatorStatementObj.getKind() != Obj.Elem) {
			analyzer.report_error(designatorStatement, "Designator is not a variable");
			return;
		}
		
		if (!designatorStatement.getExpr().struct.assignableTo(designatorStatementObj.getType())) {
			analyzer.report_error(designatorStatement, "Designator is not assignable to expression type (designator: '" + structToString(designatorStatementObj.getType()) + "', expression: '" + structToString(designatorStatement.getExpr().struct) + "')");
		}
	}
	
	public void visit(DesignatorStatementFunc designatorStatement) {
		Obj designatorObj = designatorStatement.getDesignator().obj;
		
		if (designatorObj.getKind() != Obj.Meth) {
			analyzer.report_error(designatorStatement, "Designator is not a function");
			return;
		}
		
		List<Struct> methodParams = methods.get(designatorObj);
		List<Struct> callParams = methodCalls.pop();
		
		analyzer.errorParameterNotMatch(designatorStatement, methodParams, callParams);
	}
	
	// ========================================================================
	// PRINT READ
	// ========================================================================
	
	public void visit(PrintStmt printStmt) {
		Struct exprType = printStmt.getPrintStatementOptional().struct;
		if (!Arrays.asList(Tab.intType, Tab.charType, Tab.boolType, Tab.setType).contains(exprType)) {
			analyzer.report_error(printStmt, "Print statement expression is not of type int, char, bool or set");
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
			analyzer.report_error(statement, "Read statement designator is not a variable");
			return;
		}
		
		if (!Arrays.asList(Tab.intType, Tab.charType, Tab.boolType).contains(statement.getDesignator().obj.getType())) {
			analyzer.report_error(statement, "Read statement designator is not of type int, char or bool");
		}
	}
	
	// ========================================================================
	// RETURN
	// ========================================================================
	
	public void visit(ReturnItemExpr returnItem) {
		if (currentMethod == Tab.noObj) {
			analyzer.report_error(returnItem, "Return expression outside of method");
			return;
		}
		
		if (currentMethod.getType() == Tab.noType) {
			analyzer.report_error(returnItem, "Return expression in void method");
			return;
		}
		
		if (!currentMethod.getType().assignableTo(returnItem.getExpr().struct)) {
			analyzer.report_error(returnItem, "Return expression is not of the same type as method");
		}
	}
	
	public void visit(ReturnItemVoid returnItem) {
		if (currentMethod == Tab.noObj) {
			analyzer.report_error(returnItem, "Return expression outside of method");
			return;
		}
		
		if (currentMethod.getType() != Tab.noType) {
			analyzer.report_error(returnItem, "Return expression in non-void method");
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
			analyzer.report_error(breakStmt, "Break statement outside of while loop");
		}
	}
	
	public void visit(ContinueStmt continueStmt) {
		if (whileDepth == 0) {
			analyzer.report_error(continueStmt, "Continue statement outside of while loop");
		}
	}
	
	// ========================================================================
	// METHODS
	// ========================================================================
	private void addMethod(String name, Struct returnType, List<Map.Entry<String, Struct>> formPars) {
		if (Tab.find(name) != Tab.noObj) {
			analyzer.report_error(null, "Method '" + name + "' already declared");
			return;
		}
		
		Obj method = Tab.insert(Obj.Meth, name, returnType);
		methods.put(method, new ArrayList<>());
		Tab.openScope();
		
		for (Map.Entry<String, Struct> formPar : formPars) {
			if (Tab.find(formPar.getKey()) != Tab.noObj) {
				analyzer.report_error(null, "Formal parameter '" + formPar.getKey() + "' already declared");
				continue;
			}
			
			methods.get(method).add(formPar.getValue());
			Tab.insert(Obj.Var, formPar.getKey(), formPar.getValue());
			analyzer.report_info(null, "Formal parameter '" + formPar.getKey() + "' declared with type '" + structToString(formPar.getValue()) + "'");
		}
		
		analyzer.report_info(null, "Method '" + name + "' declared with return type '" + structToString(returnType) + "'");
		Tab.closeScope();
	}
	
	public void visit(MethodTypeName methodTypeName) {
		currentMethod = methodTypeName.obj = Tab.insert(Obj.Meth, methodTypeName.getName(), requiredType);
		methods.put(currentMethod, new ArrayList<>());
		Tab.openScope(methodTypeName);
		analyzer.report_info(methodTypeName, "Method '" + methodTypeName.getName() + "' declared with return type '" + structToString(requiredType) + "'");
	}
	
	public void visit(MethodDecl methodDecl) {
		StringBuilder methodInfo = new StringBuilder();
		
		String methodName = methodDecl.getMethodTypeName().getName();
		String methodReturnType = structToString(currentMethod.getType());
		
		methodInfo.append("Finished method '").append(methodReturnType)
				.append(" ").append(methodName).append("(");
		
		List<Struct> formPars = methods.get(currentMethod);
		for (int i = 0; i < formPars.size(); i++) {
			methodInfo.append(structToString(formPars.get(i)));
			if (i < formPars.size() - 1) {
				methodInfo.append(", ");
			}
		}
		
		methodInfo.append(")'");
		
		Tab.chainLocalSymbols(currentMethod);
		Tab.closeScope(methodDecl);
		currentMethod = Tab.noObj;
		
		StringBuilder currentMethods = new StringBuilder();
		for (Obj method : methods.keySet()) {
			currentMethods.append(method.getName()).append(" (").append(structToString(method.getType())).append("), ");
		}
		
		analyzer.report_info(methodDecl, "Current methods: " + currentMethods);
		
	}
	
	public void visit(ReturnVoid returnVoid) {
		requiredType = Tab.noType;
		analyzer.report_info(returnVoid, "Required type set to 'Void'");
	}
	
	public void visit(FormParVar formPar) {
		if (Tab.find(formPar.getName()) != Tab.noObj) {
			analyzer.report_error(formPar, "Formal parameter '" + formPar.getName() + "' already declared");
			return;
		}
		
		Struct type = formPar.getType().struct;
		methods.get(currentMethod).add(type);
		Tab.insert(Obj.Var, formPar.getName(), type);
		analyzer.report_info(formPar, "Formal parameter '" + formPar.getName() + "' declared with type '" + structToString(type) + "'");
	}
	
	public void visit(FormParArray formPar) {
		if (Tab.find(formPar.getName()) != Tab.noObj) {
			analyzer.report_error(formPar, "Formal parameter '" + formPar.getName() + "' already declared");
			return;
		}
		
		Struct type = new Struct(Struct.Array, formPar.getType().struct);
		methods.get(currentMethod).add(type);
		Tab.insert(Obj.Var, formPar.getName(), type);
		analyzer.report_info(formPar, "Formal parameter '" + formPar.getName() + "' declared with type '" + structToString(type) + "'");
	}
	
	public void visit(MethodVarName methodVar) {
		if (Tab.find(methodVar.getName()) != Tab.noObj) {
			analyzer.report_error(methodVar, "Method variable '" + methodVar.getName() + "' already declared");
			return;
		}
		
		Tab.insert(Obj.Var, methodVar.getName(), requiredType);
		analyzer.report_info(methodVar, "Method variable '" + methodVar.getName() + "' declared with type '" + structToString(requiredType) + "'");
	}
	
	public void visit(MethodVarArray methodVar) {
		if (Tab.find(methodVar.getName()) != Tab.noObj) {
			analyzer.report_error(methodVar, "Method variable '" + methodVar.getName() + "' already declared");
			return;
		}
		
		Tab.insert(Obj.Var, methodVar.getName(), new Struct(Struct.Array, requiredType));
	}
	
	// ========================================================================
	// ACTUAL PARAMETERS
	// ========================================================================
	
	public void visit(ActParsListComma actParsList) {
		analyzer.report_info(actParsList, "Adding actual parameter '" + kindToString(actParsList.getExpr().struct.getKind()) + "' (" + structToString(actParsList.getExpr().struct) + ")");
		methodCalls.peek().add(actParsList.getExpr().struct);
	}
	
	public void visit(ActParsListSingle actParsList) {
		analyzer.report_info(actParsList, "Adding actual parameter '" + kindToString(actParsList.getExpr().struct.getKind()) + "' (" + structToString(actParsList.getExpr().struct) + ")");
		methodCalls.peek().add(actParsList.getExpr().struct);
	}
	
	// ========================================================================
	// CUSTOM FUNCTIONS
	// ========================================================================
	
	// ========================================================================
	// HELPER FUNCTIONS
	// ========================================================================
	
	private String kindToString(int kind) {
		switch (kind) {
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
				return "Set";
			case Struct.Interface:
				return "Interface";
			
			default:
				return "Unknown";
		}
	}
	
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
				return "Set";
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
	
	
}
