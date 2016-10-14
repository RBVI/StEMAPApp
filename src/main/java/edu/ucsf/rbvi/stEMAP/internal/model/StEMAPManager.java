package edu.ucsf.rbvi.stEMAP.internal.model;

import java.awt.Color;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.cytoscape.command.CommandExecutorTaskFactory;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.model.CyNode;
import org.cytoscape.event.CyEventHelper;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.View;
import org.cytoscape.view.presentation.property.BasicVisualLexicon;
import org.cytoscape.work.SynchronousTaskManager;
import org.cytoscape.work.FinishStatus;
import org.cytoscape.work.ObservableTask;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.TaskObserver;

import edu.ucsf.rbvi.stEMAP.internal.view.ResultsPanel;
import edu.ucsf.rbvi.stEMAP.internal.utils.ModelUtils;
import edu.ucsf.rbvi.stEMAP.internal.utils.ModelUtils.NodeType;

public class StEMAPManager implements TaskObserver { 
	final CyServiceRegistrar serviceRegistrar;
	final CyEventHelper eventHelper;
	public static final String RIN_NETWORK = "RINNetwork.SUID";
	public static final String CDT_NETWORK = "CDTNetwork.SUID";
	CommandExecutorTaskFactory commandTaskFactory = null;
	SynchronousTaskManager<?> taskManager = null;

	Set<CyNode> selectedGenes;
	Set<CyNode> selectedMutations;

	// State variables
	CyNetwork rinNetwork = null;
	CyNetwork mergedNetwork = null;
	CyNetworkView mergedNetworkView = null;
	StructureMap map = null;
	String modelName = null;
	int modelNumber = -1;
	String lastResidues = null;
	ResultsPanel currentResultsPanel = null;

	File mapFile = null;
	File pdbFile = null;

	public StEMAPManager(final CyServiceRegistrar cyRegistrar) {
		this.serviceRegistrar = cyRegistrar;
		this.eventHelper = serviceRegistrar.getService(CyEventHelper.class);
		selectedGenes = ConcurrentHashMap.newKeySet();
		selectedMutations = ConcurrentHashMap.newKeySet();
	}

	public void readStructureMap(File mapFile) throws IOException {
		map = new StructureMap(mapFile);
		if (map != null) {
			this.mapFile = mapFile;
		}
	}

	public File getMapFile() { return mapFile; }
	public File getPDBFile() { return pdbFile; }

	public String getPDB() {
		if (map == null) return null;
		return map.getPDB();
	}

	public boolean usePDBFile() {
		if (map == null) return false;
		return map.usePDBFile();
	}

	public String getPDBFileName() {
		if (map == null) return null;
		return map.getPDBFileName();
	}

	public String getChimeraCommands() {
		if (map == null) return null;
		return map.getChimeraCommands();
	}

	public String getPrimaryChain(String chain) {
		if (map == null) return null;
		return map.getPrimaryChain(chain);
	}

	public List<String> getDuplicateChains(String chain) {
		return map.getDuplicateChains(chain);
	}

	public double getPositiveCutoff() {
		return map.getPositiveCutoff();
	}

	public double getNegativeCutoff() {
		return map.getNegativeCutoff();
	}

	public void setRINNetwork(CyNetwork net) {
		rinNetwork = net;
	}

	public CyNetwork getRINNetwork() {
		return rinNetwork;
	}

	public void setMergedNetwork(CyNetwork network) {
		mergedNetwork = network;
	}

	public void setMergedNetworkView(CyNetworkView networkView) {
		mergedNetworkView = networkView;
	}

	public CyNetwork getMergedNetwork() { return mergedNetwork; }
	public CyNetworkView getMergedNetworkView() { return mergedNetworkView; }

	public ResultsPanel getResultsPanel() { return currentResultsPanel; }
	public void setResultsPanel(ResultsPanel panel) { 
		currentResultsPanel = panel; 
		if (panel == null) {
			selectedGenes.clear();
			selectedMutations.clear();
		}
	}

	public void reset() {
		mergedNetwork = null;
		rinNetwork = null;
		lastResidues = null;
		modelNumber = -1;
		modelName = null;
		selectedGenes.clear();
		selectedMutations.clear();
	}

