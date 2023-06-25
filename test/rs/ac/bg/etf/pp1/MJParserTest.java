package rs.ac.bg.etf.pp1;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;

import java_cup.runtime.Symbol;

import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;

import rs.ac.bg.etf.pp1.ast.Program;
import rs.ac.bg.etf.pp1.util.Log4JUtils;
import rs.etf.pp1.symboltable.Tab;
import rs.etf.pp1.symboltable.concepts.Obj;
import rs.etf.pp1.symboltable.concepts.Struct;

public class MJParserTest {
	static {
		DOMConfigurator.configure(Log4JUtils.instance().findLoggerConfigFile());
		Log4JUtils.instance().prepareLogFile(Logger.getRootLogger());
	}
	
	private static final String[] tests = {
			"program.mj",
			//"test301.mj",
			//"test302.mj",
	};
	
	public static void main(String[] args) throws Exception {
		Logger log = Logger.getLogger(MJParserTest.class);
		Reader br = null;
		
		for (String test : MJParserTest.tests) {
			try {
				File sourceCode = new File("test/" + test);
				log.info("Compiling source file: " + sourceCode.getAbsolutePath());
				
				br = new BufferedReader(new FileReader(sourceCode));
				Yylex lexer = new Yylex(br);
				
				MJParser p = new MJParser(lexer);
				Symbol s = p.parse();  // pocetak parsiranja
				
				Program prog = (Program)(s.value);
				
				// ispis sintaksnog stabla
				log.info(prog.toString(""));
				log.info("===================================");
				
				// ispis prepoznatih programskih konstrukcija
				SemanticAnalyzer v = new SemanticAnalyzer();
				prog.traverseBottomUp(v);
				
				
				
			} finally {
				try {
					if (br != null) br.close();
				} catch (IOException e1) {
					log.error(e1.getMessage(), e1);
				}
			}
		}

	}
}
