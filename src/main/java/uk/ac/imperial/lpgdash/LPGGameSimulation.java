package uk.ac.imperial.lpgdash;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.drools.runtime.StatefulKnowledgeSession;

import uk.ac.imperial.lpgdash.LPGPlayer.ClusterLeaveAlgorithm;
import uk.ac.imperial.lpgdash.LPGPlayer.ClusterSelectionAlgorithm;
import uk.ac.imperial.lpgdash.actions.Generate;
import uk.ac.imperial.lpgdash.actions.JoinCluster;
import uk.ac.imperial.lpgdash.actions.LPGActionHandler;
import uk.ac.imperial.lpgdash.allocators.LegitimateClaims;
import uk.ac.imperial.lpgdash.allocators.QueueAllocator;
import uk.ac.imperial.lpgdash.facts.Allocation;
import uk.ac.imperial.lpgdash.facts.Cluster;
import uk.ac.imperial.lpgdash.facts.Player;
import uk.ac.imperial.lpgdash.util.UniformDistribution;
import uk.ac.imperial.presage2.core.environment.EnvironmentServiceProvider;
import uk.ac.imperial.presage2.core.environment.UnavailableServiceException;
import uk.ac.imperial.presage2.core.event.EventBus;
import uk.ac.imperial.presage2.core.event.EventListener;
import uk.ac.imperial.presage2.core.participant.Participant;
import uk.ac.imperial.presage2.core.simulator.InjectedSimulation;
import uk.ac.imperial.presage2.core.simulator.Parameter;
import uk.ac.imperial.presage2.core.simulator.ParticipantsComplete;
import uk.ac.imperial.presage2.core.simulator.Scenario;
import uk.ac.imperial.presage2.core.util.random.Random;
import uk.ac.imperial.presage2.rules.RuleModule;
import uk.ac.imperial.presage2.rules.RuleStorage;
import uk.ac.imperial.presage2.rules.facts.SimParticipantsTranslator;
import uk.ac.imperial.presage2.util.environment.AbstractEnvironmentModule;
import uk.ac.imperial.presage2.util.network.NetworkModule;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;

public class LPGGameSimulation extends InjectedSimulation {

	private final Logger logger = Logger
			.getLogger("uk.ac.imperial.lpgdash.RuleEngine");
	private StatefulKnowledgeSession session;

	private Set<Player> players = new HashSet<Player>();
	private LPGService game;
	private Scenario scenario;

	protected int playerCtr = 0;
	protected int genCtr = 0;

	@Parameter(name = "pop", optional = true)
	public String pop = "static";

	@Parameter(name = "cCount")
	public int cCount;
	@Parameter(name = "cPCheat", optional = true)
	public double cPCheat;
	@Parameter(name = "cSize", optional = true)
	public double cSize = 1;

	@Parameter(name = "ncCount", optional = true)
	public int ncCount;
	@Parameter(name = "ncPCheat", optional = true)
	public double ncPCheat;
	@Parameter(name = "ncSize", optional = true)
	public double ncSize = 1;

	@Parameter(name = "lCount", optional = true)
	public int lCount = 0;
	@Parameter(name = "lPCheat", optional = true)
	public double lPCheat = 0.02;
	@Parameter(name = "lSize", optional = true)
	public double lSize = 5;

	@Parameter(name = "clusters")
	public String clusters;

	@Parameter(name = "alpha")
	public double alpha;
	@Parameter(name = "beta")
	public double beta;
	@Parameter(name = "gamma", optional = true)
	public double gamma = 0.1;
	@Parameter(name = "seed")
	public int seed;

	@Parameter(name = "soHd", optional = true)
	public boolean soHd = true;
	@Parameter(name = "soHack", optional = true)
	public boolean soHack = true;

	@Parameter(name = "cheatOn", optional = true)
	public String cheatOn = "provision";

	@Parameter(name = "clusterLeave", optional = true)
	public ClusterLeaveAlgorithm clusterLeave = ClusterLeaveAlgorithm.THRESHOLD;
	@Parameter(name = "clusterSelect", optional = true)
	public ClusterSelectionAlgorithm clusterSelect = ClusterSelectionAlgorithm.PREFERRED;
	@Parameter(name = "resetSatisfaction", optional = true)
	public boolean resetSatisfaction = false;

	@Parameter(name = "rankMemory", optional = true)
	public int rankMemory = 1;

	public enum Reproduction {
		NONE, PERIODIC, RANDOM
	};

	public enum MateSelection {
		RANDOM, TOTAL_UTILITY, ROLLING_UTILITY
	};

