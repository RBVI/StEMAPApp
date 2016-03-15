package edu.ucsf.rbvi.stEMAP.internal.tasks;

import java.util.Collections;
import java.util.List;
import java.util.Properties;

import org.cytoscape.model.CyNetwork;
import org.cytoscape.task.AbstractNetworkViewTaskFactory;
import org.cytoscape.task.NetworkViewTaskFactory;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.work.TaskIterator;

import static org.cytoscape.work.ServiceProperties.ENABLE_FOR;
import static org.cytoscape.work.ServiceProperties.IN_MENU_BAR;
import static org.cytoscape.work.ServiceProperties.MENU_GRAVITY;
import static org.cytoscape.work.ServiceProperties.PREFERRED_MENU;
import static org.cytoscape.work.ServiceProperties.TITLE;

import edu.ucsf.rbvi.stEMAP.internal.model.StEMAPManager;
import edu.ucsf.rbvi.stEMAP.internal.view.ResultsPanel;

public class UpdateResultsPanelFactory extends AbstractNetworkViewTaskFactory {
	private final StEMAPManager manager;

	public UpdateResultsPanelFactory(final StEMAPManager manager) {
		this.manager = manager;
	}

	@Override
	public TaskIterator createTaskIterator(CyNetworkView networkView) {
		return new TaskIterator(new UpdateResultsPanel(manager));
	}

	@Override
	public boolean isReady(CyNetworkView networkView) {
		if (networkView == null || manager.getResultsPanel() == null) return false;
		return true;
	}

}
