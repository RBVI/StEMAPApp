package edu.ucsf.rbvi.stEMAP.internal.model;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.cytoscape.command.CommandExecutorTaskFactory;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.model.CyNode;
import org.cytoscape.event.CyEventHelper;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.work.SynchronousTaskManager;
import org.cytoscape.work.FinishStatus;
import org.cytoscape.work.ObservableTask;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.TaskObserver;

import edu.ucsf.rbvi.stEMAP.internal.utils.ModelUtils;
import edu.ucsf.rbvi.stEMAP.internal.utils.ModelUtils.NodeType;

public class StEMAPManager implements TaskObserver {
	final CyServiceRegistrar serviceRegistrar;
	final CyEventHelper eventHelper;
	CommandExecutorTaskFactory commandTaskFactory = null;
	SynchronousTaskManager taskManager = null;

	// State variables
	CyNetwork rinNetwork = null;
	CyNetwork mergedNetwork = null;
	StructureMap map = null;
	String modelName = null;
	int modelNumber = -1;
	String lastResidues = null;

	public StEMAPManager(final CyServiceRegistrar cyRegistrar) {
		this.serviceRegistrar = cyRegistrar;
		this.eventHelper = serviceRegistrar.getService(CyEventHelper.class);
	}

	public void readStructureMap(File mapFile) throws IOException {
		map = new StructureMap(mapFile);
	}

	public String getPDB() {
		if (map == null) return null;
		return map.getPDB();
	}

	public boolean usePDBFile() {
		if (map == null) return false;
		return map.usePDBFile();
	}

	public String getPDBFile() {
		if (map == null) return null;
		return map.getPDBFile();
	}

	public String getChimeraCommands() {
		if (map == null) return null;
		return map.getChimeraCommands();
	}


	public String getChain(String chain) {
		if (map == null) return null;
		return map.getChain(chain);
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

	public CyNetwork getMergedNetwork() { return mergedNetwork; }

	public void reset() {
		mergedNetwork = null;
		rinNetwork = null;
		lastResidues = null;
		modelNumber = -1;
		modelName = null;
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
				if (pdb == null || pdb.length() == 0)
					geneNodes.add(neighbor);
			}
		}
		return geneNodes;
	}

	public List<CyNode> getResidueNodes(CyNetwork net, CyNode node) {
		List<CyNode> residueNodes = new ArrayList<>();
		for (CyNode neighbor: net.getNeighborList(node, CyEdge.Type.ANY)) {
			String pdb = net.getRow(neighbor).get(ModelUtils.PDB_COLUMN, String.class);
			if (pdb != null && pdb.length() > 0) {
				residueNodes.add(neighbor);
			} else {
				String mutType = net.getRow(neighbor).get(ModelUtils.MUT_TYPE_COLUMN, String.class);
				if (mutType != null && (mutType.equals("del") || mutType.equals("multiple"))) {
					List<CyNode> rn = getResidueNodes(net, neighbor);
					if (rn != null) residueNodes.addAll(rn);
				}
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
			String pdb = net.getRow(node).get(ModelUtils.PDB_COLUMN, String.class);
			if (pdb == null || pdb.length() == 0)
				continue;

			String[] model = pdb.split("#");
			residues.add(model[1]);
		}
		return new ArrayList<String>(residues);
	}

	public void loadPDB(String pdbPath, String extraCommands) {
		Map<String, Object> args = new HashMap<>();
		if (pdbPath != null)
			args.put("structureFile", pdbPath);
		else
			args.put("pdbID", getPDB());

		args.put("showDialog", "true");
		executeCommand("structureViz", "open", args, this);

		try {
			// Wait for things to process
			Thread.sleep(500);
		} catch (Exception e) {}

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

		Map<String, Object> args = new HashMap<>();
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

	public void showSpheres(List<String> residues) {
		if (lastResidues != null) {
			System.out.println("Sending command: ~disp "+lastResidues);
			chimeraCommand("~disp "+lastResidues);
		}
		// Make it a comma separated list
		String command = "#"+modelNumber+":";
		for (String r: residues)
			command += r+",";

		lastResidues = command.substring(0, command.length()-1);

		System.out.println("Sending command: sel "+lastResidues);
		// It may be redundant, but select the residues (hopefully again)
		chimeraCommand("sel "+lastResidues);

		// Change to sphere
		chimeraCommand("disp sel");
		chimeraCommand("repr sphere sel");
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

	public void allFinished(FinishStatus finishStatus) {}

	public void taskFinished(ObservableTask task) {
		String models = task.getResults(String.class);
		int offset = models.indexOf(' ');
		String model = models.substring(1, offset);
		modelName = new String(models.substring(offset+1, models.length()-1));

		try {
			modelNumber = Integer.parseInt(model);
 		} catch (Exception e) {}
	}

}
