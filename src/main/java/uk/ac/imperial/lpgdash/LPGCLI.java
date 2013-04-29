package uk.ac.imperial.lpgdash;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import uk.ac.imperial.lpgdash.LPGGameSimulation.MateSelection;
import uk.ac.imperial.lpgdash.LPGGameSimulation.Reproduction;
import uk.ac.imperial.lpgdash.LPGPlayer.ClusterSelectionAlgorithm;
import uk.ac.imperial.lpgdash.db.ConnectionlessStorage;
import uk.ac.imperial.lpgdash.db.Queries;
import uk.ac.imperial.lpgdash.facts.Allocation;
import uk.ac.imperial.lpgdash.gui.LPGGui;
import uk.ac.imperial.presage2.core.cli.Presage2CLI;
import uk.ac.imperial.presage2.core.db.DatabaseService;
import uk.ac.imperial.presage2.core.db.StorageService;
import uk.ac.imperial.presage2.core.db.persistent.PersistentAgent;
import uk.ac.imperial.presage2.core.db.persistent.PersistentSimulation;
import uk.ac.imperial.presage2.core.db.persistent.TransientAgentState;
import uk.ac.imperial.presage2.core.simulator.RunnableSimulation;

public class LPGCLI extends Presage2CLI {

	private final Logger logger = Logger.getLogger(LPGCLI.class);

	protected LPGCLI() {
		super(LPGCLI.class);
	}

	public static void main(String[] args) {
		Presage2CLI cli = new LPGCLI();
		cli.invokeCommand(args);
	}

	@Command(name = "insert", description = "Insert a batch of simulations to run.")
	public void insert_batch(String[] args) {

		Options options = new Options();

		// generate experiment types
		Map<String, String> experiments = new HashMap<String, String>();
		experiments.put("lc_comparison",
				"Compare individual legitimate claims + fixed and SO weights.");
		experiments
				.put("het_hom",
						"Compare allocation methods in heterogeneous and homogeneous populations.");
		experiments
				.put("multi_cluster",
						"Multi-cluster scenario with lc_so and random allocations over beta {0.1,0.4}");
		experiments.put("memory", "Increasing agent memory sizes");
		experiments.put("hack", "Check hacks.");
		experiments.put("large_pop", "Large population.");
		experiments.put("optimal", "Find the optimal cheat strategy.");
		experiments.put("boltzmann", "Use boltzmann dist to choose clusters");
		experiments.put("cheat", "Test different cheating strategies");
		experiments.put("evolve", "Reproducing players");

		OptionGroup exprOptions = new OptionGroup();
		for (String key : experiments.keySet()) {
			exprOptions.addOption(new Option(key, experiments.get(key)));
		}

		// check for experiment type argument
		if (args.length < 2 || !experiments.containsKey(args[1])) {
			options.addOptionGroup(exprOptions);
			HelpFormatter formatter = new HelpFormatter();
			formatter.setOptPrefix("");
			formatter.printHelp("presage2cli insert <experiment>", options,
					false);
			return;
		}

		// optional random seed arg
		options.addOption(
				"s",
				"seed",
				true,
				"Random seed to start with (subsequent repeats use incrementing seeds from this value)");

		int repeats = 0;
		try {
			repeats = Integer.parseInt(args[2]);
		} catch (ArrayIndexOutOfBoundsException e) {
			logger.warn("REPEATS argument missing");
		} catch (NumberFormatException e) {
			logger.warn("REPEATS argument is not a valid integer");
		}

		if (repeats <= 0) {
			HelpFormatter formatter = new HelpFormatter();
			// formatter.setOptPrefix("");
			formatter.printHelp("presage2cli insert " + args[1] + " REPEATS",
					options, true);
			return;
		}

		CommandLineParser parser = new GnuParser();
		CommandLine cmd;
		int seed = 0;
		try {
			cmd = parser.parse(options, args);
			seed = Integer.parseInt(cmd.getOptionValue("seed"));
		} catch (ParseException e) {
			e.printStackTrace();
			return;
		} catch (NumberFormatException e) {
		} catch (NullPointerException e) {
		}

		if (args[1].equalsIgnoreCase("lc_comparison")) {
			lc_comparison(repeats, seed);
		} else if (args[1].equalsIgnoreCase("het_hom")) {
			het_hom(repeats, seed);
		} else if (args[1].equalsIgnoreCase("multi_cluster")) {
			multi_cluster(repeats, seed);
		} else if (args[1].equalsIgnoreCase("memory")) {
			memory(repeats, seed);
		} else if (args[1].equalsIgnoreCase("hack")) {
			weight_mechanisms(repeats, seed);
		} else if (args[1].equalsIgnoreCase("large_pop")) {
			large_pop(repeats, seed);
		} else if (args[1].equalsIgnoreCase("optimal")) {
			optimal(repeats, seed);
		} else if (args[1].equalsIgnoreCase("boltzmann")) {
			boltzmann_multi_cluster(repeats, seed);
		} else if (args[1].equalsIgnoreCase("cheat")) {
			cheatOnAppropriate(repeats, seed);
		} else if (args[1].equalsIgnoreCase("evolve")) {
			evolve(repeats, seed);
		}

	}

