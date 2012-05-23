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

import uk.ac.imperial.lpgdash.actions.Allocate;
import uk.ac.imperial.lpgdash.facts.BordaRank;
import uk.ac.imperial.lpgdash.facts.Cluster;
import uk.ac.imperial.lpgdash.facts.Player;
import uk.ac.imperial.lpgdash.facts.PlayerHistory;

public class LegitimateClaims {

	private final static Logger logger = Logger
			.getLogger(LegitimateClaims.class);

	private final static double[] fixedWeights = { 1, 0.5, 1, 1, 0.1 };

	public static void allocate(StatefulKnowledgeSession session,
			List<Player> players, double poolSize, Cluster c) {

		// map player histories
		final Map<UUID, PlayerHistory> historyMap = new HashMap<UUID, PlayerHistory>(
				players.size());
		for (Player p : players) {
			historyMap.put(p.getId(), p.getHistory().get(c));
		}

		// f1: sort by average allocation ASC.
		ArrayList<Player> f1 = new ArrayList<Player>(players);
		Collections.sort(f1, new Comparator<Player>() {
			@Override
			public int compare(Player o1, Player o2) {
				// compare by average allocation
				PlayerHistory h1 = historyMap.get(o1.getId());
				PlayerHistory h2 = historyMap.get(o2.getId());
				if (h1 == null && h2 == null)
					return 0;
				else if (h1 == null) {
					return 1;
				} else if (h2 == null) {
					return -1;
				}
				return Double.compare(h1.getAverageAllocated(),
						h2.getAverageAllocated());
			}
		});

		// f1a: sort by rounds with allocation ASC
		ArrayList<Player> f1a = new ArrayList<Player>(players);
		Collections.sort(f1a, new Comparator<Player>() {
			@Override
			public int compare(Player o1, Player o2) {
				// compare by average allocation
				PlayerHistory h1 = historyMap.get(o1.getId());
				PlayerHistory h2 = historyMap.get(o2.getId());
				if (h1 == null && h2 == null)
					return 0;
				else if (h1 == null) {
					return 1;
				} else if (h2 == null) {
					return -1;
				}
				return Integer.compare(h1.getRoundsAllocated(),
						h2.getRoundsAllocated());
			}
		});

		// f2: sort by average demand DESC.
		ArrayList<Player> f2 = new ArrayList<Player>(players);
		Collections.sort(f2, Collections.reverseOrder(new Comparator<Player>() {
			@Override
			public int compare(Player o1, Player o2) {
				// compare by average allocation
				PlayerHistory h1 = historyMap.get(o1.getId());
				PlayerHistory h2 = historyMap.get(o2.getId());
				if (h1 == null && h2 == null)
					return 0;
				else if (h1 == null) {
					return 1;
				} else if (h2 == null) {
					return -1;
				}
				return Double.compare(h1.getAverageDemanded(),
						h2.getAverageDemanded());
			}
		}));

		// f3: sort by average provision DESC.
		ArrayList<Player> f3 = new ArrayList<Player>(players);
		Collections.sort(f3, Collections.reverseOrder(new Comparator<Player>() {
			@Override
			public int compare(Player o1, Player o2) {
				// compare by average allocation
				PlayerHistory h1 = historyMap.get(o1.getId());
				PlayerHistory h2 = historyMap.get(o2.getId());
				if (h1 == null && h2 == null)
					return 0;
				else if (h1 == null) {
					return 1;
				} else if (h2 == null) {
					return -1;
				}
				return Double.compare(h1.getAverageProvided(),
						h2.getAverageProvided());
			}
		}));

		// f4: sort by no of cycles in cluster DESC
		ArrayList<Player> f4 = new ArrayList<Player>(players);
		Collections.sort(f4, Collections.reverseOrder(new Comparator<Player>() {
			@Override
			public int compare(Player o1, Player o2) {
				// compare by average allocation
				PlayerHistory h1 = historyMap.get(o1.getId());
				PlayerHistory h2 = historyMap.get(o2.getId());
				if (h1 == null && h2 == null)
					return 0;
				else if (h1 == null) {
					return 1;
				} else if (h2 == null) {
					return -1;
				}
				return Integer.compare(h1.getRoundsParticipated(),
						h2.getRoundsParticipated());
			}
		}));

		Map<UUID, BordaRank> ranks = new HashMap<UUID, BordaRank>();
		for (Player p : players) {
			ranks.put(p.getId(), new BordaRank(p));
		}
		ArrayList<BordaRank> rankList = new ArrayList<BordaRank>(ranks.values());

		// associate ranks with agents
		for (int i = 0; i < f1.size(); i++) {
			Player p = f1.get(i);
			ranks.get(p.getId()).setF1(i);
		}
		for (int i = 0; i < f1a.size(); i++) {
			Player p = f1a.get(i);
			ranks.get(p.getId()).setF1a(i);
		}
		for (int i = 0; i < f2.size(); i++) {
			Player p = f2.get(i);
			ranks.get(p.getId()).setF2(i);
		}
		for (int i = 0; i < f3.size(); i++) {
			Player p = f3.get(i);
			ranks.get(p.getId()).setF3(i);
		}
		for (int i = 0; i < f4.size(); i++) {
			Player p = f4.get(i);
			ranks.get(p.getId()).setF4(i);
		}

		final int nPlayers = players.size();

		// sort BordaRanks by borda score DESC.
		Collections.sort(rankList, new Comparator<BordaRank>() {
			@Override
			public int compare(BordaRank o1, BordaRank o2) {
				double p1score = getScore(o1, nPlayers, fixedWeights);
				double p2score = getScore(o2, nPlayers, fixedWeights);
				return Double.compare(p2score, p1score);
			}

		});

		for (BordaRank p : rankList) {
			double allocation = Math.min(p.getPlayer().getD(), poolSize);
			session.insert(new Allocate(p.getPlayer(), allocation));
			poolSize -= allocation;
			logger.info(p + ": " + getScore(p, nPlayers, fixedWeights));
		}
	}

	private static double getScore(BordaRank r, int nPlayers, double[] weights) {
		double score = 0;
		score += weights[0] * (nPlayers - r.getF1());
		score += weights[1] * (nPlayers - r.getF1a());
		score += weights[2] * (nPlayers - r.getF2());
		score += weights[3] * (nPlayers - r.getF3());
		score += weights[4] * (nPlayers - r.getF4());
		return score;
	}

}