	public void selectGeneOrMutation(CyNode node, Boolean select) {
		NodeType type = getNodeType(mergedNetwork, node);
		if (type.equals(NodeType.GENE)) {
			if (select)
				selectedGenes.add(node);
			else
				selectedGenes.remove(node);
		} else if (type.equals(NodeType.MUTATION) || type.equals(NodeType.MULTIMUTATION)) {
			if (select)
				selectedMutations.add(node);
			else
				selectedMutations.remove(node);
		}
	}

	public Set<CyNode> getSelectedGenes() { return selectedGenes; }
	public Set<CyNode> getSelectedMutations() { return selectedMutations; }

	public void orderResidues(List<CyNode> mutations) {
		List<String> nodeOrder = mergedNetwork.getRow(mergedNetwork).getList("__nodeOrder", String.class);
		Collections.sort(mutations, new ClusterSort(nodeOrder));
	}

	public void orderGenes(List<CyNode> genes) {
		// Get the order
		List<String> attrOrder = mergedNetwork.getRow(mergedNetwork).getList("__arrayOrder", String.class);
		Collections.sort(genes, new ClusterSort(attrOrder));
	}

	public NodeType getNodeType(CyNetwork network, CyNode node) {
		String mutType = network.getRow(node).get(ModelUtils.MUT_TYPE_COLUMN, String.class);
		String pdb = network.getRow(node).get(ModelUtils.PDB_COLUMN, String.class);
		if (mutType == null || mutType.length() == 0) {
			if (pdb == null || pdb.length() == 0)
				return NodeType.GENE;
			return NodeType.STRUCTURE;
		}

		if (mutType.equals("single"))
			return NodeType.MUTATION;
		return NodeType.MULTIMUTATION;
	}

	public List<CyNode> getGeneNodes(CyNetwork net, CyNode node) {
		List<CyNode> geneNodes = new ArrayList<>();

		for (CyNode neighbor: net.getNeighborList(node, CyEdge.Type.ANY)) {
			String mutType = net.getRow(neighbor).get(ModelUtils.MUT_TYPE_COLUMN, String.class);
			if (mutType == null || mutType.length() == 0) {
				String pdb = net.getRow(neighbor).get(ModelUtils.PDB_COLUMN, String.class);
				if (pdb == null || pdb.length() == 0) {
					geneNodes.add(neighbor);
				}
			}
		}
		return geneNodes;
	}

	public List<CyEdge> getConnectingResidueEdges(CyNetwork net, CyNode node) {
		List<CyEdge> edges = new ArrayList<>();
		for (CyEdge edge: net.getAdjacentEdgeList(node, CyEdge.Type.ANY)) {
			CyNode neighbor = edge.getSource();
			if (edge.getSource().equals(node)) {
				neighbor = edge.getTarget();
			}
			String pdb = net.getRow(neighbor).get(ModelUtils.PDB_COLUMN, String.class);
			if (pdb != null && pdb.length() > 0) {
				edges.add(edge);
			} else {
				String mutType = net.getRow(neighbor).get(ModelUtils.MUT_TYPE_COLUMN, String.class);
				if (mutType != null && (mutType.equals("del") || mutType.equals("multiple"))) {
					edges.add(edge);
				}
			}
		}
		return edges;
	}

	public Map<String, Color> getResiduesAndColors(CyNetworkView netView, CyNode node) {
		Map<String, Color> colorMap = new HashMap<>();

		CyNetwork net = netView.getModel();
		for (CyEdge edge: net.getAdjacentEdgeList(node, CyEdge.Type.ANY)) {
			CyNode resNode = edge.getSource();
			if (edge.getSource().equals(node)) {
				resNode = edge.getTarget();
			}
			String pdb = net.getRow(resNode).get(ModelUtils.PDB_COLUMN, String.class);
			if (pdb != null && pdb.length() > 0) {
				String residue = getResidue(net, resNode);
				View<CyEdge> ev = netView.getEdgeView(edge);
				Color c = (Color)ev.getVisualProperty(BasicVisualLexicon.EDGE_STROKE_UNSELECTED_PAINT);
				colorMap.put(residue, c);
			}
		}
		return colorMap;
	}

