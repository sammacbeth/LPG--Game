package uk.ac.imperial.lpgdash.gui;

import java.awt.Color;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.Panel;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.math.stat.descriptive.SummaryStatistics;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.general.DefaultPieDataset;
import org.jfree.data.xy.DefaultXYDataset;

import uk.ac.imperial.presage2.core.db.DatabaseModule;
import uk.ac.imperial.presage2.core.db.DatabaseService;
import uk.ac.imperial.presage2.core.db.StorageService;
import uk.ac.imperial.presage2.core.db.persistent.PersistentAgent;
import uk.ac.imperial.presage2.core.db.persistent.PersistentSimulation;
import uk.ac.imperial.presage2.core.db.persistent.TransientAgentState;

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;

public class LPGGui {

	final DatabaseService db;
	final StorageService sto;

	PersistentSimulation sim;
	int t = 5;
	int windowSize = 50;
	int t0 = -1;

	boolean exportMode = false;

	final static String imagePath = "/home/sm1106/Videos/Saso2012/";

	public static void main(String[] args) throws Exception {
		DatabaseModule module = DatabaseModule.load();
		if (module != null) {
			Injector injector = Guice.createInjector(module);
			LPGGui gui = injector.getInstance(LPGGui.class);
			if (args.length > 1 && Boolean.parseBoolean(args[1]) == true)
				gui.exportMode = true;
			gui.init(Integer.parseInt(args[0]));
		}
	}

	@Inject
	public LPGGui(DatabaseService db, StorageService sto) {
		super();
		this.db = db;
		this.sto = sto;
	}