	void lc_comparison(int repeats, int seed) {
		Allocation[] clusters = { Allocation.LC_F1a, Allocation.LC_F1b,
				Allocation.LC_F1c, Allocation.LC_F2, Allocation.LC_F3,
				Allocation.LC_F4, Allocation.LC_F5, Allocation.LC_F6,
				Allocation.LC_FIXED, Allocation.LC_SO };
		int rounds = 1002;

		for (int i = 0; i < repeats; i++) {
			for (Allocation cluster : clusters) {
				PersistentSimulation sim = getDatabase().createSimulation(
						cluster.name(),
						"uk.ac.imperial.lpgdash.LPGGameSimulation",
						"AUTO START", rounds);

				sim.addParameter("finishTime", Integer.toString(rounds));
				sim.addParameter("alpha", Double.toString(0.1));
				sim.addParameter("beta", Double.toString(0.1));
				sim.addParameter("gamma", Double.toString(0.1));
				sim.addParameter("cCount", Integer.toString(20));
				sim.addParameter("cPCheat", Double.toString(0.02));
				sim.addParameter("ncCount", Integer.toString(10));
				sim.addParameter("ncPCheat", Double.toString(0.25));
				sim.addParameter("seed", Integer.toString(seed + i));
				sim.addParameter("soHack", Boolean.toString(true));
				sim.addParameter("clusters", cluster.name());
				sim.addParameter("cheatOn", Cheat.PROVISION.name());

				logger.info("Created sim: " + sim.getID() + " - "
						+ sim.getName());
			}
		}
		stopDatabase();
	}

	void het_hom(int repeats, int seed) {
		Allocation[] clusters = { Allocation.RATION, Allocation.RANDOM,
				Allocation.LC_FIXED, Allocation.LC_SO };
		String[] populations = { "het01", "hom01", "hom04" };
		int rounds = 1002;

		for (int i = 0; i < repeats; i++) {
			for (Allocation cluster : clusters) {
				for (String pop : populations) {
					double beta = 0.1;
					int c = 30;
					double cPCheat = 0.0;
					if (pop.endsWith("04"))
						beta = 0.4;
					if (pop.startsWith("het")) {
						c = 20;
						cPCheat = 0.02;
					}

					PersistentSimulation sim = getDatabase().createSimulation(
							cluster.name() + "_" + pop + "_rand",
							"uk.ac.imperial.lpgdash.LPGGameSimulation",
							"AUTO START", rounds);

					sim.addParameter("finishTime", Integer.toString(rounds));
					sim.addParameter("alpha", Double.toString(0.1));
					sim.addParameter("beta", Double.toString(beta));
					sim.addParameter("gamma", Double.toString(0.1));
					sim.addParameter("cCount", Integer.toString(c));
					sim.addParameter("cPCheat", Double.toString(cPCheat));
					sim.addParameter("ncCount", Integer.toString(30 - c));
					sim.addParameter("ncPCheat", Double.toString(0.25));
					sim.addParameter("seed", Integer.toString(seed + i));
					sim.addParameter("soHd", Boolean.toString(true));
					sim.addParameter("soHack", Boolean.toString(true));
					sim.addParameter("clusters", cluster.name());
					sim.addParameter("cheatOn", "random");

					logger.info("Created sim: " + sim.getID() + " - "
							+ sim.getName());
				}
			}
		}
		stopDatabase();
	}

	void multi_cluster(int repeats, int seed) {
		int rounds = 1000;
		double[] betas = { 0.1, 0.4 };
		String cluster = Allocation.LC_SO.name() + "," + Allocation.RANDOM;
		for (int i = 0; i < repeats; i++) {
			for (double beta : betas) {
				PersistentSimulation sim = getDatabase().createSimulation(
						cluster + "_b=" + beta,
						"uk.ac.imperial.lpgdash.LPGGameSimulation",
						"AUTO START", rounds);

				sim.addParameter("finishTime", Integer.toString(rounds));
				sim.addParameter("alpha", Double.toString(0.1));
				sim.addParameter("beta", Double.toString(beta));
				sim.addParameter("gamma", Double.toString(0.1));
				sim.addParameter("cCount", Integer.toString(20));
				sim.addParameter("cPCheat", Double.toString(0.02));
				sim.addParameter("ncCount", Integer.toString(20));
				sim.addParameter("ncPCheat", Double.toString(0.25));
				sim.addParameter("seed", Integer.toString(seed + i));
				sim.addParameter("soHack", Boolean.toString(true));
				sim.addParameter("clusters", cluster);
				sim.addParameter("cheatOn", Cheat.PROVISION.name());
			}
		}
		stopDatabase();
	}

