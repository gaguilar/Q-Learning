
public class State {
	
	public int agentRow, agentCol;
	public int hasBlock;
	
	public State(int i, int j, int x){
		agentRow = i;
		agentCol = j;
		hasBlock = x;
	}
	
	public int compareByCol(State qe) {
		return Integer.compare(this.agentCol, qe.agentCol);
	}

	public int compareByRow(State qe) {
		return Integer.compare(this.agentRow, qe.agentRow);
	}

	public int compareByBlock(State qe) {
		return Integer.compare(this.hasBlock, qe.hasBlock);
	}
	
	@Override
	public boolean equals(Object o1) {
		State e1 = (State) o1;
		return this.toString().equals(e1.toString());
	}
	
	@Override
	public int hashCode() {
		return toString().hashCode();
	}
	
	public State clone(){
		return new State(agentRow, agentCol, hasBlock);
	}
	
	public String toString(){
		return new String(agentRow+" "+agentCol+" "+hasBlock);
	}
	
}