package edu.ucsf.rbvi.stEMAP.internal.tasks;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cytoscape.model.CyColumn;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyIdentifiable;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkFactory;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyRow;
import org.cytoscape.model.CyTable;
import org.cytoscape.model.subnetwork.CyRootNetwork;
import org.cytoscape.model.subnetwork.CySubNetwork;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.CyNetworkViewFactory;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.cytoscape.view.model.View;
import org.cytoscape.view.presentation.property.BasicVisualLexicon;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.Tunable;
import org.cytoscape.work.util.BoundedDouble;
import org.cytoscape.work.util.ListSingleSelection;

import edu.ucsf.rbvi.stEMAP.internal.model.StEMAPManager;
import edu.ucsf.rbvi.stEMAP.internal.model.TreeLayout;

public class MergeTask extends AbstractTask {
	final StEMAPManager manager;

	// Tunable for RIN network
	@Tunable (description="RIN Network", gravity=1.0)
	public ListSingleSelection<NamedNetwork> rin;

	// Tunable for CDT
	@Tunable (description="EMAP Network", gravity=2.0)
	public ListSingleSelection<NamedNetwork> cdt;

	// Tunables for cutoff range
	@Tunable (description="Minimum value for positive interactions", gravity=3.0)
	public double positiveCutoff = 1.0;
	@Tunable (description="Minimum value for negative interactions", gravity=4.0)
	public double negativeCutoff = -2.0;

	public MergeTask(final StEMAPManager manager) {
		this.manager = manager;
		CyNetworkManager netManager = manager.getService(CyNetworkManager.class);
		CyNetwork rinNetwork = manager.getRINNetwork();
		List<NamedNetwork> netList = new ArrayList<>();
		NamedNetwork rinNamedNetwork = null;
		for (CyNetwork net: netManager.getNetworkSet()) {
			NamedNetwork nn = new NamedNetwork(net);
			if (net.equals(rinNetwork))
				rinNamedNetwork = nn;
			netList.add(nn);
		}
		rin = new ListSingleSelection<NamedNetwork>(netList);
		if (rinNamedNetwork != null)
			rin.setSelectedValue(rinNamedNetwork);
		cdt = new ListSingleSelection<NamedNetwork>(netList);
	}

