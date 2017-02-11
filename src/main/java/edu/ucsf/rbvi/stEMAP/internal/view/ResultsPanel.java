package edu.ucsf.rbvi.stEMAP.internal.view;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.MouseEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.image.BufferedImage;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

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
import org.cytoscape.util.swing.IconManager;

import edu.ucsf.rbvi.stEMAP.internal.model.StEMAPManager;
import edu.ucsf.rbvi.stEMAP.internal.model.HeatMapData;

public class ResultsPanel extends JPanel implements CytoPanelComponent2, ItemListener, ChangeListener {
	StEMAPManager manager;
	final CyNetwork network;
	final Logger logger = Logger.getLogger(CyUserLog.NAME);
	JPanel resultsPanel;
	JScrollPane scroller;
	JLabel imageLabel;
	HeatMap heatMap;
	Font iconFont;
	int filterCutoff = 0;

	public ResultsPanel(StEMAPManager manager) {
		this.manager = manager;
		this.network = manager.getMergedNetwork();
		IconManager iconManager = manager.getService(IconManager.class);
		iconFont = iconManager.getIconFont(15.0f);

		setLayout(new BorderLayout());
		scroller = initialize();
		if (scroller != null)
			add(scroller, BorderLayout.CENTER);


		JPanel settings = new JPanel();
		settings.setLayout(new BoxLayout(settings, BoxLayout.Y_AXIS));

		// Add Utilities checkboxes
		JPanel buttonBox = createButtonBox();
		createAutoAnnotateCheckbox(buttonBox);
		createIgnoreMultipleCheckbox(buttonBox);
		createComplexCheckbox(buttonBox);
		CollapsablePanel options = new CollapsablePanel(iconFont, "Options", buttonBox, true);
		options.setBorder(BorderFactory.createEtchedBorder());
		settings.add(options);

		// Add color scale
		JPanel slider = createColorScale();
		CollapsablePanel colorScale = new CollapsablePanel(iconFont, "Color intensity", slider, true);
		colorScale.setBorder(BorderFactory.createEtchedBorder());
		settings.add(colorScale);

		// Add mutant filter 
		JPanel filterSlider = createFilterScale();
		CollapsablePanel filterScale = new CollapsablePanel(iconFont, 
		                                                    "Minimum number of interactions", 
		                                                    filterSlider, true);
		filterScale.setBorder(BorderFactory.createEtchedBorder());
		settings.add(filterScale);

		add(settings, BorderLayout.SOUTH);

		// buttonBox.add(Box.createRigidArea(new Dimension(0,10)));
		// add(buttonBox, BorderLayout.SOUTH);
	}

	public void update() {
		Runnable scrollUpdater = new ScrollUpdater(this);
		if (SwingUtilities.isEventDispatchThread()) {
			scrollUpdater.run();
		} else {
			SwingUtilities.invokeLater(scrollUpdater);
		}
	}

	public void updateChart() {
		if (scroller != null) {
			remove(scroller);
			revalidate();
		}
		scroller = initialize();
		if (scroller != null) {
			add(scroller, BorderLayout.CENTER);
			revalidate();
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
		                                   new HashSet<CyNode>(manager.getSelectedMutations()),
																			 filterCutoff);
		} catch (IllegalArgumentException e) {
			JLabel label = new JLabel(e.getMessage());
			JScrollPane scroller = new JScrollPane(label);
			return scroller;
		}
		// Create our initial chart
		heatMap = new HeatMap(manager, data);
		JFreeChart chart = null;
		try {
			chart = heatMap.createHeatMap();
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}

