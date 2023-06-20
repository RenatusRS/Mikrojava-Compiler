package rs.ac.bg.etf.pp1;

import rs.ac.bg.etf.pp1.ast.PrintStmt;
import rs.ac.bg.etf.pp1.ast.VisitorAdaptor;

import org.apache.log4j.Logger;

public class SemanticPass extends VisitorAdaptor {
	Logger log = Logger.getLogger(MJParser.class);
	
	int printCallCount = 0;
	
	public void visit(PrintStmt printStmt) {
		printCallCount++;
		log.info("Print statement called");
	}
}
