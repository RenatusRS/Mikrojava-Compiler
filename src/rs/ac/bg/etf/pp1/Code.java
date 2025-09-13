package rs.ac.bg.etf.pp1;

import rs.ac.bg.etf.pp1.util.IfBlock;
import rs.etf.pp1.symboltable.concepts.Obj;

public class Code extends rs.etf.pp1.mj.runtime.Code {
	private static int printConst = 1337;
	
	public static final int eol = 10;
	
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
	
	/**
	 * Expected stack: ..., elem2, elem1
	 * <br>
	 * Returned stack: ..., elem2, elem1, elem1 or elem2, elem1, elem2
	 */
	
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
	
	public static void exitReturn() {
		Code.put(Code.exit);
		Code.put(Code.return_);
	}
	
	/**
	 * Expected stack: ..., setAdr
	 * <br>
	 * Returned stack: ..., setSize
	 */
	public static void setGetSize() {
		Code.loadConst(0);
		Code.put(Code.aload);
	}
	
	/**
	 * Expected stack: ..., setAdr, index
	 * <br>
	 * Returned stack: ..., elem
	 */
	public static void setGetElem() {
		Code.addNum(1);
		Code.put(Code.aload);
	}
	
	/**
	 * Expected stack: ..., setAdr, elem
	 * <br>
	 * Returned stack: ...
	 */
	public static void setAddElem() {
		JumpManager jm = new JumpManager();
		
		Code.dupli(1); // setAdr, elem, setAdr 4 5 4
		Code.setGetSize(); // setAdr, elem, setSize 4 5 0
		
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
				jm.jump("end");
			});
		});
		
		Code.dupli(1); // setAdr, elem, setAdr
		Code.loadConst(0); // setAdr, elem, setAdr, 0
		Code.dupli(1); // setAdr, elem, setAdr, 0, setAdr
		Code.setGetSize(); // setAdr, elem, setAdr, 0, setSize
		Code.addNum(1); // setAdr, elem, setAdr, 0, setSize + 1
		Code.put(Code.astore); // setAdr, elem
		
		Code.dupli(1); // setAdr, elem, setAdr
		
		Code.setGetSize(); // setAdr, elem, setSize
		Code.swap(0, 1); // setAdr, setSize, elem
		
		Code.put(Code.astore);
		
		jm.setLabel("end");
	}
	
	/**
	 * Expected stack: ..., num
	 * <br>
	 * Returned stack: ..., num + change
	 */
	
	public static void addNum(int change) {
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
	
	/**
	 * Expected stack: ..., cmp1, cmp2
	 * <br>
	 * Returned stack: ...
	 */
	public static IfBlock If(int comparison, Runnable action) {
		JumpManager jm = new JumpManager();
		jm.jump("else", inverse[comparison]);
		action.run();
		jm.jump("end");
		jm.setLabel("else");
		jm.setLabel("end");
		
		return new IfBlock(jm);
	}
	
	/**
	 * Expected stack: ..., timesToLoop
	 * <br>
	 * Pushes current loop index to the stack for the runnable action.
	 */
	public static void For(Runnable action) {
		JumpManager jm = new JumpManager();
		Obj max = Tab.insertTemp(Tab.intType);
		Code.store(max);
		
		Obj ind = Tab.insertTemp(Tab.intType);
		Code.loadConst(0);
		Code.store(ind);
		
		jm.setLabel("begin");
		
		Code.load(ind);
		Code.load(max);
		jm.jump("end", Code.ge);
		
		Code.load(ind);
		
		action.run();
		
		Code.load(ind);
		Code.addNum(1);
		Code.store(ind);
		
		jm.jump("begin");
		jm.setLabel("end");
		
		Tab.free(ind);
		Tab.free(max);
	}
	
	public static void enter(int formParam, int localParam) {
		Code.put(Code.enter);
		Code.put(formParam);
		Code.put(formParam + localParam);
	}
	
	/**
	 * Expected stack: ..., elem1, elem2
	 * <br>
	 * Returned stack: ..., elemMin
	 */
	public static void min() {
		Code.put(Code.dup2); // elem1, elem2, elem1, elem2
		Code.If(Code.lt, () -> { // elem1, elem2
			Code.put(Code.pop); // elem1
		}).Else(() -> {
			Code.put(Code.dup_x1); // elem2, elem1, elem2
			Code.put(Code.pop); // elem2, elem1
			Code.put(Code.pop); // elem2
		});
	}
	
	/**
	 * Expected stack: ..., elem1, elem2
	 * <br>
	 * Returned stack: ..., elemMax
	 */
	public static void max() {
		Code.put(Code.dup2); // elem1, elem2, elem1, elem2
		Code.If(Code.gt, () -> { // elem1, elem2
			Code.put(Code.pop); // elem1
		}).Else(() -> {
			Code.put(Code.dup_x1); // elem2, elem1, elem2
			Code.put(Code.pop); // elem2, elem1
			Code.put(Code.pop); // elem2
		});
	}
	
	public static void log() {
		Code.loadConst(printConst++);
		Code.put(Code.pop);
	}
	
	public static void printEol() {
		Code.loadConst(eol);
		Code.loadConst(1);
		Code.put(Code.bprint);
	}
}


