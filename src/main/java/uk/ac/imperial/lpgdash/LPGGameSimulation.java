package uk.ac.imperial.lpgdash;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.drools.runtime.StatefulKnowledgeSession;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;

import uk.ac.imperial.lpgdash.actions.Generate;
import uk.ac.imperial.lpgdash.actions.LPGActionHandler;
import uk.ac.imperial.lpgdash.facts.Cluster;
import uk.ac.imperial.lpgdash.facts.MemberOf;
import uk.ac.imperial.lpgdash.facts.Player;
import uk.ac.imperial.presage2.core.TimeDriven;
import uk.ac.imperial.presage2.core.environment.EnvironmentServiceProvider;
import uk.ac.imperial.presage2.core.environment.UnavailableServiceException;
import uk.ac.imperial.presage2.core.simulator.InjectedSimulation;
import uk.ac.imperial.presage2.core.simulator.Scenario;
import uk.ac.imperial.presage2.core.util.random.Random;
import uk.ac.imperial.presage2.rules.RuleModule;
import uk.ac.imperial.presage2.rules.RuleStorage;
import uk.ac.imperial.presage2.rules.facts.SimParticipantsTranslator;
import uk.ac.imperial.presage2.util.environment.AbstractEnvironmentModule;
import uk.ac.imperial.presage2.util.network.NetworkModule;

public class LPGGameSimulation extends InjectedSimulation implements TimeDriven {

	private final Logger logger = Logger
			.getLogger("uk.ac.imperial.lpgdash.RuleEngine");
	private StatefulKnowledgeSession session;

	private Set<Player> players = new HashSet<Player>();
	private LPGService game;

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
				.addStateTranslator(SimParticipantsTranslator.class));
		modules.add(NetworkModule.noNetworkModule());
		return modules;
	}

	@Override
	protected void addToScenario(Scenario s) {
		s.addTimeDriven(this);
		session.setGlobal("logger", this.logger);
		Cluster c = new Cluster(0);
		session.insert(c);
		for (int n = 0; n < 2; n++) {
			UUID pid = Random.randomUUID();
			s.addParticipant(new LPGPlayer(pid, "p" + n));
			Player p = new Player(pid, Random.randomDouble(),
					Random.randomDouble());
			players.add(p);
			session.insert(p);
			session.insert(new MemberOf(p, c));
		}
	}

	@Override
	public void incrementTime() {
		if (this.game.getRound() == RoundType.APPROPRIATE) {
			for (Player p : players) {
				session.insert(new Generate(p));
			}
		}
	}

}
