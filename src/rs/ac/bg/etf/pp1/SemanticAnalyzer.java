package rs.ac.bg.etf.pp1;

import rs.ac.bg.etf.pp1.ast.*;
import rs.etf.pp1.symboltable.concepts.Obj;

import org.apache.log4j.Logger;
import rs.etf.pp1.symboltable.concepts.Struct;

import java.util.Arrays;

public class SemanticAnalyzer extends VisitorAdaptor {
	
	Logger log = Logger.getLogger(MJParser.class);
	Obj currentMethod = Tab.noObj;
	Struct requiredType = null;
	int whileDepth = 0;
	
	public SemanticAnalyzer() {
		Tab.init();
	}
	
	private void report_error(String message, SyntaxNode info) {
		// errorDetected = true;
		
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
			report_error("Type '" + type.getTypeName() + "' not found in symbol table", null);
			return;
		}
		
		if (Obj.Type != typeNode.getKind()) {
			report_error("Name '" + type.getTypeName() + "' is not a type", type);
			return;
		}
		
		type.struct = typeNode.getType();
		requiredType = type.struct;
	}
	
	public void visit(ConstDecl constDecl) {
		if (Tab.find(constDecl.getName()) != Tab.noObj) {
			report_error("Name '" + constDecl.getName() + "' already in use", constDecl);
			return;
		}
		
		if (!constDecl.getConstValue().struct.assignableTo(requiredType)) {
			String message = "Type of constant '" + constDecl.getName() + "' does not match required type (required: '" + structToString(requiredType) + "', got: '" + structToString(constDecl.getConstValue().struct) + "')";
			report_error(message, constDecl);
			return;
		}
		
		Obj constNode = Tab.insert(Obj.Con, constDecl.getName(), requiredType);
		// constNode.setAdr(constDecl.getConstValue());
		constNode.setLevel(0);
		
		report_info("Inserted constant '" + constDecl.getName() + "'", constDecl);
	}
	
	public void visit(VarDeclName varDecl) {
		if (Tab.find(varDecl.getName()) != Tab.noObj) {
			report_error("Name '" + varDecl.getName() + "' already in use", varDecl);
			return;
		}
		
		Obj varNode = Tab.insert(Obj.Var, varDecl.getName(), requiredType);
		varNode.setLevel(0);
		
		report_info("Inserted variable '" + varDecl.getName() + "'", varDecl);
	}
	
	public void visit(VarDeclArray varDecl) {
		if (Tab.find(varDecl.getName()) != Tab.noObj) {
			report_error("Name '" + varDecl.getName() + "' already in use", varDecl);
			return;
		}
		
		Obj varNode = Tab.insert(Obj.Var, varDecl.getName(), new Struct(Struct.Array, requiredType));
		varNode.setLevel(0);
		
		report_info("Inserted array '" + varDecl.getName() + "'", varDecl);
	}
	
	public void visit(VarDeclMatrix varDecl) {
		if (Tab.find(varDecl.getName()) != Tab.noObj) {
			report_error("Name '" + varDecl.getName() + "' already in use", varDecl);
			return;
		}
		
		Obj varNode = Tab.insert(Obj.Var, varDecl.getName(), new Struct(Struct.Array, new Struct(Struct.Array, requiredType)));
		varNode.setLevel(0);
		
		report_info("Inserted matrix '" + varDecl.getName() + "'", varDecl);
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
		factor.struct = Tab.noType;
		
		if (factor.getExpr().struct != Tab.intType) {
			report_error("Array index is not of type int", factor);
			return;
		}
		
		factor.struct = new Struct(Struct.Array, factor.getType().struct);
	}
	
	public void visit(FactorMatrix factor) {
		factor.struct = Tab.noType;
		
		if (factor.getExpr().struct != Tab.intType) {
			report_error("Matrix index is not of type int", factor);
			return;
		}
		
		if (factor.getExpr1().struct != Tab.intType) {
			report_error("Matrix array index is not of type int", factor);
			return;
		}
		
		factor.struct = new Struct(Struct.Array, new Struct(Struct.Array, factor.getType().struct));
	}
	
	public void visit(FactorFuncCall factor) {
		factor.struct = Tab.noType;
		
		Obj designator = Tab.find(factor.getDesignator().obj.getName());
		
		if (designator == Tab.noObj) {
			report_error("Name '" + factor.getDesignator().obj.getName() + "' not found in symbol table", factor);
			return;
		}
		
		if (designator.getKind() != Obj.Meth) {
			report_error("Name '" + factor.getDesignator().obj.getName() + "' is not a function", factor);
			return;
		}
		
		factor.struct = designator.getType();
	}
	
	// ========================================================================
	// EXPR
	// ========================================================================
	public void visit(ExprTerm expr) {
		expr.struct = expr.getTerm().struct;
	}
	
	public void visit(ExprMinus expr) {
		expr.struct = Tab.noType;
		
		if (expr.getTerm().struct != Tab.intType) {
			report_error("Expression is not of type int", expr);
			return;
		}
		
		expr.struct = Tab.intType;
	}
	
	public void visit(ExprAdd expr) {
		expr.struct = Tab.noType;
		
		if (expr.getTerm().struct != Tab.intType) {
			report_error("Expression [term] is not of type int", expr);
			return;
		}
		
		if (expr.getExpr().struct != Tab.intType) {
			report_error("Expression [expr] is not of type int", expr);
			return;
		}
		
		expr.struct = Tab.intType;
	}
	
	public void visit(ExprMap expr) {
		expr.struct = Tab.noType;
		
		Obj designator = expr.getDesignator().obj;
		if (designator.getKind() != Obj.Var) {
			report_error("Designator is not a variable", expr);
			return;
		}
		
		if (designator.getType().getKind() != Struct.Array) {
			report_error("Designator is not an array (required: '" + structToString(Struct.Array) + "', got: '" + structToString(designator.getType().getKind()) + "')", expr);
			return;
		}
		
		if (designator.getType().getElemType().getKind() == Struct.Array) {
			report_error("Designator shouldn't be a matrix", expr);
			return;
		}
		
		Obj iterator = Tab.find(expr.getIterator());
		
		if (iterator == Tab.noObj) {
			report_error("Iterator '" + expr.getIterator() + "' not found in symbol table", expr);
			return;
		}
		
		if (iterator.getType() != designator.getType().getElemType()) {
			report_error("Iterator '" + expr.getIterator() + "' is not of type " + designator.getType().getElemType().getKind(), expr);
			return;
		}
		
		expr.struct = new Struct(Struct.Array, designator.getType().getElemType());
	}
	
	// ========================================================================
	// TERM
	// ========================================================================
	public void visit(TermMul term) {
		term.struct = Tab.noType;
		
		if (term.getFactor().struct != Tab.intType) {
			report_error("Term [factor] is not of type int", term);
			return;
		}
		
		if (term.getTerm().struct != Tab.intType) {
			report_error("Term [term] is not of type int", term);
			return;
		}
		
		term.struct = term.getFactor().struct;
	}
	
	public void visit(TermFactor term) {
		term.struct = term.getFactor().struct;
	}
	
	// ========================================================================
	// CONDITION
	// ========================================================================
	public void visit(CondFactRel condFact) {
		if (condFact.getExpr().struct.compatibleWith(condFact.getExpr1().struct)) {
			report_error("Condition expression types do not match", condFact);
			return;
		}
		
		condFact.struct = Tab.boolType;
	}
	
	public void visit(CondFactExpr condFact) {
		condFact.struct = condFact.getExpr().struct;
		
		if (condFact.struct != Tab.boolType) {
			report_error("Condition expression is not of type bool", condFact);
			return;
		}
	}
	
	
	// ========================================================================
	// DESIGNATOR
	// ========================================================================
	public void visit(DesignatorVar designator) {
		designator.obj = Tab.find(designator.getName());
		
		if (designator.obj == Tab.noObj) {
			report_error("Name '" + designator.getName() + "' not found in symbol table", designator);
			return;
		}
		
		if (!Arrays.asList(Obj.Var, Obj.Con).contains(designator.obj.getKind())) {
			report_error("Name '" + designator.getName() + "' is not a variable or constant (required: '" + objToString(Obj.Var) + "', got: '" + objToString(designator.obj) + "')", designator);
			return;
		}
	}
	
	public void visit(DesignatorArray designator) {
		designator.obj = Tab.find(designator.getName());
		
		if (designator.obj == Tab.noObj) {
			report_error("Name '" + designator.getName() + "' not found in symbol table", designator);
			return;
		}
		
		if (designator.obj.getType().getKind() != Struct.Array) {
			report_error("Designator is not an array (required: '" + structToString(Struct.Array) + "', got: '" + structToString(designator.obj.getType().getKind()) + "')", designator);
			return;
		}
		
		if (designator.getExpr().struct != Tab.intType) {
			report_error("Array index is not of type int", designator);
			return;
		}
		
		designator.obj = new Obj(Obj.Var, designator.obj.getName(), designator.obj.getType().getElemType());
	}
	
	public void visit(DesignatorMatrix designator) {
		designator.obj = Tab.find(designator.getName());
		
		if (designator.obj == Tab.noObj) {
			report_error("Name '" + designator.getName() + "' not found in symbol table", designator);
			return;
		}
		
		if (designator.obj.getType().getKind() != Struct.Array || designator.obj.getType().getElemType().getKind() != Struct.Array) {
			report_error("Name '" + designator.getName() + "' is not a matrix 1", designator);
			return;
		}
		
		designator.obj = new Obj(Obj.Var, designator.obj.getName(), designator.obj.getType().getElemType());
		
		if (designator.obj.getType().getKind() != Struct.Array) {
			report_error("Name '" + designator.getName() + "' is not a matrix 2", designator);
			return;
		}
		
		designator.obj = new Obj(Obj.Var, designator.obj.getName(), designator.obj.getType().getElemType());
		
		if (designator.getExpr().struct != Tab.intType) {
			report_error("Matrix index is not of type int", designator);
			return;
		}
		
		if (designator.getExpr1().struct != Tab.intType) {
			report_error("Matrix array index is not of type int", designator);
			return;
		}
	}
	
	// ========================================================================
	// DESIGNATOR STATEMENT
	// ========================================================================
	
	public void visit(DesignatorStatementInc designatorStatement) {
		Obj designatorObj = designatorStatement.getDesignator().obj;
		
		if (designatorObj.getKind() != Obj.Var) {
			report_error("Designator is not a variable", designatorStatement);
			return;
		}
		
		if (designatorObj.getType() != Tab.intType) {
			report_error("Designator is not of type int", designatorStatement);
			return;
		}
	}
	
	public void visit(DesignatorStatementDec designatorStatement) {
		Obj designatorObj = designatorStatement.getDesignator().obj;
		
		if (designatorObj.getKind() != Obj.Var) {
			report_error("Designator is not a variable", designatorStatement);
			return;
		}
		
		if (designatorObj.getType() != Tab.intType) {
			report_error("Designator is not of type int", designatorStatement);
			return;
		}
	}
	
	public void visit(DesignatorStatementAssign designatorStatement) {
		Obj designatorStatementObj = designatorStatement.getDesignator().obj;
		
		if (designatorStatementObj.getKind() != Obj.Var) {
			report_error("Designator is not a variable", designatorStatement);
			return;
		}
		
		if (!designatorStatement.getExpr().struct.assignableTo(designatorStatementObj.getType())) {
			report_error("Designator is not of the same type as expression", designatorStatement);
			return;
		}
	}
	
	public void visit(DesignatorStatementFunc designatorStatement) {
		if (designatorStatement.getDesignator().obj.getKind() != Obj.Meth) {
			report_error("Designator is not a function", designatorStatement);
			return;
		}
	}
	
	// ========================================================================
	// PRINT READ
	// ========================================================================

	public void visit(PrintStmt statement) {
		Struct exprType = statement.getExpr().struct;
		if (!Arrays.asList(Tab.intType, Tab.charType, Tab.boolType).contains(exprType)) {
			report_error("Print statement expression is not of type int, char or bool", statement);
			return;
		}
	}
	
	public void visit(ReadStmt statement) {
		if (statement.getDesignator().obj.getKind() != Obj.Var) {
			report_error("Read statement designator is not a variable", statement);
			return;
		}
		
		if (!Arrays.asList(Tab.intType, Tab.charType, Tab.boolType).contains(statement.getDesignator().obj.getType())) {
			report_error("Read statement designator is not of type int, char or bool", statement);
			return;
		}
	}
	
	// ========================================================================
	// RETURN
	// ========================================================================
	
	public void visit(ReturnItemExpr returnItem) {
		if (currentMethod == Tab.noObj) {
			report_error("Return expression outside of method", returnItem);
			return;
		}
		
		if (currentMethod.getType() == Tab.noType) {
			report_error("Return expression in void method", returnItem);
			return;
		}
		
		if (!currentMethod.getType().assignableTo(returnItem.getExpr().struct)) {
			report_error("Return expression is not of the same type as method", returnItem);
			return;
		}
	}
	
	public void visit(ReturnItemVoid returnItem) {
		if (currentMethod == Tab.noObj) {
			report_error("Return expression outside of method", returnItem);
			return;
		}
		
		if (currentMethod.getType() != Tab.noType) {
			report_error("Return expression in non-void method", returnItem);
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
			report_error("Break statement outside of while loop", breakStmt);
			return;
		}
	}
	
	public void visit(ContinueStmt continueStmt) {
		if (whileDepth == 0) {
			report_error("Continue statement outside of while loop", continueStmt);
			return;
		}
	}
	
	// ========================================================================
	// METHODS
	// ========================================================================
	
	public void visit(MethodTypeName methodTypeName) {
		currentMethod = methodTypeName.obj = Tab.insert(Obj.Meth, methodTypeName.getName(), requiredType);
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
			report_error("Formal parameter already declared", formPar);
			return;
		}
		
		Struct type = formPar.getType().struct;
		Tab.insert(Obj.Var, formPar.getName(), type);
	}
	
	public void visit(FormParArray formPar) {
		if (Tab.find(formPar.getName()) != Tab.noObj) {
			report_error("Formal parameter already declared", formPar);
			return;
		}
		
		Struct type = new Struct(Struct.Array, formPar.getType().struct);
		Tab.insert(Obj.Var, formPar.getName(), type);
	}
	
	public void visit(FormParMatrix formPar) {
		if (Tab.find(formPar.getName()) != Tab.noObj) {
			report_error("Formal parameter already declared", formPar);
			return;
		}
		
		Struct type = new Struct(Struct.Array, new Struct(Struct.Array, formPar.getType().struct));
		Tab.insert(Obj.Var, formPar.getName(), type);
	}
	
	public void visit(MethodVarName methodVar) {
		if (Tab.find(methodVar.getName()) != Tab.noObj) {
			report_error("Method variable already declared", methodVar);
			return;
		}
		
		Tab.insert(Obj.Var, methodVar.getName(), requiredType);
	}
	
	public void visit(MethodVarArray methodVar) {
		if (Tab.find(methodVar.getName()) != Tab.noObj) {
			report_error("Method variable already declared", methodVar);
			return;
		}

		Tab.insert(Obj.Var, methodVar.getName(), new Struct(Struct.Array, requiredType));
	}
	
	public void visit(MethodVarMatrix methodVar) {
		if (Tab.find(methodVar.getName()) != Tab.noObj) {
			report_error("Method variable already declared", methodVar);
			return;
		}
		
		Tab.insert(Obj.Var, methodVar.getName(), new Struct(Struct.Array, new Struct(Struct.Array, requiredType)));
	}
	
	// ========================================================================
	// HELPER FUNCTIONS
	// ========================================================================
	
	String structToString(int struct) {
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
	
	String structToString(Struct struct) {
		return structToString(struct.getKind());
	}
	
	String objToString(int obj) {
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
	
	String objToString(Obj obj) {
		return objToString(obj.getKind());
	}
}