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
import edu.ucsf.rbvi.stEMAP.internal.utils.ModelUtils;

public class SplitResiduesTask extends AbstractNetworkTask {
	public final StEMAPManager manager;

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

		ModelUtils.splitResidues(taskMonitor, manager, network);
	}
}
