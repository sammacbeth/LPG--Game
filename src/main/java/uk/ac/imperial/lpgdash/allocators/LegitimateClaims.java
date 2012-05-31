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

import uk.ac.imperial.lpgdash.LPGService;
import uk.ac.imperial.lpgdash.actions.Allocate;
import uk.ac.imperial.lpgdash.facts.Allocation;
import uk.ac.imperial.lpgdash.facts.BordaRank;
import uk.ac.imperial.lpgdash.facts.Cluster;
import uk.ac.imperial.lpgdash.facts.Player;
import uk.ac.imperial.lpgdash.facts.PlayerHistory;
import uk.ac.imperial.presage2.core.db.StorageService;
import uk.ac.imperial.presage2.core.db.persistent.TransientAgentState;

public class LegitimateClaims {

	private final static Logger logger = Logger
			.getLogger(LegitimateClaims.class);

	public static StorageService sto = null;
	public static LPGService game = null;

	private static double[] fixedWeights = { 1, 1, 1, 1, 1, 1, 1 };

	public static List<Player> getF1(final List<Player> players,
			final Map<UUID, PlayerHistory> historyMap) {
		// f1: sort by average allocation ASC.
		ArrayList<Player> f1 = new ArrayList<Player>(players);
		if (fixedWeights[0] != 0.0) {
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
		}
		return f1;
	}

