package uk.ac.imperial.lpgdash;

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
}
