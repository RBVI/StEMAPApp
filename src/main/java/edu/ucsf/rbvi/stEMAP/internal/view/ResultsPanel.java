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
import java.util.Set;

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
import edu.ucsf.rbvi.stEMAP.internal.utils.ModelUtils;
import edu.ucsf.rbvi.stEMAP.internal.utils.StructureUtils;

public class ResultsPanel extends JPanel 
                          implements CytoPanelComponent2, ItemListener, ChangeListener {
	StEMAPManager manager;
	final CyNetwork network;
	final Logger logger = Logger.getLogger(CyUserLog.NAME);
	JSlider filterSlider;
	JPanel filterSliderPanel;
 	JPanel resultsPanel;
	JScrollPane scroller;
	JLabel imageLabel;
	HeatMap heatMap;
	Font iconFont;
	int filterCutoff = 0;
	List<CyNode> filteredMutations = null;

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
		createSelectEdgesCheckbox(buttonBox);
		CollapsablePanel options = new CollapsablePanel(iconFont, "Options", buttonBox, true);
		options.setBorder(BorderFactory.createEtchedBorder());
		settings.add(options);

		// Add color scale
		JPanel slider = createColorScale();
		CollapsablePanel colorScale = new CollapsablePanel(iconFont, "Color intensity", slider, true);
		colorScale.setBorder(BorderFactory.createEtchedBorder());
		settings.add(colorScale);

		// Add mutant filter 
		filterSliderPanel = createFilterScale(manager.getSelectedGenes().size());
		CollapsablePanel filterScale = new CollapsablePanel(iconFont, 
		                                                    "Minimum number of interactions", 
		                                                    filterSliderPanel, true);
		filterScale.setBorder(BorderFactory.createEtchedBorder());
		settings.add(filterScale);

		add(settings, BorderLayout.SOUTH);
	}

	public void update() {
		Runnable scrollUpdater = new ScrollUpdater(this);
		if (SwingUtilities.isEventDispatchThread()) {
			scrollUpdater.run();
		} else {
			SwingUtilities.invokeLater(scrollUpdater);
		}
		updateFilterScale(manager.getSelectedGenes().size());
	}

	public void updateChart() {
		if (scroller != null) {
			// System.out.println("Removing scroller");
			remove(scroller);
			revalidate();
		}
		scroller = initialize();
		if (scroller != null) {
			// System.out.println("Adding scroller");
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

		List<CyNode> mutations = new ArrayList<CyNode>();
		List<CyNode> genes = new ArrayList<CyNode>();
		addConnections(mutations, genes);
		filteredMutations = filterMutations(filterCutoff, mutations, genes);

		if (manager.selectEdges()) {
			// Clear the existing edge selection
			for (CyEdge edge: CyTableUtil.getEdgesInState(network, CyNetwork.SELECTED, true))
				network.getRow(edge).set(CyNetwork.SELECTED, Boolean.FALSE);

			// Select the edges connecting the genes and the mutations
			for (CyNode gene: genes) {
				List<CyEdge> geneEdges = network.getAdjacentEdgeList(gene, CyEdge.Type.ANY);
				for (CyEdge edge: geneEdges) {
					if (filteredMutations.contains(edge.getSource()))
						network.getRow(edge).set(CyNetwork.SELECTED, Boolean.TRUE);
				}
			}

		}
	
		HeatMapData data;
		try {
			data	= new HeatMapData(manager, genes, filteredMutations);
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
		JScrollPane scrollPane = new JScrollPane(chartPanel);
		return scrollPane;
	}

	private JPanel createButtonBox() {
		JPanel buttonBox = new JPanel();
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

	private void createSelectEdgesCheckbox(JPanel buttonBox) {
		JCheckBox selectEdgesCB = 
			new JCheckBox("Select edges", manager.selectEdges());
		selectEdgesCB.setToolTipText("Automatically select edges between genes and mutations");
		selectEdgesCB.addItemListener(this);
		selectEdgesCB.setActionCommand("selectEdges");
		buttonBox.add(Box.createRigidArea(new Dimension(10,0)));
		buttonBox.add(selectEdgesCB);
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
	
	private JPanel createFilterScale(int size) {
		int max = getFilterMax(size);
		int majorTick = getFilterMajor(max);
		int minorTick = getFilterMinor(max, majorTick);
		Dictionary<Integer, JLabel> labelTable = createFilterLabels(max, majorTick);
		filterSlider = new JSlider(0, max, 0);
		filterSlider.setLabelTable(labelTable);
		filterSlider.setSnapToTicks(true);
		filterSlider.setPaintTicks(true);
		filterSlider.setMajorTickSpacing(majorTick);
		if (minorTick > 0)
			filterSlider.setMinorTickSpacing(minorTick);
		filterSlider.addChangeListener(new FilterSliderChanged());
		if (max > 0)
			filterSlider.setPaintLabels(true);
		JPanel sliderPanel = new JPanel();
		sliderPanel.add(filterSlider);
		return sliderPanel;
		// buttonBox.add(sliderPanel);
	}

	private void updateFilterScale(int size) {
		int max = getFilterMax(size);
		int majorTick = getFilterMajor(max);
		int minorTick = getFilterMinor(max, majorTick);
		Dictionary<Integer, JLabel> labelTable = createFilterLabels(max, majorTick);
		filterSlider.setMaximum(max);
		filterSlider.setLabelTable(labelTable);
		filterSlider.setMajorTickSpacing(majorTick);
		if (max > 0)
			filterSlider.setPaintLabels(true);
		else
			filterSlider.setPaintLabels(false);
		if (minorTick > 0)
			filterSlider.setMinorTickSpacing(minorTick);
		filterSlider.revalidate();
		// filterSlider.repaint();
		filterSliderPanel.revalidate();
		// filterSliderPanel.repaint();
	}

	private int getFilterMax(int size) {
		if (size == 0) return 0;
		size = size - 1;
		if (size <= 10) return size;
		if (size <= 25) {
			if (size%5 == 0) return size;
			return (size/5)*5;
		}
		if (size <= 100) {
			if (size%10 == 0) return size;
			return (size/10)*10;
		}

		if (size%50 == 0) return size;
		return (size/50)*50;
	}

	private int getFilterMajor(int max) {
		if (max == 0) return 0;
		if (max <= 5) return 1;
		if (max <= 10) return 2;
		if (max <= 25) return 5;
		if (max <= 100) return 10;
		return 50;
	}

	private int getFilterMinor(int max, int major) {
		if (major < 5) return 0;
		if (major < 10) return 1;
		if (major == 10) return 5;
		return 25;
	}

	private Dictionary<Integer, JLabel> createFilterLabels(int max, int majorTicks) {
		Dictionary<Integer, JLabel> labelTable = new Hashtable<>();
		if (max == 0) return labelTable;
		for (int i = 0; i <= max; i = i+majorTicks) {
			labelTable.put(i, sliderLabel(Integer.toString(i)));
		}
		return labelTable;
	}

	private JLabel sliderLabel(String label) {
		JLabel lbl = new JLabel(label);
		lbl.setFont(lbl.getFont().deriveFont(8.0f));
		return lbl;
	}

	private void addConnections(List<CyNode> mutations, List<CyNode> genes) {
		Set<CyNode> selectedMutations = manager.getSelectedMutations();
		Set<CyNode> selectedGenes = manager.getSelectedGenes();
		genes.addAll(selectedGenes);
		mutations.addAll(selectedMutations);
		// System.out.println("addConnections("+mutations.size()+", "+genes.size()+")");
		if (selectedGenes.size() == 0 || selectedMutations.size() == 0) {
			// Add connections
			// For each selected Gene, add the connected mutations
			for (CyNode node: selectedGenes) {
				for (CyNode resNode: StructureUtils.getResidueNodes(manager, network, node, false)) {
					if (!mutations.contains(resNode))
						mutations.add(resNode);
				}
			}
			// For each mutation, add the connected Genes
			for (CyNode node: selectedMutations) {
				for (CyNode gNode: ModelUtils.getGeneNodes(network, node)) {
					if (!genes.contains(gNode))
						genes.add(gNode);
				}

				// genes.addAll(manager.getGeneNodes(network, node));
			}
		}
	}

	private List<CyNode> filterMutations(int minimumColumns, List<CyNode> mutations, List<CyNode> genes) {
		// System.out.println("filterMutations("+minimumColumns+","+mutations.size()+","+genes.size()+")");
		if (minimumColumns > 0) {
			List<CyNode> filteredMutations = new ArrayList<>();
			for (int row = 0; row < mutations.size(); row++) {
				int mutationCount = 0;
				CyNode rowNode = mutations.get(row);
				for (int column = 0; column < genes.size(); column++) {
					CyNode columnNode = genes.get(column);
					List<CyEdge> edges = network.getConnectingEdgeList(columnNode, rowNode, CyEdge.Type.ANY);
					if (edges.size() > 0) {
						mutationCount++;
					}
				}
				if (mutationCount > minimumColumns)
					filteredMutations.add(rowNode);
			}
			// System.out.println("Returning "+filteredMutations.size()+" mutations");
			return filteredMutations;
		}
		// System.out.println("Returning "+mutations.size()+" mutations");
		return mutations;
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
		} else if (command.equals("selectEdges")) {
			manager.setSelectEdges(selected);
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
				manager.updateSpheres(filteredMutations);
			}
		}
	}

}
