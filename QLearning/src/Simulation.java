import java.util.HashMap;

public class Simulation {
	public enum Occupant {
		Empty, Pickup, Dropoff
	}

	Occupant[][] board;
	
	HashMap<QEntry, Float> qtable;
	
	State currentState, goalState;
	
	public Simulation(){
		qtable = new HashMap<QEntry, Float>();
		currentState = new State(1, 5, false, 5, 5, 5, 0, 0, 0);
	}
	
	/*
	 * qtable(qentry) = immediate reward for taking action 'a' while in state 's'
	 * 					+
	 * 					gamma (or ralitve value of delayed vs. immediate rewards)
	 * 					*
	 * 					max qtable(qentry) for all moves after taking action 'a'
	 */
	
	// Q(s, a) look up Q value for being in some state and taking some action
	public Float Q(QEntry entry){
		return qtable.get(entry);
	}
	
	// R(s,a) return immediate reward, where s is some state and a is action
	public Float R(QEntry entry){
		return new Float(0);
	}
	
	public void simulate(){
		while (!currentState.isGoalState()){
			// decide action
			// update utility with Q(s,a), where 's' is a state and 'a' is the action to give best utility
			// make move
		}
	}
	
	public static void main(String[] args) {
		
		
	}
}
