package rs.ac.bg.etf.pp1;

import rs.ac.bg.etf.pp1.ast.*;
import rs.ac.bg.etf.pp1.util.Analyzer;
import rs.etf.pp1.symboltable.concepts.Obj;

import java.io.File;
import java.nio.file.Files;
import java.util.Stack;


public class CodeGenerator extends VisitorAdaptor {
	
	private final Analyzer analyzer = Analyzer.getInstance();
	
	public void compile(Program program) throws Exception {
		File objFile = new File("test/program.obj");
		if (objFile.exists()) objFile.delete();
		
		program.traverseBottomUp(this);
		Code.write(Files.newOutputStream(objFile.toPath()));
	}
	
	public void visit(ProgName progName) {
		addFunc("chr", 1, 0, () -> {
			Code.put(Code.load_n);
		});
		
		addFunc("ord", 1, 0, () -> {
			Code.put(Code.load_n);
		});
		
		addFunc("len", 1, 0, () -> {
			Code.put(Code.load_n);
			Code.put(Code.arraylength);
		});
		
		addFunc("add", 2, 0, () -> {
			Code.put(Code.load_n); // Set address
			Code.put(Code.load_1); // Element to add
			
			Code.setAddElem();
		});
		
		addFunc("addAll", 2, 0, () -> {
			Code.put(Code.load_n); // Set address
			Code.put(Code.load_1); // Array address to add
			
			// setAdr, arrAdr
			
			Code.put(Code.dup); // setAdr, arrAdr, arrAdr
			Code.put(Code.arraylength); // setAdr, arrAdr, arrSize
			
			Code.For(() -> { // setAdr, arrAdr, index
				Code.dupli(1); // setAdr, arrAdr, index, arrAdr
				Code.swap(0, 1); // setAdr, arrAdr, arrAdr, index
				Code.put(Code.aload); // setAdr, arrAdr, elem
				Code.swap(1, 2); // arrAdr, setAdr, elem
				Code.dupli(1); // arrAdr, setAdr, elem, setAdr
				Code.swap(0, 1); // arrAdr, setAdr, setAdr, elem
				Code.setAddElem(); // arrAdr, setAdr
				Code.swap(0, 1); // setAdr, arrAdr
			});
			
			Code.put(Code.pop);
			Code.put(Code.pop);
		});
		
		addFunc("minSet", 1, 0, () -> {
			Code.put(Code.load_n); // Set address
			Code.put(Code.dup); // setAdr, setAdr
			Code.getSize(Code.DataStructure.SET); // setAdr, setSize
			Code.loadConst(999); // setAdr, setSize, currMin
			
			Code.swap(0, 1); // setAdr, currMin, setSize
			Code.For(() -> { // setAdr, currMin, index
				Code.swap(1, 2); // currMin, setAdr, index
				Code.dupli(1); // currMin, setAdr, index, setAdr
				Code.swap(0, 1); // currMin, setAdr, setAdr, index
				Code.getElem(Code.DataStructure.SET); // currMin, setAdr, elem
				Code.swap(1, 2); // setAdr, currMin, elem
				Code.min(); // setAdr, currMin
			});
			
			Code.swap(0, 1); // currMin, setAdr
			Code.put(Code.pop); // Pop the set address
		});
		
		addFunc("ifTest", 0, 0, () -> {
			Code.loadConst(5);
			Code.loadConst(5);
			Code.put(Code.dup2);
			Code.If(Code.eq, () -> {
				Code.loadConst(100);
				Code.loadConst(1);
				Code.put(Code.print);
			});
			
			Code.If(Code.lt, () -> {
				Code.loadConst(200);
				Code.loadConst(1);
				Code.put(Code.print);
			}).Else(() -> {
				Code.loadConst(300);
				Code.loadConst(1);
				Code.put(Code.print);
			});
		});
		
		addFunc("forTest", 0, 0, () -> {
			Code.loadConst(5);
			Code.For(() -> { // index1
				Code.loadConst(5);
				Code.loadConst(9);
				Code.ForFromTo(-1, () -> { // index1, index2
					Code.loadConst(7);
					Code.For(() -> { // index1, index2, index3
						Code.loadConst(1);
						Code.put(Code.print);
						
						Code.loadConst('-');
						Code.loadConst(1);
						Code.put(Code.bprint);
						
						Code.dupli(0); // index1, index2, index2
						Code.loadConst(1);
						Code.put(Code.print); // index1, index2
						
						Code.loadConst('-');
						Code.loadConst(1);
						Code.put(Code.bprint);
						
						Code.swap(0, 1); // index2, index1
						
						Code.dupli(0); // index2, index1, index1
						
						Code.loadConst(1);
						Code.put(Code.print); // index2, index1
						Code.swap(0, 1);
						
						Code.printEol();
					});
					Code.put(Code.pop);
				});
				Code.put(Code.pop);
			});
		});
		
		addFunc("sort", 1, 0, () -> {
			Code.put(Code.load_n);
			Code.sort(Code.DataStructure.ARRAY);
		});
	}
	
