package rs.ac.bg.etf.pp1;

import rs.ac.bg.etf.pp1.ast.*;
import rs.ac.bg.etf.pp1.util.Analyzer;
import rs.etf.pp1.mj.runtime.Code;
import rs.etf.pp1.symboltable.concepts.Obj;


import rs.etf.pp1.symboltable.concepts.Struct;

import java.util.*;

public class SemanticAnalyzer extends VisitorAdaptor {
	
	private final Analyzer analyzer = new Analyzer(MJParser.class);
	private Obj currentMethod = Tab.noObj;
	private Struct requiredType = null;
	private int whileDepth = 0;
	
	private final Stack<List<Struct>> methodCalls = new Stack<>();
	private final HashMap<Obj, List<Struct>> methods = new HashMap<>();
	
	public SemanticAnalyzer() {
		Tab.init();
	}
	
	public void analyze(Program program) throws Exception {
		program.traverseBottomUp(this);
		
		if (analyzer.isErrorDetected()) throw new Exception("Semantic analysis failed");
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
		type.struct = Tab.noType; // just for error handling
		
		if (
			analyzer.errorNotExists(type, type.getTypeName()) ||
			analyzer.errorObjWrongKind(type, type.getTypeName(), Obj.Type)
		) return;
		
		type.struct = typeNode.getType();
		requiredType = type.struct;
	}
	
