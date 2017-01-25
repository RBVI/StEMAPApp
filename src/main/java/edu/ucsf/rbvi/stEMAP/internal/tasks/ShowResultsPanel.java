package edu.ucsf.rbvi.stEMAP.internal.tasks;

import java.awt.Color;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import javax.swing.SwingUtilities;

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
	CySwingApplication swingApplication = null;
	CytoPanel cytoPanel = null;

	public ShowResultsPanel(final StEMAPManager manager, final ShowResultsPanelFactory factory, final boolean show) {
		this.manager = manager;
		this.factory = factory;
		this.show = show;
		this.swingApplication = manager.getService(CySwingApplication.class);
		this.cytoPanel = swingApplication.getCytoPanel(CytoPanelName.EAST);
	}

	public void run(TaskMonitor taskMonitor) {
		if (show) {
			factory.unregister();
			showPanel();
		} else {
			factory.unregister();
			hidePanel();
			factory.register();
		}
	}

	public void showPanel() {
		Show actualShow = new Show();
		if (SwingUtilities.isEventDispatchThread()) {
			actualShow.run();
		} else {
			SwingUtilities.invokeLater(actualShow);
		}
	}

	public void hidePanel() {
		try {
			manager.unregisterService(manager.getResultsPanel(), CytoPanelComponent.class);
		} catch (Exception e) {
			// We assume that we just hadn't registered it, yet
			// e.printStackTrace();
		}
		manager.setResultsPanel(null);
		if (cytoPanel.getCytoPanelComponentCount() == 0)
			cytoPanel.setState(CytoPanelState.HIDE);
	}

	private class Show implements Runnable {
		public void run() {
			ResultsPanel panel = new ResultsPanel(manager);
			manager.registerService(panel, CytoPanelComponent.class, new Properties());
			manager.setResultsPanel(panel);
			cytoPanel.setState(CytoPanelState.DOCK);
			factory.register();
		}
	}
}
