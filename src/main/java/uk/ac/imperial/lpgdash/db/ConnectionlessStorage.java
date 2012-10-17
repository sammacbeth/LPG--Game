package uk.ac.imperial.lpgdash.db;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import uk.ac.imperial.presage2.core.util.random.Random;
import uk.ac.imperial.presage2.db.sql.Agent;
import uk.ac.imperial.presage2.db.sql.Environment;

import com.google.inject.Inject;
import com.google.inject.name.Named;

public class ConnectionlessStorage extends LPGDashStorage {

	private enum OutputTable {
		playerScore, roundGlobals
	};

	Map<OutputTable, FileWriter> outputFiles = new HashMap<OutputTable, FileWriter>();
	int retries;
	boolean connected = false;

	@Inject
	public ConnectionlessStorage() {
		super(new Properties());
	}

	public ConnectionlessStorage(@Named("sql.info") Properties jdbcInfo,
			int retries) {
		super(jdbcInfo);
		this.retries = retries;
	}

	@Override
	protected void initTables() {
	}

	@Override
	public synchronized void incrementTime() {
		if (connected && !shutdown) {
			logger.info("Drop connection");
			super.stop();
			connected = false;
			shutdown = false;
		}
		super.incrementTime();
	}

	@Override
	public void start() throws Exception {
		shutdown = false;
		for (int i = 0; i < retries; i++) {
			try {
				super.start();
				break;
			} catch (Exception e) {
				if (i < retries - 1) {
					logger.warn("Database connection failed, retrying...");
					Thread.sleep(1000 + Random.randomInt(10000));
				} else
					throw e;
			}
		}
		connected = true;
	}

	@Override
	public synchronized void stop() {
		if (!shutdown) {
			shutdown = true;
			logger.info("Reconnect");
			if (!connected) {
				try {
					start();
				} catch (Exception e) {
					logger.warn(e);
				}
			}
			super.stop();
			connected = false;
			for (FileWriter fw : outputFiles.values()) {
				try {
					fw.flush();
					fw.close();
				} catch (IOException e) {
					e.printStackTrace();
				}

			}
			outputFiles.clear();
		}
	}

	@Override
	protected void updateSimulations() {
		if (connected)
			super.updateSimulations();
	}

	@Override
	protected synchronized void updateTransientEnvironment() {
		try {
			Writer out = getOutputFile(OutputTable.roundGlobals);
			Set<Environment> notfullyProcessed = new HashSet<Environment>();
			for (Environment e : environmentTransientQ) {
				List<Integer> forRemoval = new LinkedList<Integer>();
				for (Map.Entry<Integer, Map<String, String>> round : e.transientProperties
						.entrySet()) {
					if (!shutdown && game != null
							&& round.getKey() >= game.getRoundNumber() - 1) {
						notfullyProcessed.add(e);
						continue;
					}

					Map<String, String> props = round.getValue();

					out.append(Long.toString(e.simId));
					out.append('\t');
					out.append(Integer.toString(round.getKey() - 1));
					out.append('\t');
					out.append(Double.toString(getProperty(props,
							"c0-fairness", 0)));

					String[] canons = { "w_F1a", "w_F1b", "w_F1c", "w_F2",
							"w_F3", "w_F4", "w_F5", "w_F6" };
					for (int i = 0; i < canons.length; i++) {
						out.append('\t');
						if (props.containsKey(canons[i]))
							out.append(props.get(canons[i]));
						else
							out.append("0");
					}
					out.append('\n');

					forRemoval.add(round.getKey());
				}
				for (Integer round : forRemoval) {
					e.transientProperties.remove(round);
				}
			}
			environmentTransientQ.clear();
			environmentTransientQ.addAll(notfullyProcessed);
		} catch (IOException e) {
			logger.warn(e);
			throw new RuntimeException(e);
		}
	}

	@Override
	protected synchronized void updateTransientAgents() {
		try {
			Writer out = getOutputFile(OutputTable.playerScore);
			Set<Agent> notfullyProcessed = new HashSet<Agent>();
			for (Agent a : agentTransientQ) {
				List<Integer> forRemoval = new LinkedList<Integer>();
				for (Map.Entry<Integer, Map<String, String>> round : a.transientProperties
						.entrySet()) {
					if (!shutdown && game != null
							&& round.getKey() >= game.getRoundNumber() - 2) {
						notfullyProcessed.add(a);
						continue;
					}

					Map<String, String> props = round.getValue();

					if (!props.containsKey("g"))
						continue;

					// csv export
					out.append(Long.toString(a.simId));
					out.append('\t');
					out.append(a.getName());
					out.append('\t');
					out.append(Integer.toString(round.getKey() - 1));
					out.append('\t');
					out.append(Double.toString(getProperty(props, "g", 0.0)));
					out.append('\t');
					out.append(Double.toString(getProperty(props, "q", 0.0)));
					out.append('\t');
					out.append(Double.toString(getProperty(props, "d", 0.0)));
					out.append('\t');
					out.append(Double.toString(getProperty(props, "p", 0.0)));
					out.append('\t');
					out.append(Double.toString(getProperty(props, "r", 0.0)));
					out.append('\t');
					out.append(Double.toString(getProperty(props, "r'", 0.0)));
					out.append('\t');
					out.append(Double
							.toString(getProperty(props, "RTotal", 0.0)));
					out.append('\t');
					out.append(Double.toString(getProperty(props, "o", 0.0)));
					out.append('\t');
					out.append(Double.toString(getProperty(props, "U", 0.0)));
					out.append('\t');
					if (props.containsKey("cluster"))
						out.append(props.get("cluster"));
					else
						out.append('0');
					out.append('\n');

					forRemoval.add(round.getKey());
				}
				for (Integer round : forRemoval) {
					a.transientProperties.remove(round);
				}
			}
			agentTransientQ.clear();
			agentTransientQ.addAll(notfullyProcessed);
		} catch (IOException e) {
			logger.warn(e);
			throw new RuntimeException(e);
		}
	}

	private Writer getOutputFile(OutputTable table) throws IOException {
		FileWriter fw = outputFiles.get(table);
		if (fw == null) {
			fw = new FileWriter("" + table.name() + ".csv");
			outputFiles.put(table, fw);
		}
		return fw;
	}

}
