package edu.ucsf.rbvi.stEMAP.internal.tasks;

import java.awt.Color;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;

import edu.ucsf.rbvi.stEMAP.internal.model.StEMAPManager;
import edu.ucsf.rbvi.stEMAP.internal.utils.ModelUtils.NodeType;

public class SelectTask extends AbstractTask {
	final StEMAPManager manager;
	final List<CyNode> nodeList;
	final CyNetwork network;
	final CyNetworkView view;

	public SelectTask(final StEMAPManager manager, final List<CyNode> nodes, final CyNetworkView view) {
		this.manager = manager;
		this.nodeList = nodes;
		this.network = view.getModel();
		this.view = view;
	}

	public void run(TaskMonitor taskMonitor) {
		// General approach:
		// 	Determine the type of node:
		// 		 Single mutation structure node:
		// 		   o Highlight the genes that interact
		// 		   o Show spheres on structure
		//	   Multiple mutation structure node:
		//	     o Highlight the genes that interact
		//	     o Highlight all of the residues that were mutated
		//	     o Show spheres on structure
		//	   Gene
		//	     o Highlight the residues that interact
		//	     o If multiple mutation highlight the residues for the mutation
		//	     o Show spheres on structure
		Map<Color, Set<String>> colorMap = new HashMap<>();
		List<String> residues = new ArrayList<>();
		List<CyNode> nodesToSelect = new ArrayList<>();
		Color[] colorRange = new Color[4];
		double[] valueRange = { Double.MAX_VALUE, -Double.MAX_VALUE, Double.MAX_VALUE, Double.MIN_VALUE};
		for (CyNode node: nodeList) {
			NodeType type = manager.getNodeType(network, node);
			switch (type) {
			case MUTATION:
				{
					List<CyNode> genes = manager.getGeneNodes(network, node);
					genes.add(node);
					nodesToSelect.addAll(genes);
					residues.addAll(manager.getResidues(network, Collections.singletonList(node)));
				}
				break;
			case MULTIMUTATION:
				{
					List<CyNode> genes = manager.getGeneNodes(network, node);
					manager.selectNodes(network, genes);
					List<CyNode> residueNodes = manager.getResidueNodes(network, node, true);
					nodesToSelect.addAll(residueNodes);
					residues.addAll(manager.getResidues(network, residueNodes));
				}
				break;
			case GENE:
				{
					// Handle this carefully.  We want to get the color to map onto
					// the residues
					Map<Color, Set<String>> resCol = manager.getResiduesAndColors(view, node, colorRange, valueRange);
					for (Color color: resCol.keySet()) {
						if (colorMap.containsKey(color)) {
							colorMap.get(color).addAll(resCol.get(color));
						} else {
							colorMap.put(color, resCol.get(color));
						}
						residues.addAll(resCol.get(color));
					}
				}
				break;
			case STRUCTURE:
				{
					nodesToSelect.add(node);
					residues.addAll(manager.getResidues(network, Collections.singletonList(node)));
				}
				break;
			}

		}
		manager.showSpheres(residues);
		if (colorMap.size() > 0) {
			manager.resolveDuplicates(colorMap);
			Map<Color, Set<String>> newMap = manager.compressMap(colorMap, colorRange);
			manager.colorSpheres(newMap);
		}

		manager.selectNodes(network, nodesToSelect);
	}
}