	public void run(TaskMonitor taskMonitor) {
		// General approach:
		// 1) Every node in CDT keeps its name and all columns
		// 2) Every CDT node that has a corresponding RIN node gets RIN columns
		// 3) RIN Nodes without correponding CDT nodes keep RIN Name and all columns
		// 4) CDT nodes that represent multiple RIN nodes get links to corresponding RIN nodes
		// 5) Node positions for nodes are based on RIN positions
		// 6) CDT nodes without chain information get pushed outward
		CyNetwork cdtNetwork = cdt.getSelectedValue().getNetwork();
		CyNetwork rinNetwork = rin.getSelectedValue().getNetwork();

		CyNetworkViewManager viewManager = manager.getService(CyNetworkViewManager.class);
		CyNetworkView rinNetworkView = viewManager.getNetworkViews(rinNetwork).iterator().next();

		taskMonitor.setTitle("Merging "+rin.getSelectedValue()+" and "+cdt.getSelectedValue());

		// Create new network
		CyRootNetwork cdtRootNetwork = ((CySubNetwork)cdtNetwork).getRootNetwork();
		CySubNetwork cdtSubNetwork = cdtRootNetwork.addSubNetwork(cdtNetwork.getNodeList(), null);

		cdtSubNetwork.getRow(cdtSubNetwork).set(CyNetwork.NAME, rin.getSelectedValue()+" and "+cdt.getSelectedValue());
		taskMonitor.showMessage(TaskMonitor.Level.INFO, "Getting significant edges");
		for (CyEdge edge: cdtNetwork.getEdgeList()) {
			if (cancelled) {
				taskMonitor.showMessage(TaskMonitor.Level.INFO, "Cancelled"); return;
			}
			Double weight = cdtNetwork.getRow(edge).get("weight", Double.class);
			if (weight < negativeCutoff || weight > positiveCutoff)
				cdtSubNetwork.addEdge(edge);
		}

		taskMonitor.showMessage(TaskMonitor.Level.INFO, "Mapping residues");
		// Create a map of residue information
		Map<String, List<CyNode>> residueMap = new HashMap<>();
		List<CyNode> multipleResidues = new ArrayList<>();
		List<CyNode> targetNodes = new ArrayList<>();
		for (CyNode node: cdtSubNetwork.getNodeList()) {
			if (cancelled) {
				taskMonitor.showMessage(TaskMonitor.Level.INFO, "Cancelled"); return;
			}
			String residue = cdtSubNetwork.getRow(node).get("Residues", String.class);
			if (residue != null && residue.length() > 0) {
				if (residue.indexOf("-") == -1 &&
						residue.indexOf(",") == -1)
					addNodeToMap(residueMap, residue, node);
				else
					multipleResidues.add(node);
			} else {
				targetNodes.add(node);
			}
		}

		// Create network columns in new network
		CyTable cdtNetworkTable = cdtNetwork.getDefaultNetworkTable();
		CyTable cdtSubNetworkTable = cdtSubNetwork.getDefaultNetworkTable();
		copyColumns(cdtNetworkTable, cdtSubNetworkTable);
		CyRow cdtNetworkRow = cdtNetwork.getRow(cdtNetwork);
		CyRow cdtSubNetworkRow = cdtSubNetwork.getRow(cdtSubNetwork);

		for (CyColumn column: cdtNetworkTable.getColumns()) {
			if (column.getName().equals(CyNetwork.NAME)) continue;
			if (column.getName().equals(CyRootNetwork.SHARED_NAME)) continue;
			cdtSubNetworkRow.set(column.getName(),
			                     cdtNetworkRow.getRaw(column.getName()));
		}

		// Create RIN node columns in new network
		copyColumns(rinNetwork.getDefaultNodeTable(), cdtSubNetwork.getDefaultNodeTable());

		// Create RIN edge columns in new network
		copyColumns(rinNetwork.getDefaultEdgeTable(), cdtSubNetwork.getDefaultEdgeTable());

		manager.flushEvents();

		taskMonitor.showMessage(TaskMonitor.Level.INFO, "Copying over RIN nodes");
		double progress = 0.0;

		// Copy over all RIN nodes, matching to CDT nodes as we go
		for (CyNode node: rinNetwork.getNodeList()) {
			taskMonitor.setProgress(progress/rinNetwork.getNodeCount());
			if (cancelled) {
				taskMonitor.showMessage(TaskMonitor.Level.INFO, "Cancelled"); return;
			}
			String resString = rinNetwork.getRow(node).get("pdbFileName", String.class);
			if (!residueMap.containsKey(resString)) {
				CyNode newNode = cdtSubNetwork.addNode();
				addNodeToMap(residueMap, resString, newNode);
				copyRow(rinNetwork.getDefaultNodeTable(), cdtSubNetwork.getDefaultNodeTable(),
				        node, newNode, true);
			} else {
				for (CyNode newNode: residueMap.get(resString)) {
					copyRow(rinNetwork.getDefaultNodeTable(), cdtSubNetwork.getDefaultNodeTable(),
					        node, newNode, false);
				}
			}
			progress = progress + 1.0;
		}

		// System.out.println("Flushing events");
		// manager.flushEvents();
		// System.out.println("done");

		taskMonitor.showMessage(TaskMonitor.Level.INFO, "Copying over RIN edges");
		progress = 0.0;

		// Copy over all RIN edges
		for (CyEdge edge: rinNetwork.getEdgeList()) {
			taskMonitor.setProgress(progress/rinNetwork.getEdgeCount());
			if (cancelled) {
				taskMonitor.showMessage(TaskMonitor.Level.INFO, "Cancelled"); return;
			}
			String sourceResString = rinNetwork.getRow(edge.getSource()).get("pdbFileName", String.class);
			List<CyNode> newSources = residueMap.get(sourceResString);
			String targetResString = rinNetwork.getRow(edge.getTarget()).get("pdbFileName", String.class);
			List<CyNode> newTargets = residueMap.get(targetResString);
			for (CyNode newSource: newSources) {
				for (CyNode newTarget: newTargets) {
					CyEdge newEdge = cdtSubNetwork.addEdge(newSource, newTarget, edge.isDirected());
					copyRow(rinNetwork.getDefaultEdgeTable(), cdtSubNetwork.getDefaultEdgeTable(),
						      edge, newEdge, true);
				}
			}
			progress = progress + 1.0;
		}

		// manager.flushEvents();

		taskMonitor.showMessage(TaskMonitor.Level.INFO, "Copying over RIN node locations");
		progress = 0.0;

		CyNetworkView cdtNetworkView = manager.getService(CyNetworkViewFactory.class).createNetworkView(cdtSubNetwork);

		Rectangle2D rinBounds = new Rectangle2D.Double();
		for (CyNode node: rinNetwork.getNodeList()) {
			taskMonitor.setProgress(progress/rinNetwork.getNodeCount());
			if (cancelled) { taskMonitor.showMessage(TaskMonitor.Level.INFO, "Cancelled"); return; }
			// This seems to be redundant, but it works better if we do this after all of the nodes
			// have been created in the new network
			String resString = rinNetwork.getRow(node).get("pdbFileName", String.class);
			int offset = 0;
			for (CyNode newNode: residueMap.get(resString)) {
				rinBounds.add(copyLocation(rinNetworkView.getNodeView(node), cdtNetworkView.getNodeView(newNode), offset));
				offset++;
			}
			progress = progress + 1.0;
		}

		taskMonitor.showMessage(TaskMonitor.Level.INFO, "Creating multi-residue edges");

		// Find a place to put the multi-residue nodes that point to 
		// residues not in the RIN
		double xCenter = rinBounds.getX()+rinBounds.getWidth()/2;
		double yCenter = rinBounds.getY()+rinBounds.getHeight()/2;

		// Add multi-residue edges
		for (CyNode node: multipleResidues) {
			if (cancelled) {
				taskMonitor.showMessage(TaskMonitor.Level.INFO, "Cancelled"); return;
			}
			String residues = cdtSubNetwork.getRow(node).get("Residues", String.class);
			double xSum = 0;
			double ySum = 0;
			int count = 0;
			for (String res: parseResidues(residues)) {
				// System.out.println("Looking for residue: "+res);
				List<CyNode> targets = residueMap.get(res);
				if (targets == null) {
					xSum = xCenter;
					ySum = yCenter;
					xCenter += 10;
					yCenter += 10;
				} else {
					for (CyNode target: targets) {
						cdtSubNetwork.addEdge(node, target, true);
						View<CyNode> tv = cdtNetworkView.getNodeView(target);
						xSum += tv.getVisualProperty(BasicVisualLexicon.NODE_X_LOCATION);
						ySum += tv.getVisualProperty(BasicVisualLexicon.NODE_X_LOCATION);
						count++;
					}
					xSum = xSum/(double)count;
					ySum = ySum/(double)count;
				}
				View<CyNode> nv = cdtNetworkView.getNodeView(node);
				nv.setVisualProperty(BasicVisualLexicon.NODE_X_LOCATION, xSum);
				nv.setVisualProperty(BasicVisualLexicon.NODE_Y_LOCATION, ySum);
			}
		}

		List<View<CyNode>> targetNodeViews = new ArrayList<>();
		for (CyNode node: targetNodes) {
			targetNodeViews.add(cdtNetworkView.getNodeView(node));
		}

		// Now layout the interaction partners
		List<String> attrTree = cdtSubNetwork.getRow(cdtSubNetwork).getList("__attrClusters", String.class);
		List<String> attrOrder = cdtSubNetwork.getRow(cdtSubNetwork).getList("__arrayOrder", String.class);
		TreeLayout tl = new TreeLayout(cdtSubNetwork, targetNodeViews, attrTree);
		tl.layout(attrOrder, rinBounds.getX()-200.0, rinBounds.getY()+rinBounds.getHeight()*5);
		//
		// Finally, create a new visual style based on the RIN style
		// createStyle(cdtNetworkView);

		manager.getService(CyNetworkManager.class).addNetwork(cdtSubNetwork);
		manager.getService(CyNetworkViewManager.class).addNetworkView(cdtNetworkView);
	}

