package uk.ac.imperial.lpgdash;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math.stat.descriptive.SummaryStatistics;

import uk.ac.imperial.lpgdash.actions.Appropriate;
import uk.ac.imperial.lpgdash.actions.Demand;
import uk.ac.imperial.lpgdash.actions.JoinCluster;
import uk.ac.imperial.lpgdash.actions.LeaveCluster;
import uk.ac.imperial.lpgdash.actions.Provision;
import uk.ac.imperial.lpgdash.facts.Cluster;
import uk.ac.imperial.lpgdash.util.BolzmannDistribution;
import uk.ac.imperial.presage2.core.db.persistent.TransientAgentState;
import uk.ac.imperial.presage2.core.environment.ActionHandlingException;
import uk.ac.imperial.presage2.core.environment.ParticipantSharedState;
import uk.ac.imperial.presage2.core.environment.UnavailableServiceException;
import uk.ac.imperial.presage2.core.messaging.Input;
import uk.ac.imperial.presage2.core.util.random.Random;
import uk.ac.imperial.presage2.util.participant.AbstractParticipant;

public class LPGPlayer extends AbstractParticipant {

	enum ClusterLeaveAlgorithm {
		INSTANT, THRESHOLD, NEGATIVE_UTILITY
	};

	enum ClusterSelectionAlgorithm {
		PREFERRED, BOLTZMANN
	};

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
	Map<Cluster, SummaryStatistics> clusterUtilities = new HashMap<Cluster, SummaryStatistics>();
	DescriptiveStatistics rollingUtility = new DescriptiveStatistics(100);

	double size = 1;

	protected LPGService game;

	ClusterLeaveAlgorithm clusterLeave = ClusterLeaveAlgorithm.THRESHOLD;
	ClusterSelectionAlgorithm clusterSelection = ClusterSelectionAlgorithm.BOLTZMANN;
	int clusterDissatisfactionCount = 0;
	int leaveThreshold = 3;
	boolean resetSatisfaction = false;

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
			double beta, Cheat cheatOn, ClusterLeaveAlgorithm clLeave,
			ClusterSelectionAlgorithm clSel, boolean resetSatisfaction,
			double size) {
		this(id, name, pCheat, alpha, beta);
		this.cheatOn = cheatOn;
		this.clusterLeave = clLeave;
		this.clusterSelection = clSel;
		this.resetSatisfaction = resetSatisfaction;
		this.size = size;
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
			this.persist.setProperty("cheatOn", this.cheatOn.name());
		}
	}

	@Override
	protected Set<ParticipantSharedState> getSharedState() {
		Set<ParticipantSharedState> ss = super.getSharedState();
		return ss;
	}

	@Override
	public void execute() {
		this.cluster = this.game.getCluster(getID());

		if (this.cluster == null) {
			return;
		}

		if (game.getRound() == RoundType.DEMAND) {
			if (game.getRoundNumber() > 1) {
				// determine utility gained from last round
				calculateScores();
			}
			if (!(clusterLeave == ClusterLeaveAlgorithm.INSTANT)
					|| game.getRoundNumber() % 20 == 0) {
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
			this.cluster = null;
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

		if (rP >= d)
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

		// initialise/update cluster utilities
		if (clusterUtilities.size() == 0) {
			Set<Cluster> availableClusters = this.game.getClusters();
			for (Cluster c : availableClusters) {
				clusterUtilities.put(c, new SummaryStatistics());
			}
		}
		clusterUtilities.get(this.cluster).addValue(u);
		rollingUtility.addValue(u);
	}

	private void assessClusterMembership() {
		// initialise/update cluster satisfaction
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
		// if satisfaction is below threshold or other cluster's satisfaction
		// then increment consecutive dissatisfaction count, otherwise reset it
		// to 0.
		if (maxSatisfaction < 0.1 || !preferred.equals(this.cluster)) {
			this.clusterDissatisfactionCount++;
		} else {
			this.clusterDissatisfactionCount = 0;
		}
		// if we are dissatisfied for greater than the leave threshold, leave or
		// change cluster.
		boolean leaveCluster;
		switch (clusterLeave) {
		case INSTANT:
			leaveCluster = (this.satisfaction < 0.1);
			break;
		case NEGATIVE_UTILITY:
			leaveCluster = rollingUtility.getN() > 50
					&& rollingUtility.getSum() < 0;
			break;
		case THRESHOLD:
		default:
			leaveCluster = this.clusterDissatisfactionCount >= this.leaveThreshold;
		}
		if (leaveCluster) {
			if (clusterSelection == ClusterSelectionAlgorithm.PREFERRED) {
				if (!preferred.equals(this.cluster)) {
					leaveCluster();
					joinCluster(preferred);
					this.cluster = preferred;
					this.satisfaction = clusterSatisfaction.get(preferred);
				} else {
					leaveCluster();
					this.cluster = null;
				}
			} else if (clusterSelection == ClusterSelectionAlgorithm.BOLTZMANN) {
				BolzmannDistribution<Cluster> b = new BolzmannDistribution<Cluster>();
				// find clusters with no recorded utilities
				List<Cluster> unknownClusters = new ArrayList<Cluster>();
				for (Map.Entry<Cluster, SummaryStatistics> e : clusterUtilities
						.entrySet()) {
					if (e.getValue().getN() == 0)
						unknownClusters.add(e.getKey());
					else
						b.addValue(e.getKey(), e.getValue().getMean());
				}

				// join random unknown cluster
				if (unknownClusters.size() > 0) {
					leaveCluster();
					logger.info("Chose unknown by random.");
					joinCluster(unknownClusters.get(Random
							.randomInt(unknownClusters.size())));
				} else {
					Cluster chosen = b.choose();
					logger.info("Chose " + chosen + " by Boltzmann dist.");
					if (chosen != this.cluster) {
						leaveCluster();
						joinCluster(chosen);
						if (resetSatisfaction) {
							clusterSatisfaction.put(chosen, 0.5);
						}
						this.satisfaction = clusterSatisfaction.get(chosen);
					}
				}
			}
		}
	}
}