	public List<CyNode> getResidueNodes(CyNetwork net, CyNode node, boolean findMultiples) {
		List<CyNode> residueNodes = new ArrayList<>();
		for (CyNode neighbor: net.getNeighborList(node, CyEdge.Type.ANY)) {
			String pdb = net.getRow(neighbor).get(ModelUtils.PDB_COLUMN, String.class);
			String mutType = net.getRow(neighbor).get(ModelUtils.MUT_TYPE_COLUMN, String.class);
			if (pdb != null && pdb.length() > 0) {
				residueNodes.add(neighbor);
			} else if (findMultiples) {
				if (mutType != null && (mutType.equals("del") || mutType.equals("multiple"))) {
					List<CyNode> rn = getResidueNodes(net, neighbor, true);
					if (rn != null) residueNodes.addAll(rn);
				}
			} else if (mutType != null) {
				residueNodes.add(neighbor);
			}
		}
		return residueNodes;
	}

	public void selectNodes(CyNetwork net, List<CyNode> nodes) {
		for (CyNode node: nodes) {
			net.getRow(node).set(CyNetwork.SELECTED, true);
		}
	}

	public List<String> getResidues(CyNetwork net, List<CyNode> nodes) {
		Set<String> residues = new HashSet<>();
		for (CyNode node: nodes) {
			String residue = getResidue(net, node);
			if (residue != null)
				residues.add(residue);
		}
		return new ArrayList<String>(residues);
	}

	public String getResidue(CyNetwork net, CyNode node) {
		String pdb = net.getRow(node).get(ModelUtils.PDB_COLUMN, String.class);
		if (pdb == null || pdb.length() == 0)
			return null;
		String[] model = pdb.split("#");
		return addChains(model[1]);
	}

	public void loadPDB(String pdbPath, String extraCommands) {
		Map<String, Object> args = new HashMap<>();
		if (pdbPath != null) {
			args.put("structureFile", pdbPath);
			pdbFile = new File(pdbPath);
		} else
			args.put("pdbID", getPDB());

		args.put("showDialog", "true");
		executeCommand("structureViz", "open", args, this);

		while (modelNumber == -1) {
			try {
				// Wait for things to process
				Thread.sleep(500);
			} catch (Exception e) {}
		}

		if (modelNumber == -2) {
			return;
		}

		if (extraCommands != null) {
			args = new HashMap<>();
			args.put("command", extraCommands);
			executeCommand("structureViz", "send", args, null);
		}
	}

	public void createRIN() {
		// Get all of the current networks
		CyNetworkManager netManager = getService(CyNetworkManager.class);
		Set<CyNetwork> allNetworks = netManager.getNetworkSet();

		// First, select the model
		chimeraCommand("sel #"+getModelNumber());

		try {
			// Wait for things to process
			Thread.sleep(500);
		} catch (Exception e) {}

		Map<String, Object> args = map.getRINParameters();
		executeCommand("structureViz", "createRIN", args, null);

		// Now, figure out which network is new
		Set<CyNetwork> newNetworks = netManager.getNetworkSet();
		for (CyNetwork newNetwork: newNetworks) {
			if (!allNetworks.contains(newNetwork)) {
				setRINNetwork(newNetwork);
				break;
			}
		}
	}

	public void colorSpheres(Map<String, Color> colorMap) {
		String command = null;
		for (String residue: colorMap.keySet()) {
			String comm = colorResidue(residue, colorMap.get(residue));
			if (command == null)
				command = comm;
			else
				command = command+";"+comm;
		}
		chimeraCommand(command);
	}

	/*
	public String colorResidue(String residue, Color color) {
		double r = (double)color.getRed()/(double)255;
		double g = (double)color.getGreen()/(double)255;
		double b = (double)color.getBlue()/(double)255;
		String command = "color "+r+","+g+","+b+",a #"+modelNumber+":"+residue;
		// System.out.println("Command: "+command);
		return command;
	}
	*/
	public String colorResidue(String residue, Color color) {
		// Residue is the Cytoscape-style string of the form "nnn.a,nnn.b,...".  We need to convert
		// it to a ChimeraX list
		String xResidues = convertResiduesToX(residue);
		int r = color.getRed();
		int g = color.getGreen();
		int b = color.getBlue();
		String command = "color "+xResidues+" rgb("+r+","+g+","+b+") atoms";
		// System.out.println("Command: "+command);
		return command;
	}

