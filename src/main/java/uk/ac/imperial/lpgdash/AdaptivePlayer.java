package uk.ac.imperial.lpgdash;

import java.util.UUID;

public class AdaptivePlayer extends LPGPlayer {

	public AdaptivePlayer(UUID id, String name, double a, double b, double c,
			double pCheat, double alpha, double beta, Cheat cheatOn,
			ClusterLeaveAlgorithm clLeave, boolean resetSatisfaction,
			double size, long rndSeed) {
		super(id, name, a, b, c, pCheat, alpha, beta, cheatOn, clLeave,
				resetSatisfaction, size, rndSeed);
	}

	@Override
	protected void calculateScores() {
		super.calculateScores();
		if (game.getRoundNumber() % 25 == 0) {
			if (compliantUtility.getN() > 0 && nonCompliantUtility.getN() > 0) {
				if (compliantUtility.getMean() >= nonCompliantUtility.getMean()) {
					pCheat = Math.max(pCheat - 0.05, 0.05);
					logger.info("Revise strategy: pCheat - 0.05 = " + pCheat);
				} else {
					pCheat = Math.min(pCheat + 0.05, 1.0);
					logger.info("Revise strategy: pCheat + 0.05 = " + pCheat);
				}
			}
		}
	}

}