	@Parameter(name = "reproduction", optional = true)
	public Reproduction reproduction = Reproduction.NONE;
	/**
	 * How we choose parents to create a new player.
	 */
	@Parameter(name = "mateSelect", optional = true)
	public MateSelection mate = MateSelection.RANDOM;
	/**
	 * With periodic reproduction, this is the number of timesteps between each
	 * generation.
	 */
	@Parameter(name = "reproductionInterval", optional = true)
	public int reproductionInterval = 100;

	/**
	 * With random reproduction, this is the probability of reproduction in a
	 * timestep.
	 */
	@Parameter(name = "pReproduce", optional = true)
	public double pReproduce = 0.01;

	public LPGGameSimulation(Set<AbstractModule> modules) {
		super(modules);
	}

	@Inject
	public void setSession(StatefulKnowledgeSession session) {
		this.session = session;
	}

	@Inject
	public void setServiceProvider(EnvironmentServiceProvider serviceProvider) {
		try {
			this.game = serviceProvider.getEnvironmentService(LPGService.class);
		} catch (UnavailableServiceException e) {
			logger.warn("", e);
		}
	}

	@Inject
	public void setEventBus(EventBus eb) {
		eb.subscribe(this);
	}

	@Override
	protected Set<AbstractModule> getModules() {
		Set<AbstractModule> modules = new HashSet<AbstractModule>();
		modules.add(new AbstractEnvironmentModule()
				.addActionHandler(LPGActionHandler.class)
				.addParticipantGlobalEnvironmentService(LPGService.class)
				.setStorage(RuleStorage.class));
		modules.add(new RuleModule().addClasspathDrlFile("LPGDash.drl")
				.addClasspathDrlFile("RationAllocation.drl")
				.addClasspathDrlFile("RandomAllocation.drl")
				.addClasspathDrlFile("LegitimateClaimsAllocation.drl")
				.addClasspathDrlFile("QueueAllocation.drl")
				.addStateTranslator(SimParticipantsTranslator.class));
		modules.add(NetworkModule.noNetworkModule());
		return modules;
	}

	@Override
	protected void addToScenario(Scenario s) {
		this.scenario = s;
		Random.seed = this.seed;
		session.setGlobal("logger", this.logger);
		session.setGlobal("session", session);
		session.setGlobal("storage", this.storage);

		Cluster[] clusterArr = initClusters();

		if (pop.equalsIgnoreCase("random")) {
			// populations of random strategy agents
			DecimalFormat format = new DecimalFormat("0000");
			for (int n = 0; n < cCount + ncCount; n++) {
				createPlayer(format.format(n) + "gen0",
						0.4 * Random.randomDouble(), cSize, "0", getCheatOn(),
						clusterArr[n % clusterArr.length]);
			}
		} else {
			for (int n = 0; n < cCount; n++) {
				createPlayer("c" + n, cPCheat, cSize, "C", getCheatOn(),
						clusterArr[n % clusterArr.length]);
			}
			for (int n = 0; n < ncCount; n++) {
				createPlayer("nc" + n, ncPCheat, ncSize, "N", getCheatOn(),
						clusterArr[n % clusterArr.length]);
			}
			for (int n = 0; n < lCount; n++) {
				createPlayer("l" + n, lPCheat, lSize, "L", getCheatOn(),
						clusterArr[n % clusterArr.length]);
			}
		}
		for (Player p : players) {
			session.insert(new Generate(p, game.getRoundNumber() + 1));
		}
	}

	protected Cluster[] initClusters() {
		String[] clusterNames = StringUtils.split(this.clusters, ',');
		Cluster[] clusters = new Cluster[clusterNames.length];
		int clusterCtr = 0;
		for (int i = 0; i < clusterNames.length; i++) {
			Allocation method = null;
			for (Allocation a : Allocation.values()) {
				if (clusterNames[i].equalsIgnoreCase(a.name())) {
					method = a;
					break;
				}
			}
			if (method == null)
				throw new RuntimeException("Unknown allocation method '"
						+ clusterNames[i] + "', could not create cluster!");
			Cluster c = new Cluster(clusterCtr++, method);
			session.insert(c);
			if (c.isLC()) {
				LegitimateClaims lc = new LegitimateClaims(c, session,
						this.game);
				lc.setStorage(storage);
				lc.setGamma(gamma);
				lc.enableHack = soHack;
				lc.soHd = soHd;
				lc.rankMemory = rankMemory;
				session.insert(lc);
			} else if (c.getAllocationMethod() == Allocation.QUEUE) {
				QueueAllocator q = new QueueAllocator(c, session);
				session.insert(q);
			}
			clusters[i] = c;
		}
		return clusters;
	}

	protected Cheat getCheatOn() {
		for (Cheat c : Cheat.values()) {
			if (this.cheatOn.equalsIgnoreCase(c.name())) {
				return c;
			}
		}
		Cheat[] cs = Cheat.values();
		Cheat c = cs[Random.randomInt(cs.length)];
		logger.debug("Cheat on: " + c);
		return c;
	}