	private void addFunc(String name, int formPars, int localVars, Runnable action) {
		Obj func = Tab.find(name);
		func.setAdr(Code.pc);
		
		analyzer.report_info(null, "BEGIN METHOD " + name + " | PARS: " + formPars + ", VARS: " + localVars + " AT ADDR: " + func.getAdr());
		
		Code.enter(formPars, localVars);
		
		action.run();
		
		analyzer.report_info(null, "END METHOD " + name);
		
		Code.exitReturn();
	}
	
	Stack<Integer> fixUps = new Stack<>();
	
	// ======================================== //
	// PRINT READ
	// ======================================== //
	
	public void visit(PrintStatementOptionalNo printStatementOptional) {
		if (printStatementOptional.getExpr().struct == Tab.charType) {
			analyzer.report_info(printStatementOptional, "CODE LOAD PRINT NO WIDTH CONST: 1");
			Code.loadConst(1);
			analyzer.report_info(printStatementOptional, "CODE PUT PRINT NO WIDTH: BPRINT");
			Code.put(Code.bprint);
		} else if (printStatementOptional.getExpr().struct == Tab.intType) {
			analyzer.report_info(printStatementOptional, "CODE LOAD PRINT NO WIDTH CONST: 1");
			Code.loadConst(1);
			analyzer.report_info(printStatementOptional, "CODE PUT PRINT NO WIDTH: PRINT");
			Code.put(Code.print);
		} else if (printStatementOptional.getExpr().struct == Tab.setType) {
			analyzer.report_info(printStatementOptional, "CODE LOAD PRINT NO WIDTH CONST: 1");
			
			Code.put(Code.dup); // setAdr, setAdr
			Code.getSize(Code.DataStructure.SET); // setAdr, setSize
			Code.For(() -> { // setAdr, index
				Code.dupli(1); // setAdr, index, setAdr
				Code.swap(0, 1); // setAdr, setAdr, index
				Code.getElem(Code.DataStructure.SET); // setAdr, elem
				
				Code.loadConst(0);
				Code.put(Code.print);
				
				Code.loadConst(' ');
				Code.loadConst(1);
				Code.put(Code.bprint);
			});
			
			Code.put(Code.pop); // Pop the set address
		} else {
			analyzer.report_error(printStatementOptional, "Unsupported type for print statement");
		}
	}
	
	public void visit(PrintStatementOptionalYes printStatementOptional) {
		analyzer.report_info(printStatementOptional, "CODE LOAD CONST: " + printStatementOptional.getWidth());
		Code.loadConst(printStatementOptional.getWidth());
		
		int code = printStatementOptional.getExpr().struct == Tab.charType ? Code.bprint : Code.print;
		analyzer.report_info(printStatementOptional, "CODE PUT: " + codeToString(code));
		Code.put(code);
	}
	
	public void visit(ReadStmt readStmt) {
		int code = readStmt.getDesignator().obj.getType() == Tab.charType ? Code.bread : Code.read;
		
		analyzer.report_info(readStmt, "CODE PUT: " + codeToString(code));
		Code.put(code);
		analyzer.report_info(readStmt, "CODE STORE: " + readStmt.getDesignator().obj.getName());
		Code.store(readStmt.getDesignator().obj);
	}
	
	
	// ======================================== //
	// CONST LOAD
	// ======================================== //
	
