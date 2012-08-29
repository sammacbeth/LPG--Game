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
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.data.category.DefaultCategoryDataset;

import uk.ac.imperial.presage2.core.db.persistent.PersistentAgent;
import uk.ac.imperial.presage2.core.db.persistent.PersistentSimulation;
import uk.ac.imperial.presage2.core.db.persistent.TransientAgentState;

public class UtilityChart implements TimeSeriesChart {

	final PersistentSimulation sim;
	final int windowSize;

	final DefaultCategoryDataset data;
	final JFreeChart chart;
	final ChartPanel panel;
	Map<String, DescriptiveStatistics> agentUtilities = new HashMap<String, DescriptiveStatistics>();

	UtilityChart(PersistentSimulation sim, int windowSize) {
		super();
		this.sim = sim;
		this.windowSize = windowSize;

		data = new DefaultCategoryDataset();
		chart = ChartFactory.createBarChart(
				"Average Provision over last 50 rounds", "", "", data,
				PlotOrientation.VERTICAL, false, false, false);
		panel = new ChartPanel(chart);

		chart.getCategoryPlot().setBackgroundPaint(Color.WHITE);
		chart.getCategoryPlot().getRangeAxis().setRange(0, 1);
		BarRenderer renderer = (BarRenderer) chart.getCategoryPlot()
				.getRenderer();
		renderer.setItemMargin(-.5);
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
	public void redraw(int finish) {
		finish = Math.min(finish, sim.getFinishTime() / 2);
		int start = Math.max(finish - windowSize, 0);
		int length = Math.min(windowSize, finish - start);

		if (agentUtilities.size() == 0) {
			for (PersistentAgent a : sim.getAgents()) {
				DescriptiveStatistics ut = new DescriptiveStatistics(windowSize);
				agentUtilities.put(a.getName(), ut);
				for (int i = 0; i < length; i++) {
					int t = start + i + 1;
					TransientAgentState s = a.getState(t);
					if (s != null && s.getProperty("r") != null) {
						double u = Double.parseDouble(s.getProperty("r"));
						ut.addValue(u);
					}
				}
			}
		} else {
			for (PersistentAgent a : sim.getAgents()) {
				if (!agentUtilities.containsKey(a.getName())) {
					agentUtilities.put(a.getName(), new DescriptiveStatistics(
							windowSize));
				}
				TransientAgentState s = a.getState(finish);
				if (s != null && s.getProperty("r") != null) {
					double u = Double.parseDouble(s.getProperty("r"));
					agentUtilities.get(a.getName()).addValue(u);
				} else {
					agentUtilities.put(a.getName(), new DescriptiveStatistics(
							new double[] { 0 }));

				}
			}
		}

		List<String> agents = new ArrayList<String>(agentUtilities.keySet());
		Collections.sort(agents);
		for (String key : agents) {
			DescriptiveStatistics e = agentUtilities.get(key);
			data.addValue(e.getMean(), key.substring(0, 1), key);
		}
	}

}
