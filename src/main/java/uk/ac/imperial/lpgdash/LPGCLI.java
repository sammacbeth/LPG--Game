package uk.ac.imperial.lpgdash;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.log4j.Logger;

import uk.ac.imperial.lpgdash.facts.Allocation;
import uk.ac.imperial.presage2.core.cli.Presage2CLI;
import uk.ac.imperial.presage2.core.db.persistent.PersistentSimulation;

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
							cluster.name() + "_" + pop,
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

	void multi_cluster(int repeats, int seed) {
		int rounds = 3000;
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
			conn.createStatement()
					.execute(
							"CREATE OR REPLACE VIEW allocationRatios AS "
									+ "SELECT t.\"simId\", "
									+ "a.name, "
									+ "t.\"time\", "
									+ "t.state->'cluster' AS \"cluster\", "
									+ "LEAST(CAST(t.state->'r' AS float) / CAST( t.state->'d' AS float) ,1) AS \"ratio\" "
									+ "FROM agenttransient AS t "
									+ "JOIN agents AS a ON a.\"simId\" = t.\"simId\" AND a.aid = t.aid "
									+ "WHERE CAST(t.state->'d' AS float) > 0");

			logger.info("CREATE TABLE simulationSummary");
			conn.createStatement()
					.execute(
							"CREATE TABLE IF NOT EXISTS simulationsummary"
									+ "(\"simId\" bigint NOT NULL REFERENCES simulations,"
									+ "name varchar(255) NOT NULL,"
									+ "cluster int NOT NULL,"
									+ "\"ut. C\" float,"
									+ "\"stddev ut. C\" float,"
									+ "\"ut. NC\" float,"
									+ "\"stddev ut. NC\" float,"
									+ "\"total ut.\" float,"
									+ "\"rem. C\" int," + "\"rem. NC\" int,"
									+ "PRIMARY KEY (\"simId\", cluster))");

			logger.info("CREATE VIEW aggregatedSimulations");
			conn.createStatement().execute(
					"CREATE OR REPLACE VIEW aggregatedSimulations AS "
							+ "SELECT \"name\" AS strategy," + "cluster,"
							+ "AVG(\"ut. C\") AS \"ut. C\","
							+ "STDDEV(\"ut. C\") AS \"stddev ut. C\","
							+ "AVG(\"ut. NC\") AS \"ut. NC\","
							+ "STDDEV(\"ut. NC\") AS \"stddev ut. NC\","
							+ "AVG(\"rem. C\") AS \"rem. C\","
							+ "AVG(\"rem. NC\") AS \"rem. NC\","
							+ "COUNT(\"simId\") AS repeats "
							+ "FROM simulationsummary "
							+ "GROUP BY \"name\", cluster");

			logger.info("CREATE TABLE aggregatePlayerScore");
			conn.createStatement()
					.execute(
							"CREATE TABLE IF NOT EXISTS aggregatePlayerScore ("
									+ "\"simId\" bigint NOT NULL REFERENCES simulations,"
									+ "player varchar(10) NOT NULL,"
									+ "cluster int NOT NULL,"
									+ "USum float NOT NULL,"
									+ "PRIMARY KEY (\"simId\", player, cluster) )");
			logger.info("Processing simulations...");

			// prepare statements
			PreparedStatement clusterStats = conn
					.prepareStatement("SELECT cluster,"
							+ "AVG(CASE WHEN player LIKE 'c%' THEN USum ELSE NULL END),"
							+ "STDDEV(CASE WHEN player LIKE 'c%' THEN USum ELSE NULL END),"
							+ "AVG(CASE WHEN player LIKE 'nc%' THEN USum ELSE NULL END),"
							+ "STDDEV(CASE WHEN player LIKE 'nc%' THEN USum ELSE NULL END),"
							+ "SUM(USum) " + "FROM aggregatePlayerScore "
							+ "WHERE \"simId\" = ? " + "GROUP BY cluster");
			PreparedStatement remaining = conn
					.prepareStatement("SELECT COUNT(*) FROM agentTransient AS t JOIN agents AS a ON t.aid = a.aid AND t.\"simId\" = a.\"simId\" WHERE a.\"simId\" = ? AND a.\"name\" LIKE ? AND t.\"time\" = ?");
			PreparedStatement insertSummary = conn
					.prepareStatement("INSERT INTO simulationSummary VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");

			// get subset to process
			ResultSet unprocessed = conn
					.createStatement()
					.executeQuery(
							"SELECT s.id, s.\"name\", \"finishTime\" "
									+ "FROM simulations AS s "
									+ "LEFT JOIN simulationsummary AS ss ON ss.\"simId\" = s.id "
									+ "WHERE s.state LIKE 'COMPLETE' AND ss.\"simId\" IS NULL "
									+ "ORDER BY s.id ASC");
			while (unprocessed.next()) {
				long id = unprocessed.getLong(1);
				String name = unprocessed.getString(2);
				int finishTime = unprocessed.getInt(3);
				int cutoff = (int) (Math.floor(finishTime / 2)) - 1;

				logger.info(id + ": " + name);

				// START TRANSACTION
				conn.setAutoCommit(false);

				// generate player scores per cluster
				// note: this should be a PreparedStatement but JDBC sucks and
				// can't deal with ? as an operator
				conn.createStatement()
						.execute(
								"INSERT INTO aggregatePlayerScore "
										+ "SELECT a.\"simId\", "
										+ "a.\"name\", "
										+ "CAST(t.state->'cluster' AS int) AS cluster, "
										+ "SUM(CAST(t.state->'U' AS float)) AS usum "
										+ "FROM agents AS a "
										+ "LEFT JOIN  agenttransient AS t "
										+ "ON a.\"simId\" = t.\"simId\" AND a.aid = t.aid AND t.state ?& ARRAY['cluster','U'] "
										+ "LEFT JOIN aggregatePlayerScore AS p ON a.\"simId\" = p.\"simId\" AND a.\"name\" LIKE p.player AND p.cluster = CAST(t.state->'cluster' AS int) "
										+ "WHERE a.\"simId\" = "
										+ id
										+ " AND p.cluster IS NULL "
										+ "GROUP BY a.\"simId\", a.aid, t.state->'cluster'");

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
}
