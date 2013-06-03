package uk.ac.imperial.lpgdash;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math.stat.descriptive.SummaryStatistics;

import uk.ac.imperial.lpgdash.actions.Appropriate;
import uk.ac.imperial.lpgdash.actions.CreateCluster;
import uk.ac.imperial.lpgdash.actions.Demand;
import uk.ac.imperial.lpgdash.actions.JoinCluster;
import uk.ac.imperial.lpgdash.actions.LeaveCluster;
import uk.ac.imperial.lpgdash.actions.Provision;
import uk.ac.imperial.lpgdash.allocators.LegitimateClaims;
import uk.ac.imperial.lpgdash.facts.Allocation;
import uk.ac.imperial.lpgdash.facts.Cluster;
import uk.ac.imperial.presage2.core.db.StorageService;
import uk.ac.imperial.presage2.core.db.persistent.TransientAgentState;
import uk.ac.imperial.presage2.core.environment.ActionHandlingException;
import uk.ac.imperial.presage2.core.environment.ParticipantSharedState;
import uk.ac.imperial.presage2.core.environment.UnavailableServiceException;
import uk.ac.imperial.presage2.core.messaging.Input;
import uk.ac.imperial.presage2.util.participant.AbstractParticipant;

public class LPGPlayer extends AbstractParticipant {

	enum ClusterLeaveAlgorithm {
		INSTANT, THRESHOLD, UTILITY
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
	double tau = .1;

	int numClustersCreated = 0;
	int maxClustersCreated = 100;
	int minWaitingAgents = 10;

	Cheat cheatOn = Cheat.PROVISION;

	Cluster cluster = null;
	Map<Cluster, SummaryStatistics> clusterUtilities = new HashMap<Cluster, SummaryStatistics>();
	DescriptiveStatistics rollingUtility = new DescriptiveStatistics(100);
	SummaryStatistics overallUtility = new SummaryStatistics();

	double size = 1;

	protected LPGService game;
	private Random rnd;

	ClusterLeaveAlgorithm clusterLeave = ClusterLeaveAlgorithm.THRESHOLD;
	ClusterSelect clusterSelection;
	boolean resetSatisfaction = false;
	boolean permCreateCluster = false;
	boolean dead = false;

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

