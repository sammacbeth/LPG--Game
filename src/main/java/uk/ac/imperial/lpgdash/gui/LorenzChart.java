package uk.ac.imperial.lpgdash.gui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;

import uk.ac.imperial.presage2.core.db.persistent.PersistentAgent;
import uk.ac.imperial.presage2.core.db.persistent.PersistentSimulation;
import uk.ac.imperial.presage2.core.db.persistent.TransientAgentState;

public class LorenzChart implements TimeSeriesChart {

	final PersistentSimulation sim;
	final int windowSize;

	final DefaultCategoryDataset data;
	final JFreeChart chart;
	final ChartPanel panel;

	boolean firstRender = true;
	Map<String, Pair<String, DescriptiveStatistics>> ratios = new HashMap<String, Pair<String, DescriptiveStatistics>>();

	LorenzChart(PersistentSimulation sim, int windowSize) {
		super();
		this.sim = sim;
		this.windowSize = windowSize;

		data = new DefaultCategoryDataset();
		chart = ChartFactory.createBarChart("Fairness Distribution", "", "",
				data, PlotOrientation.VERTICAL, false, false, false);
		panel = new ChartPanel(chart);

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
		if (!firstRender) {
			firstRender = false;
			for (int i = 0; i <= t; i++) {
				redraw(i);
			}
		} else {
			for (PersistentAgent a : sim.getAgents()) {
				final String name = a.getName();
				TransientAgentState s = a.getState(t);
				if (s != null && s.getProperty("r") != null
						&& s.getProperty("d") != null) {
					double r, d, ratio = 0;
					r = Double.parseDouble(s.getProperty("r"));
					d = Double.parseDouble(s.getProperty("d"));
					if (d > 0)
						ratio = r / d;

					if (!ratios.containsKey(name)) {
						ratios.put(name, Pair.of(name,
								new DescriptiveStatistics(windowSize)));
					}
					ratios.get(name).getRight().addValue(ratio);
				}
			}

			List<Pair<String, DescriptiveStatistics>> sortedPairs = new ArrayList<Pair<String, DescriptiveStatistics>>(
					ratios.values());
			Collections.sort(sortedPairs,
					new Comparator<Pair<String, DescriptiveStatistics>>() {

						@Override
						public int compare(
								Pair<String, DescriptiveStatistics> o1,
								Pair<String, DescriptiveStatistics> o2) {
							return Double.compare(o1.getRight().getSum(), o2
									.getRight().getSum());
						}
					});
			double sum = 0;
			data.clear();
			for (Pair<String, DescriptiveStatistics> a : sortedPairs) {
				String name = a.getLeft();
				sum += a.getRight().getSum();
				data.addValue(sum, name.substring(0, 1), name);
			}
		}
	}
}
