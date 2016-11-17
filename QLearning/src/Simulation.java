import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Random;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;

public class Simulation {

	Hashtable<State, double[]> qtable;
	double alpha, gamma;
	double randomChance;

	public int p1r, p1c, p2r, p2c, p3r, p3c, d1r, d1c, d2r, d2c, d3r, d3c;

	boolean firstDropOffFilled, secondDropOffFilled;

	////// RANDOM SEED = 115 ///////
	Random random = new Random(115);
	////////////////////////////////

	FullState currentState;

	public Simulation(double learningRate, double discountRate, double randomChoice) {
		alpha = learningRate;
		gamma = discountRate;
		this.randomChance = randomChoice;
		qtable = new Hashtable<State, double[]>();
		currentState = new FullState(1, 5, 0, 5, 5, 5, 0, 0, 0);

		d1r = 5;
		d1c = 1;
		d2r = 5;
		d2c = 3;
		d3r = 2;
		d3c = 5;

		p1r = 1;
		p1c = 1;
		p2r = 3;
		p2c = 3;
		p3r = 5;
		p3c = 5;
                
                
                for (int i = 1; i <= 5; i++) {
                    for (int j = 1; j <= 5; j++) {
                        qtable.put(new State(i, j, 0), new double[6]);
                        qtable.put(new State(i, j, 1), new double[6]);
                    }
                }
                
	}

	/*
	 * qtable(qentry) = immediate reward for taking action 'a' while in state
	 * 's' + gamma (or ralitve value of delayed vs. immediate rewards) *
	 * maxqtable(qentry) for all moves after taking action 'a'
	 */
	public void updateQTable(QEntry entry) {
		double oldUtility = getUtility(entry);
		double newUtility = (1 - alpha) * oldUtility + alpha * (entry.getImmediateReward() + gamma * getMaxUtilityNextMove(applyMove(entry)));
		addQEntry(entry, newUtility);
	}

	// for utility array.... [0]=NORTH [1]=SOUTH [2]=EAST [3]=WEST [4]=PICKUP
	// [5]=DROPOFF
	public void addQEntry(QEntry entry, double utility) {
		double[] utilities = qtable.get(entry.s);

		if (utilities == null)
			utilities = new double[6];

		if (entry.movingNorth())
			utilities[0] = utility;
		if (entry.movingSouth())
			utilities[1] = utility;
		if (entry.movingEast())
			utilities[2] = utility;
		if (entry.movingWest())
			utilities[3] = utility;
		if (entry.pickingUp())
			utilities[4] = utility;
		if (entry.droppingOff())
			utilities[5] = utility;

		qtable.put(entry.s, utilities);
	}

	public void updatePickupDropoffLocations(QEntry entry) {
		// update current state pickup/dropoff location counts
		State s = entry.s;
		switch (entry.a) {
		case PICKUP:
			if (s.agentRow == p1r && s.agentCol == p1c) {
				currentState.p1--;
			} else if (s.agentRow == p2r && s.agentCol == p2c) {
				currentState.p2--;
			} else if (s.agentRow == p3r && s.agentCol == p3c) {
				currentState.p3--;
			}
			break;
		case DROPOFF:
			if (s.agentRow == d1r && s.agentCol == d1c) {
				currentState.d1++;
			} else if (s.agentRow == d2r && s.agentCol == d2c) {
				currentState.d2++;
			} else if (s.agentRow == d3r && s.agentCol == d3c) {
				currentState.d3++;
			}
		default:
			break;
		}
	}

	// Q(s, a) look up Q value for being in some state and taking some action.
	// If a QEntry does not exist add it with a default utility value.

	// for utility array.... [0]=NORTH [1]=SOUTH [2]=EAST [3]=WEST [4]=PICKUP [5]=DROPOFF
	public double getUtility(QEntry entry) {
		if (!qtable.containsKey(entry.s)) {
			qtable.put(entry.s, new double[6]);
			return 0.0;
		}
		if (entry.movingNorth())
			return qtable.get(entry.s)[0];
		if (entry.movingSouth())
			return qtable.get(entry.s)[1];
		if (entry.movingEast())
			return qtable.get(entry.s)[2];
		if (entry.movingWest())
			return qtable.get(entry.s)[3];
		if (entry.pickingUp())
			return qtable.get(entry.s)[4];
		if (entry.droppingOff())
			return qtable.get(entry.s)[5];
		return -1.0;
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
			if (state.agentCol < 5)
				state.agentCol += 1;
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

		boolean locationGoodPickup = (currentState.p1 > 0 && row == p1r && col == p1c)
				|| (currentState.p2 > 0 && row == p2r && col == p2c)
				|| (currentState.p3 > 0 && row == p3r && col == p3c);
		return state.hasBlock == 0 && locationGoodPickup;
	}

	public boolean goodDropOff(State state) {
		int row = state.agentRow;
		int col = state.agentCol;
		boolean locationGoodDropoff = (currentState.d1 < 5 && row == d1r && col == d1c)
				|| (currentState.d2 < 5 && row == d2r && col == d2c)
				|| (currentState.d3 < 5 && row == d3r && col == d3c);
		return state.hasBlock == 1 && locationGoodDropoff;
	}

