
public class State {
	
	public int agentRow, agentCol;
	public int hasBlock;
	
	public State(int i, int j, int x){
		agentRow = i;
		agentCol = j;
		hasBlock = x;
	}
	
	public State clone(){
		return new State(agentRow, agentCol, hasBlock);
	}
	
	public String toString(){
		return new String(agentRow+", "+agentCol+" "+hasBlock);
	}
	
}