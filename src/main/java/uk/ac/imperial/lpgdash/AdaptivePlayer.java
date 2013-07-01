package uk.ac.imperial.lpgdash;

import java.util.UUID;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;

public class AdaptivePlayer extends LPGPlayer {

	final int strategyLength = 20;
	boolean[] strategy;
	int strategyPtr = 0;

	double prevUtility = 0;
	boolean prevChange;

	int complyCount = 0;
	int defectCount = 0;

	public AdaptivePlayer(UUID id, String name, double a, double b, double c,
			double pCheat, double alpha, double beta, Cheat cheatOn,
			ClusterLeaveAlgorithm clLeave, boolean resetSatisfaction,
			double size, long rndSeed, double t1, double t2) {
		super(id, name, a, b, c, pCheat, alpha, beta, cheatOn, clLeave,
				resetSatisfaction, size, rndSeed, t1, t2);
		// generate strategy.
		this.strategy = new boolean[strategyLength];
		for (int i = 0; i < strategyLength; i++) {
			if (rnd.nextDouble() < this.pCheat) {
				this.strategy[i] = false;
				defectCount++;
			} else {
				this.strategy[i] = true;
				complyCount++;
			}
		}
		this.prevChange = rnd.nextBoolean();
		this.rollingUtility = new DescriptiveStatistics(strategyLength);
		this.pCheat = ((double) defectCount) / strategyLength;
		logger.info("Initial strategy: " + strategyToString(strategy));
	}

	@Override
	protected boolean chooseStrategy() {
		boolean strat = this.strategy[strategyPtr++];
		if (strategyPtr >= strategyLength) {
			// modify strategy
			double currentUtility = this.rollingUtility.getMean();
			if (currentUtility > prevUtility) {
				// last change worked, do it again
				modifyStrategy();
			} else {
				// revert last change
				prevChange = !prevChange;
				modifyStrategy();
			}
			this.strategyPtr = 0;
			this.prevUtility = currentUtility;
			this.rollingUtility.clear();
		}
		return strat;
	}

	private void modifyStrategy() {
		if (prevChange && defectCount == 0 || !prevChange && complyCount == 0) {
			logger.info("Cannot modify strategy, already pure.");
			return;
		}
		do {
			int i = rnd.nextInt(strategyLength);
			if (this.strategy[i] != prevChange) {
				this.strategy[i] = prevChange;
				if (prevChange) {
					defectCount--;
					complyCount++;
				} else {
					defectCount++;
					complyCount--;
				}
				break;
			}
		} while (true);
		this.pCheat = ((double) defectCount) / strategyLength;
		logger.info("Updated strategy: " + strategyToString(strategy));
	}

	private static String strategyToString(boolean[] strategy) {
		StringBuilder s = new StringBuilder();
		for (int i = 0; i < strategy.length; i++) {
			s.append(strategy[i] ? 'C' : 'D');
		}
		return s.toString();
	}
}