	public void visit(ConstInt constInt) {
		analyzer.report_info(constInt, "LOAD CONST: " + constInt.getN1());
		
		Obj con = Tab.insert(Obj.Con, "$", constInt.struct);
		con.setLevel(0);
		con.setAdr(constInt.getN1());
		
		Code.load(con);
	}
	
	public void visit(ConstChar constChar) {
		analyzer.report_info(constChar, constChar.getC1().toString());
		
		Obj con = Tab.insert(Obj.Con, "$", constChar.struct);
		con.setLevel(0);
		con.setAdr(constChar.getC1());
		
		Code.load(con);
	}
	
	public void visit(ConstBool constBool) {
		analyzer.report_info(constBool, constBool.getB1() ? "True" : "False");
		
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
		
		CounterVisitor.FormParamCounter formParCnt = new CounterVisitor.FormParamCounter();
		methodNode.traverseTopDown(formParCnt);
		
		CounterVisitor.VarCounter varCnt = new CounterVisitor.VarCounter();
		methodNode.traverseTopDown(varCnt);
		
		analyzer.report_info(methodTypeName, "BEGIN METHOD " + methodTypeName.getName() + " | PARS: " + formParCnt.getCount() + ", VARS: " + varCnt.getCount());
		
		Code.enter(formParCnt.getCount(), varCnt.getCount());
	}
	
	public void visit(MethodDecl methodDecl) {
		analyzer.report_info(methodDecl, "END METHOD " + methodDecl.getMethodTypeName().getName());
		
		Code.exitReturn();
	}
	
	// ======================================== //
	// DESIGNATOR STATEMENT
	// ======================================== //
	public void visit(DesignatorStatementAssign designatorStatement) {
		analyzer.report_info(designatorStatement, "ASSIGN TO: " + designatorStatement.getDesignator().obj.getName());
		
		Obj designatorObj = designatorStatement.getDesignator().obj;
		
		// for non-arrays its okay, for arrays it expects array address, index and value on stack
		Code.store(designatorObj);
	}
	
	public void visit(DesignatorName designatorName) {
		// load only for arrays because vars don't need address and index on stack
		if (designatorName.getParent() instanceof DesignatorVar) {
			analyzer.report_info(designatorName, "SKIPPING LOAD FOR PARENT " + designatorName.getParent().getClass().getSimpleName());
			return;
		}
		
		analyzer.report_info(designatorName, "DESIGNATOR LOADED FROM: " + designatorName.obj.getName());
		
		// for non-arrays its okay, for arrays it expects array address and index on stack
		Code.load(designatorName.obj);
	}
	
	public void visit(DesignatorVar designator) {
		SyntaxNode parent = designator.getParent();
		
		// Load only if designator is used in an expression, not as a target of an assignment
		if (parent instanceof DesignatorStatementInc) {
			analyzer.report_info(designator, "VAR LOAD: Inc");
			Code.load(designator.obj);
			Code.addNum(1);
			Code.store(designator.obj);
		} else if (parent instanceof DesignatorStatementDec) {
			analyzer.report_info(designator, "VAR LOAD: Dec");
			Code.load(designator.obj);
			Code.addNum(-1);
			Code.store(designator.obj);
		} else if (parent instanceof FactorVar) {
			analyzer.report_info(designator, "VAR LOAD: FactorVar");
			Code.load(designator.obj);
		} else if (parent instanceof FactorVarInc) {
			analyzer.report_info(designator, "VAR LOAD: FactorVarInc");
			Code.load(designator.obj);
			Code.put(Code.dup); // Duplicate because we need original value for expression use
			Code.addNum(1);
			Code.store(designator.obj);
		} else if (parent instanceof FactorVarDec) {
			analyzer.report_info(designator, "VAR LOAD: FactorVarDec");
			Code.put(Code.dup);
			Code.addNum(-1);
			Code.store(designator.obj);
		} else analyzer.report_info(designator, "VAR LOAD SKIP, because of parent " + parent.getClass().getSimpleName());
	}
	
