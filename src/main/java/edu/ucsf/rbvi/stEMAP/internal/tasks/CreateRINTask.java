package edu.ucsf.rbvi.stEMAP.internal.tasks;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.cytoscape.command.CommandExecutorTaskFactory;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyTable;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.FinishStatus;
import org.cytoscape.work.ObservableTask;
import org.cytoscape.work.SynchronousTaskManager;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.TaskObserver;
import org.cytoscape.work.Tunable;

import edu.ucsf.rbvi.stEMAP.internal.model.StEMAPManager;

public class CreateRINTask extends AbstractTask implements TaskObserver {
	final StEMAPManager manager;
	String modelName = null;
	int modelNumber = -1;

	@Tunable (description="Add any special Chimera commands")
	public String extraCommands = "";

	public CreateRINTask(final StEMAPManager manager) {
		this.manager = manager;
		extraCommands = manager.getChimeraCommands();
	}

	public void run(TaskMonitor taskMonitor) {
		taskMonitor.setTitle("Creating RIN");
		SynchronousTaskManager taskManager = manager.getService(SynchronousTaskManager.class);
		CommandExecutorTaskFactory commandTaskFactory = manager.getService(CommandExecutorTaskFactory.class);

		if (manager.getPDB() == null) {
			taskMonitor.showMessage(TaskMonitor.Level.ERROR, "Structure map file not loaded");
			return;
		}

		taskMonitor.showMessage(TaskMonitor.Level.INFO, "Opening PDB file: "+manager.getPDB());
		Map<String, Object> args = new HashMap<>();
		args.put("pdbID", manager.getPDB());
		TaskIterator ti = commandTaskFactory.createTaskIterator("structureViz", "open", args, this);
		taskManager.execute(ti);

		if (extraCommands != null) {
			taskMonitor.showMessage(TaskMonitor.Level.INFO, "Executing command(s): "+extraCommands);
			// We want to rotate the structure
			args = new HashMap<>();
			args.put("command", extraCommands);
			ti = commandTaskFactory.createTaskIterator("structureViz", "send", args, null);
			taskManager.execute(ti);
		}

		// Bring up the dialog
		args = new HashMap<>();
		ti = commandTaskFactory.createTaskIterator("structureViz", "showDialog", args, null);
		taskManager.execute(ti);

		// Get all of the current networks
		CyNetworkManager netManager = manager.getService(CyNetworkManager.class);
		Set<CyNetwork> allNetworks = netManager.getNetworkSet();

		// Now, create the RIN
		taskMonitor.showMessage(TaskMonitor.Level.INFO, "Creating RIN");
		// First, select the model
		args = new HashMap<>();
		args.put("command", "sel #"+modelNumber);
		ti = commandTaskFactory.createTaskIterator("structureViz", "send", args, null);
		taskManager.execute(ti);

		try {
			// Wait for things to process
			Thread.sleep(500);
		} catch (Exception e) {}

		args = new HashMap<>();
		ti = commandTaskFactory.createTaskIterator("structureViz", "createRIN", args, null);
		taskManager.execute(ti);

		// Now, figure out which network is new
		Set<CyNetwork> newNetworks = netManager.getNetworkSet();
		for (CyNetwork newNetwork: newNetworks) {
			if (!allNetworks.contains(newNetwork)) {
				manager.setRINNetwork(newNetwork);
				break;
			}
		}

	}

	public void allFinished(FinishStatus finishStatus) {}

	public void taskFinished(ObservableTask task) {
		String models = task.getResults(String.class);
		int offset = models.indexOf(' ');
		String model = models.substring(1, offset);
		modelName = new String(models.substring(offset+1, models.length()-1));

		try {
			modelNumber = Integer.parseInt(model);
 		} catch (Exception e) {}
	}
}

