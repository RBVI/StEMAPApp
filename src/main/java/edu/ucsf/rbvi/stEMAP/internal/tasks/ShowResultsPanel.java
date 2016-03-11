package edu.ucsf.rbvi.stEMAP.internal.tasks;

import java.awt.Color;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.cytoscape.application.swing.CytoPanel;
import org.cytoscape.application.swing.CytoPanelComponent;
import org.cytoscape.application.swing.CytoPanelName;
import org.cytoscape.application.swing.CytoPanelState;
import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;

import edu.ucsf.rbvi.stEMAP.internal.model.StEMAPManager;
import edu.ucsf.rbvi.stEMAP.internal.view.ResultsPanel;

public class ShowResultsPanel extends AbstractTask {
	final StEMAPManager manager;
	final ShowResultsPanelFactory factory;
	final boolean show;

	public ShowResultsPanel(final StEMAPManager manager, final ShowResultsPanelFactory factory, final boolean show) {
		this.manager = manager;
		this.factory = factory;
		this.show = show;
	}

	public void run(TaskMonitor taskMonitor) {
		CySwingApplication swingApplication = manager.getService(CySwingApplication.class);
		CytoPanel cytoPanel = swingApplication.getCytoPanel(CytoPanelName.EAST);

		if (show) {
			factory.unregister();
			ResultsPanel panel = new ResultsPanel(manager);
			manager.registerService(panel, CytoPanelComponent.class, new Properties());
			manager.setResultsPanel(panel);
			if (cytoPanel.getState() == CytoPanelState.HIDE)
				cytoPanel.setState(CytoPanelState.DOCK);

			factory.register();
		} else {
			factory.unregister();
			manager.unregisterService(manager.getResultsPanel(), CytoPanelComponent.class);
			manager.setResultsPanel(null);
			if (cytoPanel.getCytoPanelComponentCount() == 0)
				cytoPanel.setState(CytoPanelState.HIDE);

			factory.register();
		}
	}
}
