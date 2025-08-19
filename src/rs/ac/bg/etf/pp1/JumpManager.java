package rs.ac.bg.etf.pp1;

import java.util.ArrayList;
import java.util.HashMap;

public class JumpManager {
	private final HashMap<String, ArrayList<Integer>> jumps = new HashMap<>();
	private final HashMap<String, Integer> labels = new HashMap<>();
	
	public void jump(String label) {
		ArrayList<Integer> jumpList = jumps.computeIfAbsent(label, k -> new ArrayList<>());
		
		int adr = 0;
		if (labels.containsKey(label)) {
			System.out.println("Label " + label + " already exists at address " + labels.get(label));
			adr = labels.get(label);
		} else {
			System.out.println("Label " + label + " does not exist, will be fixed later");
		}
		
		Code.putJump(adr);
		System.out.println("Jump on " + (Code.pc - 2) + " currently points to " + adr + " (label: " + label + ")");
		jumpList.add(Code.pc - 2);
	}
	
	public void jump(String label, int comparison) {
		ArrayList<Integer> jumpList = jumps.computeIfAbsent(label, k -> new ArrayList<>());
		
		int adr = 0;
		if (labels.containsKey(label)) {
			System.out.println("CMP Label " + label + " already exists at address " + labels.get(label));
			adr = labels.get(label);
		} else {
			System.out.println("CMP Label " + label + " does not exist, will be fixed later");
		}
		
		Code.putFalseJump(Code.inverse[comparison], adr);
		System.out.println("CMP Jump on " + (Code.pc - 2) + " currently points to " + adr + " (label: " + label + ")");
		jumpList.add(Code.pc - 2);
	}
	
	public void addLabel(String label) {
		ArrayList<Integer> jumpList = jumps.get(label);
		
		if (jumpList != null) {
			for (Integer jumpAddress : jumpList) {
				System.out.println("Fixing jump on " + jumpAddress + " to label " + label + " at address " + Code.pc);
				Code.fixup(jumpAddress);
			}
		}
		
		System.out.println("Adding label " + label + " at address " + Code.pc);
		labels.put(label, Code.pc);
	}
}
