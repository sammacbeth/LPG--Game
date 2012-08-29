package uk.ac.imperial.lpgdash.gui;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;

interface TimeSeriesChart {

	ChartPanel getPanel();

	JFreeChart getChart();

	void redraw(int t);

}