	public void visit(DesignatorArray designator) {
		analyzer.report_info(designator, "Designator kind is: " + objKindToString(designator.obj.getKind()));
		
		// when we arrive here, the array address and index are already on stack
		if (designator.getParent() instanceof DesignatorStatementInc) {
			analyzer.report_info(designator, "ARRAY LOAD: Inc");
			// arr, ind
			Code.put(Code.dup2); // arr, ind, arr, ind
			Code.put(Code.aload); // arr, ind, val
			Code.addNum(1); // arr, ind, val+1
			Code.put(Code.astore); // ...
		} else if (designator.getParent() instanceof DesignatorStatementDec) {
			analyzer.report_info(designator, "ARRAY LOAD: Dec");
			Code.put(Code.dup2);
			Code.put(Code.aload); // arr, ind, val
			Code.addNum(-1); // arr, ind, val-1
			Code.put(Code.astore); // ...
		} else if (designator.getParent() instanceof FactorVar) {
			analyzer.report_info(designator, "ARRAY LOAD: FactorVar");
			Code.load(designator.obj);
		} else if (designator.getParent() instanceof FactorVarInc) {
			analyzer.report_info(designator, "ARRAY LOAD: FactorVarInc");
			// arr, ind
			Code.put(Code.dup2); // arr, ind, arr, ind
			Code.put(Code.aload); // arr, ind, val
			Code.swap(0, 2, 1); // val, arr, ind
			Code.put(Code.dup2); // val, arr, ind, arr, ind
			Code.put(Code.aload); // val, arr, ind, val
			Code.addNum(1); // val, arr, ind, val+1
			Code.put(Code.astore); // val
			
		} else if (designator.getParent() instanceof FactorVarDec) {
			analyzer.report_info(designator, "ARRAY LOAD: FactorVarInc");
			// arr, ind
			Code.put(Code.dup2); // arr, ind, arr, ind
			Code.put(Code.aload); // arr, ind, val
			Code.swap(0, 2, 1); // val, arr, ind
			Code.put(Code.dup2); // val, arr, ind, arr, ind
			Code.put(Code.aload); // val, arr, ind, val
			Code.addNum(-1); // val, arr, ind, val+1
			Code.put(Code.astore); // val
			
		} else analyzer.report_info(designator, "ARRAY LOAD SKIP, because of parent " + designator.getParent().getClass().getSimpleName());
	}
	
	public void visit(DesignatorStatementFunc designatorStatement) {
		analyzer.report_info(designatorStatement, "FUNC CALL: " + designatorStatement.getDesignator().obj.getName());
		
		Obj func = designatorStatement.getDesignator().obj;
		
		if (func.getType() == Tab.noType) {
			analyzer.report_error(designatorStatement, "Function '" + func.getName() + "' has no return type");
		}
		
		int offset = func.getAdr() - Code.pc;
		Code.put(Code.call);
		Code.put2(offset);
		
		if (func.getType() != Tab.noType) {
			Code.put(Code.pop);
		}
	}
	
	// ======================================== //
	// FACTOR
	// ======================================== //
	
	public void visit(FactorFuncCall designatorStatement) {
		analyzer.report_info(designatorStatement, "FACTOR FUNC CALL: " + designatorStatement.getDesignator().obj.getName() + " AT ADDR: " + designatorStatement.getDesignator().obj.getAdr());
		
		Obj func = designatorStatement.getDesignator().obj;
		
		if (func.getType() == Tab.noType) {
			analyzer.report_error(designatorStatement, "Function '" + func.getName() + "' has no return type");
		}
		
		int offset = func.getAdr() - Code.pc;
		Code.put(Code.call);
		Code.put2(offset);
	}
	
	public void visit(ReturnStmt statement) {
		analyzer.report_info(statement, "RETURN");
		Code.exitReturn();
	}
	
	public void visit(FactorNewArray factor) {
		if (factor.struct.getElemType() == Tab.charType) {
			analyzer.report_info(factor, "ARRAY CREATE: Char");
			Code.put(Code.newarray);
			Code.put(0);
		} else if (factor.struct.getElemType() == Tab.intType) {
			analyzer.report_info(factor, "ARRAY CREATE: Int");
			Code.put(Code.newarray);
			Code.put(1);
		} else if (factor.struct == Tab.setType) {
			analyzer.report_info(factor, "ARRAY CREATE: Set");
			Code.put(Code.const_1);
			Code.put(Code.add);
			Code.put(Code.newarray);
			Code.put(1);
		}
	}
	
