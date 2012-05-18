package uk.ac.imperial.lpgdash;

import java.util.Set;
import java.util.UUID;

import uk.ac.imperial.lpgdash.actions.Appropriate;
import uk.ac.imperial.lpgdash.actions.Demand;
import uk.ac.imperial.lpgdash.actions.Provision;
import uk.ac.imperial.presage2.core.environment.ActionHandlingException;
import uk.ac.imperial.presage2.core.environment.ParticipantSharedState;
import uk.ac.imperial.presage2.core.environment.UnavailableServiceException;
import uk.ac.imperial.presage2.core.messaging.Input;
import uk.ac.imperial.presage2.util.participant.AbstractParticipant;

public class LPGPlayer extends AbstractParticipant {

	double g = 0;
	double q = 0;
	double satisfaction;

	protected LPGService game;

	public LPGPlayer(UUID id, String name) {
		super(id, name);
	}

	@Override
	protected void processInput(Input in) {

	}

	@Override
	public void initialise() {
		super.initialise();
		try {
			this.game = this.getEnvironmentService(LPGService.class);
		} catch (UnavailableServiceException e) {
			logger.warn("Couldn't get environment service", e);
		}
	}

	@Override
	protected Set<ParticipantSharedState> getSharedState() {
		Set<ParticipantSharedState> ss = super.getSharedState();
		return ss;
	}

	@Override
	public void execute() {
		super.execute();
		if (game.getRound() == RoundType.DEMAND) {
			// update g and q for this round
			g = game.getG(getID());
			q = game.getQ(getID());
			logger.info("g=" + g + ",q=" + q);
			try {
				environment.act(new Provision(g), getID(), authkey);
				environment.act(new Demand(q), getID(), authkey);
			} catch (ActionHandlingException e) {
				logger.warn("Failed to act", e);
			}
		} else if (game.getRound() == RoundType.APPROPRIATE) {
			try {
				environment.act(new Appropriate(game.getR(getID())), getID(),
						authkey);
			} catch (ActionHandlingException e) {
				logger.warn("Failed to act", e);
			}
		}
	}

}
