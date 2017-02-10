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
import org.cytoscape.application.swing.CytoPanelComponent;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.session.CySession;
import org.cytoscape.session.events.SessionAboutToBeSavedEvent;
import org.cytoscape.session.events.SessionAboutToBeSavedListener;
import org.cytoscape.session.events.SessionLoadedEvent;
import org.cytoscape.session.events.SessionLoadedListener;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.work.SynchronousTaskManager;
import org.cytoscape.work.Task;
import org.cytoscape.work.TaskIterator;

import edu.ucsf.rbvi.stEMAP.internal.utils.ModelUtils;
import edu.ucsf.rbvi.stEMAP.internal.tasks.ShowResultsPanel;
import edu.ucsf.rbvi.stEMAP.internal.tasks.ShowResultsPanelFactory;

public class SessionListener implements SessionAboutToBeSavedListener, SessionLoadedListener {
	StEMAPManager manager;
	final Logger logger = Logger.getLogger(CyUserLog.NAME);
	final static String PDBFILE_COLUMN = "PDBFile";
	final static String MAPFILE_COLUMN = "MAPFile";
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
		CyNetwork mergedNetwork = null;
		for (CyNetwork network: session.getNetworks()) {
			if (network.getDefaultNetworkTable().getColumn(StEMAPManager.CDT_NETWORK) == null)
				continue;
			Long cdtSUID = network.getRow(network).get(StEMAPManager.CDT_NETWORK, Long.class);
			if (cdtSUID == null)
				continue;

			CyNetworkManager netManager = manager.getService(CyNetworkManager.class);
			CyNetwork cdtNetwork = netManager.getNetwork(cdtSUID);
			manager.setCDTNetwork(cdtNetwork);

			// OK, we've found the merged network
			mergedNetwork = network;
			manager.setMergedNetwork(network);
			Double min = network.getRow(network).get(ModelUtils.MIN_WEIGHT_COLUMN, Double.class);
			Double max = network.getRow(network).get(ModelUtils.MAX_WEIGHT_COLUMN, Double.class);
			if (min != null)
				manager.setMinWeight(min);
			if (max != null)
				manager.setMaxWeight(max);

			// Get mergedNetworkView
			for (CyNetworkView view: session.getNetworkViews()) {
				if (view.getModel().equals(network)) {
					manager.setMergedNetworkView(view);
				}
			}
			break;
		}

		Map<String, List<File>> appFileMap = e.getLoadedSession().getAppFileListMap();
		boolean showSidePanel = true;
		if (appFileMap != null && appFileMap.containsKey("StEMAPApp")) {
			List<File> fileList = appFileMap.get("StEMAPApp");
			File pdbFile = null;
			File mapFile = null;
			for (File file: fileList) {
				if (file.getAbsolutePath().endsWith(".json")) {
					try {
						mapFile = file;
						manager.readStructureMap(file);
					} catch (Exception ioe) {
						logger.error("Unable to read structure map: "+ioe.getMessage());
						return;
					}

				} else if (file.getAbsolutePath().endsWith(".pdb")) {
					pdbFile = file;
				}
			}
			System.out.println("Loading pdb file");
			if (manager.usePDBFile() && pdbFile != null) {
				manager.loadPDB(pdbFile.getAbsolutePath(), manager.getChimeraCommands());
			} else {
				manager.loadPDB(manager.getPDB(), manager.getChimeraCommands());
			}

			// Save for a possible future "reset" operation
			ModelUtils.createColumn(mergedNetwork.getDefaultNetworkTable(), PDBFILE_COLUMN, String.class);
			ModelUtils.createColumn(mergedNetwork.getDefaultNetworkTable(), MAPFILE_COLUMN, String.class);
			mergedNetwork.getRow(mergedNetwork).set(PDBFILE_COLUMN, pdbFile.getAbsolutePath());
			mergedNetwork.getRow(mergedNetwork).set(MAPFILE_COLUMN, mapFile.getAbsolutePath());
		} else if (mergedNetwork != null) {
			// This may be a reset.  See if we have paths
			String pdbFilePath = mergedNetwork.getRow(mergedNetwork).get(PDBFILE_COLUMN, String.class);
			String mapFilePath = mergedNetwork.getRow(mergedNetwork).get(MAPFILE_COLUMN, String.class);
			try {
				manager.readStructureMap(new File(mapFilePath));
				manager.loadPDB(pdbFilePath, manager.getChimeraCommands());
			} catch (Exception ioe) {
				logger.error("Unable to read files: "+ioe.getMessage());
			}
		} else {
			showSidePanel = false;
		}

		if (showSidePanel) {
			try {
				// Synchronize our colors
				Map<String, Object> args = new HashMap<>();
				args.put("chimeraToCytoscape","true");
				args.put("cytoscapeToChimera","false");
				manager.executeCommand("structureViz", "syncColors", args, null);
			} catch (Exception ex) {
				logger.error("Unable to syncrhonize colors: "+ex.getMessage());
			}

			try {
				ShowResultsPanelFactory showResults = manager.getService(ShowResultsPanelFactory.class);
				ShowResultsPanel panel = new ShowResultsPanel(manager, showResults, true);
				// Clean up step
				panel.hidePanel();
				// Show the panel
				panel.run(null);
			} catch (Exception ex) {
				logger.error("Unable to launch results panel: "+ex.getMessage());
			}
		}

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