	void takeScreenshot(JFreeChart chart, String base, int i) {
		if (t0 == -1) {
			t0 = i;
		}
		try {
			ChartUtilities
					.saveChartAsPNG(
							new File(imagePath + base + ""
									+ String.format("%04d", i - t0) + ".png"),
							chart, 1280, 720);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void init(long simId) {
		try {
			db.start();
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}

		sim = sto.getSimulationById(simId);
		if (exportMode) {
			File exportDir = new File(imagePath + sim.getName());
			if (!exportDir.exists())
				exportDir.mkdir();
			else if (!exportDir.isDirectory())
				System.exit(60);
		}

		final DefaultXYDataset utDist = new DefaultXYDataset();
		addUtilityDistributionData(utDist, t);
		final JFreeChart distributionChart = ChartFactory.createScatterPlot(
				"Utility Distribution", "Ut.", "Compliant rounds", utDist,
				PlotOrientation.HORIZONTAL, true, false, false);
		distributionChart.getXYPlot().setBackgroundPaint(Color.WHITE);
		distributionChart.getXYPlot().getDomainAxis().setRange(-0.5, 1.0);
		distributionChart.getXYPlot().getRangeAxis()
				.setRange(0, windowSize + 1);

		List<TimeSeriesChart> charts = new ArrayList<TimeSeriesChart>();
		charts.add(new FunctionWeightsChart(sim, windowSize));
		charts.add(new RemainingPlayersChart(sim, windowSize, 0));
		charts.add(new UtilityChart(sim, windowSize));
		charts.add(new SatisfactionChart(sim, windowSize));
		if (exportMode) {
			charts.add(new GiniChart(sim, windowSize));
			charts.add(new LorenzChart(sim, windowSize));
		}

		final Frame f = new Frame("LPGGAME");
		final Panel p = new Panel(new GridLayout(2, 2));
		if (!exportMode) {
			f.add(p);
			for (TimeSeriesChart chart : charts) {
				p.add(chart.getPanel());
			}

			f.pack();
			f.setVisible(true);
		}

		while (t < sim.getFinishTime() / 2) {
			t++;

			for (TimeSeriesChart chart : charts) {
				chart.redraw(t);
			}

			if (exportMode) {
				for (TimeSeriesChart chart : charts) {
					takeScreenshot(chart.getChart(), sim.getName() + "/"
							+ chart.getClass().getSimpleName().substring(0, 3),
							t);
				}

			} else {
				try {
					Thread.sleep(400);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}

		db.stop();
	}

	private void addUtilityDistributionData(DefaultXYDataset utDist, int finish) {
		finish = Math.min(t, sim.getFinishTime() / 2);
		int start = Math.max(finish - windowSize, 0);
		int length = Math.min(windowSize, finish - start);

		List<Pair<Integer, Double>> c = new ArrayList<Pair<Integer, Double>>();
		List<Pair<Integer, Double>> nc = new ArrayList<Pair<Integer, Double>>();
		for (PersistentAgent a : sim.getAgents()) {
			int compliantRounds = 0;
			SummaryStatistics utility = new SummaryStatistics();
			boolean compliant = a.getName().startsWith("c");
			for (int i = 0; i < length; i++) {
				int t = start + i + 1;
				TransientAgentState s = a.getState(t);
				if (s != null && s.getProperty("U") != null) {
					try {
						double u = Double.parseDouble(s.getProperty("U"));
						utility.addValue(u);
					} catch (NumberFormatException e) {
						continue;
					}
					try {
						double g = Double.parseDouble(s.getProperty("g"));
						double p = Double.parseDouble(s.getProperty("p"));
						if (Math.abs(g - p) <= 1E-4)
							compliantRounds++;
					} catch (NumberFormatException e) {
						continue;
					}
				}
			}
			if (compliant)
				c.add(Pair.of(compliantRounds, utility.getMean()));
			else
				nc.add(Pair.of(compliantRounds, utility.getMean()));
		}
		double[][] cArr = new double[2][c.size()];
		for (int i = 0; i < c.size(); i++) {
			Pair<Integer, Double> point = c.get(i);
			cArr[1][i] = point.getLeft();
			cArr[0][i] = point.getRight();
		}
		double[][] ncArr = new double[2][nc.size()];
		for (int i = 0; i < nc.size(); i++) {
			Pair<Integer, Double> point = nc.get(i);
			ncArr[1][i] = point.getLeft();
			ncArr[0][i] = point.getRight();
		}
		utDist.addSeries("Compliant", cArr);
		utDist.addSeries("Non compliant", ncArr);
	}

	private void addAllocPieData(DefaultPieDataset allocPieData, int finish) {
		finish = Math.min(t, sim.getFinishTime() / 2);
		int start = Math.max(finish - windowSize, 0);
		int length = Math.min(windowSize, finish - start);

		double c = 0.0;
		double nc = 0.0;

		for (int i = 0; i < length; i++) {
			int t = start + i + 1;
			for (PersistentAgent a : sim.getAgents()) {
				boolean compliant = a.getName().startsWith("c");
				TransientAgentState s = a.getState(t);
				if (s != null && s.getProperty("r") != null) {
					double u = Double.parseDouble(s.getProperty("r"));
					if (compliant)
						c += u;
					else
						nc += u;
				}
			}
		}

		allocPieData.insertValue(0, "Alloc. C", c);
		allocPieData.insertValue(1, "Alloc. NC", nc);
	}

	private void addUtilityPieData(DefaultPieDataset utPieData, int finish) {
		finish = Math.min(t, sim.getFinishTime() / 2);
		int start = Math.max(finish - windowSize, 0);
		int length = Math.min(windowSize, finish - start);

		double c = 0.0;
		double nc = 0.0;

		for (int i = 0; i < length; i++) {
			int t = start + i + 1;
			for (PersistentAgent a : sim.getAgents()) {
				boolean compliant = a.getName().startsWith("c");
				TransientAgentState s = a.getState(t);
				if (s != null && s.getProperty("U") != null) {
					double u = Double.parseDouble(s.getProperty("U"));
					if (compliant)
						c += u;
					else
						nc += u;
				}
			}
		}

		utPieData.insertValue(0, "Ut. C", c);
		utPieData.insertValue(1, "Ut. NC", nc);
	}

}
