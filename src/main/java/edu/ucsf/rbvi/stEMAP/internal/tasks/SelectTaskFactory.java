package edu.ucsf.rbvi.stEMAP.internal.tasks;

import java.util.Collections;
import java.util.List;

import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyTableUtil;
import org.cytoscape.task.AbstractNodeViewTaskFactory;
import org.cytoscape.task.NetworkViewTaskFactory;
import org.cytoscape.task.NetworkTaskFactory;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.View;
import org.cytoscape.work.TaskIterator;

import edu.ucsf.rbvi.stEMAP.internal.model.StEMAPManager;

public class SelectTaskFactory extends AbstractNodeViewTaskFactory
                               implements NetworkViewTaskFactory, NetworkTaskFactory {
	public final StEMAPManager manager;

	public SelectTaskFactory(final StEMAPManager manager) {
		this.manager = manager;
	}

	@Override
	public TaskIterator createTaskIterator(View<CyNode> nodeView, CyNetworkView networkView) {
		TaskIterator ti = new TaskIterator(new SelectTask(manager, 
		                                                  Collections.singletonList(nodeView.getModel()), 
		                                                  networkView.getModel()));
		return ti;
	}

	@Override
	public boolean isReady(View<CyNode> nodeView, CyNetworkView networkView) {
		if (networkView == null || manager.getPDB() == null)
			return false;
		return true;
	}

	@Override
	public TaskIterator createTaskIterator(CyNetworkView networkView) {
		return createTaskIterator(networkView.getModel());
	}

	@Override
	public boolean isReady(CyNetworkView networkView) {
		if (networkView == null) return false;
		return isReady(networkView.getModel());
	}

	@Override
	public TaskIterator createTaskIterator(CyNetwork network) {
		List<CyNode> selectedNodes = CyTableUtil.getNodesInState(network, CyNetwork.SELECTED, true);
		TaskIterator ti = new TaskIterator(new SelectTask(manager, selectedNodes, network));
		return ti;
	}

	@Override
	public boolean isReady(CyNetwork network) {
		if (network == null || manager.getPDB() == null)
			return false;
		if (CyTableUtil.getNodesInState(network, CyNetwork.SELECTED, true) == null)
			return false;
		return true;
	}

}
