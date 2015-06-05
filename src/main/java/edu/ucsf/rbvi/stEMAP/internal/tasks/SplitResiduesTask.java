package edu.ucsf.rbvi.stEMAP.internal.tasks;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyTable;
import org.cytoscape.task.AbstractNetworkTask;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.Tunable;

import edu.ucsf.rbvi.stEMAP.internal.model.StEMAPManager;
import edu.ucsf.rbvi.stEMAP.internal.model.StructureMap;

public class SplitResiduesTask extends AbstractNetworkTask {
	public final StEMAPManager manager;
	public static final String DELIMITER = "|";
	public static final String GENE_COLUMN = "StGene";
	public static final String CHAIN_COLUMN = "Chain";
	public static final String MUT_COLUMN = "Mutation";
	public static final String MUT_TYPE_COLUMN = "MutationType";
	public static final String RESIDUE_COLUMN = "Residues";
	public static final String ID_COLUMN = "ModID";

	@Tunable(description="Enter structure mapping JSON file:", params="input=true")
	public File structureMapFile;

	public SplitResiduesTask(final StEMAPManager manager, CyNetwork network) {
		super(network);
		this.manager = manager;
	}

	@Override
	public void run(TaskMonitor taskMonitor) {
		// Look for nodes with the NAME: GENE\b-\bST-RES|MUT
		taskMonitor.setTitle("Creating Chain, Mutation, and Residue columns");

		if (network == null) {
			taskMonitor.showMessage(TaskMonitor.Level.ERROR, "No network loaded");
			return;
		}

		if (structureMapFile == null) {
			taskMonitor.showMessage(TaskMonitor.Level.ERROR, "Structure map file must be specified");
			return;
		}

		// Reading structure map file
		taskMonitor.showMessage(TaskMonitor.Level.INFO, "Reading structure mapping file");

		try {
			manager.readStructureMap(structureMapFile);
		} catch (IOException ioe) {
			taskMonitor.showMessage(TaskMonitor.Level.ERROR, ioe.getMessage());
			return;
		} catch (RuntimeException e) {
			taskMonitor.showMessage(TaskMonitor.Level.ERROR, e.getMessage());
			return;
		}

		CyTable nodeTable = network.getDefaultNodeTable();
		createColumn(nodeTable, GENE_COLUMN, String.class);
		createColumn(nodeTable, CHAIN_COLUMN, String.class);
		createColumn(nodeTable, MUT_COLUMN, String.class);
		createColumn(nodeTable, MUT_TYPE_COLUMN, String.class);
		createColumn(nodeTable, ID_COLUMN, String.class); // Should this be a list?
		createColumn(nodeTable, RESIDUE_COLUMN, String.class); // Should this be a list?

		int count = 0;

		String pdbId = manager.getPDB();

		for (CyNode node: network.getNodeList()) {
			String name = network.getRow(node).get(CyNetwork.NAME, String.class);
			if (name.indexOf("|") < 0)
				continue;
			String[] split1 = name.split("[|]"); // Split on space and bar
			if (split1.length != 2) {
				taskMonitor.showMessage(TaskMonitor.Level.WARN, "Malformed identifier: '"+name+"'?  Can't find '|'.");
				// System.out.println("split1.length = "+split1.length);
				// System.out.println("split1[0] = "+split1[0]);
				continue;
			}
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
			String res = formatResidues(split1[1], node, MUT_TYPE_COLUMN);
			network.getRow(node).set(RESIDUE_COLUMN, pdbId+"#"+res+"."+manager.getChain(chRes[0]));
			// System.out.println("Created columns for "+name);
			count++;
		}

		taskMonitor.showMessage(TaskMonitor.Level.INFO,"Created columns for "+count+" nodes");

		// Note: need to map ST to Chain ID
	}

	private void createColumn(CyTable table, String column, Class<?> clazz) {
		if (table.getColumn(column) == null) {
			table.createColumn(column, clazz, false);
		}
		return;
	}

	private String formatResidues(String mutation, CyNode node, String typeColumn) {
		if (mutation.indexOf("del") > 0) {
			// We have a deletion range -- return it
			network.getRow(node).set(typeColumn, "del");
			return mutation.substring(5,mutation.length()-1);
		}

		// Otherwise, the first character is the residue type and the last character is the mutation
		network.getRow(node).set(typeColumn, "");
		String res = mutation.substring(1,mutation.length()-1);
		if (res.indexOf(",") > 0) {
			network.getRow(node).set(typeColumn, "multiple");
		} else {
			network.getRow(node).set(typeColumn, "single");
		}
		return res;
	}
}
