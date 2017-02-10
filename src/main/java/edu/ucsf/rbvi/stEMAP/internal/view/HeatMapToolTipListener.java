package edu.ucsf.rbvi.stEMAP.internal.view;

import javax.swing.JComponent;
import javax.swing.ToolTipManager;

import org.jfree.chart.ChartMouseEvent;
import org.jfree.chart.ChartMouseListener;
import org.jfree.chart.entity.XYItemEntity;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYZDataset;

import edu.ucsf.rbvi.stEMAP.internal.model.HeatMapData;

class HeatMapToolTipListener implements ChartMouseListener {
	final XYZDataset xyzDataset;
	final HeatMapData hmData;
	final String[] seriesHeaders;
	final String[] itemHeaders;
	int lastItem = -1;
	int lastSeries = -1;
	JComponent chart;

	public HeatMapToolTipListener(JComponent chart, HeatMapData hmData) {
		super();
		this.hmData = hmData;
		this.xyzDataset = hmData.getData();
		this.seriesHeaders = hmData.getColumnHeaders();
		this.itemHeaders = hmData.getRowHeaders();
		this.chart = chart;
	}

	@Override
	public void chartMouseClicked(ChartMouseEvent event) {}

	@Override
	public void chartMouseMoved(ChartMouseEvent event) {
		if (!(event.getEntity() instanceof XYItemEntity)) {
			chart.setToolTipText(null);
			return;
		}
		XYItemEntity entity = (XYItemEntity)event.getEntity();
		int item = entity.getItem();
		int series = entity.getSeriesIndex();
		if (item == lastItem && series == lastSeries)
			return;

		lastItem = item;
		lastSeries = series;

		int column = item/itemHeaders.length;
		String mutation = seriesHeaders[column];
		int row = item%itemHeaders.length;
		String gene = itemHeaders[row];
		double value = xyzDataset.getZValue(series,item);
		if (Double.isNaN(value)) {
			value = hmData.getWeight(gene, mutation);
			chart.setToolTipText("<html><span style=\"color:blue\">"+
			                      mutation+"</span>&rarr;<span style=\"color:blue\">"+
						                gene+"</span>: <span style=\"color:gray\"><i>"+
														value+"</i></span></html>");
		} else {
			chart.setToolTipText("<html><span style=\"color:blue\">"+
			                      mutation+"</span>&rarr;<span style=\"color:blue\">"+
						                gene+"</span>: <b>"+value+"</b></html>");
		}
		ToolTipManager.sharedInstance().mouseMoved(event.getTrigger());
	}

}