	public int simulate(int maxSteps, BiPredicate<Simulation, Integer> pred, BiConsumer<Simulation, Integer> cons) {
		for (int i = 0; i < maxSteps; i++) {
			boolean isGoalState = step();
			PrintExperiment(this, i, pred, cons);

			if (isGoalState)
				return i;
		}
		return maxSteps;
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

	public void printQTable(int i) {
		currentState.printFullState();
                System.out.printf("Iteration: %d\n", i);
                System.out.printf("Alpha: %f\n", alpha);
                System.out.printf("Gamma: %f\n", gamma);
                System.out.printf("Random Chance: %f\n", randomChance);
                
		System.out.println("--------------------------------------------------------------------------------------------------");
                System.out.println("State\t| NORTH\t\t| SOUTH\t\t| EAST\t\t| WEST\t\t| PICKUP\t| DROPOFF");
		System.out.println("--------------------------------------------------------------------------------------------------");
		List<State> entries = new ArrayList<>(qtable.keySet());
		entries.stream().sorted((qe1, qe2) -> qe1.compareByCol(qe2)).sorted((qe1, qe2) -> qe1.compareByRow(qe2))
				.sorted((qe1, qe2) -> qe1.compareByBlock(qe2))
				.forEach((e) -> {
					System.out.printf("%s\t| %+08.4f\t| %+08.4f\t| %+08.4f\t| %+08.4f\t| %+08.4f\t| %+08.4f\n",e,qtable.get(e)[0],qtable.get(e)[1],qtable.get(e)[2],qtable.get(e)[3],qtable.get(e)[4],qtable.get(e)[5]);
					if(e.agentRow==5 && e.agentCol==5 && e.hasBlock ==0){
						System.out.println();
                                                System.out.println("--------------------------------------------------------------------------------------------------");
						System.out.println("State\t| NORTH\t\t| SOUTH\t\t| EAST\t\t| WEST\t\t| PICKUP\t| DROPOFF");
						System.out.println("--------------------------------------------------------------------------------------------------");
					}
				});
		System.out.println();
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

	public void switchPickUpDropLocations() {
		int tp1r = p1r;
		int tp1c = p1c;
		int tp2r = p2r;
		int tp2c = p2c;
		int tp3r = p3r;
		int tp3c = p3c;
		p1r = d1r;
		p1c = d1c;
		p2r = d2r;
		p2c = d2c;
		p3r = d3r;
		p3c = d3c;
		d1r = tp1r;
		d1c = tp1c;
		d2r = tp2r;
		d2c = tp2c;
		d3r = tp3r;
		d3c = tp3c;
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
			BiConsumer<Simulation, Integer> cons) {
		if (pred.test(sim, i))
			cons.accept(sim, i);
	}

	public static void RunExperiment1(int iterations) {
		System.out.println("EXPERIMENT 1");
		Simulation sim = new Simulation(0.3, 0.3, 1.0);

		BiConsumer<Simulation, Integer> cons = (s,i) -> s.printQTable(i);
		BiPredicate<Simulation, Integer> pred = (s, i) -> {
			boolean firstHundred = i == 100;
			boolean firstDropOff = s.exactlyOneDropOffFilled() && !s.firstDropOffFilled;
			boolean isGoalState = s.currentState.isGoalState();

			if (firstDropOff){
				s.firstDropOffFilled = true;
                        }

			return firstHundred || firstDropOff || isGoalState;
		};

		System.out.println("STEPS TAKEN FIRST RUN " + sim.simulate(iterations, pred, cons) + "\n");
		sim.resetFullState();
		System.out.println("STEPS TAKEN SECOND RUN " + sim.simulate(iterations, pred, cons) + "\n");
	}

	public static void RunExperiment2(int iterations) {
		System.out.println("EXPERIMENT 2");
		Simulation sim = new Simulation(0.3, 0.3, 1.0);

		BiConsumer<Simulation, Integer> cons = (s,i) -> s.printQTable(i);
		BiPredicate<Simulation, Integer> pred = (s, i) -> {
			boolean firstHundred = i == 100;
			boolean everyHundred = i != 0 && i % 100 == 0;
			boolean firstDropOff = s.exactlyOneDropOffFilled() && !s.firstDropOffFilled;
			boolean isGoalState = s.currentState.isGoalState();

			if (firstDropOff)
				s.firstDropOffFilled = true;
                        
			 if (i == 99) // i starts at 0 so i == 99 is the 100th, needs to be Exploit 1 for i == 100
				 s.randomChance = 0.35; // Change it to Exploit 1

			return firstHundred || everyHundred || firstDropOff || isGoalState;
		};

		sim.setRandomChance(1.0);
		sim.simulate(100, pred, cons);
		sim.setRandomChance(0.35);
		System.out.println("STEPS TAKEN FIRST RUN " + (100 + sim.simulate(iterations - 100, pred, cons)));

		sim.resetFullState();

		sim.setRandomChance(1.0);
		sim.simulate(100, pred, cons);
		sim.setRandomChance(0.35);
		System.out.println("STEPS TAKEN SECOND RUN " + (100 + sim.simulate(iterations - 100, pred, cons)));
	}

	public static void RunExperiment3(int iterations) {
		System.out.println("EXPERIMENT 3");
		Simulation sim = new Simulation(0.3, 0.3, 1.0);
		BiConsumer<Simulation, Integer> cons = (s,i) -> s.printQTable(i);
		BiPredicate<Simulation, Integer> pred = (s, i) -> {
			if (i == 99) // i starts at 0 so i == 99 is the 100th, needs to be Exploit 2 for i == 100
				s.randomChance = 0.1; // Change it to Exploit 2
			 
			return s.currentState.isGoalState();
		};

		System.out.println("STEPS TAKEN FIRST RUN " + (sim.simulate(iterations, pred, cons)));

		sim.resetFullState();

		sim.setRandomChance(1.0);
		System.out.println("STEPS TAKEN SECOND RUN " + (sim.simulate(iterations, pred, cons)));
	}

	public static void RunExperiment4(int iterations) {
		System.out.println("EXPERIMENT 4");
		Simulation sim = new Simulation(0.5, 0.3, 1.0);
		BiConsumer<Simulation, Integer> cons = (s,i) -> s.printQTable(i);
		BiPredicate<Simulation, Integer> pred = (s, i) -> {
			boolean everyHundred = i != 0 && i % 100 == 0;
			boolean isGoalState = s.currentState.isGoalState();
			
			if (i == 99) // i starts at 0 so i == 99 is the 100th, needs to be Exploit 2 for i == 100
				s.randomChance = 0.1; // Change it to Exploit 2
			 
			return everyHundred || isGoalState;
		};

		System.out.println("STEPS TAKEN FIRST RUN " + (sim.simulate(iterations, pred, cons)));

		sim.resetFullState();

		sim.setRandomChance(1.0);
		System.out.println("STEPS TAKEN SECOND RUN " + (sim.simulate(iterations, pred, cons)));
	}

	public static void RunExperiment5(int iterations) {
		System.out.println("EXPERIMENT 5");
		Simulation sim = new Simulation(0.5, 0.3, 0.1);

		BiConsumer<Simulation, Integer> cons = (s,i) -> s.printQTable(i);
		BiPredicate<Simulation, Integer> pred = (s, i) -> {
			boolean firstHundred = i == 100;
			boolean firstDropOff = s.exactlyOneDropOffFilled() && !s.firstDropOffFilled;
			boolean isGoalState = s.currentState.isGoalState();

			if (firstDropOff)
				s.firstDropOffFilled = true;
			
			if (i == 99) // i starts at 0 so i == 99 is the 100th, needs to be Exploit 2 for i == 100
				s.randomChance = 0.1; // Change it to Exploit 2

			return firstHundred || firstDropOff || isGoalState;
		};

		System.out.println("STEPS TAKEN FIRST RUN " + (sim.simulate(iterations, pred, cons)));

		sim.resetFullState();
		sim.switchPickUpDropLocations();

		sim.setRandomChance(0.1);
		System.out.println("STEPS TAKEN SECOND RUN " + (sim.simulate(iterations, pred, cons)));
	}

	public static void RunExperiment6(int iterations) {
		System.out.println("EXPERIMENT 6");
		Simulation sim = new Simulation(0.5, 0.3, 1.0);
		BiConsumer<Simulation, Integer> cons = (s,i) -> s.printQTable(i);
		BiPredicate<Simulation, Integer> pred = (s, i) -> {
			if (i == 99) // i starts at 0 so i == 99 is the 100th, needs to be Exploit 1 for i == 100
				s.randomChance = 0.35; // Change it to Exploit 1
			
			return s.currentState.isGoalState();
		};

		System.out.println("STEPS TAKEN FIRST RUN " + (sim.simulate(iterations, pred, cons)));

		sim.resetFullState();

		// change pickup locations
		sim.p1r = 2;
		sim.p1c = 2;
		sim.p2r = 4;
		sim.p2c = 4;
		sim.p3r = 1;
		sim.p3c = 5;

		sim.setRandomChance(1.0);
		System.out.println("STEPS TAKEN SECOND RUN " + (sim.simulate(iterations, pred, cons)));
	}

	public static void main(String[] args) {
		int iterations = 10000;
                
                //QTableGUI qTableGUI = QTableGUI.GetQTableGUI();
                //qTableGUI.setVisible(true);
                
		RunExperiment1(iterations);
		RunExperiment2(iterations);
		RunExperiment3(iterations);
		RunExperiment4(iterations);
		RunExperiment5(iterations);
		RunExperiment6(iterations);
                
                //qTableGUI.setLocationValues(0, 0, 0.1f, 0.1f, 0.1f, 0.1f);
	}
}
