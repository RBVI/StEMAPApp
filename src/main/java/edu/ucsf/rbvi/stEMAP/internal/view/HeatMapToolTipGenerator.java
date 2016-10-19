package edu.ucsf.rbvi.stEMAP.internal.view;

import org.jfree.chart.labels.StandardXYZToolTipGenerator;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYZDataset;

import edu.ucsf.rbvi.stEMAP.internal.model.HeatMapData;

class HeatmapToolTipGenerator extends StandardXYZToolTipGenerator {
	final XYZDataset xyzDataset;
	final String[] seriesHeaders;
	final String[] itemHeaders;

	public HeatmapToolTipGenerator(HeatMapData hmData) {
		super();
		this.xyzDataset = hmData.getData();
		this.seriesHeaders = hmData.getColumnHeaders();
		this.itemHeaders = hmData.getRowHeaders();
	}

	@Override
	public String generateToolTip(XYZDataset data, int series, int item) {
		int x = (int)data.getXValue(series, item);
		int y = (int)data.getYValue(series, item);
		double z = data.getZValue(series, item);
		String mutationLabel = seriesHeaders[x];
		String geneLabel = itemHeaders[y];
		return mutationLabel+" --> "+geneLabel+": "+z;
	}

	@Override
	public String generateLabelString(XYDataset data, int series, int item) {
		XYZDataset xyzData = (XYZDataset)data;
		return generateToolTip(xyzData, series, item);
	}

}
