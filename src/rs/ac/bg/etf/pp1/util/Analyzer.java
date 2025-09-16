package rs.ac.bg.etf.pp1.util;

import org.apache.log4j.Logger;
import rs.ac.bg.etf.pp1.MJParser;
import rs.ac.bg.etf.pp1.Tab;
import rs.ac.bg.etf.pp1.ast.SyntaxNode;
import rs.etf.pp1.mj.runtime.Code;
import rs.etf.pp1.symboltable.concepts.Obj;
import rs.etf.pp1.symboltable.concepts.Struct;

import java.util.List;

public class Analyzer {
	private boolean errorDetected = false;
	private final Logger logger;
	
	public Analyzer() {
		logger = Logger.getLogger(MJParser.class);
	}
	
	private static Analyzer instance = null;
	
	public static Analyzer getInstance() {
		if (instance == null) {
			instance = new Analyzer();
		}
		return instance;
	}
	
	private String formatMessage(String message, SyntaxNode info) {
		// Extract parent information all the way to the root
		String parentInfo = "";
		SyntaxNode parent = info;
		while (parent != null) {
			if (parent.getParent() == null) {
				break;
			}
			parent = parent.getParent();
			
			// Don't include if it has 'Derived' in its name
			if (parent.getClass().getSimpleName().contains("Derived")) {
				continue;
			}
			
			parentInfo = parent.getClass().getSimpleName() + " > " + parentInfo;
		}
		
		if (info == null) {
			return "(Built-In, PC " + Code.pc + ") " + message;
		}
		
		return "(Line " + info.getLine() + ", PC " + Code.pc + ") [" + parentInfo + info.getClass().getSimpleName() + "]: " + message;
	}
	
	public void report_error(SyntaxNode info, String message) {
		errorDetected = true;
		
		logger.error(formatMessage(message, info));
	}
	
	public void report_info(SyntaxNode info, String message) {
		logger.info(formatMessage(message, info));
	}
	
	public boolean isErrorDetected() {
		return errorDetected;
	}
	
	public boolean ifErrorNotExists(SyntaxNode node, String name) {
		Obj obj = Tab.find(name);
		
		if (obj == Tab.noObj) {
			report_error(node, "Symbol '" + name + "' is not declared");
			return true;
		}
		
		return false;
	}
	
	public boolean errorAlreadyExists(SyntaxNode node, String name) {
		Obj obj = Tab.find(name);
		
		if (obj != Tab.noObj) {
			report_error(node, "Symbol '" + name + "' is already declared as " + objKindToString(obj));
			return true;
		}
		
		return false;
	}
	
	public boolean errorObjWrongKind(SyntaxNode node, String name, int kind) {
		Obj obj = Tab.find(name);
		
		if (obj.getKind() != kind) {
			report_error(node, "Symbol '" + name + "' is not a " + objKindToString(obj));
			return true;
		}
		
		return false;
	}
	
	public boolean isErrorStructWrongKind(SyntaxNode node, Struct struct, Struct kind) {
		if (struct != kind) {
			report_error(node, "Symbol '" + structKindToString(struct) + "' is not a " + structKindToString(kind));
			return true;
		}
		
		return false;
	}
	
	public boolean isErrorNotAssignable(SyntaxNode node, Struct nodeStruct, Struct typeStruct) {
		if (!nodeStruct.assignableTo(typeStruct)) {
			String message = "Cannot assign " + nodeStruct.getKind() + " to " + typeStruct.getKind();
			report_error(node, message);
			return true;
		}
		
		return false;
	}
	
	public boolean isErrorParameterNumberNotMatch(SyntaxNode node, List<Struct> one, List<Struct> two) {
		if (one == null || two == null) {
			report_error(node, "One of the parameter lists is null");
			return true;
		}
		
		if (one.size() != two.size()) {
			report_error(node, "Number of parameters does not match " + one.size() + " != " + two.size());
			return true;
		}
		
		return false;
	}
	
	public boolean isErrorParameterTypesNotMatch(SyntaxNode node, List<Struct> one, List<Struct> two) {
		for (int i = 0; i < one.size(); i++) {
			// Check if they are arrays
			if (one.get(i).getKind() == Struct.Array && two.get(i).getKind() == Struct.Array) {
				// If they are arrays and required element is of noType then it is ok, universal
				if (one.get(i).getElemType() == Tab.noType) {
					continue;
				}
				
				if (!one.get(i).getElemType().equals(two.get(i).getElemType())) {
					report_error(node, "Both parameters are arrays but item type do not match " + structKindToString(one.get(i).getElemType()) + " != " + structKindToString(two.get(i).getElemType()));
					return true;
				}
				continue;
			}
			
			if (!one.get(i).equals(two.get(i))) {
				report_error(node, "Parameter types do not match " + structKindToString(one.get(i)) + " != " + structKindToString(two.get(i)));
				return true;
			}
		}
		
		return false;
	}
	
	public boolean errorParameterNotMatch(SyntaxNode node, List<Struct> one, List<Struct> two) {
		return isErrorParameterNumberNotMatch(node, one, two) || isErrorParameterTypesNotMatch(node, one, two);
	}
	
	public Obj infoInsert(SyntaxNode node, int kind, String name, Struct type) {
		if (errorAlreadyExists(node, name)) return Tab.noObj;
		
		Obj obj = Tab.insert(kind, name, type);
		obj.setLevel(0);
		
		report_info(node, "Inserted kind '" + objKindToString(obj) + "' with name '" + name + "' and type '" + structKindToString(type) + "'");
		
		return obj;
	}
	
	private String objKindToString(Obj obj) {
		switch (obj.getKind()) {
			case Obj.Con:
				return "Constant";
			case Obj.Var:
				return "Variable";
			case Obj.Type:
				return "Type";
			case Obj.Meth:
				return "Method";
			case Obj.Fld:
				return "Field";
			case Obj.Prog:
				return "Program";
			case Obj.Elem:
				return "Element";
			case Obj.NO_VALUE:
				return "NO_VALUE";
			default:
				return "UNKNOWN";
		}
	}
	
	private String structKindToString(Struct struct) {
		switch (struct.getKind()) {
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
			case Struct.Interface:
				return "Interface";
			case Struct.Enum:
				return "Set";
			default:
				return "UNKNOWN";
		}
	}
}
