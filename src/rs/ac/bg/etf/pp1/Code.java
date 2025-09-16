package rs.ac.bg.etf.pp1;

import rs.ac.bg.etf.pp1.util.Analyzer;
import rs.ac.bg.etf.pp1.util.IfBlock;
import rs.etf.pp1.symboltable.concepts.Obj;

import javax.xml.crypto.Data;

public class Code extends rs.etf.pp1.mj.runtime.Code {
	private static int printConst = 1337;
	
	private static final Analyzer analyzer = Analyzer.getInstance();
	
	public static final int eol = 10;
	
	public enum DataStructure {
		ARRAY,
		SET
	}
	
	public static void swap(int pos1, int pos2) {
		if (pos1 == pos2) {
			return; // No need to swap if positions are the same
		}
		
		if (pos1 > pos2) {
			int temp = pos1;
			pos1 = pos2;
			pos2 = temp; // Ensure pos1 is always less than pos2
		}
		
		if (pos1 == 0 && pos2 == 1) { // 2 1 0 -> 2 0 1
			Code.put(Code.dup_x1); // 1 0   -> 0 1 0
			Code.put(Code.pop);    // 0 1 0 -> 0 1
			
		} else if (pos1 == 0 && pos2 == 2) { // 2 1 0 -> 0 1 2
			Code.put(Code.dup_x2); // 2 1 0   -> 0 2 1 0
			Code.put(Code.pop);    // 0 2 1 0 -> 0 2 1
			Code.put(Code.dup_x1); // 0 2 1   -> 0 1 2 1
			Code.put(Code.pop);    // 0 1 2 1 -> 0 1 2
			
		} else if (pos1 == 1 && pos2 == 2) { // 2 1 0 -> 1 2 0
			Code.put(Code.dup_x1); // 2 1 0   -> 2, 0 1 0
			Code.put(Code.pop);    // 2 0 1 0 -> 2 0 1
			Code.put(Code.dup_x2); // 2 0 1   -> 1 2 0 1
			Code.put(Code.pop);    // 1 2 0 1 -> 1 2 0
			
		} else {
			throw new IllegalArgumentException("Invalid positions for swap: " + pos1 + ", " + pos2);
		}
	}
	
