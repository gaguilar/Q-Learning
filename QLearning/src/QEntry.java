public class QEntry{
	
	public enum Action{
		NORTH,
		SOUTH,
		EAST,
		WEST,
		PICKUP,
		DROPOFF
	}
	
	State s;
	Action a;
	
	public QEntry(State s, Action a){
		this.s = s;
		this.a = a;
	}

	
	public static QEntry MoveNorth(State s){
		return new QEntry(s, Action.NORTH);
	}
	
	public static QEntry MoveSouth(State s){
		return new QEntry(s, Action.SOUTH);
	}
	
	public static QEntry MoveEast(State s){
		return new QEntry(s, Action.EAST);
	}
	
	public static QEntry MoveWest(State s){
		return new QEntry(s, Action.WEST);
	}
	
	public static QEntry DropOff(State s){
		return new QEntry(s, Action.DROPOFF);
	}
	
	public static QEntry PickUp(State s){
		return new QEntry(s, Action.PICKUP);
	}
}