	void memory(int repeats, int seed) {
		int rounds = 2002;
		for (int i = 0; i < repeats; i++) {
			for (int memory : new int[] { 1, 10, 50, 100, 200 }) {
				PersistentSimulation sim = getDatabase().createSimulation(
						"memory_nh_" + String.format("%3d", memory),
						"uk.ac.imperial.lpgdash.LPGGameSimulation",
						"AUTO START", rounds);
				sim.addParameter("finishTime", Integer.toString(rounds));
				sim.addParameter("alpha", Double.toString(0.1));
				sim.addParameter("beta", Double.toString(0.1));
				sim.addParameter("gamma", Double.toString(0.1));
				sim.addParameter("cCount", Integer.toString(20));
				sim.addParameter("cPCheat", Double.toString(0.02));
				sim.addParameter("ncCount", Integer.toString(10));
				sim.addParameter("ncPCheat", Double.toString(0.25));
				sim.addParameter("seed", Integer.toString(seed + i));
				sim.addParameter("soHd", Boolean.toString(false));
				sim.addParameter("soHack", Boolean.toString(false));
				sim.addParameter("clusters", Allocation.LC_SO.name());
				sim.addParameter("cheatOn", Cheat.PROVISION.name());
				sim.addParameter("rankMemory", Integer.toString(memory));

				logger.info("Created sim: " + sim.getID() + " - "
						+ sim.getName());
			}
		}
		stopDatabase();
	}

	void weight_mechanisms(int repeats, int seed) {
		int rounds = 10000;
		for (int i = 0; i < repeats; i++) {
			for (boolean soHd : new boolean[] { false, true }) {
				for (boolean soHack : new boolean[] { false, true }) {
					PersistentSimulation sim = getDatabase().createSimulation(
							"wm_" + (soHd ? "t" : "f") + "_"
									+ (soHack ? "t" : "f"),
							"uk.ac.imperial.lpgdash.LPGGameSimulation",
							"AUTO START", rounds);
					sim.addParameter("finishTime", Integer.toString(rounds));
					sim.addParameter("alpha", Double.toString(0.1));
					sim.addParameter("beta", Double.toString(0.1));
					sim.addParameter("gamma", Double.toString(0.1));
					sim.addParameter("cCount", Integer.toString(20));
					sim.addParameter("cPCheat", Double.toString(0.02));
					sim.addParameter("ncCount", Integer.toString(10));
					sim.addParameter("ncPCheat", Double.toString(0.25));
					sim.addParameter("seed", Integer.toString(seed + i));
					sim.addParameter("soHd", Boolean.toString(soHd));
					sim.addParameter("soHack", Boolean.toString(soHack));
					sim.addParameter("clusters", Allocation.LC_SO.name());
					sim.addParameter("cheatOn", Cheat.PROVISION.name());

					logger.info("Created sim: " + sim.getID() + " - "
							+ sim.getName());
				}
			}
		}
		stopDatabase();
	}

	void large_pop(int repeats, int seed) {
		Allocation[] clusters = { Allocation.RANDOM, Allocation.LC_SO };
		int rounds = 2002;
		int agents = 100;

		for (int i = 0; i < repeats; i++) {
			for (Allocation cluster : clusters) {
				for (double ncProp : new double[] { 0.0, 0.1, 0.2, 0.3, 0.4,
						0.5, 0.6, 0.7, 0.8, 0.9, 1.0 }) {
					int nc = (int) Math.round(agents * ncProp);

					PersistentSimulation sim = getDatabase().createSimulation(
							cluster.name() + "_" + String.format("%03d", nc)
									+ "_pro",
							"uk.ac.imperial.lpgdash.LPGGameSimulation",
							"AUTO START", rounds);

					sim.addParameter("finishTime", Integer.toString(rounds));
					sim.addParameter("alpha", Double.toString(0.1));
					sim.addParameter("beta", Double.toString(0.1));
					sim.addParameter("gamma", Double.toString(0.1));
					sim.addParameter("cCount", Integer.toString(agents - nc));
					sim.addParameter("cPCheat", Double.toString(0.02));
					sim.addParameter("ncCount", Integer.toString(nc));
					sim.addParameter("ncPCheat", Double.toString(0.25));
					sim.addParameter("seed", Integer.toString(seed + i));
					sim.addParameter("soHd", Boolean.toString(true));
					sim.addParameter("soHack", Boolean.toString(true));
					sim.addParameter("clusters", cluster.name());
					sim.addParameter("cheatOn", Cheat.PROVISION.name());

					logger.info("Created sim: " + sim.getID() + " - "
							+ sim.getName());
				}
			}
		}
		stopDatabase();
	}

