package rs.ac.bg.etf.pp1;

import java.io.*;

import java_cup.runtime.Symbol;

import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;

import rs.ac.bg.etf.pp1.ast.Program;
import rs.ac.bg.etf.pp1.util.Log4JUtils;

public class Compiler {
	static {
		DOMConfigurator.configure(Log4JUtils.instance().findLoggerConfigFile());
		Log4JUtils.instance().prepareLogFile(Logger.getRootLogger());
	}
	
	private static final Logger log = Logger.getLogger(Compiler.class);
	private static final SemanticAnalyzer semanticAnalyzer = new SemanticAnalyzer();
	private static final CodeGenerator codeGenerator = new CodeGenerator();
	
	private static Program parse() throws Exception {
		File source = new File("test/program.mj");
		
		try (Reader br = new BufferedReader(new FileReader(source))) {
			Yylex lexer = new Yylex(br);
			MJParser p = new MJParser(lexer);
			
			Symbol s = p.parse();
			
			return (Program) (s.value);
		}
	}
	
	public static void main(String[] args) {
		try {
			log.info("===================================");
			log.info("Starting compilation...");
			log.info("===================================");
			log.info("Parsing source file...");
			log.info("===================================");
			
			Program prog = parse();
			
			log.info("===================================");
			log.info("Parsing finished!");
			log.info("===================================");
			log.info("Abstract syntax tree:");
			log.info(prog.toString(""));
			log.info("===================================");
			log.info("Semantic analysis started...");
			log.info("===================================");

			semanticAnalyzer.analyze(prog);
			
			log.info("===================================");
			log.info("Semantic analysis finished!");
			log.info("===================================");
			log.info("Code generation started...");
			log.info("===================================");
			
			codeGenerator.compile(prog);
			
			log.info("===================================");
			log.info("Code generation finished!");
			log.info("===================================");
			
		} catch (Exception e) {
			log.error("===================================");
			log.error(e.getMessage(), e);
			log.error("===================================");
		}
	}
}
