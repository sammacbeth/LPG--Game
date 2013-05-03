package uk.ac.imperial.lpgdash;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import uk.ac.imperial.lpgdash.actions.Appropriate;
import uk.ac.imperial.lpgdash.actions.CreateCluster;
import uk.ac.imperial.lpgdash.actions.Demand;
import uk.ac.imperial.lpgdash.actions.JoinCluster;
import uk.ac.imperial.lpgdash.actions.LeaveCluster;
import uk.ac.imperial.lpgdash.actions.Provision;
import uk.ac.imperial.lpgdash.allocators.LegitimateClaims;
import uk.ac.imperial.lpgdash.facts.Allocation;
import uk.ac.imperial.lpgdash.facts.Cluster;
import uk.ac.imperial.lpgdash.facts.Player;
import uk.ac.imperial.presage2.core.db.StorageService;
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
	double tau = .1;
	
	int numClustersCreated = 0;
	int maxClustersCreated = 100;
	int minWaitingAgents = 10;

	Cheat cheatOn = Cheat.PROVISION;

	Cluster cluster = null;
	Map<Cluster, Double> clusterSatisfaction; 
	Set<Cluster> definitelyLeftClusters;
	
	protected LPGService game;

	boolean newLeave = true;
	int clusterDissatisfactionCount = 0;
	int leaveThreshold = 3;

	public LPGPlayer(UUID id, String name) {
		super(id, name);
	}

	public LPGPlayer(UUID id, String name, double pCheat, double alpha,
			double beta) {
		super(id, name);
		this.pCheat = pCheat;
		this.alpha = alpha;
		this.beta = beta;
		clusterSatisfaction = new HashMap<Cluster, Double>();
		definitelyLeftClusters = new HashSet<Cluster>();
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

		if (this.cluster == null && game.getRoundNumber() > 1) {
			assessClusterMembership();
			if (this.cluster==null)
				assessClusterCreation();
			if (game.getRound() == RoundType.APPROPRIATE) 
				return;
		}

		if (game.getRound() == RoundType.DEMAND) {
			if (this.cluster!=null && game.getRoundNumber() > 1) {
				// determine utility gained from last round
				calculateScores();
			}
			if (newLeave || game.getRoundNumber() % 20 == 0) {
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

		} else if (this.cluster!=null){
			
			if (game.getRound() == RoundType.APPROPRIATE) {
				if (this.cheatOn == Cheat.APPROPRIATE
						&& Random.randomDouble() < pCheat) {
					double allocated = game.getAllocated(getID());
					appropriate(allocated + Random.randomDouble() * (1 - allocated));
				} else {
					appropriate(game.getAllocated(getID()));
				}
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
		if (this.cluster==null) return;
		try {
			environment.act(new LeaveCluster(this.cluster), getID(), authkey);
			if (clusterSatisfaction.get(this.cluster) < this.tau) definitelyLeftClusters.add(this.cluster);
			this.cluster = null;
		} catch (ActionHandlingException e) {
			logger.warn("Failed to leave cluster", e);
		}
	}

	protected void joinCluster(Cluster c) {
		try {
			environment.act(new JoinCluster(c), getID(), authkey);
			this.cluster = c;
			this.satisfaction = clusterSatisfaction.get(c);
		} catch (ActionHandlingException e) {
			logger.warn("Failed to join cluster", e);
		}
	}
	
	protected Cluster createCluster() {
		try {
			Allocation[] methods = {Allocation.RANDOM, Allocation.RATION, Allocation.LC_SO};
			int pick = Random.randomInt(3);
			Allocation method = methods[pick];
			Cluster newCluster = new Cluster(this.game.getNextNumCluster(),method);
			if (newCluster.isLC()) {
				LegitimateClaims lc = new LegitimateClaims(newCluster, this.game.session,
						this.game);
				StorageService sto = (StorageService) this.game.session.getGlobal("storage");
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
		 
		//  Update cluster satisfaction set (to account for newly created clusters)
		Set<Cluster> availableClusters = this.game.getClusters();
		logger.info("Avail cluster: " + availableClusters);
		for (Cluster c : availableClusters) {
			if (!clusterSatisfaction.containsKey(c) && !definitelyLeftClusters.contains(c)){
				clusterSatisfaction.put(c, 0.5);
			}
			if (definitelyLeftClusters.contains(c)){
				clusterSatisfaction.remove(c);
			}
		}

		// Remove non existing clusters -- use iterator to avoid concurrentModificationException
		Iterator<Entry<Cluster, Double>> it = clusterSatisfaction.entrySet().iterator();
		while (it.hasNext()) {
			Entry<Cluster, Double> entry = it.next(); 
		   if (!availableClusters.contains(entry.getKey())) {
		      it.remove();
		   }
		}
		
		if (this.cluster!=null) {clusterSatisfaction.put(this.cluster, this.satisfaction);}

		Cluster preferred = this.cluster;
		double maxSatisfaction = this.satisfaction;
		for (Map.Entry<Cluster, Double> e : clusterSatisfaction.entrySet()) {
			if (e.getValue() > maxSatisfaction) {
				maxSatisfaction = e.getValue();
				preferred = e.getKey();
			}
		}
		// if satisfaction is below threshold or other cluster's satisfaction
		// then increment consecutive dissatisfaction count, otherwise reset it to 0.
		if (maxSatisfaction < this.tau || !preferred.equals(this.cluster)) {
			this.clusterDissatisfactionCount++;
		} else {
			this.clusterDissatisfactionCount = 0;
		}
		// if we are dissatisfied for greater than the leave threshold, leave or
		// change cluster.
		if (!newLeave || (this.cluster==null) 
				|| this.clusterDissatisfactionCount >= this.leaveThreshold) {
			if (maxSatisfaction < this.tau) {
				if (this.cluster != null){
					leaveCluster();
				}
			} else if (!preferred.equals(this.cluster)) {
				leaveCluster();
				joinCluster(preferred);
			}
		}
	}
	
	private void assessClusterCreation() {
		Set<UUID> op = this.game.getOrphanPlayers();
		if (op.size()>=this.minWaitingAgents){
			logger.info("More than "+this.minWaitingAgents+" orphans... why not create cluster?");
			if (numClustersCreated<maxClustersCreated){
				createCluster();
			}
		}
	}
}
