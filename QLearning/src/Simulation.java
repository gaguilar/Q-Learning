import java.util.HashMap;

public class Simulation {
	public enum Occupant {
		Empty, PickUp, DropOff
	}

	Occupant[][] board = { 
			{ Occupant.PickUp, Occupant.Empty, Occupant.Empty, Occupant.Empty, Occupant.Empty },
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
	public void updateQTable(QEntry entry){
		/*
		double oldUtility = getUtility(entry);
		double newUtility = (1 - alpha) * oldUtility + alpha * (entry.getImmediateReward() + gamma * getMaxUtilityNextMove(entry));
		qtable.put(entry, newUtility);
		*/
	}

	// Q(s, a) look up Q value for being in some state and taking some action. If a QEntry does not exist add it with a default utility value.
	public double getUtility(QEntry entry) {
		if (!qtable.containsKey(entry))
			qtable.put(entry, 0.0);
		return qtable.get(entry);
	}

	public void simulate(int maxSteps) {
		for(int i = 0; i < maxSteps; i++){
			
		}
	}

	public static void main(String[] args) {
		Simulation sim = new Simulation(0.9, 0.1);
		sim.simulate(1);
	}
}
