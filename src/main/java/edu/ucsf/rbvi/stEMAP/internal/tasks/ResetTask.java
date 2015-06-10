package edu.ucsf.rbvi.stEMAP.internal.tasks;

import java.util.HashMap;

import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;

import edu.ucsf.rbvi.stEMAP.internal.model.StEMAPManager;

public class ResetTask extends AbstractTask {
	final StEMAPManager manager;
	final CyNetwork cdtNetwork;

	public ResetTask(final StEMAPManager manager, CyNetwork network) {
		this.manager = manager;
		this.cdtNetwork = network;
	}

	public void run(TaskMonitor taskMonitor) {
		taskMonitor.setTitle("Resetting");

		// Close structure
		taskMonitor.showMessage(TaskMonitor.Level.INFO, "Closing Chimera");
		manager.executeCommand("structureViz", "exit", new HashMap<String, Object>(), null);

		CyNetworkManager netManager = manager.getService(CyNetworkManager.class);

		// Delete merged network
		taskMonitor.showMessage(TaskMonitor.Level.INFO, "Deleting merged network");
		if (manager.getMergedNetwork() != null)
			netManager.destroyNetwork(manager.getMergedNetwork());

		// Delete rin
		taskMonitor.showMessage(TaskMonitor.Level.INFO, "Deleting RIN");
		if (manager.getRINNetwork() != null)
			netManager.destroyNetwork(manager.getRINNetwork());

		manager.reset();
	}
}
