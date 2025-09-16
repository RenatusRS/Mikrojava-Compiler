package rs.ac.bg.etf.pp1;

import rs.ac.bg.etf.pp1.util.Analyzer;

import java.util.ArrayList;
import java.util.HashMap;

public class JumpManager {
	private final HashMap<String, ArrayList<Integer>> jumps = new HashMap<>();
	private final HashMap<String, Integer> labels = new HashMap<>();
	private final Analyzer analyzer = Analyzer.getInstance();
	
	public void jump(String label) {
		ArrayList<Integer> jumpList = jumps.computeIfAbsent(label, k -> new ArrayList<>());
		
		int adr = 0;
		if (labels.containsKey(label)) {
			adr = labels.get(label);
			analyzer.report_info(null, "Label '" + label + "' already exists at address '" + labels.get(label) + "', fixing jump on '" + (Code.pc - 2) + "' to '" + adr + "' now");
		} else {
			analyzer.report_info(null, "Label '" + label + "' for jump on '" + (Code.pc - 2) + "' does not exist, will be fixed later");
		}
		
		Code.putJump(adr);
		jumpList.add(Code.pc - 2);
	}
	
	public void jump(String label, int comparison) {
		ArrayList<Integer> jumpList = jumps.computeIfAbsent(label, k -> new ArrayList<>());
		
		int adr = 0;
		if (labels.containsKey(label)) {
			adr = labels.get(label);
			analyzer.report_info(null, "CMP Label '" + label + "' already exists at address '" + labels.get(label) + "', fixing jump on '" + (Code.pc - 2) + "' to '" + adr + "' now");
		} else {
			analyzer.report_info(null, "CMP Label '" + label + "' for jump on '" + (Code.pc - 2) + "' does not exist, will be fixed later");
		}
		
		Code.putFalseJump(Code.inverse[comparison], adr);
		jumpList.add(Code.pc - 2);
	}
	
	public void setLabel(String label) {
		ArrayList<Integer> jumpList = jumps.get(label);
		
		if (jumpList != null) {
			for (Integer jumpAddress : jumpList) {
				analyzer.report_info(null, "Fixing jump on '" + jumpAddress + "' to label '" + label + "' at address '" + Code.pc + "'");
				Code.fixup(jumpAddress);
			}
		}
		
		analyzer.report_info(null, "Adding label '" + label + "' at address '" + Code.pc + "'");
		labels.put(label, Code.pc);
	}
}
