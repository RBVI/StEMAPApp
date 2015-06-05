package edu.ucsf.rbvi.stEMAP.internal.tasks;

import org.cytoscape.model.CyNetwork;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.task.AbstractNetworkTaskFactory;
import org.cytoscape.work.TaskIterator;

import edu.ucsf.rbvi.stEMAP.internal.model.StEMAPManager;

public class SplitResiduesTaskFactory extends AbstractNetworkTaskFactory {
	public final StEMAPManager manager;

	public SplitResiduesTaskFactory(final StEMAPManager manager) {
		this.manager = manager;
	}

	@Override
	public TaskIterator createTaskIterator(CyNetwork network) {
		TaskIterator ti = new TaskIterator(new SplitResiduesTask(manager, network));
		return ti;
	}
}