	void optimal(int repeats, int seed) {
		Allocation[] clusters = { Allocation.RANDOM, Allocation.LC_FIXED,
				Allocation.LC_SO };
		Cheat[] cheatMethods = { Cheat.DEMAND, Cheat.PROVISION,
				Cheat.APPROPRIATE };
		int rounds = 2002;

		// minority optimal
		for (int i = 0; i < repeats; i++) {
			for (Allocation cluster : clusters) {
				for (Cheat ch : cheatMethods) {
					double ncStrat = 0.0;
					while (ncStrat <= 1.0) {
						String stratStr = Double.toString(ncStrat);
						stratStr = stratStr.substring(0,
								Math.min(4, stratStr.length()));

						PersistentSimulation sim = getDatabase()
								.createSimulation(
										"min_" + cluster.name() + "_"
												+ stratStr + "_"
												+ ch.name().substring(0, 3),
										"uk.ac.imperial.lpgdash.LPGGameSimulation",
										"AUTO START", rounds);

						sim.addParameter("finishTime", Integer.toString(rounds));
						sim.addParameter("alpha", Double.toString(0.1));
						sim.addParameter("beta", Double.toString(0.1));
						sim.addParameter("gamma", Double.toString(0.1));
						sim.addParameter("cCount", Integer.toString(20));
						sim.addParameter("cPCheat", Double.toString(0.02));
						sim.addParameter("ncCount", Integer.toString(10));
						sim.addParameter("ncPCheat", Double.toString(ncStrat));
						sim.addParameter("seed", Integer.toString(seed + i));
						sim.addParameter("soHd", Boolean.toString(true));
						sim.addParameter("soHack", Boolean.toString(true));
						sim.addParameter("clusters", cluster.name());
						sim.addParameter("cheatOn", ch.name());

						ncStrat += 0.05;
					}
				}
			}
		}
		// majority optimal
		for (int i = 0; i < repeats; i++) {
			for (Allocation cluster : clusters) {
				for (Cheat ch : cheatMethods) {
					double cStrat = 0.0;
					while (cStrat <= 1.0) {
						String stratStr = Double.toString(cStrat);
						stratStr = stratStr.substring(0,
								Math.min(4, stratStr.length()));

						PersistentSimulation sim = getDatabase()
								.createSimulation(
										"maj_" + cluster.name() + "_"
												+ stratStr + "_"
												+ ch.name().substring(0, 3),
										"uk.ac.imperial.lpgdash.LPGGameSimulation",
										"AUTO START", rounds);

						sim.addParameter("finishTime", Integer.toString(rounds));
						sim.addParameter("alpha", Double.toString(0.1));
						sim.addParameter("beta", Double.toString(0.1));
						sim.addParameter("gamma", Double.toString(0.1));
						sim.addParameter("cCount", Integer.toString(20));
						sim.addParameter("cPCheat", Double.toString(cStrat));
						sim.addParameter("ncCount", Integer.toString(10));
						sim.addParameter("ncPCheat", Double.toString(0.25));
						sim.addParameter("seed", Integer.toString(seed + i));
						sim.addParameter("soHd", Boolean.toString(true));
						sim.addParameter("soHack", Boolean.toString(true));
						sim.addParameter("clusters", cluster.name());
						sim.addParameter("cheatOn", ch.name());

						cStrat += 0.05;
					}
				}
			}
		}
		stopDatabase();
	}

