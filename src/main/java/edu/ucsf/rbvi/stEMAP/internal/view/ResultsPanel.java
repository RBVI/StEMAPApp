package edu.ucsf.rbvi.stEMAP.internal.view;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.image.BufferedImage;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.apache.log4j.Logger;
import org.jfree.data.general.HeatMapDataset;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;

import org.cytoscape.application.CyUserLog;
import org.cytoscape.application.swing.CytoPanelComponent2;
import org.cytoscape.application.swing.CytoPanelName;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyTableUtil;

import edu.ucsf.rbvi.stEMAP.internal.model.StEMAPManager;
import edu.ucsf.rbvi.stEMAP.internal.model.HeatMapData;

public class ResultsPanel extends JPanel implements CytoPanelComponent2 {
	StEMAPManager manager;
	final CyNetwork network;
	final Logger logger = Logger.getLogger(CyUserLog.NAME);
	JPanel resultsPanel;
	JLabel imageLabel;

	public ResultsPanel(StEMAPManager manager) {
		this.manager = manager;
		this.network = manager.getMergedNetwork();
		setLayout(new BorderLayout());
		initialize();
	}

	private void initialize() {
		if (network == null)
			return;

		// Get the currently selected nodes
		List<CyNode> selectedNodes = CyTableUtil.getNodesInState(network, CyNetwork.SELECTED, true);
		for (CyNode node: selectedNodes) {
			manager.selectGeneOrMutation(node, Boolean.TRUE);
		}

		HeatMapData data = new HeatMapData(manager, new HashSet<CyNode>(manager.getSelectedGenes()),	
		                                            new HashSet<CyNode>(manager.getSelectedMutations()));
		// Create our initial chart
		HeatMap heatMap = new HeatMap(manager, data);
		JFreeChart chart = null;
		try {
			chart = heatMap.createHeatMap();
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}

		int width = data.getColumnHeaders().length*10+200;
		int height = data.getRowHeaders().length*10+200;
		ChartPanel chartPanel = new ChartPanel(chart, width, height, width, height, width*4, height*4, true, true, true, true, true, true);
		JScrollPane scroller = new JScrollPane(chartPanel);
		add(scroller, BorderLayout.CENTER);
	}

	@Override
	public Component getComponent() {
		return this;
	}

	@Override
	public String getIdentifier() { return "StEMAPResults"; }

	@Override
	public CytoPanelName getCytoPanelName() { return CytoPanelName.EAST; }

	@Override
	public Icon getIcon() { return null; }

	@Override
	public String getTitle() { return "StEMAP HeatMap"; }

}