	public static List<Player> getF1a(final List<Player> players,
			final Map<UUID, PlayerHistory> historyMap) {
		// f1a: sort by rounds with allocation ASC
		ArrayList<Player> f1a = new ArrayList<Player>(players);
		if (fixedWeights[1] != 0.0) {
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
					return h1.getRoundsAllocated() - h2.getRoundsAllocated();
				}
			});
		}
		return f1a;
	}

	public static List<Player> getF2(final List<Player> players,
			final Map<UUID, PlayerHistory> historyMap) {
		// f2: sort by average demand ASC.
		ArrayList<Player> f2 = new ArrayList<Player>(players);
		if (fixedWeights[2] != 0.0) {
			Collections.sort(f2, new Comparator<Player>() {
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
			});
		}
		return f2;
	}

	public static List<Player> getF3(final List<Player> players,
			final Map<UUID, PlayerHistory> historyMap) {
		// f3: sort by average provision DESC.
		ArrayList<Player> f3 = new ArrayList<Player>(players);
		if (fixedWeights[3] != 0.0) {
			Collections.sort(f3,
					Collections.reverseOrder(new Comparator<Player>() {
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
		}
		return f3;
	}

	public static List<Player> getF4(final List<Player> players,
			final Map<UUID, PlayerHistory> historyMap) {
		// f4: sort by no of cycles in cluster DESC
		ArrayList<Player> f4 = new ArrayList<Player>(players);
		if (fixedWeights[4] != 0.0) {
			Collections.sort(f4, new Comparator<Player>() {
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
					return h2.getRoundsParticipated()
							- h1.getRoundsParticipated();
				}
			});
		}
		return f4;
	}

	public static List<Player> getF5(final List<Player> players,
			final Map<UUID, PlayerHistory> historyMap) {
		// f5: sort by no of cycles as head DESC
		ArrayList<Player> f5 = new ArrayList<Player>(players);
		if (fixedWeights[5] != 0.0) {
			Collections.sort(f5, new Comparator<Player>() {
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
					return h2.getRoundsAsHead() - h1.getRoundsAsHead();
				}
			});
		}
		return f5;
	}

	public static List<Player> getF6(final List<Player> players,
			final Map<UUID, PlayerHistory> historyMap) {
		// f6: sort by no of compliant cycles DESC
		ArrayList<Player> f6 = new ArrayList<Player>(players);
		if (fixedWeights[6] != 0.0) {
			Collections.sort(f6, new Comparator<Player>() {
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
					return h2.getCompliantRounds() - h1.getCompliantRounds();
				}
			});
		}
		return f6;
	}

	public static List<Player> getF7(final List<Player> players,
			final Map<UUID, PlayerHistory> historyMap) {
		// f7: sort by satisfaction ASC
		ArrayList<Player> f7 = new ArrayList<Player>(players);
		if (fixedWeights[7] != 0.0) {
			Collections.sort(f7, new Comparator<Player>() {
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
					return Double.compare(h1.getSatisfaction(),
							h2.getSatisfaction());
				}
			});
		}
		return f7;
	}

	public static void allocate(StatefulKnowledgeSession session,
			List<Player> players, double poolSize, Cluster c,
			final Allocation method, int t) {

		switch (method) {
		case LC_F1:
			fixedWeights = new double[] { 1, 0, 0, 0, 0, 0, 0, 0 };
			break;
		case LC_F1a:
			fixedWeights = new double[] { 0, 1, 0, 0, 0, 0, 0, 0 };
			break;
		case LC_F2:
			fixedWeights = new double[] { 0, 0, 1, 0, 0, 0, 0, 0 };
			break;
		case LC_F3:
			fixedWeights = new double[] { 0, 0, 0, 1, 0, 0, 0, 0 };
			break;
		case LC_F4:
			fixedWeights = new double[] { 0, 0, 0, 0, 1, 0, 0, 0 };
			break;
		case LC_F5:
			fixedWeights = new double[] { 0, 0, 0, 0, 0, 1, 0, 0 };
			break;
		case LC_F6:
			fixedWeights = new double[] { 0, 0, 0, 0, 0, 0, 1, 0 };
			break;
		case LC_F7:
			fixedWeights = new double[] { 0, 0, 0, 0, 0, 0, 0, 1 };
			break;
		case LC_FIXED:
		default:
			fixedWeights = new double[] { 0.125, 0.125, 0.125, 0.125, 0.125,
					0.125, 0.125, 0.125 };
			break;
		}

		// map player histories
		final Map<UUID, PlayerHistory> historyMap = new HashMap<UUID, PlayerHistory>(
				players.size());
		for (Player p : players) {
			historyMap.put(p.getId(), p.getHistory().get(c));
			storeHistory(p.getId(), p.getHistory().get(c));
		}

		players = new ArrayList<Player>(players);
		Collections.shuffle(players);
		List<Player> f1 = getF1(players, historyMap);
		Collections.shuffle(players);
		List<Player> f1a = getF1a(players, historyMap);
		Collections.shuffle(players);
		List<Player> f2 = getF2(players, historyMap);
		Collections.shuffle(players);
		List<Player> f3 = getF3(players, historyMap);
		Collections.shuffle(players);
		List<Player> f4 = getF4(players, historyMap);
		Collections.shuffle(players);
		List<Player> f5 = getF5(players, historyMap);
		Collections.shuffle(players);
		List<Player> f6 = getF6(players, historyMap);
		Collections.shuffle(players);
		List<Player> f7 = getF7(players, historyMap);

		Map<UUID, BordaRank> ranks = new HashMap<UUID, BordaRank>();
		for (Player p : players) {
			ranks.put(p.getId(), new BordaRank(p));
		}
		ArrayList<BordaRank> rankList = new ArrayList<BordaRank>(ranks.values());

		// associate ranks with agents
		for (int i = 0; i < players.size(); i++) {
			ranks.get(f1.get(i).getId()).setF1(i);
			ranks.get(f1a.get(i).getId()).setF1a(i);
			ranks.get(f2.get(i).getId()).setF2(i);
			ranks.get(f3.get(i).getId()).setF3(i);
			ranks.get(f4.get(i).getId()).setF4(i);
			ranks.get(f5.get(i).getId()).setF5(i);
			ranks.get(f6.get(i).getId()).setF6(i);
			ranks.get(f7.get(i).getId()).setF7(i);
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
			session.insert(new Allocate(p.getPlayer(), allocation, t));
			poolSize -= allocation;
			logger.info(p + ": " + getScore(p, nPlayers, fixedWeights));
			storeRanks(p, nPlayers, fixedWeights);
		}
	}

	private static double getScore(BordaRank r, int nPlayers, double[] weights) {
		double score = 0;
		score += weights[0] * (nPlayers - r.getF1());
		score += weights[1] * (nPlayers - r.getF1a());
		score += weights[2] * (nPlayers - r.getF2());
		score += weights[3] * (nPlayers - r.getF3());
		score += weights[4] * (nPlayers - r.getF4());
		score += weights[5] * (nPlayers - r.getF5());
		score += weights[6] * (nPlayers - r.getF6());
		score += weights[7] * (nPlayers - r.getF7());
		return score;
	}

	private static void storeHistory(UUID id, PlayerHistory playerHistory) {
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

	private static void storeRanks(BordaRank r, int n, double[] weights) {
		if (sto != null) {
			TransientAgentState s = sto.getAgentState(r.getPlayer().getId(),
					game.getRoundNumber());
			s.setProperty("f1", Double.toString(weights[0] * (n - r.getF1())));
			s.setProperty("f1a", Double.toString(weights[1] * (n - r.getF1a())));
			s.setProperty("f2", Double.toString(weights[2] * (n - r.getF2())));
			s.setProperty("f3", Double.toString(weights[3] * (n - r.getF3())));
			s.setProperty("f4", Double.toString(weights[4] * (n - r.getF4())));
			s.setProperty("f5", Double.toString(weights[5] * (n - r.getF5())));
			s.setProperty("f6", Double.toString(weights[6] * (n - r.getF6())));
			s.setProperty("f7", Double.toString(weights[7] * (n - r.getF7())));
		}
	}

}