	protected LPGPlayer createPlayer(String name, double pCheat, double size,
			String type, Cheat cheatOn, Cluster cluster) {
		UUID pid = Random.randomUUID();
		LPGPlayer ag = new LPGPlayer(pid, name, pCheat, alpha, beta, cheatOn,
				getClusterLeave(), getClusterSelect(), resetSatisfaction, size);
		scenario.addParticipant(ag);
		Player p = new Player(pid, name, type, alpha, beta, size);
		players.add(p);
		session.insert(p);
		session.insert(new JoinCluster(p, cluster));
		// session.insert(new Generate(p, game.getRoundNumber() + 1));
		playerCtr++;
		return ag;
	}

	public ClusterLeaveAlgorithm getClusterLeave() {
		return clusterLeave;
	}

	public ClusterSelectionAlgorithm getClusterSelect() {
		return clusterSelect;
	}

	@EventListener
	public void incrementTime(ParticipantsComplete e) {

		if (this.game.getRound() == RoundType.APPROPRIATE) {
			// reproduction
			if ((reproduction == Reproduction.PERIODIC
					&& game.getRoundNumber() % reproductionInterval == 0 && game
					.getRoundNumber() > 0)
					|| reproduction == Reproduction.RANDOM
					&& Random.randomDouble() < pReproduce) {
				genCtr++;
				// collect players in each cluster
				Map<Cluster, List<LPGPlayer>> clusterMembers = new HashMap<Cluster, List<LPGPlayer>>();
				for (Participant p : scenario.getParticipants()) {
					LPGPlayer player = (LPGPlayer) p;
					if (player.cluster != null) {
						if (!clusterMembers.containsKey(player.cluster)) {
							clusterMembers.put(player.cluster,
									new ArrayList<LPGPlayer>());
						}
						clusterMembers.get(player.cluster).add(player);
					}
				}

				// create new generation
				for (Map.Entry<Cluster, List<LPGPlayer>> clEntry : clusterMembers
						.entrySet()) {
					List<LPGPlayer> members = clEntry.getValue();
					int childCount = reproduction == Reproduction.PERIODIC ? members
							.size() / 2 : 1;
					for (int i = 0; i < childCount; i++) {
						conceivePlayer(clEntry.getKey(), members);
					}
				}

			}
			// generate new g and q
			for (Player p : players) {
				session.insert(new Generate(p, game.getRoundNumber() + 1));
			}
		}
		// analyse objects in working memory.
		// analyseDroolsUsage();
	}

	@SuppressWarnings("unused")
	private void analyseDroolsUsage() {
		Map<Class<?>, Integer> typeCards = new HashMap<Class<?>, Integer>();
		for (Object o : session.getObjects()) {
			if (!typeCards.containsKey(o.getClass())) {
				typeCards.put(o.getClass(), 0);
			}
			typeCards.put(o.getClass(), typeCards.get(o.getClass()) + 1);
		}
		logger.info("Drools memory:");
		for (Map.Entry<Class<?>, Integer> entry : typeCards.entrySet()) {
			logger.info(entry.getKey().getSimpleName() + " - "
					+ entry.getValue());
		}
	}

	protected void conceivePlayer(Cluster cl, List<LPGPlayer> members) {
		if (members.size() > 1) {
			// create a uniform distribution to choose mates from
			UniformDistribution<LPGPlayer> dist = new UniformDistribution<LPGPlayer>();
			for (LPGPlayer p : members) {
				// chose weighting based on mate selection parameter.
				switch (this.mate) {
				case ROLLING_UTILITY:
					dist.addValue(p, p.rollingUtility.getSum());
					break;
				case TOTAL_UTILITY:
					dist.addValue(p, p.clusterUtilities.get(p.cluster).getSum());
					break;
				case RANDOM:
				default:
					dist.addValue(p, 1.0);
				}
			}
			// chose two random cluster members from the distribution
			LPGPlayer first = dist.choose();
			LPGPlayer second;
			do {
				second = dist.choose();
			} while (first == second);
			// determine offspring characteristics
			double childPCheat = ((first.pCheat + second.pCheat) / 2);
			Cheat childCheatOn = first.cheatOn;
			double childSize = ((first.pCheat + second.pCheat) / 2);
			// mutation
			childPCheat += (0.1 * Random.randomDouble()) - 0.05;
			childPCheat = Math.max(0.0, childPCheat);
			childPCheat = Math.min(1.0, childPCheat);
			DecimalFormat format = new DecimalFormat("0000");
			LPGPlayer child = createPlayer(format.format(playerCtr) + "gen"
					+ genCtr, childPCheat, childSize, "" + genCtr,
					childCheatOn, cl);
			child.initialise();
			logger.info("New child, generation " + genCtr + ", pCheat="
					+ childPCheat);
		}
	}

}
