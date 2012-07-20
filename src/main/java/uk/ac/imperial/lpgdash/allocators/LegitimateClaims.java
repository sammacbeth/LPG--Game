package uk.ac.imperial.lpgdash.allocators;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.drools.runtime.StatefulKnowledgeSession;
import org.drools.runtime.rule.QueryResults;
import org.drools.runtime.rule.Variable;

import uk.ac.imperial.lpgdash.LPGService;
import uk.ac.imperial.lpgdash.actions.Allocate;
import uk.ac.imperial.lpgdash.allocators.canons.Canon;
import uk.ac.imperial.lpgdash.allocators.canons.F1a;
import uk.ac.imperial.lpgdash.allocators.canons.F7;
import uk.ac.imperial.lpgdash.allocators.canons.F2;
import uk.ac.imperial.lpgdash.allocators.canons.F3;
import uk.ac.imperial.lpgdash.allocators.canons.F4;
import uk.ac.imperial.lpgdash.allocators.canons.F5;
import uk.ac.imperial.lpgdash.allocators.canons.F6;
import uk.ac.imperial.lpgdash.allocators.canons.F1b;
import uk.ac.imperial.lpgdash.allocators.canons.LegitimateClaimsCanon;
import uk.ac.imperial.lpgdash.facts.Allocation;
import uk.ac.imperial.lpgdash.facts.BordaRank;
import uk.ac.imperial.lpgdash.facts.Cluster;
import uk.ac.imperial.lpgdash.facts.Player;
import uk.ac.imperial.lpgdash.facts.PlayerHistory;
import uk.ac.imperial.presage2.core.db.StorageService;
import uk.ac.imperial.presage2.core.db.persistent.PersistentEnvironment;
import uk.ac.imperial.presage2.core.db.persistent.TransientAgentState;

public class LegitimateClaims {

	private final Logger logger = Logger.getLogger(LegitimateClaims.class);

	private final Cluster c;
	private double gamma = 0.1;

	public boolean ratelimit = false;
	public boolean enableHack = true;
	private boolean soHd = true;

	private final StatefulKnowledgeSession session;
	private final LPGService game;
	private StorageService sto = null;

	private Map<Canon, Double> weight = new HashMap<Canon, Double>();
	private Map<Canon, LegitimateClaimsCanon> lcCanons = new HashMap<Canon, LegitimateClaimsCanon>();

	public LegitimateClaims(Cluster c, StatefulKnowledgeSession session,
			LPGService game) {
		super();
		this.c = c;
		this.session = session;

		Allocation a = c.getAllocationMethod();
		boolean allCanons = (a == Allocation.LC_FIXED || a == Allocation.LC_SO);
		if (allCanons || a == Allocation.LC_F1) {
			weight.put(Canon.F1a, 1.0);
			lcCanons.put(Canon.F1a, new F1a(c));
		}
		if (allCanons || a == Allocation.LC_F1a) {
			weight.put(Canon.F1b, 1.0);
			lcCanons.put(Canon.F1b, new F1b(c));
		}
		if (allCanons || a == Allocation.LC_F2) {
			weight.put(Canon.F2, 1.0);
			lcCanons.put(Canon.F2, new F2(c));
		}
		if (allCanons || a == Allocation.LC_F3) {
			weight.put(Canon.F3, 1.0);
			lcCanons.put(Canon.F3, new F3(c));
		}
		if (allCanons || a == Allocation.LC_F4) {
			weight.put(Canon.F4, 1.0);
			lcCanons.put(Canon.F4, new F4(c));
		}
		if (allCanons || a == Allocation.LC_F5) {
			weight.put(Canon.F5, 1.0);
			lcCanons.put(Canon.F5, new F5(c));
		}
		if (allCanons || a == Allocation.LC_F6) {
			weight.put(Canon.F6, 1.0);
			lcCanons.put(Canon.F6, new F6(c));
		}
		if (allCanons || a == Allocation.LC_F7) {
			weight.put(Canon.F7, 1.0);
			lcCanons.put(Canon.F7, new F7(c));
		}
		if (allCanons) {
			normaliseWeights();
		}
		this.game = game;
	}

	public void setStorage(StorageService sto) {
		this.sto = sto;
	}

	public void setGamma(double gamma) {
		this.gamma = gamma;
	}

	public Cluster getC() {
		return c;
	}

	@Override
	public String toString() {
		return "LegitimateClaims [c=" + c + ", weights=" + weight.toString()
				+ "]";
	}

