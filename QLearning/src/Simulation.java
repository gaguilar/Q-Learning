import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;

public class Simulation {
	public static enum Occupant {
		Empty, PickUp, DropOff
	}

	Occupant[][] board = { { Occupant.PickUp, Occupant.Empty, Occupant.Empty, Occupant.Empty, Occupant.Empty },
			{ Occupant.Empty, Occupant.Empty, Occupant.Empty, Occupant.Empty, Occupant.DropOff },
			{ Occupant.Empty, Occupant.Empty, Occupant.PickUp, Occupant.Empty, Occupant.Empty },
			{ Occupant.Empty, Occupant.Empty, Occupant.Empty, Occupant.Empty, Occupant.Empty },
			{ Occupant.DropOff, Occupant.Empty, Occupant.DropOff, Occupant.Empty, Occupant.PickUp } };

	Hashtable<QEntry, Double> qtable;
	double alpha, gamma;
	double randomChance;

	FullState currentState;

	public Simulation(double learningRate, double discountRate, double randomChoice) {
		alpha = learningRate;
		gamma = discountRate;
		this.randomChance = randomChoice;
		qtable = new Hashtable<QEntry, Double>();
		currentState = new FullState(1, 5, 0, 5, 5, 5, 0, 0, 0);
	}

	/*
	 * qtable(qentry) = immediate reward for taking action 'a' while in state
	 * 's' + gamma (or ralitve value of delayed vs. immediate rewards) *
	 * maxqtable(qentry) for all moves after taking action 'a'
	 */
	public void updateQTable(QEntry entry) {
		double oldUtility = getUtility(entry);
		double newUtility = (1 - alpha) * oldUtility + alpha * (entry.getImmediateReward() + gamma * getMaxUtilityNextMove(applyMove(entry)));
		qtable.put(entry, newUtility);

	}

	public void updatePickupDropoffLocations(QEntry entry) {
		// update current state pickup/dropoff location counts
		State s = entry.s;
		switch (entry.a) {
		case PICKUP:
			if (s.agentRow == 1 && s.agentCol == 1) {
				currentState.p1--;
			} else if (s.agentRow == 3 && s.agentCol == 3) {
				currentState.p2--;
			} else if (s.agentRow == 5 && s.agentCol == 5) {
				currentState.p3--;
			}
			break;
		case DROPOFF:
			if (s.agentRow == 5 && s.agentCol == 1) {
				currentState.d1++;
			} else if (s.agentRow == 5 && s.agentCol == 3) {
				currentState.d2++;
			} else if (s.agentRow == 2 && s.agentCol == 5) {
				currentState.d3++;
			}
		}
	}

	// Q(s, a) look up Q value for being in some state and taking some action.
	// If a QEntry does not exist add it with a default utility value.
	public double getUtility(QEntry entry) {
		if (!qtable.containsKey(entry))
			qtable.put(entry, 0.0);
		return qtable.get(entry);
	}

	public double getMaxUtilityNextMove(State state) {
		ArrayList<QEntry> futureMoves = getValidMoves(state);
		double bestUtility = Double.MIN_VALUE;
		for (QEntry e : futureMoves) {
			if (getUtility(e) > bestUtility)
				bestUtility = getUtility(e);
		}
		return bestUtility;
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
			if (state.agentCol < 5) {
				state.agentCol += 1;
			}
		} else if (entry.movingWest()) {
			if (state.agentCol > 1)
				state.agentCol -= 1;
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
		int row = state.agentRow;
		int col = state.agentCol;
		boolean locationEmpty = (currentState.p1 == 0 && row == 1 && col == 1) || (currentState.p2 == 0 && row == 3 && col == 3) || (currentState.p3 == 0 && row == 5 && col == 5);
		return 	state.hasBlock == 0 && 
				board[row - 1][col - 1] == Occupant.PickUp &&
				!locationEmpty;
	}

	public boolean goodDropOff(State state) {
		int row = state.agentRow;
		int col = state.agentCol;
		boolean locationFull = (currentState.d1 == 5 && row == 5 && col == 1) || (currentState.d2 == 5 && row == 5 && col == 3) || (currentState.d3 == 5 && row == 2 && col == 5);
		return 	state.hasBlock == 1 && 
				board[row - 1][col - 1] == Occupant.DropOff &&
				!locationFull;
	}

