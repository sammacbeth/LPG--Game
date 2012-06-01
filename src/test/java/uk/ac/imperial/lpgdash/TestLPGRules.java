package uk.ac.imperial.lpgdash;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.drools.runtime.ObjectFilter;
import org.drools.runtime.StatefulKnowledgeSession;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import uk.ac.imperial.lpgdash.actions.Appropriate;
import uk.ac.imperial.lpgdash.actions.Demand;
import uk.ac.imperial.lpgdash.actions.Generate;
import uk.ac.imperial.lpgdash.actions.JoinCluster;
import uk.ac.imperial.lpgdash.actions.Provision;
import uk.ac.imperial.lpgdash.facts.Allocation;
import uk.ac.imperial.lpgdash.facts.Cluster;
import uk.ac.imperial.lpgdash.facts.Player;
import uk.ac.imperial.lpgdash.facts.Round;
import uk.ac.imperial.presage2.core.util.random.Random;
import uk.ac.imperial.presage2.rules.RuleModule;
import uk.ac.imperial.presage2.rules.RuleStorage;

import com.google.inject.Guice;
import com.google.inject.Injector;

public class TestLPGRules {

	final private Logger logger = Logger.getLogger(TestLPGRules.class);

	Injector injector;
	RuleStorage rules;
	StatefulKnowledgeSession session;

	@Before
	public void setUp() throws Exception {
		injector = Guice.createInjector(new RuleModule()
				.addClasspathDrlFile("LPGDash.drl")
				.addClasspathDrlFile("LegitimateClaimsAllocation.drl")
				.addClasspathDrlFile("RandomAllocation.drl")
				.addClasspathDrlFile("RationAllocation.drl"));
		rules = injector.getInstance(RuleStorage.class);
		session = injector.getInstance(StatefulKnowledgeSession.class);
		session.setGlobal("logger", this.logger);
		session.setGlobal("session", session);
		session.setGlobal("storage", null);
	}

	@After
	public void tearDown() throws Exception {
		session.dispose();
	}

	@Test
	public void testRoundPruning() {
		session.insert(new Round(1, RoundType.DEMAND));
		session.insert(new Round(1, RoundType.APPROPRIATE));
		session.insert(new Round(2, RoundType.DEMAND));

		rules.incrementTime();

		Collection<Object> rounds = session.getObjects(new ObjectFilter() {
			@Override
			public boolean accept(Object object) {
				return object instanceof Round;
			}
		});
		assertEquals(1, rounds.size());
		Round r = (Round) rounds.iterator().next();
		assertEquals(2, r.getNumber());
		assertEquals(RoundType.DEMAND, r.type);

		session.insert(new Round(2, RoundType.APPROPRIATE));
		rules.incrementTime();

		rounds = session.getObjects(new ObjectFilter() {
			@Override
			public boolean accept(Object object) {
				return object instanceof Round;
			}
		});
		assertEquals(1, rounds.size());
		r = (Round) rounds.iterator().next();
		assertEquals(2, r.getNumber());
		assertEquals(RoundType.APPROPRIATE, r.type);
	}

	@Test
	public void testRationSingleCluster() {

		SimulatedGame game = new SimulatedGame();

		game.addCluster(Allocation.RATION);
		int agents = Random.randomInt(30);
		char name = 'a';
		for (int n = 0; n < agents; n++) {
			game.addPlayer(String.valueOf(name), 0.1, 0.1, 0);
			name++;
		}
		game.initRound();

		for (int i = 0; i < 5; i++) {
			for (Player p : game.players) {
				Provision provision = new Provision(game.currentRound, p, p.getG());
				Demand demand = new Demand(game.currentRound, p, p.getQ());
				session.insert(provision);
				session.insert(demand);
			}
			game.demandRound();

			double provisionPool = 0;
			for (Player p : game.players) {
				provisionPool += p.getG();
			}
			double expectedAllocation = provisionPool / game.players.size();
			for (Player p : game.players) {
				assertEquals(expectedAllocation, p.getAllocated(), 0.000001);
			}

			for (Player p : game.players) {
				Appropriate app = new Appropriate(game.currentRound, p,
						p.getAllocated());
				session.insert(app);
			}
			game.appropriateRound();

			for (Player p : game.players) {
				assertEquals(p.getAllocated(), p.getAppropriated(), 0.000001);
			}
		}
	}

	@Test
	public void testRandomSingleCluster() {
		SimulatedGame game = new SimulatedGame();

		game.addCluster(Allocation.RANDOM);
		int agents = Random.randomInt(30);
		char name = 'a';
		for (int n = 0; n < agents; n++) {
			game.addPlayer(String.valueOf(name), 0.1, 0.1, 0);
			name++;
		}
		game.initRound();

		for (int i = 0; i < 5; i++) {
			for (Player p : game.players) {
				Provision provision = new Provision(game.currentRound, p, p.getG());
				Demand demand = new Demand(game.currentRound, p, p.getQ());
				session.insert(provision);
				session.insert(demand);
			}
			game.demandRound();

			int incorrectCount = 0;
			for (Player p : game.players) {
				// assertEquals(expectedAllocation, p.getAllocated(), 0.000001);
				if (Math.abs(p.getQ() - p.getAllocated()) > 0.00001
						&& p.getAllocated() != 0) {
					incorrectCount++;
				}
			}
			logger.info(incorrectCount + " non 0 or q allocations");
			assertFalse("No more than one non 0 or q allocation",
					incorrectCount > 1);

			for (Player p : game.players) {
				Appropriate app = new Appropriate(game.currentRound, p,
						p.getAllocated());
				session.insert(app);
			}
			game.appropriateRound();

			for (Player p : game.players) {
				assertEquals(p.getAllocated(), p.getAppropriated(), 0.000001);
			}
		}
	}

	class SimulatedGame {

		List<Cluster> clusters = new ArrayList<Cluster>();
		List<Player> players = new ArrayList<Player>();
		Map<UUID, Generate> generated = new HashMap<UUID, Generate>();
		Map<UUID, Provision> provisioned = new HashMap<UUID, Provision>();
		Map<UUID, Demand> demanded = new HashMap<UUID, Demand>();
		int currentRound = 1;

		SimulatedGame() {
			super();
			logger.info("New game:");
		}

		SimulatedGame addCluster(Allocation all) {
			Cluster c = new Cluster(clusters.size(), all);
			clusters.add(c);
			session.insert(c);
			return this;
		}

		SimulatedGame addPlayer(String name, double alpha, double beta,
				int cluster) {
			Player p = new Player(Random.randomUUID(), name, "c", alpha, beta);
			players.add(p);
			session.insert(p);
			try {
				Cluster c = clusters.get(cluster);
				session.insert(new JoinCluster(p, c));
			} catch (IndexOutOfBoundsException e) {
			}
			return this;
		}

		void initRound() {
			for (Player p : players) {
				Generate g = new Generate(p, currentRound);
				generated.put(p.getId(), g);
				session.insert(g);
			}
			rules.incrementTime();
		}

		void demandRound() {
			session.insert(new Round(currentRound, RoundType.DEMAND));
			for (Player p : players) {
				Generate g = generated.get(p.getId());
				assertEquals(g.getG(), p.getG(), 0.000001);
				assertEquals(g.getQ(), p.getQ(), 0.000001);
			}

			rules.incrementTime();
		}

		void appropriateRound() {
			session.insert(new Round(currentRound, RoundType.APPROPRIATE));
			rules.incrementTime();
			currentRound++;
		}

	}

}
