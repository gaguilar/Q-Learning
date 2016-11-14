import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Random;
import java.util.function.BiPredicate;
import java.util.function.Consumer;

public class Simulation {

	ArrayList<String> dropOffLocations, pickUpLocations;	

	Hashtable<QEntry, Double> qtable;
	double alpha, gamma;
	double randomChance;

	boolean firstDropOffFilled, secondDropOffFilled;

	////// RANDOM SEED = 115 ///////
	Random random = new Random(115);
	////////////////////////////////

	FullState currentState;

	public Simulation(double learningRate, double discountRate, double randomChoice) {
		alpha = learningRate;
		gamma = discountRate;
		this.randomChance = randomChoice;
		qtable = new Hashtable<QEntry, Double>();
		currentState = new FullState(1, 5, 0, 5, 5, 5, 0, 0, 0);
		
		dropOffLocations = new ArrayList<String>();
		dropOffLocations.add(5 + " " + 1);
		dropOffLocations.add(5 + " " + 3);
		dropOffLocations.add(2 + " " + 5);
		
		pickUpLocations = new ArrayList<String>();
		pickUpLocations.add(1 + " " + 1);
		pickUpLocations.add(3 + " " + 3);
		pickUpLocations.add(5 + " " + 5);
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
		default:
			break;
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
		String location = row+" "+col;
		boolean locationEmpty = (currentState.p1 == 0 && row == 1 && col == 1)
				|| (currentState.p2 == 0 && row == 3 && col == 3) || (currentState.p3 == 0 && row == 5 && col == 5);
		return state.hasBlock == 0 && pickUpLocations.contains(location) && !locationEmpty;
	}

	public boolean goodDropOff(State state) {
		int row = state.agentRow;
		int col = state.agentCol;
		String location = row+" "+col;
		boolean locationFull = (currentState.d1 == 5 && row == 5 && col == 1)
				|| (currentState.d2 == 5 && row == 5 && col == 3) || (currentState.d3 == 5 && row == 2 && col == 5);
		return state.hasBlock == 1 && dropOffLocations.contains(location) && !locationFull;
	}

	public void simulate(int maxSteps, BiPredicate<Simulation, Integer> pred, Consumer<Simulation> cons) {
		for (int i = 0; i < maxSteps; i++) {
			Boolean isGoalState = step();
			PrintExperiment(this, i, pred, cons);

			if (isGoalState)
				return;
		}
	}

	public boolean step() {
		State state = new State(currentState.agentRow, currentState.agentCol, currentState.hasBlock);
		QEntry e = policy(state);
		updateQTable(e);
		State nextState = applyMove(e);
		updatePickupDropoffLocations(e);

		currentState.agentRow = nextState.agentRow;
		currentState.agentCol = nextState.agentCol;
		currentState.hasBlock = nextState.hasBlock;

		return currentState.isGoalState();
	}

	// Call after simulation is complete to reset agent but keep learned QTable
	// values
	public void resetFullState() {
		currentState = new FullState(1, 5, 0, 5, 5, 5, 0, 0, 0);
	}

