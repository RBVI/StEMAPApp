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

import edu.ucsf.rbvi.stEMAP.internal.model.MutationStats;
import edu.ucsf.rbvi.stEMAP.internal.model.StEMAPManager;
import edu.ucsf.rbvi.stEMAP.internal.utils.ColorUtils;
import edu.ucsf.rbvi.stEMAP.internal.utils.ModelUtils;
import edu.ucsf.rbvi.stEMAP.internal.utils.ModelUtils.NodeType;
import edu.ucsf.rbvi.stEMAP.internal.utils.StructureUtils;

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
		Color[] colorRange = null;

		// Check to see if we're using complex coloring.  If we are, we need to do the coloring
		// in one shot, not node by node
		boolean complexColoring = manager.useComplexColoring();
		boolean medianResidueValues = manager.medianResidueValues();
		boolean medianMutationValues = manager.medianMutationValues();
		if (complexColoring || medianResidueValues || medianMutationValues) {
			// Make sure we've only selected GENEs
			for (CyNode node: nodeList) {
				if (ModelUtils.getNodeType(network, node) == NodeType.GENE)
					continue;
				complexColoring = false;
				medianResidueValues = false;
				medianMutationValues = false;
				break;
			}
		}

		if (complexColoring) {
			colorRange = manager.getMixedColorMap();
			colorMap = 
							MutationStats.getComplexResiduesAndColors(manager, view, nodeList, null, manager.getScale());
			for (Color clr: colorMap.keySet()) {
				residues.addAll(colorMap.get(clr));
			}
		} else if (medianResidueValues) {
			colorRange = manager.getResidueColorMap();
			colorMap = 
				StructureUtils.getComplexResiduesAndColors(manager, view, nodeList, manager.getScale(), true);
			for (Color clr: colorMap.keySet()) {
				residues.addAll(colorMap.get(clr));
			}
		} else if (medianMutationValues) {
			colorRange = manager.getResidueColorMap();
			colorMap = 
				StructureUtils.getComplexResiduesAndColors(manager, view, nodeList, manager.getScale(), false);
			for (Color clr: colorMap.keySet()) {
				residues.addAll(colorMap.get(clr));
			}
		} else {
			colorRange = manager.getResidueColorMap();
			for (CyNode node: nodeList) {
				NodeType type = ModelUtils.getNodeType(network, node);
				switch (type) {
				case MUTATION:
					{
						List<CyNode> genes = ModelUtils.getGeneNodes(network, node);
						genes.add(node);
						nodesToSelect.addAll(genes);
						residues.addAll(StructureUtils.getResidues(manager, network, Collections.singletonList(node)));
					}
					break;
				case MULTIMUTATION:
					{
						List<CyNode> genes = ModelUtils.getGeneNodes(network, node);
						ModelUtils.selectNodes(network, genes);
						List<CyNode> residueNodes = StructureUtils.getResidueNodes(manager, network, node, true);
						nodesToSelect.addAll(residueNodes);
						residues.addAll(StructureUtils.getResidues(manager, network, residueNodes));
					}
					break;
				case GENE:
					{
						// Handle this carefully.  We want to get the color to map onto
						// the residues
						Map<Color, Set<String>> resCol = StructureUtils.getResiduesAndColors(manager, view, node, null);
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
						residues.addAll(StructureUtils.getResidues(manager, network, Collections.singletonList(node)));
					}
					break;
				}
			}
		}


		manager.showSpheres(residues);
		if (colorMap.size() > 0) {
			ColorUtils.resolveDuplicates(colorMap);
			Map<Color, Set<String>> newMap = ColorUtils.compressMap(colorMap, colorRange);
			manager.colorSpheres(newMap);
		}

		ModelUtils.selectNodes(network, nodesToSelect);
	}
}
