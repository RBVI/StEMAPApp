package edu.ucsf.rbvi.stEMAP.internal.tasks;

import java.util.Collections;
import java.util.List;

import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyTableUtil;
import org.cytoscape.task.AbstractNetworkTaskFactory;
import org.cytoscape.work.TaskIterator;

import edu.ucsf.rbvi.stEMAP.internal.model.StEMAPManager;

public class BuildMergedNetworkTaskFactory extends AbstractNetworkTaskFactory {
	public final StEMAPManager manager;

	public BuildMergedNetworkTaskFactory(final StEMAPManager manager) {
		this.manager = manager;
	}

	@Override
	public TaskIterator createTaskIterator(CyNetwork network) {
		TaskIterator ti = new TaskIterator(new BuildMergedNetworkTask(manager, network));
		return ti;
	}

}