	void boltzmann_multi_cluster(int repeats, int seed) {
		Allocation[] clusters = { Allocation.RANDOM, Allocation.LC_FIXED,
				Allocation.LC_SO };
		Cheat[] cheatMethods = { Cheat.DEMAND, Cheat.PROVISION,
				Cheat.APPROPRIATE };
		int rounds = 5002;

		for (int i = 0; i < repeats; i++) {
			for (boolean resetSat : new boolean[] { false, true }) {
				for (Cheat ch : cheatMethods) {
					PersistentSimulation sim = getDatabase().createSimulation(
							"boltzmann_" + (resetSat ? "reset" : "base") + "_"
									+ ch.name().substring(0, 3),
							"uk.ac.imperial.lpgdash.LPGGameSimulation",
							"AUTO START", rounds);
					sim.addParameter("finishTime", Integer.toString(rounds));
					sim.addParameter("alpha", Double.toString(0.1));
					sim.addParameter("beta", Double.toString(0.1));
					sim.addParameter("gamma", Double.toString(0.1));
					sim.addParameter("cCount", Integer.toString(60));
					sim.addParameter("cPCheat", Double.toString(0.02));
					sim.addParameter("ncCount", Integer.toString(30));
					sim.addParameter("ncPCheat", Double.toString(0.25));
					sim.addParameter("seed", Integer.toString(seed + i));
					sim.addParameter("soHd", Boolean.toString(true));
					sim.addParameter("soHack", Boolean.toString(true));
					sim.addParameter("clusters",
							StringUtils.join(clusters, ','));
					sim.addParameter("cheatOn", ch.name());
					sim.addParameter("clusterSelect",
							ClusterSelectionAlgorithm.BOLTZMANN.name());
					sim.addParameter("resetSatisfaction",
							Boolean.toString(resetSat));
				}
			}
		}
		// control
		for (int i = 0; i < repeats; i++) {
			for (Cheat ch : cheatMethods) {
				PersistentSimulation sim = getDatabase().createSimulation(
						"control_" + ch.name().substring(0, 3),
						"uk.ac.imperial.lpgdash.LPGGameSimulation",
						"AUTO START", rounds);
				sim.addParameter("finishTime", Integer.toString(rounds));
				sim.addParameter("alpha", Double.toString(0.1));
				sim.addParameter("beta", Double.toString(0.1));
				sim.addParameter("gamma", Double.toString(0.1));
				sim.addParameter("cCount", Integer.toString(60));
				sim.addParameter("cPCheat", Double.toString(0.02));
				sim.addParameter("ncCount", Integer.toString(30));
				sim.addParameter("ncPCheat", Double.toString(0.25));
				sim.addParameter("seed", Integer.toString(seed + i));
				sim.addParameter("soHd", Boolean.toString(true));
				sim.addParameter("soHack", Boolean.toString(true));
				sim.addParameter("clusters", StringUtils.join(clusters, ','));
				sim.addParameter("cheatOn", ch.name());
				sim.addParameter("clusterSelect",
						ClusterSelectionAlgorithm.PREFERRED.name());
			}
		}
		stopDatabase();
	}

	void cheatOnAppropriate(int repeats, int seed) {
		Allocation[] clusters = { Allocation.RANDOM, Allocation.LC_FIXED,
				Allocation.LC_SO };
		Cheat[] cheatMethods = { Cheat.DEMAND, Cheat.PROVISION,
				Cheat.APPROPRIATE };
		int rounds = 1002;
		for (int i = 0; i < repeats; i++) {
			for (Allocation cl : clusters) {
				for (Cheat ch : cheatMethods) {
					PersistentSimulation sim = getDatabase().createSimulation(
							cl.name() + "_" + ch.name().substring(0, 3),
							"uk.ac.imperial.lpgdash.LPGGameSimulation",
							"AUTO START", rounds);

					sim.addParameter("finishTime", Integer.toString(rounds));
					sim.addParameter("alpha", Double.toString(0.1));
					sim.addParameter("beta", Double.toString(0.1));
					sim.addParameter("gamma", Double.toString(0.1));
					sim.addParameter("cCount", Integer.toString(20));
					sim.addParameter("cPCheat", Double.toString(0.02));
					sim.addParameter("ncCount", Integer.toString(10));
					sim.addParameter("ncPCheat", Double.toString(0.25));
					sim.addParameter("seed", Integer.toString(seed + i));
					sim.addParameter("soHd", Boolean.toString(true));
					sim.addParameter("soHack", Boolean.toString(true));
					sim.addParameter("clusters", cl.name());
					sim.addParameter("cheatOn", ch.name());

					logger.info("Created sim: " + sim.getID() + " - "
							+ sim.getName());
				}
			}
		}
		stopDatabase();
	}