	public void visit(ConstDecl constDecl) {
		if (analyzer.errorNotAssignable(constDecl, constDecl.getConstValue().struct, requiredType)) return;
		
		Obj obj = analyzer.infoInsert(constDecl, Obj.Con, constDecl.getName(), requiredType);
		obj.setLevel(0);
		
		if (constDecl.getConstValue() instanceof ConstBool) {
			obj.setAdr(((ConstBool) constDecl.getConstValue()).getB1() ? 1 : 0);
		} else if (constDecl.getConstValue() instanceof ConstChar) {
			obj.setAdr(((ConstChar) constDecl.getConstValue()).getC1());
		} else if (constDecl.getConstValue() instanceof ConstInt) {
			obj.setAdr(((ConstInt) constDecl.getConstValue()).getN1());
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
	
	public void visit(FactorArray factor) {
		factor.struct = new Struct(Struct.Array, factor.getType().struct);
		
		analyzer.errorStructWrongKind(factor, factor.getExpr().struct, Tab.intType);
	}
	
	public void visit(FactorFuncCall factor) {
		Obj designatorObj = factor.getDesignator().obj;
		
		factor.struct = designatorObj.getType();
		
		if (
			analyzer.errorNotExists(factor, designatorObj.getName()) ||
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
		
		analyzer.errorNotAssignable(expr, expr.getTerm().struct, Tab.intType);
	}
	
	public void visit(ExprAdd expr) {
		expr.struct = Tab.intType;
		
		analyzer.errorNotAssignable(expr, expr.getTerm().struct, Tab.intType);
		analyzer.errorNotAssignable(expr, expr.getExpr().struct, Tab.intType);
	}
	
	public void visit(ExprMap expr) {
		expr.struct = Tab.noType;
		
		Obj designator = expr.getDesignator().obj;
		
		
		
		if (designator.getKind() != Obj.Var) {
			analyzer.report_error("Designator is not a variable", expr);
			return;
		}
		
		if (designator.getType().getKind() != Struct.Array) {
			analyzer.report_error("Designator is not an array (required: '" + structToString(Struct.Array) + "', got: '" + structToString(designator.getType().getKind()) + "')", expr);
			return;
		}
		
		Obj iterator = Tab.find(expr.getIterator());
		
		if (iterator == Tab.noObj) {
			analyzer.report_error("Iterator '" + expr.getIterator() + "' not found in symbol table", expr);
			return;
		}
		
		if (iterator.getType() != designator.getType().getElemType()) {
			analyzer.report_error("Iterator '" + expr.getIterator() + "' is not of type " + designator.getType().getElemType().getKind(), expr);
			return;
		}
		
		expr.struct = new Struct(Struct.Array, designator.getType().getElemType());
	}
	
	// ========================================================================
	// TERM
	// ========================================================================
	public void visit(TermMul term) {
		term.struct = term.getFactor().struct;
		
		analyzer.errorNotAssignable(term, term.getFactor().struct, Tab.intType);
		analyzer.errorNotAssignable(term, term.getTerm().struct, Tab.intType);
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
			analyzer.report_error("Condition expression types do not match (left: '" + structToString(condFact.getExpr().struct) + "', right: '" + structToString(condFact.getExpr1().struct) + "')", condFact);
			return;
		}
	}
	
	public void visit(CondFactExpr condFact) {
		condFact.struct = condFact.getExpr().struct;
		
		analyzer.errorNotAssignable(condFact, condFact.getExpr().struct, Tab.boolType);
	}
	
	
	// ========================================================================
	// DESIGNATOR
	// ========================================================================
	public void visit(DesignatorName designatorName) {
		designatorName.obj = Tab.find(designatorName.getName());
		
		analyzer.errorNotExists(designatorName, designatorName.getName());
	}
	
	public void visit(DesignatorVar designator) {
		designator.obj = Tab.find(designator.getDesignatorName().getName());
		
		if (analyzer.errorNotExists(designator, designator.getDesignatorName().getName())) return;
		
		SyntaxNode parent = designator.getParent();
		
		if (parent instanceof DesignatorStatementFunc || parent instanceof FactorFuncCall) {
			methodCalls.push(new ArrayList<>());
		}
	}
	
	public void visit(DesignatorArray designator) {
		designator.obj = Tab.find(designator.getDesignatorName().getName());
		
		if (
			analyzer.errorNotExists(designator, designator.getDesignatorName().getName())
		) return;
		
		if (designator.obj.getType().getKind() != Struct.Array) {
			analyzer.report_error("Designator is not an array (required: '" + structToString(Struct.Array) + "', got: '" + structToString(designator.obj.getType().getKind()) + "')", designator);
			return;
		}
		
		if (designator.getExpr().struct != Tab.intType) {
			analyzer.report_error("Array index is not of type int", designator);
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
			analyzer.report_error("Designator is not a variable", designatorStatement);
			return;
		}
		
		if (designatorObj.getType() != Tab.intType) {
			analyzer.report_error("Designator is not of type int", designatorStatement);
			return;
		}
	}
	
	public void visit(DesignatorStatementDec designatorStatement) {
		Obj designatorObj = designatorStatement.getDesignator().obj;
		
		if (designatorObj.getKind() != Obj.Var) {
			analyzer.report_error("Designator is not a variable", designatorStatement);
			return;
		}
		
		if (designatorObj.getType() != Tab.intType) {
			analyzer.report_error("Designator is not of type int", designatorStatement);
			return;
		}
	}
	
	public void visit(DesignatorStatementAssign designatorStatement) {
		Obj designatorStatementObj = designatorStatement.getDesignator().obj;
		
		if (designatorStatementObj.getKind() != Obj.Var && designatorStatementObj.getKind() != Obj.Elem) {
			analyzer.report_error("Designator is not a variable", designatorStatement);
			return;
		}
		
		if (!designatorStatement.getExpr().struct.assignableTo(designatorStatementObj.getType())) {
			analyzer.report_error("Designator is not of the same type as expression", designatorStatement);
			return;
		}
	}
	
	public void visit(DesignatorStatementFunc designatorStatement) {
		Obj designatorObj = designatorStatement.getDesignator().obj;
		
		if (designatorObj.getKind() != Obj.Meth) {
			analyzer.report_error("Designator is not a function", designatorStatement);
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
		if (!Arrays.asList(Tab.intType, Tab.charType, Tab.boolType).contains(exprType)) {
			analyzer.report_error("Print statement expression is not of type int, char or bool", printStmt);
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
			analyzer.report_error("Read statement designator is not a variable", statement);
			return;
		}
		
		if (!Arrays.asList(Tab.intType, Tab.charType, Tab.boolType).contains(statement.getDesignator().obj.getType())) {
			analyzer.report_error("Read statement designator is not of type int, char or bool", statement);
			return;
		}
	}
	
	// ========================================================================
	// RETURN
	// ========================================================================
	
	public void visit(ReturnItemExpr returnItem) {
		if (currentMethod == Tab.noObj) {
			analyzer.report_error("Return expression outside of method", returnItem);
			return;
		}
		
		if (currentMethod.getType() == Tab.noType) {
			analyzer.report_error("Return expression in void method", returnItem);
			return;
		}
		
		if (!currentMethod.getType().assignableTo(returnItem.getExpr().struct)) {
			analyzer.report_error("Return expression is not of the same type as method", returnItem);
			return;
		}
	}
	
	public void visit(ReturnItemVoid returnItem) {
		if (currentMethod == Tab.noObj) {
			analyzer.report_error("Return expression outside of method", returnItem);
			return;
		}
		
		if (currentMethod.getType() != Tab.noType) {
			analyzer.report_error("Return expression in non-void method", returnItem);
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
			analyzer.report_error("Break statement outside of while loop", breakStmt);
			return;
		}
	}
	
	public void visit(ContinueStmt continueStmt) {
		if (whileDepth == 0) {
			analyzer.report_error("Continue statement outside of while loop", continueStmt);
			return;
		}
	}
	
	// ========================================================================
	// METHODS
	// ========================================================================
	
	public void visit(MethodTypeName methodTypeName) {
		currentMethod = methodTypeName.obj = Tab.insert(Obj.Meth, methodTypeName.getName(), requiredType);
		methods.put(currentMethod, new ArrayList<>());
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
			analyzer.report_error("Formal parameter '" + formPar.getName() + "' already declared", formPar);
			return;
		}
		
		Struct type = formPar.getType().struct;
		methods.get(currentMethod).add(type);
		Tab.insert(Obj.Var, formPar.getName(), type);
	}
	
	public void visit(FormParArray formPar) {
		if (Tab.find(formPar.getName()) != Tab.noObj) {
			analyzer.report_error("Formal parameter '" + formPar.getName() + "' already declared", formPar);
			return;
		}
		
		Struct type = new Struct(Struct.Array, formPar.getType().struct);
		methods.get(currentMethod).add(type);
		Tab.insert(Obj.Var, formPar.getName(), type);
	}
	
	public void visit(MethodVarName methodVar) {
		if (Tab.find(methodVar.getName()) != Tab.noObj) {
			analyzer.report_error("Method variable '" + methodVar.getName() + "' already declared", methodVar);
			return;
		}
		
		Tab.insert(Obj.Var, methodVar.getName(), requiredType);
	}
	
	public void visit(MethodVarArray methodVar) {
		if (Tab.find(methodVar.getName()) != Tab.noObj) {
			analyzer.report_error("Method variable '" + methodVar.getName() + "' already declared", methodVar);
			return;
		}

		Tab.insert(Obj.Var, methodVar.getName(), new Struct(Struct.Array, requiredType));
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
	// CUSTOM FUNCTIONS
	// ========================================================================
	
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
	

}