	private List<Player> getFunctionRanking(Canon f, List<Player> players) {
		List<Player> rankedPlayers = new ArrayList<Player>(players);
		Collections.shuffle(rankedPlayers);
		LegitimateClaimsCanon canon = lcCanons.get(f);
		Collections.sort(rankedPlayers, canon);
		return rankedPlayers;
	}

	public void allocate(List<Player> players, double poolSize, int t) {

		Map<Canon, List<Player>> rankOrders = new HashMap<Canon, List<Player>>();
		players = new ArrayList<Player>(players);
		for (Canon f : lcCanons.keySet()) {
			rankOrders.put(f, getFunctionRanking(f, players));
		}

		Map<UUID, BordaRank> ranks = new HashMap<UUID, BordaRank>();
		for (Player p : players) {
			ranks.put(p.getId(), new BordaRank(p));
			storeHistory(p.getId(), p.getHistory().get(c));
		}
		ArrayList<BordaRank> bordaPtq = new ArrayList<BordaRank>(ranks.values());

		// associate ranks with agents
		for (int i = 0; i < players.size(); i++) {
			for (Map.Entry<Canon, List<Player>> entry : rankOrders.entrySet()) {
				ranks.get(entry.getValue().get(i).getId()).set(entry.getKey(),
						i);
			}
		}

		final int nPlayers = players.size();

		// sort BordaRanks by borda score DESC.
		sortBordaRanks(bordaPtq);

		for (BordaRank p : bordaPtq) {
			double allocation = Math.min(p.getPlayer().getD(), poolSize);
			session.insert(new Allocate(p.getPlayer(), allocation, t));
			poolSize -= allocation;
			logger.info(p + ": " + getScore(p, nPlayers));
			storeRanks(p, nPlayers);
		}

		if (c.getAllocationMethod() == Allocation.LC_SO) {
			updateWeights(bordaPtq, rankOrders);
		}
	}

	void sortBordaRanks(List<BordaRank> bordaPtq) {
		final int nPlayers = bordaPtq.size();
		Collections.sort(bordaPtq, new Comparator<BordaRank>() {
			@Override
			public int compare(BordaRank o1, BordaRank o2) {
				double p1score = getScore(o1, nPlayers);
				double p2score = getScore(o2, nPlayers);
				return Double.compare(p2score, p1score);
			}
		});
	}

	private double getScore(BordaRank r, int nPlayers) {
		double score = 0;
		for (Canon canon : weight.keySet()) {
			score += weight.get(canon) * (nPlayers - r.get(canon));
		}
		return score;
	}

	private void updateWeights(List<BordaRank> bordaPtq,
			Map<Canon, List<Player>> canonRankings) {
		Map<Canon, Double> fBorda = fBorda(bordaPtq);

		logger.info("Borda(f, C) = " + fBorda.toString());

		// update weights
		double averageBorda = 0;
		double totalBorda = 0;
		for (Double v : fBorda.values()) {
			totalBorda += v;
		}
		averageBorda = totalBorda / fBorda.size();

		for (Map.Entry<Canon, Double> fb : fBorda.entrySet()) {
			Double w = weight.get(fb.getKey());
			weight.put(fb.getKey(), w
					+ (w * (fb.getValue() - averageBorda) / totalBorda));
		}

		normaliseWeights();
		logger.info("w*(t) = " + weight.toString());

		if (enableHack) {
			if (soHd) {
				Map<Canon, Integer> fHd = new HashMap<Canon, Integer>();
				for (Canon f : weight.keySet()) {
					fHd.put(f, hdFBorda(canonRankings.get(f), bordaPtq));
				}
				double averageHd = 0;
				int totalHd = 0;
				for (int v : fHd.values()) {
					totalHd += v;
				}
				averageHd = totalHd / (double) fHd.size();
				for (Map.Entry<Canon, Integer> f : fHd.entrySet()) {
					Canon c = f.getKey();
					double delta = 0;
					if (totalHd > 0)
						delta = weight.get(c) * (fHd.get(c) - averageHd)
								/ totalHd;
					if (ratelimit) {
						if (delta > 0.0007)
							delta = 0.0007;
						else if (delta < -0.0007)
							delta = -0.0007;
					}
					weight.put(c, weight.get(c) + delta);
				}
				normaliseWeights();
				logger.info("w*(t) = " + weight.toString());
			}

			if (bordaPtq.size() == getCompliantCount()) {
				final double equalWeight = 1 / (double) weight.size();
				for (Canon c : weight.keySet()) {
					Double w = weight.get(c);
					if (w > equalWeight) {
						weight.put(c, w - (gamma * (w - equalWeight)));
					} else {
						weight.put(c, w + (gamma * (equalWeight - w)));
					}
				}

				normaliseWeights();
				logger.info("w*(t+1) = " + weight.toString());
			}
		}
		storeWeights();
	}

