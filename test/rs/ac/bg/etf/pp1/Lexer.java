package rs.ac.bg.etf.pp1;

import java_cup.runtime.Symbol;
import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;
import rs.ac.bg.etf.pp1.util.Log4JUtils;

import java.io.*;

public class Lexer {
	static {
		DOMConfigurator.configure(Log4JUtils.instance().findLoggerConfigFile());
		Log4JUtils.instance().prepareLogFile(Logger.getRootLogger());
	}
	
	private static final String[] tests = {
			"tests/test301.mj",
			//"test301.mj",
			//"test302.mj",
	};
	
	public static void main(String[] args) throws IOException {
		Logger log = Logger.getLogger(Lexer.class);
		Reader br = null;
		
		for (String test : Lexer.tests) {
			try {
				File sourceCode = new File("test/" + test);
				log.info("Compiling source file: " + sourceCode.getAbsolutePath());
				
				br = new BufferedReader(new FileReader(sourceCode));
				
				Yylex lexer = new Yylex(br);
				
				Symbol currToken;
				while ((currToken = lexer.next_token()).sym != sym.EOF) if (currToken.value != null) log.info(currToken + " " + currToken.value);
				
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