	// ======================================== //
	// EXPRESSION TERM
	// ======================================== //
	
	public void visit(ExprAdd expr) {
		SyntaxNode operation = expr.getAddOp();
		
		boolean isPlus = operation instanceof PlusAO;
		
		analyzer.report_info(expr, "ADD: " + (isPlus ? "Plus" : "Minus"));
		
		int code = isPlus ? Code.add : Code.sub;
		Code.put(code);
	}
	
	public void visit(ExprMinus expr) {
		analyzer.report_info(expr, "UNARY MINUS");
		Code.put(Code.neg);
	}
	
	public void visit(ExprSet expr) {
		analyzer.report_info(expr, "SET UNION");
		
		// Expected stack: ..., set2, set1
		
		Code.swap(0, 1); // ..., set1, set2
		Code.put(Code.dup2); // ..., set1, set2, set1, set2
		
		Code.getSize(Code.DataStructure.SET); // ..., set1, set2, set1, sizeSet2
		Code.swap(0, 1); // ..., set1, set2, sizeSet2, set1
		Code.getSize(Code.DataStructure.SET); // ..., set1, set2, sizeSet2, sizeSet1
		
		Code.put(Code.add); // ..., set1, set2, sizeSet1 + sizeSet2
		Code.loadConst(1);
		Code.put(Code.add); // ..., set1, set2, sizeSet1 + sizeSet2 + 1
		Code.put(Code.newarray);
		Code.put(1); // ..., set1, set2, newSet
		
		Code.swap(0, 1); // ..., set1, newSet, set2
		
		Code.dupli(0); // ..., set1, newSet, set2, set2
		Code.getSize(Code.DataStructure.SET); // ..., set1, newSet, set2, sizeSet2
		Code.For(() -> { // ..., set1, newSet, set2, index);
			Code.dupli(1); // ..., set1, newSet, set2, index, set2
			Code.swap(0, 1); // ..., set1, newSet, set2, set2, index
			Code.getElem(Code.DataStructure.SET); // ..., set1, newSet, set2, elem
			Code.swap(1, 2); // ..., set1, set2, newSet, elem
			Code.dupli(1); // ..., set1, set2, newSet, elem, newSet
			Code.swap(0, 1); // ..., set1, set2, newSet, newSet, elem
			
			Code.setAddElem(); // ..., set1, set2, newSet
			Code.swap(0, 1); // ..., set1, newSet, set2
		});
		
		Code.put(Code.pop); // set1, newSet
		
		Code.dupli(1); // set1, newSet, set1
		Code.getSize(Code.DataStructure.SET); // set1, newSet, sizeSet1
		Code.swap(1, 2); // newSet, set1, sizeSet1
		Code.For(() -> { // newSet, set1, index
			Code.dupli(1); // newSet, set1, index, set1
			Code.swap(0, 1); // newSet, set1, set1, index
			Code.getElem(Code.DataStructure.SET); // newSet, set1, elem
			Code.swap(1, 2); // set1, newSet, elem
			Code.dupli(1); // set1, newSet, elem, newSet
			Code.swap(0, 1); // set1, newSet, newSet, elem
			
			Code.setAddElem(); // set1, newSet
			Code.swap(0, 1); // newSet, set1
		});
		
		Code.put(Code.pop); // Pop the set1 address
	}
	
	public void visit(TermMul term) {
		SyntaxNode operation = term.getMulOp();
		
		int code = operation instanceof MulMO ? Code.mul : operation instanceof DivMO ? Code.div : Code.rem;
		
		String codeText = operation instanceof MulMO ? "MUL" : operation instanceof DivMO ? "DIV" : "MOD";
		
		analyzer.report_info(term, "MUL: " + codeText);
		
		Code.put(code);
	}
	
	// ======================================== //
	// CONDITION
	// ======================================== //
	
	public void visit(CondFactRel condFact) {
		SyntaxNode operation = condFact.getRelOp();
		
		int code;
		
		if (operation instanceof EqualRO) code = Code.eq;
		else if (operation instanceof NotEqualRO) code = Code.ne;
		else if (operation instanceof GreaterRO) code = Code.gt;
		else if (operation instanceof GreaterEqualRO) code = Code.ge;
		else if (operation instanceof LessRO) code = Code.lt;
		else code = Code.le;
		
		analyzer.report_info(condFact, "REL: " + operation.getClass().getSimpleName());
		
		Code.putFalseJump(code, 0);
	}
	
