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

public class ShowResultsPanelFactory extends AbstractNetworkViewTaskFactory {
	private final StEMAPManager manager;
	boolean show = true;

	public ShowResultsPanelFactory(final StEMAPManager manager) {
		this.manager = manager;
	}

	@Override
	public TaskIterator createTaskIterator(CyNetworkView networkView) {
		return new TaskIterator(new ShowResultsPanel(manager, this, show));
	}

	@Override
	public boolean isReady(CyNetworkView networkView) {
		if (networkView == null) return false;
		return true;
	}

	public void unregister() {
		manager.unregisterService(this, NetworkViewTaskFactory.class);
	}

	public void register() {
		Properties props = new Properties();
		props.setProperty(PREFERRED_MENU, "Apps.stEMAP");
		props.setProperty(MENU_GRAVITY, "4.0");
		props.setProperty(ENABLE_FOR, "networkView");
		if (manager.getResultsPanel() == null) {
			props.setProperty(TITLE, "Show results panel");
			show = true;
		} else {
			props.setProperty(TITLE, "Hide results panel");
			show = false;
		}
		manager.registerService(this, NetworkViewTaskFactory.class, props);
	}

}
