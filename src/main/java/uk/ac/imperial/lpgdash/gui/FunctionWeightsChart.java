package uk.ac.imperial.lpgdash.gui;

import java.awt.Color;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.DefaultXYDataset;

import uk.ac.imperial.lpgdash.allocators.canons.Canon;
import uk.ac.imperial.presage2.core.db.persistent.PersistentEnvironment;
import uk.ac.imperial.presage2.core.db.persistent.PersistentSimulation;

class FunctionWeightsChart implements TimeSeriesChart {

	final PersistentSimulation sim;
	final int windowSize;
	
	final DefaultXYDataset data;
	final JFreeChart chart;
	final ChartPanel panel;

	FunctionWeightsChart(PersistentSimulation sim, int windowSize) {
		super();
		this.sim = sim;
		this.windowSize = windowSize;
		
		data = new DefaultXYDataset();
		chart = ChartFactory.createXYLineChart("Function weights", "",
				"timestep", data, PlotOrientation.HORIZONTAL, true, false,
				false);
		panel = new ChartPanel(chart);

		chart.getXYPlot().setBackgroundPaint(Color.WHITE);
		chart.getXYPlot().setWeight(2);
		chart.getXYPlot().getRangeAxis().setAutoRange(true);
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
	public void redraw(final int t) {
		data.addSeries("f1a", getSeries(Canon.F1a, t));
		data.addSeries("f1b", getSeries(Canon.F1b, t));
		data.addSeries("f1c", getSeries(Canon.F1c, t));
		data.addSeries("f2", getSeries(Canon.F2, t));
		data.addSeries("f3", getSeries(Canon.F3, t));
		data.addSeries("f4", getSeries(Canon.F4, t));
		data.addSeries("f5", getSeries(Canon.F5, t));
		data.addSeries("f6", getSeries(Canon.F6, t));
		
		ValueAxis ax = chart.getXYPlot().getRangeAxis();
		ax.setRange(Math.max(1.0, t - windowSize + 1), t);
	}
	
	private double[][] getSeries(Canon c, int finish) {
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
