package edu.ucsf.rbvi.stEMAP.internal.model;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import org.cytoscape.application.CyUserLog;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.session.CySession;
import org.cytoscape.session.events.SessionAboutToBeSavedEvent;
import org.cytoscape.session.events.SessionAboutToBeSavedListener;
import org.cytoscape.session.events.SessionLoadedEvent;
import org.cytoscape.session.events.SessionLoadedListener;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.work.SynchronousTaskManager;
import org.cytoscape.work.Task;
import org.cytoscape.work.TaskIterator;

import edu.ucsf.rbvi.stEMAP.internal.tasks.ShowResultsPanelFactory;

public class SessionListener implements SessionAboutToBeSavedListener, SessionLoadedListener {
	StEMAPManager manager;
	final Logger logger = Logger.getLogger(CyUserLog.NAME);
	public SessionListener(StEMAPManager manager) {
		this.manager = manager;
	}

	public void handleEvent(SessionAboutToBeSavedEvent e) {
		List<File> appFiles = new ArrayList<>();
		// Create a copy of the JSON file
		File jsonFile = manager.getMapFile();
		appFiles.add(jsonFile);

		// Create a copy of the PDB file (if there is one)
		if (manager.usePDBFile()) {
			File pdbFile = manager.getPDBFile();
			appFiles.add(pdbFile);
		}
		try {
			e.addAppFiles("StEMAPApp", appFiles);
		} catch (Exception ioe) {
			logger.error("Unable to add files to session: "+ioe.getMessage());
		}
	}

	public void handleEvent(SessionLoadedEvent e) {
		// Get mergedNetwork
		CySession session = e.getLoadedSession();
		for (CyNetwork network: session.getNetworks()) {
			if (network.getDefaultNetworkTable().getColumn(StEMAPManager.CDT_NETWORK) == null)
				continue;
			if (network.getRow(network).get(StEMAPManager.CDT_NETWORK, Long.class) == null)
				continue;

			// OK, we've found the merged network
			manager.setMergedNetwork(network);
			// Get mergedNetworkView
			for (CyNetworkView view: session.getNetworkViews()) {
				if (view.getModel().equals(network)) {
					manager.setMergedNetworkView(view);
				}
			}
			break;
		}

		Map<String, List<File>> appFileMap = e.getLoadedSession().getAppFileListMap();
		if (appFileMap != null && appFileMap.containsKey("StEMAPApp")) {
			List<File> fileList = appFileMap.get("StEMAPApp");
			File pdbFile = null;
			for (File file: fileList) {
				if (file.getAbsolutePath().endsWith(".json")) {
					try {
						manager.readStructureMap(file);
					} catch (Exception ioe) {
						logger.error("Unable to read structure map: "+ioe.getMessage());
						return;
					}

				} else if (file.getAbsolutePath().endsWith(".pdb")) {
					pdbFile = file;
				}
			}
			if (manager.usePDBFile() && pdbFile != null) {
				manager.loadPDB(pdbFile.getAbsolutePath(), manager.getChimeraCommands());
			} else {
				manager.loadPDB(manager.getPDB(), manager.getChimeraCommands());
			}
		}

		// Synchronize our colors
		Map<String, Object> args = new HashMap<>();
		args.put("chimeraToCytoscape","true");
		args.put("cytoscapeToChimera","false");
		manager.executeCommand("structureViz", "syncColors", args, null);

		manager.setResultsPanel(null);

		// Finally, open up our results panel
		ShowResultsPanelFactory showResults = manager.getService(ShowResultsPanelFactory.class);
		showResults.unregister();
		showResults.register();
	
		SynchronousTaskManager taskManager = manager.getService(SynchronousTaskManager.class);
		TaskIterator ti = showResults.createTaskIterator(manager.getMergedNetworkView());
		try {
			while (ti.hasNext())
				ti.next().run(null);
		} catch(Exception tie) { }
	}

	private void copyFile(File from, File to) throws IOException {
		BufferedReader input = new BufferedReader(new FileReader(from));
		BufferedWriter output = new BufferedWriter(new FileWriter(to));
		String line;
		while ((line = input.readLine()) != null) {
			output.write(line);
			output.newLine();
		}
		input.close();
		output.close();
	}
}
