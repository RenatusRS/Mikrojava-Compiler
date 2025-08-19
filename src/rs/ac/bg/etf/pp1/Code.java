package rs.ac.bg.etf.pp1;

import rs.etf.pp1.symboltable.concepts.Obj;

public class Code extends rs.etf.pp1.mj.runtime.Code {
	public static void swap(int pos1, int pos2) {
		if (pos1 == pos2) {
			return; // No need to swap if positions are the same
		}
		
		if (pos1 > pos2) {
			int temp = pos1;
			pos1 = pos2;
			pos2 = temp; // Ensure pos1 is always less than pos2
		}
		
		if (pos1 == 0 && pos2 == 1) { // 1, 0 -> 0, 1
			Code.put(Code.dup_x1); // 1, 0    -> 0, 1, 0
			Code.put(Code.pop);    // 0, 1, 0 -> 0, 1
			
		} else if (pos1 == 0 && pos2 == 2) { // 2, 1, 0 -> 0, 1, 2
			Code.put(Code.dup_x2); // 2, 1, 0    -> 0, 2, 1, 0
			Code.put(Code.pop);    // 0, 2, 1, 0 -> 0, 2, 1
			Code.put(Code.dup_x1); // 0, 2, 1    -> 0, 1, 2, 1
			Code.put(Code.pop);    // 0, 1, 2, 1 -> 0, 1, 2
			
		} else if (pos1 == 1 && pos2 == 2) { // 2, 1, 0 -> 1, 2, 0
			Code.put(Code.dup_x2); // 2, 1, 0    -> 0, 2, 1, 0
			Code.put(Code.pop);    // 0, 2, 1, 0 -> 0, 2, 1
			Code.put(Code.dup_x2); // 0, 2, 1    -> 1, 0, 2, 1
			Code.put(Code.pop);    // 1, 0, 2, 1 -> 1, 0, 2
			Code.put(Code.dup_x1); // 1, 0, 2    -> 1, 2, 0, 2
			Code.put(Code.pop);    // 1, 2, 0, 2 -> 1, 2, 0
			
		} else {
			throw new IllegalArgumentException("Invalid positions for swap: " + pos1 + ", " + pos2);
		}
	}
	
	public static void swap(int pos2, int pos1, int pos0) {
		if (pos2 == 2 && pos1 == 1 && pos0 == 0) {
			return;
		}
		
		if (pos2 == 2 && pos1 == 0 && pos0 == 1) {        // 2 1 0  ->  2 0 1
			Code.put(Code.dup_x1);
			Code.put(Code.pop);
			
		} else if (pos2 == 1 && pos1 == 2 && pos0 == 0) { // 2 1 0  ->  1 2 0
			Code.put(Code.dup_x1);  // 2 1 0   -> 2 0 1 0
			Code.put(Code.pop);     // 2 0 1 0 -> 2 0 1
			Code.put(Code.dup_x2);  // 2 0 1   -> 1 2 0 1
			Code.put(Code.pop);     // 1 2 0 1 -> 1 2 0
			
		} else if (pos2 == 1 && pos1 == 0 && pos0 == 2) { // 2 1 0  ->  1 0 2
			Code.put(Code.dup_x2);  // 2 1 0   -> 0 2 1 0
			Code.put(Code.pop);     // 0 2 1 0 -> 0 2 1
			Code.put(Code.dup_x2);  // 0 2 1   -> 1 0 2 1
			Code.put(Code.pop);     // 1 0 2 1 -> 1 0 2
			
		} else if (pos2 == 0 && pos1 == 2 && pos0 == 1) { // 2 1 0  ->  0 2 1
			Code.put(Code.dup_x2);  // 2 1 0 -> 0 2 1 0
			Code.put(Code.pop);     // 0 2 1 0 -> 0 2 1

		} else if (pos2 == 0 && pos1 == 1 && pos0 == 2) { // 2 1 0  ->  0 1 2
			Code.put(Code.dup_x2);  // 2 1 0   -> 0 2 1 0
			Code.put(Code.pop);     // 0 2 1 0 -> 0 2 1
			Code.put(Code.dup_x1);  // 0 2 1   -> 0 1 2 1
			Code.put(Code.pop);     // 0 1 2 1 -> 0 1 2
			
		} else {
			throw new IllegalStateException("Unhandled 3-param permutation");
		}
	}
	
