package edu.ucsf.rbvi.stEMAP.internal.tasks;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyTable;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.ContainsTunables;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.Tunable;

import edu.ucsf.rbvi.stEMAP.internal.model.StEMAPManager;

public class CreateRINTask extends AbstractTask {
	final StEMAPManager manager;

	@Tunable (description="Add any special Chimera commands")
	public String extraCommands = "";

	@ContainsTunables
	public PDBFile pdbFile = null;

	public CreateRINTask(final StEMAPManager manager) {
		this.manager = manager;
		extraCommands = manager.getChimeraCommands();
		if (manager.usePDBFile())
			pdbFile = new PDBFile(manager);

	}

	public void run(TaskMonitor taskMonitor) {
		taskMonitor.setTitle("Creating RIN");

		if (manager.getPDB() == null && pdbFile.getPDBFile() == null) {
			taskMonitor.showMessage(TaskMonitor.Level.ERROR, "Structure map file not loaded");
			return;
		}

		String pdbFilePath = null;
		if (pdbFile != null)
			pdbFilePath = pdbFile.getPDBFile().getAbsolutePath();

		taskMonitor.showMessage(TaskMonitor.Level.ERROR, "Loading PDB structure");
		manager.loadPDB(pdbFilePath, extraCommands);

		taskMonitor.showMessage(TaskMonitor.Level.INFO, "Creating RIN");
		manager.createRIN();

	}
}

