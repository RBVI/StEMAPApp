package edu.ucsf.rbvi.stEMAP.internal.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.jfree.data.xy.MatrixSeries;

import org.cytoscape.application.CyUserLog;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;

public class HeatMapData {
	StEMAPManager manager;
	final CyNetwork network;
	final Logger logger = Logger.getLogger(CyUserLog.NAME);
	List<CyNode> genes;
	List<CyNode> mutations;
	MatrixSeries data;

	public HeatMapData(StEMAPManager manager, List<CyNode> selectedGenes, List<CyNode> selectedMutations) {
		this.manager = manager;
		this.network = manager.getMergedNetwork();

		init(selectedGenes, selectedMutations);
	}

	private void init(List<CyNode> selectedGenes, List<CyNode> selectedMutations) {
		this.genes = new ArrayList<CyNode>(selectedGenes);
		this.mutations = new ArrayList<CyNode>(selectedMutations);

		// Add connections
		// For each selected Gene, add the connected mutations
		for (CyNode node: genes) {
			mutations.addAll(manager.getResidueNodes(network, node));
		}
		// For each mutation, add the connected Genes
		for (CyNode node: mutations) {
			genes.addAll(manager.getGeneNodes(network, node));
		}
		// Build matrix
		data = new MatrixSeries("HeatMap", genes.size(), mutations.size());
		for (int column = 0; column < mutations.size(); column++) {
			CyNode columnNode = mutations.get(column);
			for (int row = 0; row < genes.size(); row++) {
				CyNode rowNode = genes.get(row);
				List<CyEdge> edges = network.getConnectingEdgeList(columnNode, rowNode, CyEdge.Type.ANY);
				double weight = network.getRow(edges.get(0)).get("weight", Double.class);
				data.update(row, column, weight);
			}
		}
	}

	public void update(List<CyNode> selectedGenes, List<CyNode> selectedMutations) {
		init (selectedGenes, selectedMutations);
	}

	public MatrixSeries getData() { return data; }

}