	void copyColumns(CyTable fromTable, CyTable toTable) {
		for (CyColumn column: fromTable.getColumns()) {
			String name = column.getName();
			if (name.equals(CyNetwork.NAME) ||
					name.equals(CyRootNetwork.SHARED_NAME) ||
					name.equals(CyNetwork.SELECTED))
				continue;
			if (toTable.getColumn(name) != null)
				continue;

			if (column.getType().equals(List.class)) {
				toTable.createListColumn(name, column.getListElementType(), false);
			} else {
				toTable.createColumn(name, column.getType(), false);
			}
		}
	}

	void copyRow(CyTable fromTable, CyTable toTable, CyIdentifiable fromCyId, CyIdentifiable toCyId,
	             boolean includeNames) {
		CyRow fromRow = fromTable.getRow(fromCyId.getSUID());
		CyRow toRow = toTable.getRow(toCyId.getSUID());
		for (CyColumn column: fromTable.getColumns()) {
			String name = column.getName();
			if (name.equals(CyNetwork.SELECTED)) continue;

			if (!includeNames && 
					(name.equals(CyNetwork.NAME) ||
					 name.equals(CyRootNetwork.SHARED_NAME)))
				continue;
			Object rawValue = fromRow.getRaw(name);
			if (rawValue != null)
				toRow.set(name, rawValue);
		}
	}

