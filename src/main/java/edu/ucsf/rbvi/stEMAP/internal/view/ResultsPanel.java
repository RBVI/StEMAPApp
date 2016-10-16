package edu.ucsf.rbvi.stEMAP.internal.view;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.MouseEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.image.BufferedImage;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

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

public class ResultsPanel extends JPanel implements CytoPanelComponent2, ItemListener {
	StEMAPManager manager;
	final CyNetwork network;
	final Logger logger = Logger.getLogger(CyUserLog.NAME);
	JPanel resultsPanel;
	JScrollPane scroller;
	JLabel imageLabel;

	public ResultsPanel(StEMAPManager manager) {
		this.manager = manager;
		this.network = manager.getMergedNetwork();
		setLayout(new BorderLayout());
		scroller = initialize();
		if (scroller != null)
			add(scroller, BorderLayout.CENTER);
		// Add Utilities checkboxes
		JPanel buttonBox = createButtonBox();
		createAutoAnnotateCheckbox(buttonBox);
		createIgnoreMultipleCheckbox(buttonBox);
		buttonBox.add(Box.createRigidArea(new Dimension(0,10)));
		add(buttonBox, BorderLayout.SOUTH);
	}

	public void update() {
		Runnable scrollUpdater = new ScrollUpdater(this);
		if (SwingUtilities.isEventDispatchThread()) {
			scrollUpdater.run();
		} else {
			SwingUtilities.invokeLater(scrollUpdater);
		}
	}



	private JScrollPane initialize() {
		if (network == null)
			return null;

		if (manager.getSelectedGenes().size() == 0 && 
		    manager.getSelectedMutations().size() == 0) 
			return null;
	
		// Get the currently selected nodes
		// List<CyNode> selectedNodes = CyTableUtil.getNodesInState(network, CyNetwork.SELECTED, true);
		// for (CyNode node: selectedNodes) {
		// 	manager.selectGeneOrMutation(node, Boolean.TRUE);
		// }
		
		HeatMapData data;
		try {
			data	= new HeatMapData(manager, new HashSet<CyNode>(manager.getSelectedGenes()),	
		                                   new HashSet<CyNode>(manager.getSelectedMutations()));
		} catch (IllegalArgumentException e) {
			JLabel label = new JLabel(e.getMessage());
			JScrollPane scroller = new JScrollPane(label);
			return scroller;
		}
		// Create our initial chart
		HeatMap heatMap = new HeatMap(manager, data);
		JFreeChart chart = null;
		try {
			chart = heatMap.createHeatMap();
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}

		int width = data.getColumnHeaders().length*8+200;
		int height = data.getRowHeaders().length*8+200;
		// System.out.println("Chart size="+width+"x"+height);
		ChartPanel chartPanel = new MyChartPanel(chart, width, height);
		chartPanel.setPreferredSize(new Dimension(width, height));
		chartPanel.setSize(new Dimension(width, height));
		chartPanel.addChartMouseListener(new HeatMapToolTipListener(chartPanel, data));
		JScrollPane scroller = new JScrollPane(chartPanel);
		return scroller;
	}

	private JPanel createButtonBox() {
		JPanel buttonBox = new JPanel();
		buttonBox.setAlignmentX(Component.LEFT_ALIGNMENT);
		buttonBox.setLayout(new BoxLayout(buttonBox, BoxLayout.PAGE_AXIS));
		return buttonBox;
	}

	private void createAutoAnnotateCheckbox(JPanel buttonBox) {
		JCheckBox autoAnnotateCB = new JCheckBox("Auto-annotate structure");
		autoAnnotateCB.setToolTipText("Automatically show genetic interactions of selected genes on structure");
		autoAnnotateCB.addItemListener(this);
		autoAnnotateCB.setActionCommand("autoAnnotate");
		buttonBox.add(Box.createRigidArea(new Dimension(10,0)));
		buttonBox.add(autoAnnotateCB);
	}

	private void createIgnoreMultipleCheckbox(JPanel buttonBox) {
		JCheckBox ignoreMultiplesCB = new JCheckBox("Ignore multiple mutations");
		ignoreMultiplesCB.setToolTipText("Don't show interactions that involved multiple mutations");
		ignoreMultiplesCB.addItemListener(this);
		ignoreMultiplesCB.setActionCommand("ignoreMultiples");
		buttonBox.add(Box.createRigidArea(new Dimension(10,0)));
		buttonBox.add(ignoreMultiplesCB);
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

	@Override
	public void itemStateChanged(ItemEvent e) {
		JCheckBox cb = (JCheckBox)e.getItemSelectable();
		String command = cb.getActionCommand();
		boolean selected = cb.isSelected();
		if (command.equals("autoAnnotate")) {
			manager.setAutoAnnotate(selected);
		} else if (command.equals("ignoreMultiples")) {
			manager.setIgnoreMultiples(selected);
		}
	}

	class MyChartPanel extends ChartPanel {
		String tt = null;
		public MyChartPanel(JFreeChart chart, int width, int height) {
			super(chart, width, height, width, height, width*4, height*4, true, true, true, true, true, true);
		}

		@Override
		public String getToolTipText(MouseEvent e) {
			return tt;
		}

		@Override
		public void setToolTipText(String text) {
			tt = text;
		}
	}

	class ScrollUpdater implements Runnable {
		JPanel panel;

		ScrollUpdater(JPanel p) { panel = p; }

		public void run() {
			JScrollPane newScroller = initialize();
			if (scroller != null) {
				BorderLayout layout = (BorderLayout)panel.getLayout();
				Component c;
				do {
					c = layout.getLayoutComponent(BorderLayout.CENTER);
					if (c != null) {
						layout.removeLayoutComponent(c);
						remove(c);
					}
				} while (c != null);
				layout.layoutContainer(panel);
			}
			scroller = newScroller;
			if (scroller != null) {
				add(scroller, BorderLayout.CENTER);
			}
	
			panel.invalidate();
			panel.repaint();
		}
	}

}
