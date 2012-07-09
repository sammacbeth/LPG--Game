package uk.ac.imperial.lpgdash;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import uk.ac.imperial.lpgdash.actions.Appropriate;
import uk.ac.imperial.lpgdash.actions.Demand;
import uk.ac.imperial.lpgdash.actions.JoinCluster;
import uk.ac.imperial.lpgdash.actions.LeaveCluster;
import uk.ac.imperial.lpgdash.actions.Provision;
import uk.ac.imperial.lpgdash.facts.Cluster;
import uk.ac.imperial.presage2.core.db.persistent.TransientAgentState;
import uk.ac.imperial.presage2.core.environment.ActionHandlingException;
import uk.ac.imperial.presage2.core.environment.ParticipantSharedState;
import uk.ac.imperial.presage2.core.environment.UnavailableServiceException;
import uk.ac.imperial.presage2.core.messaging.Input;
import uk.ac.imperial.presage2.core.util.random.Random;
import uk.ac.imperial.presage2.util.participant.AbstractParticipant;

public class LPGPlayer extends AbstractParticipant {

	double g = 0;
	double q = 0;
	double d = 0;
	double p = 0;

	double a = 2;
	double b = 1;
	double c = 3;

	double pCheat = 0.0;

	double alpha = .1;
	double beta = .1;
	double satisfaction = 0.5;

	Cheat cheatOn = Cheat.PROVISION;

	Cluster cluster = null;
	Map<Cluster, Double> clusterSatisfaction = new HashMap<Cluster, Double>();

	protected LPGService game;

	public LPGPlayer(UUID id, String name) {
		super(id, name);
	}

	public LPGPlayer(UUID id, String name, double pCheat, double alpha,
			double beta) {
		super(id, name);
		this.pCheat = pCheat;
		this.alpha = alpha;
		this.beta = beta;
	}

	public LPGPlayer(UUID id, String name, double pCheat, double alpha,
			double beta, Cheat cheatOn) {
		this(id, name, pCheat, alpha, beta);
		this.cheatOn = cheatOn;
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
		if (this.persist != null) {
			this.persist.setProperty("pCheat", Double.toString(this.pCheat));
			this.persist.setProperty("a", Double.toString(this.a));
			this.persist.setProperty("b", Double.toString(this.b));
			this.persist.setProperty("c", Double.toString(this.c));
			this.persist.setProperty("alpha", Double.toString(this.alpha));
			this.persist.setProperty("beta", Double.toString(this.beta));
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
		this.cluster = this.game.getCluster(getID());

		if (this.cluster == null) {
			return;
		}

		if (game.getRound() == RoundType.DEMAND) {
			if (game.getRoundNumber() > 1) {
				// determine utility gained from last round
				calculateScores();
			}
			if (game.getRoundNumber() % 20 == 0) {
				assessClusterMembership();
				if (this.cluster == null) {
					return;
				}
			}
			// update g and q for this round
			g = game.getG(getID());
			q = game.getQ(getID());

			if ((this.cheatOn == Cheat.PROVISION || this.cheatOn == Cheat.DEMAND)
					&& Random.randomDouble() < pCheat) {
				switch (this.cheatOn) {
				case PROVISION:
				default:
					// cheat: provision less than g
					provision(g * Random.randomDouble());
					demand(q);
					break;
				case DEMAND:
					provision(g);
					demand(q + Random.randomDouble() * (1 - q));
					break;
				}
			} else {
				provision(g);
				demand(q);
			}

		} else if (game.getRound() == RoundType.APPROPRIATE) {
			if (this.cheatOn == Cheat.APPROPRIATE
					&& Random.randomDouble() < pCheat) {
				double allocated = game.getAllocated(getID());
				appropriate(allocated + Random.randomDouble() * (1 - allocated));
			} else {
				appropriate(game.getAllocated(getID()));
			}
		}
	}

	protected void demand(double d) {
		try {
			environment.act(new Demand(d), getID(), authkey);
			this.d = d;
		} catch (ActionHandlingException e) {
			logger.warn("Failed to demand", e);
		}
	}

	protected void provision(double p) {
		try {
			environment.act(new Provision(p), getID(), authkey);
			this.p = p;
		} catch (ActionHandlingException e) {
			logger.warn("Failed to provision", e);
		}
	}

	protected void appropriate(double r) {
		try {
			environment.act(new Appropriate(r), getID(), authkey);
		} catch (ActionHandlingException e) {
			logger.warn("Failed to appropriate", e);
		}
	}

	protected void leaveCluster() {
		try {
			environment.act(new LeaveCluster(this.cluster), getID(), authkey);
		} catch (ActionHandlingException e) {
			logger.warn("Failed to leave cluster", e);
		}
	}

	protected void joinCluster(Cluster c) {
		try {
			environment.act(new JoinCluster(c), getID(), authkey);
		} catch (ActionHandlingException e) {
			logger.warn("Failed to join cluster", e);
		}
	}

	protected void calculateScores() {
		double r = game.getAllocated(getID());
		double rP = game.getAppropriated(getID());
		double rTotal = rP + (this.g - this.p);
		double u = 0;
		if (rTotal >= q)
			u = a * q + b * (rTotal - q);
		else
			u = a * rTotal - c * (q - rTotal);

		if (r >= d)
			satisfaction = satisfaction + alpha * (1 - satisfaction);
		else
			satisfaction = satisfaction - beta * satisfaction;

		logger.info("[" + cluster + ", g=" + g + ", q=" + q + ", d=" + d
				+ ", p=" + p + ", r=" + r + ", r'=" + rP + ", R=" + rTotal
				+ ", U=" + u + ", o=" + satisfaction + "]");

		if (this.persist != null) {
			TransientAgentState state = this.persist.getState(game
					.getRoundNumber() - 1);
			state.setProperty("g", Double.toString(g));
			state.setProperty("q", Double.toString(q));
			state.setProperty("d", Double.toString(d));
			state.setProperty("p", Double.toString(p));
			state.setProperty("r", Double.toString(r));
			state.setProperty("r'", Double.toString(rP));
			state.setProperty("RTotal", Double.toString(rTotal));
			state.setProperty("U", Double.toString(u));
			state.setProperty("o", Double.toString(satisfaction));
			state.setProperty("cluster", Integer.toString(this.cluster.getId()));
		}
	}

	private void assessClusterMembership() {
		if (clusterSatisfaction.size() == 0) {
			Set<Cluster> availableClusters = this.game.getClusters();
			for (Cluster c : availableClusters) {
				if (this.cluster.equals(c))
					clusterSatisfaction.put(c, this.satisfaction);
				else
					clusterSatisfaction.put(c, 0.5);
			}
		} else {
			clusterSatisfaction.put(this.cluster, this.satisfaction);
		}
		Cluster preferred = this.cluster;
		double maxSatisfaction = this.satisfaction;
		for (Map.Entry<Cluster, Double> e : clusterSatisfaction.entrySet()) {
			if (e.getValue() > maxSatisfaction) {
				maxSatisfaction = e.getValue();
				preferred = e.getKey();
			}
		}
		if (maxSatisfaction < 0.1) {
			leaveCluster();
			this.cluster = null;
		} else if (!preferred.equals(this.cluster)) {
			leaveCluster();
			joinCluster(preferred);
			this.cluster = preferred;
			this.satisfaction = clusterSatisfaction.get(preferred);
		}
	}
}
