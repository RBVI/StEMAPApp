package edu.ucsf.rbvi.stEMAP.internal.tasks;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cytoscape.event.CyEventHelper;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.session.CySession;
import org.cytoscape.session.CySessionManager;
import org.cytoscape.session.events.SessionLoadedEvent;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;

import edu.ucsf.rbvi.stEMAP.internal.model.SessionListener;
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
		taskMonitor.showMessage(TaskMonitor.Level.INFO, "Closing Model");
		Map<String, Object> args = new HashMap<String, Object>();
		args.put("atomSpec", "#1");
		manager.executeCommand("structureViz", "close", args, null);

		manager.reset();

		taskMonitor.showMessage(TaskMonitor.Level.INFO, "Resetting session");

		try {
		// Now pretend we just opened the session
		CySessionManager sessionManager = manager.getService(CySessionManager.class);
		CySession thisSession = sessionManager.getCurrentSession();
		String sessionFile = sessionManager.getCurrentSessionFileName();
		SessionLoadedEvent loaded = new SessionLoadedEvent(sessionManager, thisSession, sessionFile);
		SessionListener listener = new SessionListener(manager);
		listener.handleEvent(loaded);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
