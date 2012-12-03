package uk.ac.imperial.lpgdash.db;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import uk.ac.imperial.presage2.core.cli.run.InsufficientResourcesException;
import uk.ac.imperial.presage2.core.cli.run.SimulationExecutor;

public class HPCExecutor implements SimulationExecutor {

	protected Logger logger;

	final String hpcLoginHost;
	final String hpcLoginUser;
	final String runScript;
	final String qstat;
	final int maxQueue;

	final Pattern jobRegex = Pattern.compile("^([0-9]+).([A-Za-z0-9]+)$");

	Set<Job> submitted = new HashSet<Job>();

	enum Status {
		INVALID, PENDING, QUEUED, RUNNING, COMPLETE
	}

	class Job {
		final String name;
		final int jobId;
		final long simId;
		Status status;

		Job(long simId, String name) {
			this.simId = simId;
			this.name = name;
			Matcher m = jobRegex.matcher(name);
			if (m.matches()) {
				jobId = Integer.parseInt(m.group(1));
				status = Status.PENDING;
			} else {
				jobId = 0;
				status = Status.INVALID;
			}
		}
	}

	public HPCExecutor(String hpcLoginHost, String hpcLoginUser,
			String runScript) {
		this(hpcLoginHost, hpcLoginUser, runScript,
				"/opt/pbs/default/bin/qstat", 100);
	}

	public HPCExecutor(String hpcLoginHost, String hpcLoginUser,
			String runScript, String qstat) {
		this(hpcLoginHost, hpcLoginUser, runScript, qstat, 100);
	}

	public HPCExecutor(String hpcLoginHost, String hpcLoginUser,
			String runScript, String qstat, int maxQueue) {
		super();
		this.hpcLoginHost = hpcLoginHost;
		this.hpcLoginUser = hpcLoginUser;
		this.runScript = runScript;
		this.qstat = qstat;
		this.maxQueue = maxQueue;
		this.logger = Logger.getLogger(this.toString());

		Timer t = new Timer("qstat", true);
		t.schedule(new TimerTask() {

			@Override
			public void run() {
				List<String> args = new LinkedList<String>();
				args.add("ssh");
				args.add(HPCExecutor.this.hpcLoginUser + "@"
						+ HPCExecutor.this.hpcLoginHost);
				args.add(HPCExecutor.this.qstat);

				ProcessBuilder builder = new ProcessBuilder(args);

				Map<String, Status> qstat = new HashMap<String, Status>();

				try {
					Process process = builder.start();

					BufferedReader br = new BufferedReader(
							new InputStreamReader(process.getInputStream()));
					int lineNo = 0;
					String line = br.readLine();
					do {
						// skip header
						if (lineNo > 1) {
							String[] pieces = line.split("\\s+");
							if (pieces.length >= 5) {
								char state = pieces[4].charAt(0);
								Status status;
								switch (state) {
								case 'R':
									status = Status.RUNNING;
									break;
								case 'F':
								case 'E':
									status = Status.COMPLETE;
									break;
								case 'Q':
									status = Status.QUEUED;
									break;
								default:
									status = Status.PENDING;
								}
								qstat.put(pieces[0], status);
							}
						}

						line = br.readLine();
						lineNo++;
					} while (line != null);
					br.close();
				} catch (IOException e) {
					logger.warn("qstat error", e);
				}

				int complete = 0;
				int running = 0;
				int queued = 0;
				int pending = 0;
				for (Job j : submitted) {
					Status original = j.status;
					if (qstat.containsKey(j.name)) {
						j.status = qstat.get(j.name);
					} else {
						j.status = Status.COMPLETE;
					}
					if (j.status == Status.COMPLETE
							&& original != Status.COMPLETE) {
						logger.info("Simulation " + j.simId + " complete ("
								+ j.name + ")");
					}
					switch (j.status) {
					case COMPLETE:
						complete++;
						break;
					case RUNNING:
						running++;
						break;
					case QUEUED:
						queued++;
						break;
					case PENDING:
					default:
						pending++;
					}
				}
				logger.info("Running: " + running + ", Queued: " + queued
						+ ", Complete: " + complete + ", Pending: " + pending);
			}
		}, 5000, 10000);
	}

	@Override
	public void run(long simId) throws InsufficientResourcesException {
		// build program args
		List<String> args = new LinkedList<String>();
		args.add("ssh");
		args.add(hpcLoginUser + "@" + hpcLoginHost);
		args.add("bash");
		args.add(runScript);
		args.add(Long.toString(simId));

		ProcessBuilder builder = new ProcessBuilder(args);
		logger.debug(builder.command());
		builder.redirectErrorStream(true);

		try {
			logger.info("Starting simulation ID: " + simId + "");
			logger.debug("Process args: " + builder.command());
			Process process = builder.start();
			BufferedReader br = new BufferedReader(new InputStreamReader(
					process.getInputStream()));
			String out = br.readLine();
			Job job = new Job(simId, out);
			if (job.status == Status.INVALID)
				throw new RuntimeException("Failed to submit job.");
			submitted.add(job);
		} catch (IOException e) {
			logger.warn("Error submitting job", e);
		}
	}

	@Override
	public int running() {
		int running = 0;
		for (Job j : submitted) {
			if (j.status != Status.COMPLETE) {
				running++;
			}
		}
		return running;
	}

	@Override
	public int maxConcurrent() {
		return maxQueue;
	}

	@Override
	public void enableLogs(boolean saveLogs) {

	}

	@Override
	public boolean enableLogs() {
		return false;
	}

	@Override
	public void setLogsDirectory(String logsDir) {

	}

	@Override
	public String getLogsDirectory() {
		return null;
	}

	@Override
	public String toString() {
		return "HPCExecutor @ " + hpcLoginHost;
	}

}