	public void showSpheres(List<String> residues) {
		if (lastResidues != null) {
			// System.out.println("Sending command: ~disp "+lastResidues);
			chimeraCommand("~disp "+lastResidues);
		}
		// Make it a comma separated list
		String command = null;
		for (String r: residues) {
			if (command == null)
				command = r;
			else
				command += ","+r;
		}

		lastResidues = convertResiduesToX(command);

		// System.out.println("Sending command: sel "+lastResidues);
		// It may be redundant, but select the residues (hopefully again)
		// chimeraCommand("sel "+lastResidues);
		// System.out.println("chimera: sel "+lastResidues);

		// Change to sphere
		chimeraCommand("disp "+lastResidues);
		// System.out.println("Sending command: disp sel");
		chimeraCommand("style "+lastResidues+" sphere");
		// System.out.println("Sending command: repr sphere sel");
	}

	public String convertResiduesToX(String residue) {
		String[] resArray = residue.split(",");
		String xResidues = null;
		for (String res: resArray) {
			String[] resChain = res.split("\\.");
			if (xResidues == null) {
				xResidues = "#"+modelNumber+"/"+resChain[1]+":"+resChain[0];
			} else {
				xResidues += "|#"+modelNumber+"/"+resChain[1]+":"+resChain[0];
			}
		}
		return xResidues;
	}

	public void chimeraCommand(String command) {
		Map<String, Object> args = new HashMap<>();
		args.put("command", command);
		executeCommand("structureViz", "send", args, null);
	}

	public void executeCommand(String namespace, String command, 
	                           Map<String, Object> args, TaskObserver observer) {
		if (commandTaskFactory == null)
			commandTaskFactory = getService(CommandExecutorTaskFactory.class);

		if (taskManager == null)
			taskManager = getService(SynchronousTaskManager.class);
		TaskIterator ti = commandTaskFactory.createTaskIterator(namespace, command, args, observer);
		taskManager.execute(ti);
	}

	public int getModelNumber() {
		return this.modelNumber;
	}

	public void setModelNumber(int number) {
		this.modelNumber = number;
	}

	public String getModelName() {
		return this.modelName;
	}

	public void setModelName(String name) {
		this.modelName = name;
	}

	public void flushEvents() {
		eventHelper.flushPayloadEvents();
	}

	public <S> S getService(Class<S> serviceClass) {
		return serviceRegistrar.getService(serviceClass);
	}

	public <S> S getService(Class<S> serviceClass, String filter) {
		return serviceRegistrar.getService(serviceClass, filter);
	}

	public void registerService(Object service, Class<?> serviceClass, Properties props) {
		serviceRegistrar.registerService(service, serviceClass, props);
	}

	public void unregisterService(Object service, Class<?> serviceClass) {
		serviceRegistrar.unregisterService(service, serviceClass);
	}

	public void allFinished(FinishStatus finishStatus) {
	}

	public void taskFinished(ObservableTask task) {
		String models = task.getResults(String.class);
		int offset = models.indexOf(' ');
		if (offset >= 0) {
			String model = models.substring(1, offset);
			modelName = new String(models.substring(offset+1, models.length()-1));

			try {
				modelNumber = Integer.parseInt(model);
 			} catch (Exception e) {}
		} else {
			modelNumber = -2;
		}
	}

	private String addChains(String resChain) {
		String [] rc = resChain.split("[.]");
		String chain = rc[1];
		String residue = rc[0];
		// System.out.println("Looking for duplicate chain for '"+chain+"'");
		List<String> chains = getDuplicateChains(chain); // Get the chain aliases
		// System.out.println("Got "+chains.size()+" chains: ");
		if (chains != null && chains.size() > 0) {
			for (String ch: chains) {
				// System.out.println("...chain "+ch);
				resChain += ","+residue+"."+ch;
			}
		}
		// System.out.println("addChains returning: "+resChain);
		return resChain;
	}

	private class ClusterSort implements Comparator<CyNode> {
		final Map<String, Integer> orderMap;
		public ClusterSort(List<String> order) {
			orderMap = new HashMap<>();
			for (int i = 0; i < order.size(); i++) {
				orderMap.put(order.get(i), i);
			}
		}

		public int compare(CyNode a, CyNode b) {
			String nameA = ModelUtils.getName(mergedNetwork, a);
			String nameB = ModelUtils.getName(mergedNetwork, b);
			Integer orderA = orderMap.get(nameA);
			Integer orderB = orderMap.get(nameB);
			return orderA.compareTo(orderB);
		}
	}

}
