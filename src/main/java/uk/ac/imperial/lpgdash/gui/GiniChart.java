package uk.ac.imperial.lpgdash.gui;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.DefaultXYDataset;

import uk.ac.imperial.presage2.core.db.persistent.PersistentAgent;
import uk.ac.imperial.presage2.core.db.persistent.PersistentSimulation;
import uk.ac.imperial.presage2.core.db.persistent.TransientAgentState;

public class GiniChart implements TimeSeriesChart {

	final PersistentSimulation sim;
	final int windowSize;

	final DefaultXYDataset data;
	final JFreeChart chart;
	final ChartPanel panel;

	Map<String, DescriptiveStatistics> roundGini = null;
	Map<String, DescriptiveStatistics> frameGini = null;
	Map<String, DescriptiveStatistics> ratios = new HashMap<String, DescriptiveStatistics>();

	GiniChart(PersistentSimulation sim, int windowSize) {
		super();
		this.sim = sim;
		this.windowSize = windowSize;

		data = new DefaultXYDataset();
		chart = ChartFactory.createXYLineChart("Gini Index", "", "timestep",
				data, PlotOrientation.HORIZONTAL, true, false, false);
		panel = new ChartPanel(chart);

		chart.getXYPlot().setBackgroundPaint(Color.WHITE);
		//chart.getXYPlot().getDomainAxis().setRange(0, 1);
	}

	@Override
	public ChartPanel getPanel() {
		return panel;
	}

	@Override
	public JFreeChart getChart() {
		return chart;
	}

	@Override
	public void redraw(int t) {
		String[] keys = new String[] { "c", "nc", "all" };

		if (roundGini == null) {
			// first draw, go from t=0
			roundGini = new HashMap<String, DescriptiveStatistics>();
			frameGini = new HashMap<String, DescriptiveStatistics>();
			for (String k : keys) {
				roundGini.put(k, new DescriptiveStatistics(windowSize));
				frameGini.put(k, new DescriptiveStatistics(windowSize));
			}

			for (int i = 0; i <= t; i++) {
				redraw(i);
			}
		} else {

			// current gini
			List<Double> cRatios = new ArrayList<Double>();
			List<Double> ncRatios = new ArrayList<Double>();
			List<Double> allRatios = new ArrayList<Double>();

			// frame gini
			Map<String, List<Double>> frameRatios = new HashMap<String, List<Double>>();
			for (String k : keys) {
				frameRatios.put(k, new ArrayList<Double>());
			}

			for (PersistentAgent a : sim.getAgents()) {
				final String name = a.getName();
				boolean compliant = name.startsWith("c");
				TransientAgentState s = a.getState(t);
				if (s != null && s.getProperty("r") != null
						&& s.getProperty("d") != null) {
					double r, d, ratio = 0;
					r = Double.parseDouble(s.getProperty("r"));
					d = Double.parseDouble(s.getProperty("d"));
					if (d > 0)
						ratio = r / d;

					if (!ratios.containsKey(name)) {
						ratios.put(name, new DescriptiveStatistics(windowSize*2));
					}
					ratios.get(name).addValue(ratio);

					if (compliant) {
						cRatios.add(ratio);
						frameRatios.get("c").add(ratios.get(name).getSum());
					} else {
						ncRatios.add(ratio);
						frameRatios.get("nc").add(ratios.get(name).getSum());
					}

				}
			}
			allRatios.addAll(cRatios);
			allRatios.addAll(ncRatios);
			frameRatios.get("all").addAll(frameRatios.get("c"));
			frameRatios.get("all").addAll(frameRatios.get("nc"));

			roundGini.get("c").addValue(calc_gini(cRatios));
			roundGini.get("nc").addValue(calc_gini(ncRatios));
			roundGini.get("all").addValue(calc_gini(allRatios));
			for (String k : keys) {
				frameGini.get(k).addValue(calc_gini(frameRatios.get(k)));
			}

			int start = Math.max(t - windowSize, 0);

			/*for (int k = 0; k < keys.length; k++) {
				data.addSeries("round " + keys[k],
						generateSeries(start, roundGini.get(keys[k])));
			}*/
			for (int k = 0; k < keys.length; k++) {
				data.addSeries("frame " + keys[k],
						generateSeries(start, frameGini.get(keys[k])));
			}

			if (t > windowSize) {
				chart.getXYPlot().getRangeAxis()
						.setRange(Math.max(1.0, t - windowSize + 1), t);
			}
		}

	}

	private static double[][] generateSeries(final int start,
			DescriptiveStatistics data) {
		int length = (int) data.getN();
		double[][] series = new double[2][length];
		for (int i = 0; i < length; i++) {
			int t = i + start;
			series[1][i] = t;
			series[0][i] = data.getElement(i);
		}
		return series;
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

}
