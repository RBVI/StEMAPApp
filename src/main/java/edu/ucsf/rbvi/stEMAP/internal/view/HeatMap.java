package edu.ucsf.rbvi.stEMAP.internal.view;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.AxisLocation;
import org.jfree.chart.axis.SymbolAxis;
import org.jfree.chart.labels.StandardXYZToolTipGenerator;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYBlockRenderer;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYZDataset;
import org.jfree.ui.RectangleInsets;

import edu.ucsf.rbvi.stEMAP.internal.model.HeatMapData;
import edu.ucsf.rbvi.stEMAP.internal.model.StEMAPManager;

/**
 * Class used to create heat maps from 3 column-data files (XYZ)
 * @author Julien Thibault
 *
 */
public class HeatMap
{
	private HeatMapData heatMapData;
	private StEMAPManager manager;
	private XYPlot plot;

	/**
	 * Initialize new heatmap
	 * @param manager the StEMAPManager
	 * @param dataset the dataset containing the heatmap data
	 */
	public HeatMap(StEMAPManager manager, HeatMapData dataset){
		this.heatMapData = dataset;
		this.manager = manager;
	}

	/**
	 * Generate heatmap
	 * @param title Main title
	 * @param xTitle X-axis title
	 * @param yTitle Y-axis title
	 * @param zTitle Z-axis title
	 * @return Plot object
	 * @throws Exception
	 */
	public JFreeChart createHeatMap() throws Exception
	{
		final PlotOrientation plotOrientation = PlotOrientation.VERTICAL;

		final SymbolAxis xAxis = new MySymbolAxis("Genes", heatMapData.getColumnHeaders());
		xAxis.setVisible(true);
		xAxis.setAxisLineVisible(false);
		xAxis.setTickMarksVisible(false);
		xAxis.setTickLabelInsets(new RectangleInsets(0.0,0.0,0.0,0.0));
		xAxis.setTickLabelPaint(Color.BLUE);
		xAxis.setTickLabelFont(xAxis.getTickLabelFont().deriveFont(8.0f));
		xAxis.setVerticalTickLabels(true);
		xAxis.setLowerMargin(10.0);
		xAxis.setUpperMargin(0.0);

		final SymbolAxis yAxis = new MySymbolAxis("Mutations", heatMapData.getRowHeaders());
		yAxis.setVisible(true);
		yAxis.setAxisLineVisible(false);
		yAxis.setTickMarksVisible(false);
		yAxis.setTickLabelInsets(new RectangleInsets(0.0,0.0,0.0,0.0));
		yAxis.setTickLabelPaint(Color.BLUE);
		yAxis.setTickLabelFont(yAxis.getTickLabelFont().deriveFont(8.0f));
		yAxis.setVerticalTickLabels(false);
		yAxis.setLowerMargin(10.0);
		yAxis.setUpperMargin(0.0);
		yAxis.setInverted(true);

		final XYBlockRenderer renderer = createRenderer();

		plot = new XYPlot(heatMapData.getData(), xAxis, yAxis, renderer);
		plot.setDomainAxis(xAxis);
		plot.setDomainAxisLocation(AxisLocation.TOP_OR_LEFT);
		plot.setDomainGridlinesVisible(false);

		// plot.setRangeAxis(yAxis);
		plot.setRangeAxisLocation(AxisLocation.TOP_OR_LEFT);
		plot.setRangeGridlinesVisible(false);

		// plot.setBackgroundPaint(TRANSPARENT_COLOR);
		plot.setOutlineVisible(false);
		plot.setInsets(new RectangleInsets(1.0, 1.0, 1.0, 1.0));
		plot.setAxisOffset(new RectangleInsets(0.0, 0.0, 0.0, 0.0));
		plot.setOrientation(plotOrientation);

		final JFreeChart chart = new JFreeChart(null, plot);
		chart.removeLegend();
		chart.setBorderVisible(true);
		// chart.setBackgroundPaint(TRANSPARENT_COLOR);
		chart.setPadding(new RectangleInsets(5.0, 5.0, 5.0, 5.0));
		chart.setAntiAlias(false);
		chart.setTextAntiAlias(true);

		return chart;

	}

	public void updatePlot() {
		XYBlockRenderer renderer = createRenderer();
		plot.setRenderer(renderer);
	}

	XYBlockRenderer createRenderer() {
		final XYBlockRenderer renderer = new XYBlockRenderer();

		// Set up tooltips
		// renderer.setBaseToolTipGenerator(new HeatmapToolTipGenerator(heatMapData));
		renderer.setBaseCreateEntities(true);

		Color[] colors = heatMapData.getColorMap();
		double minValue = heatMapData.getMinimumZ();
		double maxValue = heatMapData.getMaximumZ();
		double scaleFactor = manager.getScale();
		final ColorScale scale = new ColorScale(minValue/scaleFactor, maxValue/scaleFactor, colors[2], colors[1], colors[0], colors[3]);
		renderer.setPaintScale(scale);
		return renderer;
	}

}