		int width = data.getColumnHeaders().length*14+200;
		int height = data.getRowHeaders().length*14+200;
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
		// return buttonBox;
		// buttonBox.setAlignmentX(Component.LEFT_ALIGNMENT);
		buttonBox.setLayout(new BoxLayout(buttonBox, BoxLayout.PAGE_AXIS));
		return buttonBox;
	}

	private void createAutoAnnotateCheckbox(JPanel buttonBox) {
		JCheckBox autoAnnotateCB = 
			new JCheckBox("Auto-annotate structure", manager.autoAnnotate());
		autoAnnotateCB.setToolTipText("Automatically show genetic interactions of selected genes on structure");
		autoAnnotateCB.addItemListener(this);
		autoAnnotateCB.setActionCommand("autoAnnotate");
		buttonBox.add(Box.createRigidArea(new Dimension(10,0)));
		buttonBox.add(autoAnnotateCB);
		buttonBox.add(Box.createHorizontalGlue());
	}

	private void createIgnoreMultipleCheckbox(JPanel buttonBox) {
		JCheckBox ignoreMultiplesCB = 
			new JCheckBox("Ignore multiple mutations", manager.ignoreMultiples());
		ignoreMultiplesCB.setToolTipText("Don't show interactions that involved multiple mutations");
		ignoreMultiplesCB.addItemListener(this);
		ignoreMultiplesCB.setActionCommand("ignoreMultiples");
		buttonBox.add(Box.createRigidArea(new Dimension(10,0)));
		buttonBox.add(ignoreMultiplesCB);
		buttonBox.add(Box.createHorizontalGlue());
	}

	private void createComplexCheckbox(JPanel buttonBox) {
		JCheckBox complexCB = 
			new JCheckBox("Use complex coloring", manager.useComplexColoring());
		complexCB.setToolTipText("When multiple genes are selected, assume they are in a complex");
		complexCB.addItemListener(this);
		complexCB.setActionCommand("useComplexColoring");
		buttonBox.add(Box.createRigidArea(new Dimension(10,0)));
		buttonBox.add(complexCB);
		buttonBox.add(Box.createHorizontalGlue());
	}

	private JPanel createColorScale() {
		// JLabel scaleLabel = new JLabel("Color intensity:");
		// scaleLabel.setBorder(BorderFactory.createEmptyBorder(10,0,0,0));
		// buttonBox.add(scaleLabel);
		JSlider scaleSlider = new JSlider(0, 500, 100);
		scaleSlider.addChangeListener(new ColorSliderChanged());
		Dictionary<Integer, JLabel> labelTable = new Hashtable<>();
		labelTable.put(0, sliderLabel("0"));
		labelTable.put(100, sliderLabel("1"));
		labelTable.put(200, sliderLabel("2"));
		labelTable.put(300, sliderLabel("3"));
		labelTable.put(400, sliderLabel("4"));
		labelTable.put(500, sliderLabel("5"));
		scaleSlider.setLabelTable(labelTable);
		scaleSlider.setPaintLabels(true);
		JPanel sliderPanel = new JPanel();
		sliderPanel.add(scaleSlider);
		return sliderPanel;
		// buttonBox.add(sliderPanel);
	}
	
	private JPanel createFilterScale() {
		// JLabel scaleLabel = new JLabel("Color intensity:");
		// scaleLabel.setBorder(BorderFactory.createEmptyBorder(10,0,0,0));
		// buttonBox.add(scaleLabel);
		JSlider scaleSlider = new JSlider(0, 10, 0);
		Dictionary<Integer, JLabel> labelTable = new Hashtable<>();
		labelTable.put(0, sliderLabel("0"));
		labelTable.put(2, sliderLabel("2"));
		labelTable.put(4, sliderLabel("4"));
		labelTable.put(6, sliderLabel("6"));
		labelTable.put(8, sliderLabel("8"));
		labelTable.put(10, sliderLabel("10"));
		scaleSlider.setLabelTable(labelTable);
		scaleSlider.setSnapToTicks(true);
		scaleSlider.setPaintTicks(true);
		scaleSlider.setMajorTickSpacing(2);
		scaleSlider.setMinorTickSpacing(1);
		scaleSlider.addChangeListener(new FilterSliderChanged());
		scaleSlider.setPaintLabels(true);
		JPanel sliderPanel = new JPanel();
		sliderPanel.add(scaleSlider);
		return sliderPanel;
		// buttonBox.add(sliderPanel);
	}

	private JLabel sliderLabel(String label) {
		JLabel lbl = new JLabel(label);
		lbl.setFont(lbl.getFont().deriveFont(8.0f));
		return lbl;
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
	public void stateChanged(ChangeEvent e) {
		Object s = e.getSource();
		if (s instanceof JSlider) {
			JSlider slider = (JSlider)s;
			if (slider.getValueIsAdjusting())
				return;
			// Scale to 0-5
			double sValue = (((double)slider.getValue())/100.0);
			manager.setScale(sValue);
		}

		heatMap.updatePlot();
		manager.updateSpheres();
	}

	@Override
	public void itemStateChanged(ItemEvent e) {
		JCheckBox cb = (JCheckBox)e.getItemSelectable();
		String command = cb.getActionCommand();
		boolean selected = cb.isSelected();
		if (command.equals("autoAnnotate")) {
			manager.setAutoAnnotate(selected);
		} else if (command.equals("ignoreMultiples")) {
			manager.setIgnoreMultiples(selected);
		} else if (command.equals("useComplexColoring")) {
			manager.setUseComplexColoring(selected);
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
			BorderLayout layout = (BorderLayout)panel.getLayout();
			Component c;
			do {
				c = layout.getLayoutComponent(BorderLayout.CENTER);
				if (c != null) {
					layout.removeLayoutComponent(c);
					panel.remove(c);
				}
			} while (c != null);
			layout.layoutContainer(panel);

			scroller = newScroller;
			if (scroller != null) {
				panel.add(scroller, BorderLayout.CENTER);
			}
	
			panel.invalidate();
			panel.repaint();
		}
	}

	class ColorSliderChanged implements ChangeListener {
		@Override
		public void stateChanged(ChangeEvent e) {
			Object s = e.getSource();
			if (s instanceof JSlider) {
				JSlider slider = (JSlider)s;
				if (slider.getValueIsAdjusting())
					return;
				// Scale to 0-5
				double sValue = (((double)slider.getValue())/100.0);
				manager.setScale(sValue);

				heatMap.updatePlot();
				manager.updateSpheres();
			}
		}
	}
	
	class FilterSliderChanged implements ChangeListener {
		@Override
		public void stateChanged(ChangeEvent e) {
			Object s = e.getSource();
			if (s instanceof JSlider) {
				JSlider slider = (JSlider)s;
				if (slider.getValueIsAdjusting())
					return;
				filterCutoff = slider.getValue();

				updateChart();
				manager.updateSpheres();
			}
		}
	}

}