	void evolve(int repeats, int seed) {
		Allocation[] clusters = { Allocation.RANDOM, Allocation.QUEUE,
				Allocation.LC_FIXED, Allocation.LC_SO };
		int rounds = 5000;
		for (int i = 0; i < repeats; i++) {
			for (String pop : new String[] { "random", "static" }) {
				for (Reproduction rep : new Reproduction[] { Reproduction.NONE,
						Reproduction.PERIODIC, Reproduction.RANDOM }) {
					for (MateSelection mate : new MateSelection[] {
							MateSelection.RANDOM, MateSelection.TOTAL_UTILITY,
							MateSelection.ROLLING_UTILITY }) {
						for (Allocation cl : clusters) {
							PersistentSimulation sim = getDatabase()
									.createSimulation(
											StringUtils.join(
													new String[] { cl.name(),
															pop, rep.name(),
															mate.name() }, '_'),
											"uk.ac.imperial.lpgdash.LPGGameSimulation",
											"AUTO START", rounds);
							sim.addParameter("finishTime",
									Integer.toString(rounds));
							sim.addParameter("alpha", Double.toString(0.1));
							sim.addParameter("beta", Double.toString(0.1));
							sim.addParameter("gamma", Double.toString(0.1));
							sim.addParameter("cCount", Integer.toString(20));
							sim.addParameter("cPCheat", Double.toString(0.02));
							sim.addParameter("ncCount", Integer.toString(10));
							sim.addParameter("ncPCheat", Double.toString(0.25));
							sim.addParameter("seed", Integer.toString(seed + i));
							sim.addParameter("clusters", cl.name());
							sim.addParameter("cheatOn", "random");
							sim.addParameter("reproduction", rep.name());
							sim.addParameter("pop", pop);
							sim.addParameter("mateSelect", mate.name());
						}
					}
				}
			}
		}
		stopDatabase();
	}

	@Command(name = "summarise", description = "Process raw simulation data to generate evaluation metrics.")
	public void summarise(String[] args) {
		logger.warn("This implementation assumes you are using postgresql >=9.1 with hstore, it will fail otherwise.");
		// get database to trigger injector creation
		getDatabase();
		// pull JDBC connection from injector
		Connection conn = injector.getInstance(Connection.class);

		try {
			logger.info("Creating tables and views. ");

			logger.info("CREATE VIEW allocationRatios");
			conn.createStatement().execute(
					Queries.getQuery("create_allocationratios"));

			logger.info("CREATE TABLE simulationSummary");
			conn.createStatement().execute(
					Queries.getQuery("create_simulationsummary"));

			logger.info("CREATE VIEW aggregatedSimulations");
			conn.createStatement().execute(
					Queries.getQuery("create_aggregatedsimulations"));

			logger.info("CREATE TABLE aggregatePlayerScore");
			conn.createStatement().execute(
					Queries.getQuery("create_aggregateplayerscore"));

			logger.info("Vacuuming database...");
			conn.createStatement().execute("VACUUM FULL");

			logger.info("Processing simulations...");

			// prepare statements
			PreparedStatement aggregatePlayerScore = conn
					.prepareStatement(Queries
							.getQuery("insert_aggregateplayerscore"));
			PreparedStatement clusterStats = conn.prepareStatement(Queries
					.getQuery("select_clusters"));
			PreparedStatement remaining = conn.prepareStatement(Queries
					.getQuery("select_agentsremaining"));
			PreparedStatement insertSummary = conn.prepareStatement(Queries
					.getQuery("insert_simulationsummary"));

			// get subset to process
			ResultSet unprocessed = conn.createStatement().executeQuery(
					Queries.getQuery("select_unprocessedsimulations"));

			while (unprocessed.next()) {
				long id = unprocessed.getLong(1);
				String name = unprocessed.getString(2);
				int finishTime = unprocessed.getInt(3);
				int cutoff = (int) (Math.floor(finishTime / 2)) - 1;

				logger.info(id + ": " + name);

				// START TRANSACTION
				conn.setAutoCommit(false);

				// generate player scores per cluster
				aggregatePlayerScore.setLong(1, id);
				aggregatePlayerScore.setLong(2, id);
				aggregatePlayerScore.execute();

				clusterStats.setLong(1, id);
				ResultSet clusters = clusterStats.executeQuery();
				logger.debug("Cutoff: " + cutoff);
				while (clusters.next()) {
					int cluster = clusters.getInt(1);
					logger.debug("Cluster " + cluster);

					// calculate c and nc remaining
					int crem = 0;
					int ncrem = 0;

					remaining.setLong(1, id);
					remaining.setString(2, "c%");
					remaining.setInt(3, cutoff);
					remaining.setInt(4, cluster);
					ResultSet rs = remaining.executeQuery();
					if (rs.next()) {
						crem = rs.getInt(1);
					}

					remaining.setString(2, "nc%");
					rs = remaining.executeQuery();
					if (rs.next()) {
						ncrem = rs.getInt(1);
					}

					// insert summary
					insertSummary.setLong(1, id);
					insertSummary.setString(2, name);
					insertSummary.setInt(3, cluster);
					insertSummary.setDouble(4, clusters.getDouble(2));
					insertSummary.setDouble(5, clusters.getDouble(3));
					insertSummary.setDouble(6, clusters.getDouble(4));
					insertSummary.setDouble(7, clusters.getDouble(5));
					insertSummary.setDouble(8, clusters.getDouble(6));
					insertSummary.setInt(9, crem);
					insertSummary.setInt(10, ncrem);
					insertSummary.execute();

				}

				// COMMIT TRANSACTION
				conn.commit();
			}
		} catch (SQLException e) {
			throw new RuntimeException(e);
		} finally {
			stopDatabase();
		}
	}

