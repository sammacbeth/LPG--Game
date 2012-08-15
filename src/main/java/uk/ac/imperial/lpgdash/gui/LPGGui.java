package uk.ac.imperial.lpgdash.gui;

import java.awt.Color;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.Panel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math.stat.descriptive.SummaryStatistics;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.general.DefaultPieDataset;
import org.jfree.data.xy.DefaultXYDataset;

import uk.ac.imperial.lpgdash.allocators.canons.Canon;
import uk.ac.imperial.presage2.core.db.DatabaseModule;
import uk.ac.imperial.presage2.core.db.DatabaseService;
import uk.ac.imperial.presage2.core.db.StorageService;
import uk.ac.imperial.presage2.core.db.persistent.PersistentAgent;
import uk.ac.imperial.presage2.core.db.persistent.PersistentEnvironment;
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

	public static void main(String[] args) throws Exception {
		DatabaseModule module = DatabaseModule.load();
		if (module != null) {
			Injector injector = Guice.createInjector(module);
			LPGGui gui = injector.getInstance(LPGGui.class);
			gui.init();
		}
	}

	@Inject
	public LPGGui(DatabaseService db, StorageService sto) {
		super();
		this.db = db;
		this.sto = sto;
	}

	public void init() {
		try {
			db.start();
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}

		sim = sto.getSimulationById(11);
		final DefaultXYDataset weightsData = new DefaultXYDataset();
		addWeightsSeries(weightsData, t);
		final JFreeChart weightsChart = ChartFactory.createXYLineChart(
				"Function weights", "", "timestep", weightsData,
				PlotOrientation.HORIZONTAL, true, false, false);
		weightsChart.getXYPlot().setBackgroundPaint(Color.WHITE);
		ValueAxis ax = weightsChart.getXYPlot().getRangeAxis();
		ax.setAutoRange(true);

		final DefaultXYDataset remData = new DefaultXYDataset();
		addRemData(remData, t);
		final JFreeChart remChart = ChartFactory.createXYLineChart(
				"Remaining agents", "", "timestep", remData,
				PlotOrientation.HORIZONTAL, true, false, false);
		remChart.getXYPlot().setBackgroundPaint(Color.WHITE);
		remChart.getXYPlot().getDomainAxis().setRange(0, 30);

		final DefaultXYDataset utData = new DefaultXYDataset();
		addUtilityData(utData, t);
		final JFreeChart utChart = ChartFactory.createXYLineChart(
				"Utility Distribution", "", "timestep", utData,
				PlotOrientation.HORIZONTAL, true, false, false);
		utChart.getXYPlot().setBackgroundPaint(Color.WHITE);

		final DefaultXYDataset satisfactionData = new DefaultXYDataset();
		addSatisfactionData(satisfactionData, t);
		final JFreeChart satisfactionChart = ChartFactory.createXYLineChart(
				"Agent satisfaction", "", "timestep", satisfactionData,
				PlotOrientation.HORIZONTAL, true, false, false);
		satisfactionChart.getXYPlot().setBackgroundPaint(Color.WHITE);
		satisfactionChart.getXYPlot().getDomainAxis().setRange(0, 1);

		final DefaultXYDataset utDist = new DefaultXYDataset();
		addUtilityDistributionData(utDist, t);
		final JFreeChart distributionChart = ChartFactory.createScatterPlot(
				"Utility Distribution", "Ut.", "Compliant rounds", utDist,
				PlotOrientation.HORIZONTAL, true, false, false);
		distributionChart.getXYPlot().setBackgroundPaint(Color.WHITE);
		distributionChart.getXYPlot().getDomainAxis().setRange(-0.5, 1.0);
		distributionChart.getXYPlot().getRangeAxis()
				.setRange(0, windowSize + 1);

		ChartPanel weights = new ChartPanel(weightsChart);
		ChartPanel rem = new ChartPanel(remChart);
		ChartPanel ut = new ChartPanel(distributionChart);
		ChartPanel satisfaction = new ChartPanel(satisfactionChart);

		Frame f = new Frame("LPGGAME");
		Panel p = new Panel(new GridLayout(2, 2));
		f.add(p);
		p.add(weights);
		p.add(ut);
		p.add(rem);
		p.add(satisfaction);
		f.pack();
		f.setVisible(true);

		final Timer updater = new Timer("Update weights");
		updater.schedule(new TimerTask() {

			@Override
			public void run() {
				if (t > sim.getFinishTime())
					updater.cancel();

				else {
					addWeightsSeries(weightsData, ++t);
					ValueAxis ax = weightsChart.getXYPlot().getRangeAxis();
					ax.setRange(Math.max(1.0, t - windowSize + 1), t);

					addRemData(remData, t);
					remChart.getXYPlot().getRangeAxis()
							.setRange(Math.max(1.0, t - windowSize + 1), t);

					addUtilityData(utData, t);
					utChart.getXYPlot().getRangeAxis()
							.setRange(Math.max(1.0, t - windowSize + 1), t);

					addSatisfactionData(satisfactionData, t);
					satisfactionChart.getXYPlot().getRangeAxis()
							.setRange(Math.max(1.0, t - windowSize + 1), t);

					addUtilityDistributionData(utDist, t);
				}
			}
		}, 100, 500);

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

	private void addSatisfactionData(DefaultXYDataset satisfactionData,
			int finish) {
		finish = Math.min(t, sim.getFinishTime() / 2);
		int start = Math.max(finish - windowSize, 0);
		int length = Math.min(windowSize, finish - start);
		double[][] c = new double[2][length];
		double[][] nc = new double[2][length];
		Arrays.fill(c[0], 0);
		Arrays.fill(nc[0], 0);
		for (int i = 0; i < length; i++) {
			int t = start + i + 1;
			c[1][i] = t;
			nc[1][i] = t;
			SummaryStatistics satC = new SummaryStatistics();
			SummaryStatistics satNC = new SummaryStatistics();
			for (PersistentAgent a : sim.getAgents()) {
				boolean compliant = a.getName().startsWith("c");
				TransientAgentState s = a.getState(t);
				if (s != null && s.getProperty("o") != null) {
					double o = Double.parseDouble(s.getProperty("o"));
					if (compliant)
						satC.addValue(o);
					else
						satNC.addValue(o);
				}
			}
			c[0][i] = satC.getMean();
			nc[0][i] = satNC.getMean();
		}
		satisfactionData.addSeries("Compliant", c);
		satisfactionData.addSeries("Non compliant", nc);
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

	DescriptiveStatistics utilities[] = null;
	DescriptiveStatistics utMovingAvg[] = null;

	private void addUtilityData(DefaultXYDataset utData, int finish) {
		finish = Math.min(t, sim.getFinishTime() / 2);
		int start = Math.max(finish - windowSize, 0);
		int length = Math.min(windowSize, finish - start);

		if (utilities == null) {
			utilities = new DescriptiveStatistics[2];
			utilities[0] = new DescriptiveStatistics(windowSize);
			utilities[1] = new DescriptiveStatistics(windowSize);
			utMovingAvg = new DescriptiveStatistics[2];
			utMovingAvg[0] = new DescriptiveStatistics(windowSize / 2);
			utMovingAvg[1] = new DescriptiveStatistics(windowSize / 2);
			for (int i = 0; i < length; i++) {
				int t = start + i + 1;
				double c = 0.0;
				double nc = 0.0;
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
				utMovingAvg[0].addValue(c);
				utMovingAvg[1].addValue(nc);

				utilities[0].addValue(utMovingAvg[0].getMean());
				utilities[1].addValue(utMovingAvg[1].getMean());
			}
		} else {
			double c = 0.0;
			double nc = 0.0;
			for (PersistentAgent a : sim.getAgents()) {
				boolean compliant = a.getName().startsWith("c");
				TransientAgentState s = a.getState(finish);
				if (s != null && s.getProperty("U") != null) {
					double u = Double.parseDouble(s.getProperty("U"));
					if (compliant)
						c += u;
					else
						nc += u;
				}
			}
			utMovingAvg[0].addValue(c);
			utMovingAvg[1].addValue(nc);

			utilities[0].addValue(utMovingAvg[0].getMean());
			utilities[1].addValue(utMovingAvg[1].getMean());
		}

		double[][] c = new double[2][length];
		double[][] nc = new double[2][length];
		for (int i = 0; i < length; i++) {
			int t = start + i + 1;
			c[1][i] = t;
			c[0][i] = utilities[0].getElement(i);
			nc[1][i] = t;
			nc[0][i] = utilities[1].getElement(i);
		}

		utData.addSeries("Compliant", c);
		utData.addSeries("Non compliant", nc);
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

	private void addRemData(DefaultXYDataset remData, int finish) {
		finish = Math.min(t, sim.getFinishTime() / 2);
		int start = Math.max(finish - windowSize, 0);
		int length = Math.min(windowSize, finish - start);
		double[][] c = new double[2][length];
		double[][] nc = new double[2][length];
		Arrays.fill(c[0], 0);
		Arrays.fill(nc[0], 0);
		for (int i = 0; i < length; i++) {
			int t = start + i + 1;
			c[1][i] = t;
			nc[1][i] = t;
			for (PersistentAgent a : sim.getAgents()) {
				boolean compliant = a.getName().startsWith("c");
				TransientAgentState s = a.getState(t);
				if (s != null && s.getProperty("U") != null) {
					if (compliant)
						c[0][i]++;
					else
						nc[0][i]++;
				}
			}
		}
		remData.addSeries("Compliant", c);
		remData.addSeries("Non compliant", nc);
	}

	private void addWeightsSeries(DefaultXYDataset data, int t) {
		data.addSeries("f1a", getSeries(sim, Canon.F1a, t));
		data.addSeries("f1b", getSeries(sim, Canon.F1b, t));
		data.addSeries("f1c", getSeries(sim, Canon.F1c, t));
		data.addSeries("f2", getSeries(sim, Canon.F2, t));
		data.addSeries("f3", getSeries(sim, Canon.F3, t));
		data.addSeries("f4", getSeries(sim, Canon.F4, t));
		data.addSeries("f5", getSeries(sim, Canon.F5, t));
		data.addSeries("f6", getSeries(sim, Canon.F6, t));
	}

	private double[][] getSeries(PersistentSimulation sim, Canon c, int finish) {
		finish = Math.min(finish, sim.getFinishTime() / 2);
		int start = Math.max(finish - windowSize, 0);
		int length = Math.min(windowSize, finish - start);
		double[][] series = new double[2][length];
		PersistentEnvironment env = sim.getEnvironment();
		for (int i = 0; i < length; i++) {
			int t = start + i + 1;
			String wStr = env.getProperty("w_" + c.name(), t);
			double w = 0.125;
			if (wStr != null)
				w = Double.parseDouble(wStr);
			series[1][i] = t;
			series[0][i] = w;
		}
		return series;
	}
}
