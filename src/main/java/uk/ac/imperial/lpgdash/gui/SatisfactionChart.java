package uk.ac.imperial.lpgdash.gui;

import java.awt.Color;
import java.util.Arrays;

import org.apache.commons.math.stat.descriptive.SummaryStatistics;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.DefaultXYDataset;

import uk.ac.imperial.presage2.core.db.persistent.PersistentAgent;
import uk.ac.imperial.presage2.core.db.persistent.PersistentSimulation;
import uk.ac.imperial.presage2.core.db.persistent.TransientAgentState;

public class SatisfactionChart implements TimeSeriesChart {

	final PersistentSimulation sim;
	final int windowSize;

	final DefaultXYDataset data;
	final JFreeChart chart;
	final ChartPanel panel;

	SatisfactionChart(PersistentSimulation sim, int windowSize) {
		super();
		this.sim = sim;
		this.windowSize = windowSize;

		data = new DefaultXYDataset();
		chart = ChartFactory.createXYLineChart("Agent satisfaction", "",
				"timestep", data, PlotOrientation.HORIZONTAL, true, false,
				false);
		panel = new ChartPanel(chart);

		chart.getXYPlot().setBackgroundPaint(Color.WHITE);
		chart.getXYPlot().getDomainAxis().setRange(0, 1);
		chart.getXYPlot().setWeight(2);
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
		data.addSeries("Compliant", c);
		data.addSeries("Non compliant", nc);

		chart.getXYPlot().getRangeAxis()
				.setRange(Math.max(1.0, finish - windowSize + 1), finish);
	}

}