	@Command(name = "verify", description = "Check a result set for errors.")
	public void verify(String[] args) {
		long simulationID = 0;
		try {
			simulationID = Long.parseLong(args[1]);
		} catch (NumberFormatException e) {
			System.err.println("Simulation ID should be an integer.");
			return;
		} catch (ArrayIndexOutOfBoundsException e) {
			System.err.println("Please specify a simulation ID.");
		}

		StorageService storage = getDatabase();
		PersistentSimulation sim = storage.getSimulationById(simulationID);

		logger.info("Check for missing round data");
		for (PersistentAgent a : sim.getAgents()) {
			logger.info("Agent " + a.getName() + "...");
			boolean current;
			boolean next = hasMissingData(a.getState(1));
			for (int t = 2; t < (sim.getFinishTime() / 2); t++) {
				current = next;
				next = hasMissingData(a.getState(t));
				if (current && !next) {
					logger.warn("Missing round for agent " + a.getName()
							+ " timestep " + (t - 1));
				} else if (!current && next) {
					logger.info(a.getName() + " didn't play at round " + t);
				}
			}
		}
		stopDatabase();
	}

	private static boolean hasMissingData(TransientAgentState s) {
		Map<String, String> p = s.getProperties();
		return !p.containsKey("g") || !p.containsKey("d")
				|| !p.containsKey("p") || !p.containsKey("q")
				|| !p.containsKey("r") || !p.containsKey("r'")
				|| !p.containsKey("RTotal") || !p.containsKey("U");
	}

	@Command(name = "gini", description = "Calculate gini coefficient for simulations.")
	public void calculate_gini(String[] args) {
		// get database to trigger injector creation
		getDatabase();
		// pull JDBC connection from injector
		Connection conn = injector.getInstance(Connection.class);

		try {
			PreparedStatement clusterStats = conn.prepareStatement(Queries
					.getQuery("select_clusters"));
			PreparedStatement allocationRatios = conn.prepareStatement(Queries
					.getQuery("select_allocationratios"));
			// copied from PostgreSQLStorage
			PreparedStatement insertEnvironment = conn
					.prepareStatement("INSERT INTO environmentTransient (\"simId\", \"time\")"
							+ "	SELECT ?, ?"
							+ "	WHERE NOT EXISTS (SELECT 1 FROM environmentTransient WHERE \"simId\"=? AND \"time\" = ?);");
			PreparedStatement updateEnvironment = conn
					.prepareStatement("UPDATE environmentTransient SET state = state || hstore(?, ?) "
							+ "WHERE \"simId\" = ? AND \"time\" = ?");

			long simId = 70;

			PersistentSimulation sim = storage.getSimulationById(simId);
			int finishTime = sim.getFinishTime();
			clusterStats.setLong(1, simId);
			ResultSet clusters = clusterStats.executeQuery();
			while (clusters.next()) {
				int cluster = clusters.getInt(1);
				int t = 1;
				while (t <= finishTime / 2) {

					List<Double> cRatios = new ArrayList<Double>();
					List<Double> ncRatios = new ArrayList<Double>();
					List<Double> allRatios = new ArrayList<Double>();

					allocationRatios.setLong(1, simId);
					allocationRatios.setInt(2, cluster);
					allocationRatios.setInt(3, t);
					ResultSet ratios = allocationRatios.executeQuery();
					while (ratios.next()) {
						double r = ratios.getDouble(2);
						if (ratios.getString(1).startsWith("c"))
							cRatios.add(r);
						else
							ncRatios.add(r);
					}
					allRatios.addAll(cRatios);
					allRatios.addAll(ncRatios);

					double gini_all = calc_gini(allRatios);
					double gini_c = calc_gini(cRatios);
					double gini_nc = calc_gini(ncRatios);

					// ensure environment row is inserted
					insertEnvironment.setLong(1, simId);
					insertEnvironment.setLong(3, simId);
					insertEnvironment.setInt(2, t);
					insertEnvironment.setInt(4, t);
					insertEnvironment.addBatch();

					// insert gini values
					updateEnvironment.setString(1, "c" + cluster + "_gini_all");
					updateEnvironment.setString(2, Double.toString(gini_all));
					updateEnvironment.setLong(3, simId);
					updateEnvironment.setInt(4, t);
					updateEnvironment.addBatch();
					updateEnvironment.setString(1, "c" + cluster + "_gini_c");
					updateEnvironment.setString(2, Double.toString(gini_c));
					updateEnvironment.setLong(3, simId);
					updateEnvironment.setInt(4, t);
					updateEnvironment.addBatch();
					updateEnvironment.setString(1, "c" + cluster + "_gini_nc");
					updateEnvironment.setString(2, Double.toString(gini_nc));
					updateEnvironment.setLong(3, simId);
					updateEnvironment.setInt(4, t);
					updateEnvironment.addBatch();

					logger.info("c:" + cluster + ", t:" + t + " - g_c = "
							+ gini_c + ", g_nc = " + gini_nc + ", g_all = "
							+ gini_all);

					t++;
				}
				insertEnvironment.executeBatch();
				updateEnvironment.executeBatch();
			}
		} catch (SQLException e) {
			throw new RuntimeException(e);
		} finally {
			stopDatabase();
		}

	}