	// ======================================== //
	// JUMPS
	// ======================================== //
	
	public void visit(ConditionOr condition) {
		analyzer.report_info(condition, "OR");
	}
	
	public void visit(IfStmt statement) {
		analyzer.report_info(statement, "IF");
		
		Code.fixup(fixUps.pop());
	}
	
	public void visit(ElseStmt statement) {
		analyzer.report_info(statement, "ELSE");
		
		Code.fixup(fixUps.pop());
	}
	
	public void visit(IfBase ifBase) {
		analyzer.report_info(ifBase, "IF BASE");
		
		Code.loadConst(0);
		Code.putFalseJump(Code.gt, 0);
		fixUps.push(Code.pc - 2);
	}
	
	public void visit(ElseBase elseBase) {
		analyzer.report_info(elseBase, "ELSE BASE");
		
		Code.putJump(0);
		Code.fixup(fixUps.pop());
		fixUps.push(Code.pc - 2);
	}
	
	// ======================================== //
	// CUSTOM FUNCTIONS
	// ======================================== //
	
	public static String objKindToString(int kind) {
		switch (kind) {
			case Obj.Con:
				return "Constant";
			case Obj.Type:
				return "Type";
			case Obj.Var:
				return "Variable";
			case Obj.Fld:
				return "Field";
			case Obj.Meth:
				return "Method";
			case Obj.Prog:
				return "Program";
			case Obj.Elem:
				return "Element";
			default:
				return "unknown";
		}
	}
	
	public static String codeToString(int code) {
		switch (code) {
			case 1:
				return "LOAD";
			case 2:
				return "LOAD_N";
			case 3:
				return "LOAD_1";
			case 4:
				return "LOAD_2";
			case 5:
				return "LOAD_3";
			case 6:
				return "STORE";
			case 7:
				return "STORE_N";
			case 8:
				return "STORE_1";
			case 9:
				return "STORE_2";
			case 10:
				return "STORE_3";
			case 11:
				return "GETSTATIC";
			case 12:
				return "PUTSTATIC";
			case 13:
				return "GETFIELD";
			case 14:
				return "PUTFIELD";
			case 15:
				return "CONST_N";
			case 16:
				return "CONST_1";
			case 17:
				return "CONST_2";
			case 18:
				return "CONST_3";
			case 19:
				return "CONST_4";
			case 20:
				return "CONST_5";
			case 21:
				return "CONST_M1";
			case 22:
				return "CONST_";
			case 23:
				return "ADD";
			case 24:
				return "SUB";
			case 25:
				return "MUL";
			case 26:
				return "DIV";
			case 27:
				return "REM";
			case 28:
				return "NEG";
			case 29:
				return "SHL";
			case 30:
				return "SHR";
			case 31:
				return "INC";
			case 32:
				return "NEW_";
			case 33:
				return "NEWARRAY";
			case 34:
				return "ALOAD";
			case 35:
				return "ASTORE";
			case 36:
				return "BALOAD";
			case 37:
				return "BASTORE";
			case 38:
				return "ARRAYLENGTH";
			case 39:
				return "POP";
			case 40:
				return "DUP";
			case 41:
				return "DUP2";
			case 42:
				return "JMP";
			case 43:
				return "JCC";
			case 44:
				return "JCC_1";
			case 45:
				return "JCC_2";
			case 46:
				return "JCC_3";
			case 47:
				return "JCC_4";
			case 48:
				return "JCC_5";
			case 49:
				return "CALL";
			case 50:
				return "RETURN_";
			case 51:
				return "ENTER";
			case 52:
				return "EXIT";
			case 53:
				return "READ";
			case 54:
				return "PRINT";
			case 55:
				return "BREAD";
			case 56:
				return "BPRINT";
			case 57:
				return "TRAP";
			case 58:
				return "INVOKEVIRTUAL";
			case 59:
				return "DUP_X1";
			case 60:
				return "DUP_X2";
			default:
				return "UNKNOWN";
		}
	}
	
	
}

