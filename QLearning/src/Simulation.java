import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

public class Simulation {
	public static enum Occupant {
		Empty, PickUp, DropOff
	}

	Occupant[][] board = { { Occupant.PickUp, Occupant.Empty, Occupant.Empty, Occupant.Empty, Occupant.Empty },
			{ Occupant.Empty, Occupant.Empty, Occupant.Empty, Occupant.Empty, Occupant.DropOff },
			{ Occupant.Empty, Occupant.Empty, Occupant.PickUp, Occupant.Empty, Occupant.Empty },
			{ Occupant.Empty, Occupant.Empty, Occupant.Empty, Occupant.Empty, Occupant.Empty },
			{ Occupant.DropOff, Occupant.Empty, Occupant.DropOff, Occupant.Empty, Occupant.PickUp } };

	HashMap<QEntry, Double> qtable;
	double alpha, gamma;

	FullState currentState;

	public Simulation(double learningRate, double discountRate) {
		alpha = learningRate;
		gamma = discountRate;
		qtable = new HashMap<QEntry, Double>();
		currentState = new FullState(1, 5, 0, 5, 5, 5, 0, 0, 0);
	}

	/*
	 * qtable(qentry) = immediate reward for taking action 'a' while in state
	 * 's' + gamma (or ralitve value of delayed vs. immediate rewards) * max
	 * qtable(qentry) for all moves after taking action 'a'
	 */
	public void updateQTable(QEntry entry) {
		 double oldUtility = getUtility(entry); 
		 double newUtility = (1 - alpha) * oldUtility + alpha * (entry.getImmediateReward() + gamma * getMaxUtilityNextMove(applyMove(entry))); 
		 qtable.put(entry, newUtility);
	}

	// Q(s, a) look up Q value for being in some state and taking some action.
	// If a QEntry does not exist add it with a default utility value.
	public double getUtility(QEntry entry) {
		if (!qtable.containsKey(entry))
			qtable.put(entry, 0.0);
		return qtable.get(entry);
	}

	public double getMaxUtilityNextMove(State state) {
		ArrayList<Double> futureUtilityList = new ArrayList<Double>();
		int row = state.agentRow;
		int col = state.agentCol;
		if (row > 1) futureUtilityList.add(getUtility(QEntry.MoveNorth(state)));
		if (row < 5) futureUtilityList.add(getUtility(QEntry.MoveSouth(state)));
		if (col > 1) futureUtilityList.add(getUtility(QEntry.MoveEast(state)));
		if (col < 5) futureUtilityList.add(getUtility(QEntry.MoveWest(state)));
		if (goodPickUp(state)) futureUtilityList.add(getUtility(QEntry.PickUp(state)));
		if (goodDropOff(state)) futureUtilityList.add(getUtility(QEntry.DropOff(state)));
		Collections.sort(futureUtilityList);
		return futureUtilityList.get(futureUtilityList.size() - 1);
	}

	public State applyMove(QEntry entry) {
		State state = entry.s.clone();
		if (entry.movingNorth()) {
			if (state.agentRow > 1)
				state.agentRow -= 1;
		} else if (entry.movingSouth()) {
			if (state.agentRow < 5)
				state.agentRow += 1;
		} else if (entry.movingEast()) {
			if (state.agentCol > 1) {
				state.agentCol -= 1;
			}
		} else if (entry.movingWest()) {
			if (state.agentCol < 5)
				state.agentCol += 1;
		} else if (entry.droppingOff()) {
			if (goodDropOff(state))
				state.hasBlock = 0;
		} else if (entry.pickingUp()) {
			if (goodPickUp(state))
				state.hasBlock = 1;
		}
		return state;
	}

	public boolean goodPickUp(State state) {
		return agentInBounds(state) && state.hasBlock == 0
				&& board[state.agentRow - 1][state.agentCol - 1] == Occupant.PickUp;
	}

	public boolean goodDropOff(State state) {
		return agentInBounds(state) && state.hasBlock == 0
				&& board[state.agentRow - 1][state.agentCol - 1] == Occupant.DropOff;
	}

	private boolean agentInBounds(State state) {
		int r = state.agentRow;
		int c = state.agentCol;
		return r >= 1 && r <= 5 && c >= 1 && c <= 5;
	}

	public void simulate(int maxSteps) {
		for (int i = 0; i < maxSteps; i++) {

		}
	}
	
	public void printQTable(){
		for(QEntry e : qtable.keySet()){
			System.out.println(qtable.get(e));
		}
	}

	public static void main(String[] args) {
		Simulation sim = new Simulation(0.9, 0.1);
		sim.simulate(1);
		sim.printQTable();
	}
}