	public LPGPlayer(UUID id, String name, double a, double b, double c,
			double pCheat, double alpha, double beta, Cheat cheatOn,
			ClusterLeaveAlgorithm clLeave, boolean resetSatisfaction,
			double size, long rndSeed) {
		this(id, name, pCheat, alpha, beta);
		this.a = a;
		this.b = b;
		this.c = c;
		this.cheatOn = cheatOn;
		this.resetSatisfaction = resetSatisfaction;
		this.size = size;
		this.rnd = new Random(rndSeed);
		this.clusterLeave = clLeave;
		int leaveThreshold = 3;
		switch (clLeave) {
		case INSTANT:
			leaveThreshold = 1;
		case THRESHOLD:
			this.clusterSelection = new SatisfiedClustering(leaveThreshold);
			break;
		case UTILITY:
			this.clusterSelection = new UtilityClustering();
		}
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

	@SuppressWarnings("deprecation")
	@Override
	public void execute() {
		super.execute();
		this.cluster = this.game.getCluster(getID());

		if (dead)
			return;

		if (!dead && permCreateCluster && this.cluster == null
				&& game.getRoundNumber() > 1
				&& game.getRound() == RoundType.DEMAND) {
			clusterSelection.assessClusters();
			if (this.cluster == null)
				assessClusterCreation();
		}

		if (game.getRound() == RoundType.DEMAND) {
			if (this.cluster != null && game.getRoundNumber() > 1) {
				// determine utility gained from last round
				calculateScores();
			}
			if (!(clusterLeave == ClusterLeaveAlgorithm.INSTANT)
					|| game.getRoundNumber() % 20 == 0) {
				clusterSelection.assessClusters();
				if (this.cluster == null) {
					return;
				}
			}
			// update g and q for this round
			g = game.getG(getID());
			q = game.getQ(getID());

			if ((this.cheatOn == Cheat.PROVISION || this.cheatOn == Cheat.DEMAND)
					&& rnd.nextDouble() < pCheat) {
				switch (this.cheatOn) {
				case PROVISION:
				default:
					// cheat: provision less than g
					provision(g * rnd.nextDouble());
					demand(q);
					break;
				case DEMAND:
					provision(g);
					demand(q + rnd.nextDouble() * (1 - q));
					break;
				}
			} else {
				provision(g);
				demand(q);
			}

		} else if (game.getRound() == RoundType.APPROPRIATE) {
			if (this.cheatOn == Cheat.APPROPRIATE && rnd.nextDouble() < pCheat) {
				double allocated = game.getAllocated(getID());
				appropriate(allocated + rnd.nextDouble() * (1 - allocated));
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
		if (this.cluster == null)
			return;
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
			this.cluster = c;
		} catch (ActionHandlingException e) {
			logger.warn("Failed to join cluster", e);
		}
	}

	protected Cluster createCluster() {
		try {
			Allocation[] methods = { Allocation.RANDOM, Allocation.QUEUE,
					Allocation.LC_FIXED, Allocation.LC_SO };
			int pick = rnd.nextInt(methods.length);
			Allocation method = methods[pick];
			Cluster newCluster = new Cluster(this.game.getNextNumCluster(),
					method);
			if (newCluster.isLC()) {
				LegitimateClaims lc = new LegitimateClaims(newCluster,
						this.game.session, this.game);
				StorageService sto = (StorageService) this.game.session
						.getGlobal("storage");
				lc.setStorage(sto);
				lc.setGamma(0.1);
				lc.enableHack = true;
				lc.soHd = true;
				lc.rankMemory = 1;
				this.game.session.insert(lc);
			}
			environment.act(new CreateCluster(newCluster), getID(), authkey);
			numClustersCreated++;
			return newCluster;
		} catch (ActionHandlingException e) {
			logger.warn("Failed to create cluster", e);
		}
		return null;
	}

	protected void calculateScores() {
		double r = game.getAllocated(getID());
		double rP = game.getAppropriated(getID());
		double rTotal = rP + (this.g - this.p);
		double u = 0;
		if (rTotal >= q)
			u = a * q + b * (rTotal - q);
		else
			// alternate utility: use b instead of a for accrued resource payoff
			u = (Globals.alternateUtility ? b : a) * rTotal - c * (q - rTotal);

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

		if (!clusterUtilities.containsKey(this.cluster)) {
			SummaryStatistics s = new SummaryStatistics();
			clusterUtilities.put(cluster, s);
		}
		clusterUtilities.get(this.cluster).addValue(u);
		rollingUtility.addValue(u);
		overallUtility.addValue(u);
	}

	private void assessClusterCreation() {
		Set<UUID> op = this.game.getOrphanPlayers();
		if (op.size() >= this.minWaitingAgents) {
			logger.info("More than " + this.minWaitingAgents
					+ " orphans... why not create cluster?");
			if (numClustersCreated < maxClustersCreated) {
				createCluster();
			}
		}
	}

	interface ClusterSelect {
		void assessClusters();
	}

	class SatisfiedClustering implements ClusterSelect {
		int dissatisfactionCount = 0;
		int leaveThreshold = 3;
		Map<Cluster, Double> clusterSatisfaction = new HashMap<Cluster, Double>();
		Set<Cluster> definitelyLeftClusters = new HashSet<Cluster>();

		SatisfiedClustering() {
			super();
		}

		SatisfiedClustering(int leaveThreshold) {
			super();
			this.leaveThreshold = leaveThreshold;
		}

		public void assessClusters() {
			// Add new available clusters
			Set<Cluster> availableClusters = game.getClusters();
			// logger.info("Avail cluster: " + availableClusters);
			for (Cluster c : availableClusters) {
				if (!clusterSatisfaction.containsKey(c)
						&& !definitelyLeftClusters.contains(c)) {
					clusterSatisfaction.put(c, 0.5);
				}
				if (definitelyLeftClusters.contains(c)) {
					clusterSatisfaction.remove(c);
				}
			}
			// Remove non existing clusters -- use iterator to avoid
			// concurrentModificationException
			Iterator<Entry<Cluster, Double>> it = clusterSatisfaction
					.entrySet().iterator();
			while (it.hasNext()) {
				Entry<Cluster, Double> entry = it.next();
				if (!availableClusters.contains(entry.getKey())) {
					it.remove();
				}
			}
			if (cluster != null) {
				clusterSatisfaction.put(cluster, satisfaction);
			}

			Cluster preferred = preferredCluster();
			if (preferred == null)
				return;
			double maxSatisfaction = clusterSatisfaction.get(preferred);
			// if satisfaction is below threshold or other cluster's
			// satisfaction then increment consecutive dissatisfaction count,
			// otherwise reset it to 0.
			if (maxSatisfaction < tau || !preferred.equals(cluster)) {
				this.dissatisfactionCount++;
			} else {
				this.dissatisfactionCount = 0;
			}

			boolean leaveCluster = this.dissatisfactionCount >= this.leaveThreshold;
			if (leaveCluster) {
				if (cluster == null) {
					joinCluster(preferred);
					cluster = preferred;
					satisfaction = clusterSatisfaction.get(cluster);
				} else if (!preferred.equals(cluster)) {
					if (clusterSatisfaction.get(cluster) < tau) {
						definitelyLeftClusters.add(cluster);
					}
					leaveCluster();
					joinCluster(preferred);
					cluster = preferred;
					satisfaction = clusterSatisfaction.get(cluster);
				} else {
					if (clusterSatisfaction.get(cluster) < tau) {
						definitelyLeftClusters.add(cluster);
					}
					leaveCluster();
					cluster = null;
				}
			}
		}

		Cluster preferredCluster() {
			Cluster preferred = cluster;
			double maxSatisfaction = satisfaction;
			for (Map.Entry<Cluster, Double> e : clusterSatisfaction.entrySet()) {
				if (e.getValue() > maxSatisfaction) {
					maxSatisfaction = e.getValue();
					preferred = e.getKey();
				}
			}
			return preferred;
		}

	}

	class UtilityClustering implements ClusterSelect {
		double deathThreshold = -100;
		double leaveThreshold = -20;
		Set<Cluster> definitelyLeftClusters = new HashSet<Cluster>();

		@Override
		public void assessClusters() {
			// death
			if (overallUtility.getSum() < deathThreshold) {
				if (cluster != null)
					leaveCluster();
				dead = true;
				return;
			}
			checkNewClusters();

			// find preferred cluster
			Cluster preferred = cluster;
			double maxUtility = clusterUtilities.containsKey(preferred) ? clusterUtilities
					.get(cluster).getSum() : deathThreshold;
			for (Entry<Cluster, SummaryStatistics> e : clusterUtilities
					.entrySet()) {
				if (e.getValue().getSum() > maxUtility + 5) {
					maxUtility = e.getValue().getSum();
					preferred = e.getKey();
				}
			}

			if (preferred == null) {
				return;
			}
			if (!preferred.equals(cluster) && maxUtility > leaveThreshold) {
				leaveCluster();
				joinCluster(preferred);
				cluster = preferred;
			} else if (overallUtility.getN() > 10
					&& maxUtility < leaveThreshold && cluster != null) {
				leaveCluster();
			}
		}

		private void checkNewClusters() {
			// initialise/update cluster utilities
			// Add new available clusters
			Set<Cluster> availableClusters = game.getClusters();
			// logger.info("Avail cluster: " + availableClusters);
			for (Cluster c : availableClusters) {
				if (!clusterUtilities.containsKey(c)) {
					SummaryStatistics s = new SummaryStatistics();
					s.addValue(0);
					clusterUtilities.put(c, s);
				}
			}
			// Remove non existing clusters -- use iterator to avoid
			// concurrentModificationException
			Iterator<Entry<Cluster, SummaryStatistics>> it = clusterUtilities
					.entrySet().iterator();
			while (it.hasNext()) {
				Entry<Cluster, SummaryStatistics> entry = it.next();
				if (!availableClusters.contains(entry.getKey())) {
					it.remove();
				}
			}
		}

	}
}
