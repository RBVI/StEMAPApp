package edu.ucsf.rbvi.stEMAP.internal.utils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.cytoscape.model.CyColumn;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyIdentifiable;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyRow;
import org.cytoscape.model.CyTable;
import org.cytoscape.model.subnetwork.CyRootNetwork;
import org.cytoscape.work.TaskMonitor;

import edu.ucsf.rbvi.stEMAP.internal.model.StEMAPManager;

public class ModelUtils {
	// Column names
	// public static final String GENE_COLUMN = "StGene";
	public static final String CHAIN_COLUMN = "Chain";
	public static final String MUT_COLUMN = "Mutation";
	public static final String MUT_TYPE_COLUMN = "MutationType";
	public static final String RESIDUE_COLUMN = "Residues";
	// public static final String ID_COLUMN = "ModID";
	public static final String PDB_COLUMN = "pdbFileName";
	public static final String WEIGHT_COLUMN = "weight";

	// Mutation types
	public static final String SINGLE = "single";
	public static final String MULTIPLE = "multiple";
	public static final String DELETION = "del";

	public enum NodeType {MUTATION, MULTIMUTATION, STRUCTURE, GENE};

	/**
	 * Called from SplitResiduesTask.  This method reads the data from the
	 * CDT network and splits out the residue identifiers for later merging
	 * with the RIN
	 */
	public static void splitResidues(TaskMonitor taskMonitor, 
	                                 StEMAPManager manager, CyNetwork network) {
		CyTable nodeTable = network.getDefaultNodeTable();
		// createColumn(nodeTable, GENE_COLUMN, String.class);
		createColumn(nodeTable, CHAIN_COLUMN, String.class);
		createColumn(nodeTable, MUT_COLUMN, String.class);
		createColumn(nodeTable, MUT_TYPE_COLUMN, String.class);
		// createColumn(nodeTable, ID_COLUMN, String.class); // Should this be a list?
		createColumn(nodeTable, RESIDUE_COLUMN, String.class); // Should this be a list?

		int count = 0;

		String pdbId = manager.getPDB();
		if (manager.usePDBFile() && manager.getPDBFileName() != null)
			pdbId = manager.getPDBFileName();

		for (CyNode node: network.getNodeList()) {
			String name = network.getRow(node).get(CyNetwork.NAME, String.class);
			if (name.indexOf("|") < 0)
				continue;
			String[] split1 = name.split("[|]"); // Split on space and bar
			// System.out.println("Residue: "+name+".  Chain = "+split1[0]+", Mutation = "+split1[1]);
			if (split1.length != 2) {
				taskMonitor.showMessage(TaskMonitor.Level.WARN, "Malformed identifier: '"+name+"'?  Can't find '|'.");
				// System.out.println("split1.length = "+split1.length);
				// System.out.println("split1[0] = "+split1[0]);
				continue;
			}
			// split1[0] has the chain
			// split1[1] has the residue and mutation
			network.getRow(node).set(CHAIN_COLUMN, split1[0]);
			network.getRow(node).set(MUT_COLUMN, split1[1]);
			String res = ModelUtils.formatResidues(split1[1], network, node, MUT_TYPE_COLUMN);
			// System.out.println("res = "+res);
			// System.out.println("Primary chain = "+manager.getPrimaryChain(split1[0]));
			network.getRow(node).set(RESIDUE_COLUMN, pdbId+"#"+res+"."+manager.getPrimaryChain(split1[0]));
			
			/*
			// split1[0] has the chain and gene information.  Split it further on space
			String[] split2 = split1[0].split(" ");
			if (split2.length != 3) {
				taskMonitor.showMessage(TaskMonitor.Level.WARN, "Malformed identifier: '"+name+"'?");
				// System.out.println("split2.length = "+split2.length);
				continue;
			}
	
			network.getRow(node).set(GENE_COLUMN, split2[0]);
			network.getRow(node).set(MUT_COLUMN, split1[1]);
			String[] chRes = split2[2].split("-");
			network.getRow(node).set(CHAIN_COLUMN, chRes[0]);
			network.getRow(node).set(ID_COLUMN, chRes[1]);
			String res = ModelUtils.formatResidues(split1[1], network, node, MUT_TYPE_COLUMN);
			network.getRow(node).set(RESIDUE_COLUMN, pdbId+"#"+res+"."+manager.getPrimaryChain(chRes[0]));
			*/
			// System.out.println("Created columns for "+name);
			count++;
		}

		taskMonitor.showMessage(TaskMonitor.Level.INFO,"Created columns for "+count+" nodes");
	}

	/**
	 * Given a list of genes, return all of the mutations for those genes
	 */
	public static List<CyNode> getMutations(CyNetwork net, List<CyNode> genes) {
		Set<CyNode> mutations = new HashSet<CyNode>();
		for (CyNode node: genes) {
			List<CyNode> muts = net.getNeighborList(node, CyEdge.Type.ANY);
			if (muts != null && muts.size() > 0)
				mutations.addAll(muts);
		}
		return new ArrayList<CyNode>(mutations);
	}

	/**
	 * Given a list of genes, return all of the interactions (edges)
	 */
	public static List<CyEdge> getInteractions(CyNetwork net, List<CyNode> genes) {
		List<CyEdge> interactions = new ArrayList<CyEdge>();
		for (CyNode node: genes) {
			List<CyEdge> gis = net.getAdjacentEdgeList(node, CyEdge.Type.ANY);
			if (gis != null && gis.size() > 0)
				interactions.addAll(gis);
		}
		return interactions;
	}


	public static void createColumn(CyTable table, String column, Class<?> clazz) {
		if (table.getColumn(column) == null) {
			table.createColumn(column, clazz, false);
		}
		return;
	}

	public static String formatResidues(String mutation, CyNetwork network, CyNode node, String typeColumn) {
		if (mutation.startsWith("del")) {
			// We have a deletion range -- return it
			network.getRow(node).set(typeColumn, DELETION);
			return mutation.substring(4,mutation.length());
		}

		// Otherwise, the first character is the residue type and the last character is the mutation
		network.getRow(node).set(typeColumn, "");
		String res = mutation.substring(1,mutation.length()-1);
		if (res.indexOf(",") > 0) {
			network.getRow(node).set(typeColumn, MULTIPLE);
		} else {
			network.getRow(node).set(typeColumn, SINGLE);
		}
		return res;
	}

	public static void copyColumns(CyTable fromTable, CyTable toTable) {
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

	public static void copyRow(CyTable fromTable, CyTable toTable, CyIdentifiable fromCyId, CyIdentifiable toCyId,
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

	public static void nameEdge(CyNetwork network, CyEdge edge, String interaction) {
		String sourceName = getName(network, edge.getSource());
		String targetName = getName(network, edge.getTarget());
		network.getRow(edge).set(CyNetwork.NAME, sourceName+" ("+interaction+") "+targetName);
	}

	public static String getName(CyNetwork network, CyIdentifiable cyId) {
		return network.getRow(cyId).get(CyNetwork.NAME, String.class);
	}
}
