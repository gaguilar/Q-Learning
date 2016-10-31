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
		double newUtility = (1 - alpha) * oldUtility
				+ alpha * (entry.getImmediateReward() + gamma * getMaxUtilityNextMove(applyMove(entry)));
		qtable.put(entry, newUtility);
                
                // update current state pickup/dropoff location counts
                State s = entry.s;
                switch (entry.a) {
                    case PICKUP:
                        if (s.agentRow == 1 && s.agentCol == 1)
                           currentState.p1--;
                        else if (s.agentRow == 3 && s.agentCol == 3)
                           currentState.p2--;
                        else if (s.agentRow == 5 && s.agentCol == 5)
                           currentState.p3--;

                        break;
                    case DROPOFF:
                        if (s.agentRow == 5 && s.agentCol == 1)
                           currentState.d1++;
                        else if (s.agentRow == 5 && s.agentCol == 3)
                           currentState.d2++;
                        else if (s.agentRow == 2 && s.agentCol == 5)
                           currentState.d3++;
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
		return agentInBounds(state) && state.hasBlock == 0
				&& board[state.agentRow - 1][state.agentCol - 1] == Occupant.PickUp;
	}

	public boolean goodDropOff(State state) {
		return agentInBounds(state) && state.hasBlock == 1
				&& board[state.agentRow - 1][state.agentCol - 1] == Occupant.DropOff;
	}

	private boolean agentInBounds(State state) {
		int r = state.agentRow;
		int c = state.agentCol;
		return r >= 1 && r <= 5 && c >= 1 && c <= 5;
	}

	public void simulate(int maxSteps) {
		for (int i = 0; i < maxSteps; i++) {
			State state = new State(currentState.agentRow, currentState.agentCol, currentState.hasBlock);
			QEntry e = policy(state);
//                        System.out.println(e);
                        updateQTable(e);
                        State nextState = applyMove(e);
                        currentState.agentRow = nextState.agentRow;
                        currentState.agentCol = nextState.agentCol;
                        currentState.hasBlock = nextState.hasBlock;
		}
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
		
                if(roll <= randomChance){
			// do random
			Collections.shuffle(validMoves);
			return validMoves.get(0);
		} else {
			// do highest utility
			QEntry choice = null;
			double bestUtility = Integer.MIN_VALUE;
			for (QEntry e : validMoves) {
                            double thisUtility = getUtility(e);
                            if(thisUtility > bestUtility){
                                
                                    bestUtility = thisUtility;
                                    choice = e;
                            }
			}
			return choice;
		}
	}

	public void printQTable() {
//		for (QEntry e : qtable.keySet()) {
//			System.out.println(e+" "+qtable.get(e));
//		}

                List<QEntry> entries = new ArrayList(qtable.keySet());
                entries
                    .stream()
                    .filter((qe) -> qtable.get(qe) != 0.0) // Reducing noise
                    .sorted((qe1, qe2) -> qe1.compareByCol(qe2))
                    .sorted((qe1, qe2) -> qe1.compareByRow(qe2))
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
		if (goodPickUp(state))
			moves.add(QEntry.PickUp(state));
		if (goodDropOff(state))
			moves.add(QEntry.DropOff(state));
		return moves;
	}

	public static void main(String[] args) {
            
		double alpha = .5; // Learning rate
		double gamma = .3; // Discount factor
		double randomChoice = .35;//Double.parseDouble(args[2]);
		Simulation sim = new Simulation(alpha, gamma, randomChoice);
		sim.simulate(10000);
		sim.printQTable();
                    
                /*
                NOTES FROM CLASS
                
                * Do not use the whole state space, it will never learn anything —or at least it will take too long to do it
                * Reduce the State space to the Reinforcement Learning space (either 1 or 2 options)
                
                State Space 1: <-- Which is our case..
                * Position of the agent and if it carries a block
                * Totally ignores how many blocks are in both pickup/dropoff locations
                */
                
//            Hashtable<String, Double> table = new Hashtable();
//            QEntry e1 = QEntry.MoveSouth(new State(1,5,0));
//            QEntry e2 = QEntry.MoveSouth(new State(1,5,0));
//            System.out.println(e1.equals(e2));
//            table.put(e1.toString(), -0.9);
//            System.out.println(table.size());
//            table.put(e2.toString(), 100.0);
//            System.out.println(table.size());
//            for(String e:table.keySet())
//                System.out.println(e + " " + table.get(e));
	}
}