	List<String> parseResidues(String residues) {
		List<String> resList = new ArrayList<>();

		String[] split1 = residues.split("[#.]");
		String pdb = split1[0];
		String chain = split1[2];
		if (split1[1].indexOf("-") > 0) {
			String[] range = split1[1].split("-");
			for (int i = Integer.parseInt(range[0]); i < Integer.parseInt(range[1]); i++) {
				resList.add(pdb+"#"+i+"."+chain);
			}
		} else {
			String[] resArray = split1[1].split(",");
			for (String res: resArray) {
				resList.add(pdb+"#"+res+"."+chain);
			}
		}

		return resList;
	}

	void addNodeToMap(Map<String, List<CyNode>> map, String res, CyNode node) {
		if (!map.containsKey(res))
			map.put(res, new ArrayList<CyNode>());
		map.get(res).add(node);
	}

	Point2D copyLocation(View<CyNode> from, View<CyNode> to, int offset) {
		double shift = 30.0*(double)offset;
		double x = from.getVisualProperty(BasicVisualLexicon.NODE_X_LOCATION)+shift;
		double y = from.getVisualProperty(BasicVisualLexicon.NODE_Y_LOCATION)+shift;
		to.setVisualProperty(BasicVisualLexicon.NODE_X_LOCATION, x);
		to.setVisualProperty(BasicVisualLexicon.NODE_Y_LOCATION, y);
		return new Point2D.Double(x,y);
	}

	class NamedNetwork {
		final CyNetwork network;
		public NamedNetwork(final CyNetwork net) {
			this.network = net;
		}

		public String toString() {
			return network.getRow(network).get(CyNetwork.NAME, String.class);
		}

		public CyNetwork getNetwork() { return network; }
	}
}
