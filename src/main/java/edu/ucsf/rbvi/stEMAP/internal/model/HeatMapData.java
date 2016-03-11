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

public class HeatMapData {
	StEMAPManager manager;
	final CyNetwork network;
	final CyNetworkView networkView;
	final Logger logger = Logger.getLogger(CyUserLog.NAME);
	List<CyNode> genes;
	List<CyNode> mutations;
	Color[] colorMap;
	DefaultXYZDataset data;
	double minZ = Double.MAX_VALUE;
	double maxZ = Double.MIN_VALUE;

	public HeatMapData(StEMAPManager manager, Set<CyNode> selectedGenes, Set<CyNode> selectedMutations) {
		this.manager = manager;
		this.network = manager.getMergedNetwork();
		this.networkView = manager.getMergedNetworkView();

		init(selectedGenes, selectedMutations);
	}

	private void init(Set<CyNode> selectedGenes, Set<CyNode> selectedMutations) {
		// Add connections
		// For each selected Gene, add the connected mutations
		for (CyNode node: selectedGenes) {
			selectedMutations.addAll(manager.getResidueNodes(network, node));
		}
		// For each mutation, add the connected Genes
		for (CyNode node: selectedMutations) {
			selectedGenes.addAll(manager.getGeneNodes(network, node));
		}

		this.genes = new ArrayList<CyNode>(selectedGenes);
		this.mutations = new ArrayList<CyNode>(selectedMutations);

		System.out.println("HeatMapData after additions, have "+selectedGenes.size()+" genes and "+selectedMutations.size()+" mutations selected");

		// Order the genes and mutations to have the same order
		// as in the cluster
		manager.orderResidues(mutations);
		manager.orderGenes(genes);

		// Build matrix
		data = new DefaultXYZDataset();
		final double[][] seriesData = new double[3][mutations.size()*genes.size()];

		colorMap = new Color[4];
		colorMap[1] = Color.WHITE;
		colorMap[3] = Color.GRAY;

		for (int column = 0; column < mutations.size(); column++) {
			CyNode columnNode = mutations.get(column);
			String seriesLabel = ModelUtils.getName(manager.getMergedNetwork(), columnNode);
			for (int row = 0; row < genes.size(); row++) {
				int z = genes.size()*column+row;
				seriesData[0][z] = column;
				seriesData[1][z] = row;
				seriesData[2][z] = Double.NaN;
				CyNode rowNode = genes.get(row);
				List<CyEdge> edges = network.getConnectingEdgeList(columnNode, rowNode, CyEdge.Type.ANY);
				if (edges.size() > 0) {
					Double dWeight = network.getRow(edges.get(0)).get("weight", Double.class);
					if (dWeight == null)
						continue;
					double weight = dWeight.doubleValue();
					Color color = getColor(edges.get(0));
					System.out.println("Color = "+color);
					if (weight > maxZ) {
						maxZ = weight;
						colorMap[0] = color;
						System.out.println("Color[max] = "+color);
					} else if (weight < minZ) {
						minZ = weight;
						colorMap[2] = color;
						System.out.println("Color[min] = "+color);
					} else if (weight == 0.0) {
						colorMap[1] = color;
					}
					minZ = Math.min(weight, minZ);
					seriesData[2][z] = weight;
					System.out.println("x = "+seriesData[0][z]+" y = "+seriesData[1][z]+" z = "+seriesData[2][z]);
				}
				data.addSeries(seriesLabel, seriesData);
			}
			System.out.println("seriesData[3]["+seriesData[0].length+"] range="+minZ+"-"+maxZ);
		}

	}

	public void update(Set<CyNode> selectedGenes, Set<CyNode> selectedMutations) {
		init (selectedGenes, selectedMutations);
	}

	public XYZDataset getData() { return data; }

	public double getMaximumZ() {
		return maxZ;
	}

	public double getMinimumZ() {
		return minZ;
	}

	public String[] getColumnHeaders() {
		String[] h = new String[mutations.size()];
		for (int column = 0; column < h.length; column++) {
			CyNode node = mutations.get(column);
			h[column] = ModelUtils.getName(network, node);
		}
		return h;
	}

	public String[] getRowHeaders() {
		String[] h = new String[genes.size()];
		for (int row = 0; row < h.length; row++) {
			CyNode node = genes.get(row);
			h[row] = ModelUtils.getName(network, node);
		}
		return h;
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
