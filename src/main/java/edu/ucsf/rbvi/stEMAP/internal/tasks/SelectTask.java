package edu.ucsf.rbvi.stEMAP.internal.tasks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;

import edu.ucsf.rbvi.stEMAP.internal.model.StEMAPManager;
import edu.ucsf.rbvi.stEMAP.internal.utils.ModelUtils.NodeType;

public class SelectTask extends AbstractTask {
	final StEMAPManager manager;
	final List<CyNode> nodeList;
	final CyNetwork network;

	public SelectTask(final StEMAPManager manager, final List<CyNode> nodes, final CyNetwork net) {
		this.manager = manager;
		this.nodeList = nodes;
		this.network = net;
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
		List<String> residues = new ArrayList<>();
		for (CyNode node: nodeList) {
			NodeType type = manager.getNodeType(network, node);
			switch (type) {
			case MUTATION:
				{
					List<CyNode> genes = manager.getGeneNodes(network, node);
					genes.add(node);
					manager.selectNodes(network, genes);
					residues.addAll(manager.getResidues(network, Collections.singletonList(node)));
				}
				break;
			case MULTIMUTATION:
				{
					List<CyNode> genes = manager.getGeneNodes(network, node);
					manager.selectNodes(network, genes);
					List<CyNode> residueNodes = manager.getResidueNodes(network, node);
					manager.selectNodes(network, residueNodes);
					residues.addAll(manager.getResidues(network, residueNodes));
				}
				break;
			case GENE:
				{
					List<CyNode> residueNodes = manager.getResidueNodes(network, node);
					manager.selectNodes(network, residueNodes);
					residues.addAll(manager.getResidues(network, residueNodes));
				}
				break;
			case STRUCTURE:
				{
					manager.selectNodes(network, Collections.singletonList(node));
					residues.addAll(manager.getResidues(network, Collections.singletonList(node)));
				}
				break;
			}

		}
		manager.showSpheres(residues);

	}
}
