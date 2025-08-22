package rs.ac.bg.etf.pp1.util;

import rs.ac.bg.etf.pp1.JumpManager;

public class IfBlock {
	private final JumpManager jm;
	
	
	public IfBlock(JumpManager jm) {
		this.jm = jm;
	}
	
	public void Else(Runnable action) {
		action.run();
		jm.setLabel("end");
	}
}