	private static double calc_gini(List<Double> x) {
		Collections.sort(x);
		int n = x.size();
		double b = 0;
		double sum = 0;
		for (int i = 0; i < n; i++) {
			double xi = x.get(i);
			sum += xi;
			b += (xi * (n - i));
		}

		if (sum == 0)
			return 1;

		b = b / (n * sum);
		return 1 + (1. / n) - 2 * b;
	}

	@Command(name = "graph", description = "Export graphs for simulation.")
	public void export_graphs(String[] args) throws Exception {
		if (args.length > 1) {
			args = new String[] { args[1], Boolean.toString(true) };
			LPGGui.main(args);
		}
	}

	@SuppressWarnings("static-access")
	@Command(name = "run_hpc", description = "Run sim in hpc mode (reduced db connections)")
	public void run_connectionless(String[] args) throws Exception {

		int threads = 4;
		int retries = 3;

		Options options = new Options();
		options.addOption(OptionBuilder.withArgName("url").hasArg()
				.withDescription("Database url.").isRequired().create("url"));
		options.addOption(OptionBuilder.withArgName("user").hasArg()
				.withDescription("Database user.").isRequired().create("user"));
		options.addOption(OptionBuilder.withArgName("password").hasArg()
				.withDescription("Database user's password.").isRequired()
				.create("password"));
		options.addOption("r", "retry", true,
				"Number of times to attempt db reconnect.");
		options.addOption("t", "threads", true,
				"Number of threads for the simulator (default " + threads
						+ ").");
		options.addOption("h", "help", false, "Show help");

		CommandLineParser parser = new GnuParser();
		CommandLine cmd;
		try {
			cmd = parser.parse(options, args);
		} catch (ParseException e) {
			System.err.println(e.getMessage());
			new HelpFormatter()
					.printHelp("presage2cli run <ID>", options, true);
			return;
		}
		if (cmd.hasOption("h") || args.length < 2) {
			new HelpFormatter()
					.printHelp("presage2cli run <ID>", options, true);
			return;
		}
		if (cmd.hasOption("t")) {
			try {
				threads = Integer.parseInt(cmd.getOptionValue("t"));
			} catch (NumberFormatException e) {
				System.err.println("Thread no. should be in integer.");
				return;
			}
		}
		if (cmd.hasOption("r")) {
			try {
				retries = Integer.parseInt(cmd.getOptionValue("r"));
			} catch (NumberFormatException e) {
				System.err.println("Retries no. should be in integer.");
				return;
			}
		}

		long simulationID;
		try {
			simulationID = Long.parseLong(args[1]);
		} catch (NumberFormatException e) {
			System.err.println("Simulation ID should be an integer.");
			return;
		}

		Properties jdbcInfo = new Properties();
		jdbcInfo.put("driver", "com.mysql.jdbc.Driver");
		jdbcInfo.put("url", cmd.getOptionValue("url"));
		jdbcInfo.put("user", cmd.getOptionValue("user"));
		jdbcInfo.put("password", cmd.getOptionValue("password"));
		ConnectionlessStorage storage = new ConnectionlessStorage(jdbcInfo,
				retries);
		DatabaseService db = storage;
		db.start();

		RunnableSimulation.runSimulationID(db, storage, simulationID, threads);

		db.stop();
	}
}
