package edu.ucsf.rbvi.stEMAP.internal.tasks;

import org.cytoscape.model.CyNetwork;
import org.cytoscape.work.AbstractTaskFactory;
import org.cytoscape.work.TaskIterator;

import edu.ucsf.rbvi.stEMAP.internal.model.StEMAPManager;

public class MergeTaskFactory extends AbstractTaskFactory {
	public final StEMAPManager manager;

	public MergeTaskFactory(final StEMAPManager manager) {
		this.manager = manager;
	}

	@Override
	public TaskIterator createTaskIterator() {
		TaskIterator ti = new TaskIterator(new MergeTask(manager));
		return ti;
	}

	public boolean isReady() {
		if (manager.getPDB() == null)
			return false;
		return true;
	}
}