	// Expected stack: ..., elem2, elem1
	// Returned stack: ..., elem2, elem1, elem1 or elem2, elem1, elem2
	public static void dupli(int pos) {
		if (pos < 0 || pos > 1) {
			throw new IllegalArgumentException("Position must be between 0 and 1, inclusive: " + pos);
		}
		
		if (pos == 0) {
			Code.put(Code.dup); // Duplicate the top element
		} else {
			Code.put(Code.dup2);
			Code.put(Code.pop);
		}
	}
	
	// Expected stack: ...
	public static void exitReturn() {
		Code.put(Code.exit);
		Code.put(Code.return_);
	}
	
	// Expected stack: ..., setAdr
	// Retureed stack: ..., setSize
	public static void setGetSize() {
		Code.loadConst(0);
		Code.put(Code.aload);
	}
	
	// Expected stack: ..., setAdr, index
	// Returned stack: ..., elem
	public static void setGetElem() {
		Code.changeNum(1);
		Code.put(Code.aload);
	}
	
	// Expected stack: ..., setAdr, elem
	// Returned stack: ...
	public static void setAddElem() {
		JumpManager jm = new JumpManager();
		
		Code.dupli(1); // setAdr, elem, setAdr
		Code.setGetSize(); // setAdr, elem, setSize
		
		Code.For(() -> { // setAdr, elem, index
			Code.swap(1, 0, 2); // elem, index, setAdr
			Code.dupli(0); // elem, index, setAdr, setAdr
			Code.swap(0, 2); // elem, setAdr, setAdr, index
			Code.setGetElem(); // elem, setAdr, currElem
			Code.swap(1, 2); // setAdr, elem, currElem
			Code.dupli(1); // setAdr, elem, currElem, elem
			
			Code.If(Code.eq, () -> { // if elem == currElem
				Code.put(Code.pop);
				Code.put(Code.pop);
				jm.addJump("end");
			});
		});
		
		Code.dupli(1); // setAdr, elem, setAdr
		Code.loadConst(0); // setAdr, elem, setAdr, 0
		Code.dupli(1); // setAdr, elem, setAdr, 0, setAdr
		Code.setGetSize(); // setAdr, elem, setAdr, 0, setSize
		Code.changeNum(1); // setAdr, elem, setAdr, 0, setSize + 1
		Code.put(Code.astore); // setAdr, elem
		
		Code.dupli(1); // setAdr, elem, setAdr
		
		Code.setGetSize(); // setAdr, elem, setSize
		Code.swap(0, 1); // setAdr, setSize, elem
		
		Code.put(Code.astore);
		
		jm.addLabel("end");
	}
	
	// Expected stack: ..., num
	// Returned stack: ..., num + change
	public static void changeNum(int change) {
		if (change == 0) {
			return;
		}
		
		Code.loadConst(change);
		
		if (change > 0) {
			Code.put(Code.add);
		} else {
			Code.put(Code.sub);
		}
	}
	
	// Expected stack: ..., cmp1, cmp2
	// Returned stack: ...
	public static void If(int comparison, Runnable action) {
		Code.putFalseJump(comparison, 0);
		int jumpAddress = Code.pc - 2; // Save the jump address
		action.run(); // Execute the action
		Code.fixup(jumpAddress);
	}
	
	// Expected stack: ..., timesToLoop
	// Pushes current loop index to the stack for the runnable action.
	public static void For(Runnable action) {
		JumpManager jm = new JumpManager();
		Obj max = Tab.insertTemp(Obj.Var, "forMax", Tab.intType);
		Code.store(max);
		
		Obj ind = Tab.insertTemp(Obj.Var, "forInd", Tab.intType);
		Code.loadConst(0);
		Code.store(ind);
		
		jm.addLabel("forBegin");
		
		Code.load(ind);
		Code.load(max);
		jm.addJump("forEnd", Code.ge);
		
		Code.load(ind);
		
		action.run();
		
		Code.load(ind);
		Code.changeNum(1);
		Code.store(ind);
		
		jm.addJump("forBegin"); // Jump back to the beginning if condition is met
		jm.addLabel("forEnd");
	}
	
	public static void enter(int formParam, int localParam) {
		Code.put(Code.enter);
		Code.put(formParam);
		Code.put(formParam + localParam);
	}
}