	public static void swap(int pos2, int pos1, int pos0) {
		if (pos2 == 2 && pos1 == 1 && pos0 == 0) {
			return;
		}
		
		if (pos2 == 2 && pos1 == 0 && pos0 == 1) { // 2 1 0 -> 2 0 1
			swap(0, 1);
			
		} else if (pos2 == 1 && pos1 == 2 && pos0 == 0) { // 2 1 0 -> 1 2 0
			swap(1, 2);
			
		} else if (pos2 == 1 && pos1 == 0 && pos0 == 2) { // 2 1 0 -> 1 0 2
			Code.put(Code.dup_x2);  // 2 1 0   -> 0 2 1 0
			Code.put(Code.pop);     // 0 2 1 0 -> 0 2 1
			Code.put(Code.dup_x2);  // 0 2 1   -> 1 0 2 1
			Code.put(Code.pop);     // 1 0 2 1 -> 1 0 2
			
		} else if (pos2 == 0 && pos1 == 2 && pos0 == 1) { // 2 1 0 -> 0 2 1
			Code.put(Code.dup_x2);  // 2 1 0 -> 0 2 1 0
			Code.put(Code.pop);     // 0 2 1 0 -> 0 2 1
			
		} else if (pos2 == 0 && pos1 == 1 && pos0 == 2) { // 2 1 0 -> 0 1 2
			swap(0, 2);
			
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
	 * Expected stack: ..., dsAdr
	 * <br>
	 * Returned stack: ..., dsSize
	 */
	public static void getSize(DataStructure dataStructure) {
		switch (dataStructure) {
			case ARRAY:
				Code.put(Code.arraylength);
				break;
			case SET:
				Code.loadConst(0);
				Code.put(Code.aload);
				break;
			default:
				throw new IllegalArgumentException("Unsupported data structure " + dataStructure);
		}
	}
	
	/**
	 * Expected stack: ..., dsAdr, index
	 * <br>
	 * Returned stack: ..., elem
	 */
	public static void getElem(DataStructure dataStructure) {
		switch (dataStructure) {
			case ARRAY:
				Code.put(Code.aload);
				break;
			case SET:
				Code.addNum(1);
				Code.put(Code.aload);
				break;
			default:
				throw new IllegalArgumentException("Unsupported data structure " + dataStructure);
		}
	}
	
	/**
	 * Expected stack: ..., dsAdr, index, elem
	 * <br>
	 * Returned stack: ...
	 */
	public static void setElem(DataStructure dataStructure) {
		switch (dataStructure) {
			case ARRAY:
				Code.put(Code.astore);
				break;
			case SET:
				Code.swap(0, 1); // dsAdr, elem, index
				Code.addNum(1);
				Code.swap(0, 1); // dsAdr, index + 1, elem
				Code.put(Code.astore);
				break;
			default:
				throw new IllegalArgumentException("Unsupported data structure " + dataStructure);
		}
	}
	
	/**
	 * Expected stack: ..., dsAdr, index1, index2
	 * <br>
	 * Returned stack: ...
	 */
	public static void swapElem(DataStructure dataStructure) {
		Code.log();
		Obj index2 = Tab.insertTemp(Tab.intType); // dsAdr, index1, index2
		Code.store(index2); // dsAdr, index1
		Obj index1 = Tab.insertTemp(Tab.intType); // dsAdr, index1
		Code.store(index1); // dsAdr
		
		Code.put(Code.dup); // dsAdr, dsAdr
		Code.load(index1); // dsAdr, dsAdr, index1
		
		Code.getElem(dataStructure); // dsAdr, elem1
		Code.swap(0, 1); // elem1, dsAdr
		Code.dupli(0); // elem1, dsAdr, dsAdr
		Code.dupli(0); // elem1, dsAdr, dsAdr, dsAdr
		Code.load(index2); // elem1, dsAdr, dsAdr, dsAdr, index2
		Code.getElem(dataStructure); // elem1, dsAdr, dsAdr, elem2
		Code.load(index1); // elem1, dsAdr, dsAdr, elem2, index1
		Code.swap(0, 1); // elem1, dsAdr, dsAdr, index1, elem2
		Code.setElem(dataStructure); // elem1, dsAdr
		Code.load(index2); // elem1, dsAdr, index2
		Code.swap(1, 0, 2); // dsAdr, index2, elem1
		Code.setElem(dataStructure); // ...
		
		Code.log();
		
		Tab.free(index1);
		Tab.free(index2);
	}
	
	/**
	 * Expected stack: ..., setAdr, elem
	 * <br>
	 * Returned stack: ...
	 */
	public static void setAddElem() {
		JumpManager jm = new JumpManager();
		
		Code.dupli(1); // setAdr, elem, setAdr 4 5 4
		Code.getSize(DataStructure.SET); // setAdr, elem, setSize 4 5 0
		
		Code.For(() -> { // setAdr, elem, index
			Code.swap(1, 0, 2); // elem, index, setAdr
			Code.dupli(0); // elem, index, setAdr, setAdr
			Code.swap(0, 2); // elem, setAdr, setAdr, index
			Code.getElem(DataStructure.SET); // elem, setAdr, currElem
			Code.swap(1, 2); // setAdr, elem, currElem
			Code.dupli(1); // setAdr, elem, currElem, elem
			
			Code.If(Code.eq, () -> { // if elem == currElem
				// setAdr, elem
				Code.put(Code.pop); // setAdr
				Code.put(Code.pop); // ...
				jm.jump("end");
			});
		});
		
		Code.dupli(1); // setAdr, elem, setAdr
		Code.loadConst(0); // setAdr, elem, setAdr, 0
		Code.dupli(1); // setAdr, elem, setAdr, 0, setAdr
		Code.getSize(DataStructure.SET); // setAdr, elem, setAdr, 0, setSize
		Code.addNum(1); // setAdr, elem, setAdr, 0, setSize + 1
		Code.put(Code.astore); // setAdr, elem
		
		Code.dupli(1); // setAdr, elem, setAdr
		Code.dupli(0); // setAdr, elem, setAdr, setAdr
		Code.getSize(DataStructure.SET); // setAdr, elem, setAdr, setSize
		Code.swap(1, 0, 2); // setAdr, setAdr, setSize, elem
		
		Code.put(Code.astore); // setAdr
		
		Code.sort(DataStructure.SET);
		
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
		
		Code.loadConst(Math.abs(change));
		Code.put(change > 0 ? Code.add : Code.sub);
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
		For(1, action);
	}
	
	/**
	 * Expected stack: ..., timesToLoop
	 * <br>
	 * Pushes current loop index to the stack for the runnable action.
	 */
	public static void For(int step, Runnable action) {
		Code.loadConst(0);
		ForFromTo(step, action);
	}
	
	/**
	 * Expected stack: ..., to, from
	 * <br>
	 * Pushes current loop index to the stack for the runnable action.
	 */
	public static void ForFromTo(Runnable action) {
		ForFromTo(1, action);
	}
	
	/**
	 * Expected stack: ..., to, from
	 * <br>
	 * Pushes current loop index to the stack for the runnable action.
	 */
	public static void ForFromTo(int step, Runnable action) {
		JumpManager jm = new JumpManager();
		
		Obj from = Tab.insertTemp(Tab.intType);
		Code.store(from);
		
		Obj to = Tab.insertTemp(Tab.intType);
		Code.store(to);
		
		jm.setLabel("begin");
		
		Code.load(from);
		Code.load(to);
		jm.jump("end", step > 0 ? Code.ge : Code.le);
		
		Code.load(from);
		
		action.run();
		
		Code.load(from);
		Code.addNum(step);
		Code.store(from);
		
		jm.jump("begin");
		jm.setLabel("end");
		
		Tab.free(from);
		Tab.free(to);
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
			Code.swap(0, 1); // elem2, elem1
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
			Code.swap(0, 1); // elem2, elem1
			Code.put(Code.pop);
		});
	}
	
