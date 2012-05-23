package uk.ac.imperial.lpgdash;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.drools.runtime.StatefulKnowledgeSession;

import uk.ac.imperial.lpgdash.actions.Generate;
import uk.ac.imperial.lpgdash.actions.JoinCluster;
import uk.ac.imperial.lpgdash.actions.LPGActionHandler;
import uk.ac.imperial.lpgdash.facts.Allocation;
import uk.ac.imperial.lpgdash.facts.Cluster;
import uk.ac.imperial.lpgdash.facts.Player;
import uk.ac.imperial.presage2.core.TimeDriven;
import uk.ac.imperial.presage2.core.environment.EnvironmentServiceProvider;
import uk.ac.imperial.presage2.core.environment.UnavailableServiceException;
import uk.ac.imperial.presage2.core.simulator.InjectedSimulation;
import uk.ac.imperial.presage2.core.simulator.Parameter;
import uk.ac.imperial.presage2.core.simulator.Scenario;
import uk.ac.imperial.presage2.core.util.random.Random;
import uk.ac.imperial.presage2.rules.RuleModule;
import uk.ac.imperial.presage2.rules.RuleStorage;
import uk.ac.imperial.presage2.rules.facts.SimParticipantsTranslator;
import uk.ac.imperial.presage2.util.environment.AbstractEnvironmentModule;
import uk.ac.imperial.presage2.util.network.NetworkModule;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;

public class LPGGameSimulation extends InjectedSimulation implements TimeDriven {

	private final Logger logger = Logger
			.getLogger("uk.ac.imperial.lpgdash.RuleEngine");
	private StatefulKnowledgeSession session;

	private Set<Player> players = new HashSet<Player>();
	private LPGService game;

	@Parameter(name = "cCount")
	public int cCount;
	@Parameter(name = "cPCheat")
	public double cPCheat;

	@Parameter(name = "ncCount")
	public int ncCount;
	@Parameter(name = "ncPCheat")
	public double ncPCheat;

	@Parameter(name = "clusters")
	public String clusters;

	@Parameter(name = "alpha")
	public double alpha;
	@Parameter(name = "beta")
	public double beta;

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
				.addStateTranslator(SimParticipantsTranslator.class));
		modules.add(NetworkModule.noNetworkModule());
		return modules;
	}

	@Override
	protected void addToScenario(Scenario s) {
		s.addTimeDriven(this);
		session.setGlobal("logger", this.logger);
		session.setGlobal("session", session);
		session.setGlobal("storage", this.graphDb);
		Cluster c;
		if (clusters.equalsIgnoreCase(Allocation.RANDOM.name()))
			c = new Cluster(0, Allocation.RANDOM);
		else if (clusters.equalsIgnoreCase(Allocation.LC_FIXED.name()))
			c = new Cluster(0, Allocation.LC_FIXED);
		else
			c = new Cluster(0, Allocation.RATION);
		session.insert(c);
		for (int n = 0; n < cCount; n++) {
			UUID pid = Random.randomUUID();
			s.addParticipant(new LPGPlayer(pid, "c" + n, cPCheat, alpha, beta));
			Player p = new Player(pid, Random.randomDouble(),
					Random.randomDouble());
			players.add(p);
			session.insert(p);
			session.insert(new JoinCluster(p, c));
		}
		for (int n = 0; n < ncCount; n++) {
			UUID pid = Random.randomUUID();
			s.addParticipant(new LPGPlayer(pid, "c" + n, ncPCheat, alpha, beta));
			Player p = new Player(pid, Random.randomDouble(),
					Random.randomDouble());
			players.add(p);
			session.insert(p);
			session.insert(new JoinCluster(p, c));
		}
	}

	@Override
	public void incrementTime() {
		if (this.game.getRound() == RoundType.APPROPRIATE) {
			// generate new g and q
			for (Player p : players) {
				session.insert(new Generate(p));
			}
		}
	}

}