	public int simulate(int maxSteps) {
		for (int i = 0; i < maxSteps; i++) {
			State state = new State(currentState.agentRow, currentState.agentCol, currentState.hasBlock);
			QEntry e = policy(state);
			updateQTable(e);
			State nextState = applyMove(e);
			updatePickupDropoffLocations(e);
			currentState.agentRow = nextState.agentRow;
			currentState.agentCol = nextState.agentCol;
			currentState.hasBlock = nextState.hasBlock;

			if (currentState.isGoalState()) {
				//System.out.println("GOAL STATE REACHED AT ITERATION " + i);
				return i;
			}
		}
		return maxSteps;
	}

	public QEntry policy(State state) {
		// Give priority to dropoff and pickup locations
		if (goodDropOff(state)) {
			return QEntry.DropOff(state);
		} else if (goodPickUp(state)) {
			return QEntry.PickUp(state);
		}

		ArrayList<QEntry> validMoves = getValidMoves(state);
		double roll = Math.random();

		if (roll <= randomChance) {
			// do random
			Collections.shuffle(validMoves);
			return validMoves.get(0);
		} else {
			// do highest utility
			QEntry choice = null;
			double bestUtility = Integer.MIN_VALUE;
			for (QEntry e : validMoves) {
				double thisUtility = getUtility(e);
				if (thisUtility > bestUtility) {
					bestUtility = thisUtility;
					choice = e;
				}
			}
			return choice;
		}
	}

	public void printQTable() {
		// for (QEntry e : qtable.keySet()) {
		// System.out.println(e+" "+qtable.get(e));
		// }

		List<QEntry> entries = new ArrayList(qtable.keySet());
		entries.stream().filter((qe) -> qtable.get(qe) != 0.0) // Reducing noise
				.sorted((qe1, qe2) -> qe1.compareByCol(qe2)).sorted((qe1, qe2) -> qe1.compareByRow(qe2))
				.sorted((qe1, qe2) -> qe1.compareByBlock(qe2))
				.forEach((e) -> System.out.println(e + " " + qtable.get(e)));
	}

	public ArrayList<QEntry> getValidMoves(State state) {
		ArrayList<QEntry> moves = new ArrayList<QEntry>();
		int row = state.agentRow;
		int col = state.agentCol;
		if (row > 1)
			moves.add(QEntry.MoveNorth(state));
		if (row < 5)
			moves.add(QEntry.MoveSouth(state));
		if (col > 1)
			moves.add(QEntry.MoveWest(state));
		if (col < 5)
			moves.add(QEntry.MoveEast(state));
		if (goodDropOff(state))
			moves.add(QEntry.DropOff(state));
		if (goodPickUp(state))
			moves.add(QEntry.PickUp(state));
		return moves;
	}
	
	public void printOccupantBoard(){
		for(Occupant[] row : board){
			for(Occupant o : row)
				System.out.print(o+" ");
			System.out.println();
		}
	}

	public static void main(String[] args) {		
		double randomChoice = .35;
		for(int numTests = 0; numTests < 10; numTests++){
			int minIterations = Integer.MAX_VALUE;
			double bestAlpha = 0.0;
			double bestGamma = 0.0;
			for(double alpha = 0.1; alpha<1.0;alpha+=0.1){
				for(double gamma = 0.1; gamma < 1.0; gamma+=0.1){
					//System.out.println("Simulating with learning rate: "+alpha+ " and discount rate: "+gamma);
					Simulation sim = new Simulation(alpha, gamma, randomChoice);
					int iterations = sim.simulate(5000);
					if(iterations < minIterations){
						minIterations = iterations;
						bestAlpha = alpha;
						bestGamma = gamma;
					}
				}	
			}
			System.out.printf("Best performance: alpha: %1.1f gamma: %1.1f with iterations: %d\n",bestAlpha,bestGamma,minIterations);	
		}
		
	}
}