	/**
	 * Expected stack: ..., setAdr
	 * <br>
	 * Returned stack: ...
	 */
	public static void sort(DataStructure dataStructure) {
		Obj temp = Tab.insertTemp(Tab.intType);
		Code.log();
		Code.store(temp); // ...
		
		Code.load(temp); // setAdr
		Code.getSize(dataStructure); // setSize
		Code.addNum(-1); // setSize - 1
		
		Code.For(() -> { // indexI
			Code.log();
			Code.load(temp); // indexI, setAdr
			Code.getSize(dataStructure); // indexI, setSize
			Code.dupli(1); // indexI, setSize, indexI
			Code.addNum(1); // indexI, setSize, indexI + 1
			
			Code.ForFromTo(() -> { // indexI, indexJ
				Code.log();
				Code.dupli(1); // indexI, indexJ, indexI
				Code.load(temp); // indexI, indexJ, indexI, setAdr
				Code.swap(0, 1); // indexI, indexJ, setAdr, indexI
				Code.getElem(dataStructure); // indexI, indexJ, elemI
				Code.dupli(1); // indexI, indexJ, elemI, indexJ
				Code.load(temp); // indexI, indexJ, elemI, indexJ, setAdr
				Code.swap(0, 1); // indexI, indexJ, elemI, setAdr, indexJ
				Code.getElem(dataStructure); // indexI, indexJ, elemI, elemJ
				
				Code.If(Code.gt, () -> { // indexI, indexJ
					Code.dupli(1); // indexI, indexJ, indexI
					Code.load(temp); // indexI, indexJ, indexI, setAdr
					Code.swap(0, 2); // indexI, setAdr, indexI, indexJ
					Code.swapElem(dataStructure);
				}).Else(() -> {
					Code.put(Code.pop); // indexI
				});
			});
			
			Code.put(Code.pop); // ...
		});
		
		Tab.free(temp);
	}
	
	public static void log() {
		StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
		// Index 0 is getStackTrace, 1 is log(), so 2 is the caller
		if (stackTrace.length > 2) {
			StackTraceElement caller = stackTrace[2];
			analyzer.report_info(null, "Log value '" + printConst + "' belongs to line '" + caller.getLineNumber() + "'");
		}
		
		Code.loadConst(printConst++);
		Code.put(Code.pop);
	}
	
	public static void printEol() {
		Code.loadConst(eol);
		Code.loadConst(1);
		Code.put(Code.bprint);
	}
}