	private void normaliseWeights() {
		double weightsSum = 0.0;
		for (Double w : weight.values()) {
			weightsSum += w;
		}
		for (Map.Entry<Canon, Double> w : weight.entrySet()) {
			weight.put(w.getKey(), w.getValue() / weightsSum);
		}
	}

	Map<Canon, Double> fBorda(List<BordaRank> bordaPtq) {
		Map<Canon, Double> fBorda = new HashMap<Canon, Double>();

		for (Canon c : weight.keySet()) {
			fBorda.put(c, 0.0);
		}

		// calculate Borda(f, C)
		for (BordaRank p : bordaPtq) {
			List<FunctionRank> playerRanks = new ArrayList<LegitimateClaims.FunctionRank>();
			for (Canon f : weight.keySet()) {
				playerRanks.add(new FunctionRank(f, p.get(f)));
			}

			Collections.sort(playerRanks);

			double bordaAvailable = 0;
			int lastIndex = 0;
			int lastRank = 0;
			int score = weight.size();
			for (int i = 0; i <= playerRanks.size(); i++) {
				FunctionRank f;
				if (i == playerRanks.size())
					// stub functionrank for last iteration
					f = new FunctionRank(null, -1);
				else
					f = playerRanks.get(i);

				if (f.rank != lastRank) {
					// shared score between fns
					int fnCount = i - lastIndex;
					double bordaPerFn = ((double) bordaAvailable) / fnCount;
					for (int j = lastIndex; j < i; j++) {
						FunctionRank fAdd = playerRanks.get(j);
						fBorda.put(fAdd.f, fBorda.get(fAdd.f) + bordaPerFn);
					}

					bordaAvailable = score;
					lastIndex = i;
					lastRank = f.rank;
				} else {
					bordaAvailable += score;
				}
				score--;
			}

		}
		return fBorda;
	}

	private int hdFBorda(List<Player> functionRanking, List<BordaRank> bordaPtq) {
		int hd = 0;
		for (int i = 0; i < functionRanking.size(); i++) {
			BordaRank r = bordaPtq.get(i);
			Player p = functionRanking.get(i);
			if (!r.getPlayer().getId().equals(p.getId())) {
				hd++;
			}
		}
		return hd;
	}

	private void storeHistory(UUID id, PlayerHistory playerHistory) {
		if (sto != null) {
			TransientAgentState s = sto
					.getAgentState(id, game.getRoundNumber());
			s.setProperty("averageAllocated",
					Double.toString(playerHistory.getAverageAllocated()));
			s.setProperty("averageDemanded",
					Double.toString(playerHistory.getAverageDemanded()));
			s.setProperty("averageProvided",
					Double.toString(playerHistory.getAverageProvided()));
			s.setProperty("compliantRounds",
					Integer.toString(playerHistory.getCompliantRounds()));
			s.setProperty("roundsAllocated",
					Integer.toString(playerHistory.getRoundsAllocated()));
			s.setProperty("roundsAsHead",
					Integer.toString(playerHistory.getRoundsAsHead()));
			s.setProperty("roundsParticipanted",
					Integer.toString(playerHistory.getRoundsParticipated()));
		}
	}

	private void storeRanks(BordaRank r, int n) {
		if (sto != null) {
			TransientAgentState s = sto.getAgentState(r.getPlayer().getId(),
					game.getRoundNumber());
			for (Canon ca : weight.keySet()) {
				s.setProperty(ca.name(),
						Double.toString(weight.get(ca) * (n - r.get(ca))));
			}
		}
	}

	private void storeWeights() {
		if (sto != null) {
			PersistentEnvironment e = sto.getSimulation().getEnvironment();
			int round = game.getRoundNumber();
			for (Map.Entry<Canon, Double> w : weight.entrySet()) {
				e.setProperty("w_" + w.getKey(), round,
						Double.toString(w.getValue()));
			}
		}
	}

	private static class FunctionRank implements Comparable<FunctionRank> {
		Canon f;
		int rank;

		FunctionRank(Canon f, int rank) {
			super();
			this.f = f;
			this.rank = rank;
		}

		@Override
		public int compareTo(FunctionRank o) {
			return rank - o.rank;
		}

		@Override
		public String toString() {
			return "FunctionRank [f=" + f + ", rank=" + rank + "]";
		}

	}

	private int getCompliantCount() {
		QueryResults results = session.getQueryResults("compliantRound",
				new Object[] { this.c, Variable.v, game.getRoundNumber() - 1 });
		int size = results.size();
		return size;
	}

}
