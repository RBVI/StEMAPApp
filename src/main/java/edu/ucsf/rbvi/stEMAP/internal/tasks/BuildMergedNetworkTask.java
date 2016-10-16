package edu.ucsf.rbvi.stEMAP.internal.tasks;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.ContainsTunables;
import org.cytoscape.work.ProvidesTitle;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.Tunable;

import edu.ucsf.rbvi.stEMAP.internal.model.StEMAPManager;
import edu.ucsf.rbvi.stEMAP.internal.utils.ModelUtils;
import edu.ucsf.rbvi.stEMAP.internal.utils.ModelUtils.NodeType;

public class BuildMergedNetworkTask extends AbstractTask {
	final StEMAPManager manager;
	final CyNetwork cdtNetwork;

	@Tunable(description="Select structure mapping JSON file:", params="input=true", gravity=1.0)
	public File structureMapFile;

	@Tunable(description="Select the PDB file:", params="input=true", gravity=2.0)
	public File pdbFile;

	public BuildMergedNetworkTask(final StEMAPManager manager, CyNetwork network) {
		this.manager = manager;
		this.cdtNetwork = network;
	}

	public void run(TaskMonitor taskMonitor) {
		taskMonitor.setTitle("Building merged network");

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

		ModelUtils.splitResidues(taskMonitor, manager, cdtNetwork);

		String pdbFilePath = null;

		if (manager.getPDB() == null && pdbFile == null) {
			taskMonitor.showMessage(TaskMonitor.Level.ERROR, "Structure map file not loaded");
			return;
		}

		if (pdbFile != null)
			pdbFilePath = pdbFile.getAbsolutePath();

		taskMonitor.showMessage(TaskMonitor.Level.INFO, "Loading PDB structure");
		manager.loadPDB(pdbFilePath, manager.getChimeraCommands());

		taskMonitor.showMessage(TaskMonitor.Level.INFO, "Creating RIN");
		manager.createRIN();

		CyNetwork rinNetwork = manager.getRINNetwork();
		MergeTask mergeTask = new MergeTask(manager);

		mergeTask.merge(taskMonitor, cdtNetwork, rinNetwork);

		ShowResultsPanelFactory showResults = manager.getService(ShowResultsPanelFactory.class);
		insertTasksAfterCurrentTask(showResults.createTaskIterator(manager.getMergedNetworkView()));
	}

	@ProvidesTitle
	public String getTitle() { return "Merged Network Parameters"; }
}
