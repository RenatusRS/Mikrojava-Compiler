package rs.ac.bg.etf.pp1;

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
}