
public class State{

	public int x, y;
	public boolean hasBlock;
	public int p1, p2, p3;
	public int d1, d2, d3;

	public State(int x, int y, boolean hasBlock, int p1, int p2, int p3, int d1, int d2, int d3) {
		this.x = x;
		this.y = y;
		this.hasBlock = hasBlock;
		this.p1 = p1;
		this.p2 = p2;
		this.p3 = p3;
		this.d1 = d1;
		this.d2 = d2;
		this.d3 = d3;
	}
	
	public boolean isGoalState(){
		return (!hasBlock && p1==0 && p2==0 && p3==0 && d1==5 && d2==5 && d3==5);
	}
	
	public boolean equals(Object other){
		if (!(other instanceof State))
			return false;
		State otherState = (State)other;
		return (otherState.x == x && otherState.y == y && otherState.hasBlock == hasBlock && otherState.p1 == p1 && otherState.p2 == p2 && otherState.p3 == p3 && otherState.d1 == d1 && otherState.d2 == d2 && otherState.d3 == d3);
	}
}