	public QEntry policy(State state) {
		// Give priority to dropoff and pickup locations
		if (goodDropOff(state)) {
			return QEntry.DropOff(state);
		} else if (goodPickUp(state)) {
			return QEntry.PickUp(state);
		}

		ArrayList<QEntry> validMoves = getValidMoves(state);
		double roll = random.nextDouble();

		if (roll <= randomChance) {
			return validMoves.get(random.nextInt(validMoves.size()));
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
		List<QEntry> entries = new ArrayList<>(qtable.keySet());
		entries.stream().filter((qe) -> qtable.get(qe) != 0.0).sorted((qe1, qe2) -> qe1.compareByCol(qe2))
				.sorted((qe1, qe2) -> qe1.compareByRow(qe2)).sorted((qe1, qe2) -> qe1.compareByBlock(qe2))
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

	public void setRandomChance(double r) {
		randomChance = r;
	}
	
	public void switchPickUpDropLocations(){
		ArrayList<String> temp = new ArrayList<String>();
		for(String s:pickUpLocations)
			temp.add(s);
		pickUpLocations.clear();
		for(String s:dropOffLocations)
			pickUpLocations.add(s);
		dropOffLocations.clear();
		for(String s:temp)
			dropOffLocations.add(s);
	}

	/*
	 * Run 10 tests of: choose alpha/gamma values from 0.1 to 0.9 and do
	 * simulations. All results of same randomChoice should be the same because
	 * the Random Number Generator has the same seed.
	 */
	public static void testRandomSeed(double randomChoice) {
		for (int numTests = 0; numTests < 10; numTests++) {
			int minIterations = Integer.MAX_VALUE;
			double bestAlpha = 0.0;
			double bestGamma = 0.0;
			for (double alpha = 0.1; alpha < 1.0; alpha += 0.1) {
				for (double gamma = 0.1; gamma < 1.0; gamma += 0.1) {
					Simulation sim = new Simulation(alpha, gamma, randomChoice);
					int iterations = 0;
					for (int i = 0; i < 10000; i++) {
						boolean finished = sim.step();
						if (finished) {
							iterations = i;
							break;
						}
					}
					if (iterations < minIterations) {
						minIterations = iterations;
						bestAlpha = alpha;
						bestGamma = gamma;
					}
				}
			}
			System.out.printf("Best performance: alpha: %1.1f gamma: %1.1f with iterations: %d\n", bestAlpha, bestGamma,
					minIterations);
		}
	}

	public boolean exactlyOneDropOffFilled() {
		return (currentState.d1 == 5 && currentState.d2 != 5 && currentState.d3 != 5)
				|| (currentState.d1 != 5 && currentState.d2 == 5 && currentState.d3 != 5)
				|| (currentState.d1 != 5 && currentState.d2 != 5 && currentState.d3 == 5);
	}

	public boolean exactlyTwoDropOffFilled() {
		return (currentState.d1 == 5 && currentState.d2 == 5 && currentState.d3 != 5)
				|| (currentState.d1 == 5 && currentState.d2 != 5 && currentState.d3 == 5)
				|| (currentState.d1 != 5 && currentState.d2 == 5 && currentState.d3 == 5);
	}

	public static void PrintExperiment(Simulation sim, int i, BiPredicate<Simulation, Integer> pred,
			Consumer<Simulation> cons) {
		if (pred.test(sim, i))
			cons.accept(sim);
	}

	public static void Experiment1(int iterations) {
                Simulation sim = new Simulation(0.3, 0.3, 1.0);
                
		Consumer<Simulation> cons = (s) -> s.printQTable();
		BiPredicate<Simulation, Integer> pred = (s, i) -> {
                    boolean firstHundred = i == 100;
                    boolean firstDropOff = s.exactlyOneDropOffFilled() && !s.firstDropOffFilled;
                    boolean isGoalState = s.currentState.isGoalState();
                    
                    if (firstDropOff)
                            s.firstDropOffFilled = true;
                    
                    return firstHundred || firstDropOff || isGoalState;
                };

		sim.simulate(iterations, pred, cons);
		sim.resetFullState();
		sim.simulate(iterations, pred, cons);
	}

	public static void Experiment2(int iterations) {
                Simulation sim = new Simulation(0.3, 0.3, 1.0);
                
		Consumer<Simulation> cons = (s) -> s.printQTable();
		BiPredicate<Simulation, Integer> pred = (s, i) -> {
                    boolean everyHundred = i != 0 && i % 100 == 0;
                    boolean firstDropOff = s.exactlyOneDropOffFilled() && !s.firstDropOffFilled;
                    boolean isGoalState = s.currentState.isGoalState();
                    
                    if (firstDropOff)
                            s.firstDropOffFilled = true;
                    
                    if (i > 100)
                            s.randomChance = 0.35; //Change it to Exploit 1
                    
                    return everyHundred || firstDropOff || isGoalState;
                };

		sim.simulate(iterations, pred, cons);
		sim.resetFullState();
		sim.simulate(iterations, pred, cons);
	}
        
	public static void Experiment5(int iterations) {
                Simulation sim = new Simulation(0.5, 0.3, 0.1);
                
		Consumer<Simulation> cons = (s) -> s.printQTable();
		BiPredicate<Simulation, Integer> pred = (s, i) -> {
                    boolean firstHundred = i == 100;
                    boolean firstDropOff = s.exactlyOneDropOffFilled() && !s.firstDropOffFilled;
                    boolean isGoalState = s.currentState.isGoalState();
                    
                    if (firstDropOff)
                            s.firstDropOffFilled = true;
                    
                    return firstHundred || firstDropOff || isGoalState;
                };

		sim.simulate(iterations, pred, cons);
		sim.resetFullState();
                sim.switchPickUpDropLocations();
		sim.simulate(iterations, pred, cons);
	}
        

	public static void RunExperiment3() {
		Simulation sim = new Simulation(0.3, 0.3, 1.0);
		Consumer<Simulation> cons = (s) -> s.printQTable();
		BiPredicate<Simulation, Integer> pred = (s, i) -> {
            return s.currentState.isGoalState();
        };

		sim.simulate(100, pred, cons);
		sim.setRandomChance(0.1);
		sim.simulate(9900, pred, cons);
		
		sim.resetFullState();
		
		sim.simulate(100, pred, cons);
		sim.setRandomChance(0.1);
		sim.simulate(9900, pred, cons);
	}

	public static void RunExperiment4() {
		Simulation sim = new Simulation(0.5, 0.3, 1.0);
		Consumer<Simulation> cons = (s) -> s.printQTable();
		BiPredicate<Simulation, Integer> pred = (s, i) -> {
			boolean everyHundred = i != 0 && i % 100 == 0;
			boolean isGoalState = s.currentState.isGoalState();
			
            return everyHundred || isGoalState;
        };

		sim.simulate(100, pred, cons);
		sim.setRandomChance(0.1);
		sim.simulate(9900, pred, cons);
		
		sim.resetFullState();
		
		sim.simulate(100, pred, cons);
		sim.setRandomChance(0.1);
		sim.simulate(9900, pred, cons);
	}

	public static void RunExperiment6() {
		Simulation sim = new Simulation(0.5, 0.3, 1.0);
		Consumer<Simulation> cons = (s) -> s.printQTable();
		BiPredicate<Simulation, Integer> pred = (s, i) -> {
            return s.currentState.isGoalState();
        };

		sim.simulate(100, pred, cons);
		sim.setRandomChance(0.35);
		sim.simulate(9900, pred, cons);
		
		sim.resetFullState();
		
		sim.simulate(100, pred, cons);
		sim.setRandomChance(0.35);
		sim.simulate(9900, pred, cons);
	}

	public static void main(String[] args) {
                int iterations = 10000;
                Experiment1(iterations);
                Experiment2(iterations);
                Experiment5(iterations);
		RunExperiment3();
		RunExperiment4();
		RunExperiment6();
	}
}
