package edu.ucsf.rbvi.stEMAP.internal.model;

import java.awt.Color;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.jfree.data.xy.DefaultXYZDataset;
import org.jfree.data.xy.XYZDataset;

import org.cytoscape.application.CyUserLog;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.presentation.property.BasicVisualLexicon;

import edu.ucsf.rbvi.stEMAP.internal.utils.ModelUtils;
import edu.ucsf.rbvi.stEMAP.internal.utils.StructureUtils;

public class HeatMapData {
	StEMAPManager manager;
	final CyNetwork network;
	final CyNetworkView networkView;
	final Logger logger = Logger.getLogger(CyUserLog.NAME);
	List<CyNode> genes;
	List<CyNode> mutations;
	Color[] colorMap;
	DefaultXYZDataset data;
	double minZ = 0.0;
	double maxZ = 0.0;
	String[] rowHeaders = null;
	String[] columnHeaders = null;
	Map<String, CyNode> nameMap = null;

	public HeatMapData(StEMAPManager manager, 
	                   List<CyNode> selectedGenes, List<CyNode> selectedMutations) {
		this.manager = manager;
		this.network = manager.getMergedNetwork();
		this.networkView = manager.getMergedNetworkView();

		nameMap = new HashMap<String, CyNode>();
		if (selectedGenes != null && selectedMutations != null)
			init(selectedGenes, selectedMutations);
	}

	private void init(List<CyNode> genesIn, List<CyNode> mutationsIn) {
		List<CyNode> genes = new ArrayList<>(genesIn);
		List<CyNode> mutations = new ArrayList<>(mutationsIn);
		// If we already have both genes and mutations, we probably
		// don't want to add connections
		if (genes.size() == 0 || mutations.size() == 0) {
			// Add connections
			// For each selected Gene, add the connected mutations
			for (CyNode node: genes) {
				for (CyNode resNode: StructureUtils.getResidueNodes(manager, network, node, false)) {
					if (!mutations.contains(resNode))
						mutations.add(resNode);
				}
			}
			// For each mutation, add the connected Genes
			for (CyNode node: mutations) {
				for (CyNode gNode: ModelUtils.getGeneNodes(network, node)) {
					if (!genes.contains(gNode))
						genes.add(gNode);
				}

				// genes.addAll(manager.getGeneNodes(network, node));
			}
		}

		// this.genes = new ArrayList<CyNode>(selectedGenes);
		// this.mutations = new ArrayList<CyNode>(selectedMutations);

		// System.out.println("HeatMapData after additions, have "+genes.size()+" genes and "+mutations.size()+" mutations selected");
		if (genes.size() > 1000)
			throw new IllegalArgumentException("Too many genes ("+genes.size()+") to display");
		if (mutations.size() > 1000)
			throw new IllegalArgumentException("Too many mutations ("+mutations.size()+") to display");

		// Order the genes and mutations to have the same order
		// as in the cluster
		ModelUtils.orderResidues(network, mutations);
		ModelUtils.orderGenes(network, genes);

		// Build matrix
		data = new DefaultXYZDataset();
		final double[][] seriesData = new double[3][mutations.size()*genes.size()];

		colorMap = new Color[4];
		colorMap[0] = StEMAPManager.MAX_COLOR;
		colorMap[1] = StEMAPManager.ZERO_COLOR;
		colorMap[2] = StEMAPManager.MIN_COLOR;
		colorMap[3] = StEMAPManager.MISSING_COLOR;

		columnHeaders = new String[genes.size()];
		rowHeaders = new String[mutations.size()];

		for (int column = 0; column < genes.size(); column++) {
			CyNode columnNode = genes.get(column);
			String seriesLabel = ModelUtils.getName(manager.getMergedNetwork(), columnNode);
			nameMap.put(seriesLabel, columnNode);
			columnHeaders[column] = seriesLabel;
			for (int row = 0; row < mutations.size(); row++) {
				int z = mutations.size()*column+row;
				seriesData[0][z] = column;
				seriesData[1][z] = row;
				seriesData[2][z] = Double.NaN;
				CyNode rowNode = mutations.get(row);
				List<CyEdge> edges = network.getConnectingEdgeList(columnNode, rowNode, CyEdge.Type.ANY);
				if (edges.size() > 0) {
					Double dWeight = network.getRow(edges.get(0)).get(ModelUtils.WEIGHT_COLUMN, Double.class);
					if (dWeight == null)
						continue;
					double weight = dWeight.doubleValue();
					seriesData[2][z] = weight;
				}
				data.addSeries(seriesLabel, seriesData);
			}
		}

		for (int row = 0; row < rowHeaders.length; row++) {
			CyNode node = mutations.get(row);
			String name = ModelUtils.getName(network, node);
			nameMap.put(name, node);
			rowHeaders[row] = name;
		}
	}

	public double getWeight(String gene, String mutation) {
		CyNetwork cdtNetwork = manager.getCDTNetwork();
		CyNode node1 = nameMap.get(gene);
		CyNode node2 = nameMap.get(mutation);
		if (node1 != null && node2 != null) {
			List<CyEdge> edges = cdtNetwork.getConnectingEdgeList(node1, node2, CyEdge.Type.ANY);
			if (edges != null) {
				return cdtNetwork.getRow(edges.get(0)).get(ModelUtils.WEIGHT_COLUMN, Double.class);
			}
		}
		return Double.NaN;
	}

	public void update(List<CyNode> selectedGenes, List<CyNode> selectedMutations) {
		init (selectedGenes, selectedMutations);
	}

	public XYZDataset getData() { return data; }

	public double getMaximumZ() {
		return manager.getMaxWeight();
	}

	public double getMinimumZ() {
		return manager.getMinWeight();
	}

	public String[] getColumnHeaders() {
		return columnHeaders;
	}

	public String[] getRowHeaders() {
		return rowHeaders;
	}

	public Color[] getColorMap() { return colorMap; }

	private double binWeight(double minZ, double maxZ, double weight, int nBins) {
		if (weight < 0.0) {
			return Math.round((weight-minZ)/(double)nBins);
		} else if (weight > 0.0) {
			return Math.round((maxZ-weight)/(double)nBins);
		} else
			return 0;
	}

	private Color getColor(CyEdge edge) {
		return (Color)networkView.getEdgeView(edge).getVisualProperty(BasicVisualLexicon.EDGE_STROKE_UNSELECTED_PAINT);
	}
}
