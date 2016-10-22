
public class FullState extends State {

	public int p1, p2, p3;
	public int d1, d2, d3;

	public FullState(int x, int y, int hasBlock, int p1, int p2, int p3, int d1, int d2, int d3) {
		super(x, y, hasBlock);
		this.p1 = p1;
		this.p2 = p2;
		this.p3 = p3;
		this.d1 = d1;
		this.d2 = d2;
		this.d3 = d3;
	}

	public boolean isGoalState() {
		return (hasBlock==0 && p1 == 0 && p2 == 0 && p3 == 0 && d1 == 5 && d2 == 5 && d3 == 5);
	}
}