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
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

import org.cytoscape.command.AvailableCommands;
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

import edu.ucsf.rbvi.stEMAP.internal.view.ColorScale;
import edu.ucsf.rbvi.stEMAP.internal.view.ResultsPanel;
import edu.ucsf.rbvi.stEMAP.internal.tasks.ShowResultsPanel;
import edu.ucsf.rbvi.stEMAP.internal.tasks.ShowResultsPanelFactory;
import edu.ucsf.rbvi.stEMAP.internal.utils.ColorUtils;
import edu.ucsf.rbvi.stEMAP.internal.utils.ModelUtils;
import edu.ucsf.rbvi.stEMAP.internal.utils.ModelUtils.NodeType;
import edu.ucsf.rbvi.stEMAP.internal.utils.StructureUtils;

public class StEMAPManager implements TaskObserver { 
	final CyServiceRegistrar serviceRegistrar;
	final CyEventHelper eventHelper;
	public static final String RIN_NETWORK = "RINNetwork.SUID";
	public static final String CDT_NETWORK = "CDTNetwork.SUID";
	public static Color MAX_COLOR = Color.YELLOW;
	public static Color MIN_COLOR = Color.CYAN;
	public static Color MIXED_COLOR = Color.GREEN;
	public static Color ZERO_COLOR = Color.WHITE;
	public static Color MISSING_COLOR = Color.GRAY;
	public ColorScale sColor = null;
	public ColorScale eColor = null;
	public ColorScale mColor = null;

	public Color[] heatMapRange = {ZERO_COLOR, MIN_COLOR, ZERO_COLOR, MAX_COLOR};
	public Color[] mixedMapRange = {ZERO_COLOR, MIN_COLOR, ZERO_COLOR, MAX_COLOR, ZERO_COLOR, MIXED_COLOR};

	CommandExecutorTaskFactory commandTaskFactory = null;
	AvailableCommands availableCommands = null;
	SynchronousTaskManager<?> taskManager = null;

	Set<CyNode> selectedGenes;
	Set<CyNode> selectedMutations;

	// State variables
	CyNetwork rinNetwork = null;
	CyNetwork mergedNetwork = null;
	CyNetwork cdtNetwork = null;
	CyNetworkView mergedNetworkView = null;
	StructureMap map = null;
	String modelName = null;
	int modelNumber = -1;
	String lastResidues = null;
	ResultsPanel currentResultsPanel = null;

	boolean autoAnnotate = true;
	boolean ignoreMultiples = false;
	boolean useComplexColoring = true;
	double minWeight = 0.0;
	double maxWeight = 0.0;
	double scale = 1.0;

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

	public void setCDTNetwork(CyNetwork network) {
		cdtNetwork = network;
	}
	public CyNetwork getCDTNetwork() { return cdtNetwork; }


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
		if (currentResultsPanel != null) {
			// System.out.println("Hiding results panel");
			ShowResultsPanelFactory showResults = getService(ShowResultsPanelFactory.class);
			ShowResultsPanel panel = new ShowResultsPanel(this, showResults, false);
			panel.hidePanel();
			currentResultsPanel = null;
		}
	}

	public void selectGenesOrMutations(List<CyNode> nodes, Boolean select) {
		boolean genesChanged = false;
		for (CyNode node: nodes) {
			NodeType type = ModelUtils.getNodeType(mergedNetwork, node);
			if (type.equals(NodeType.GENE)) {
				if (select) {
					selectedGenes.add(node);
				} else {
					selectedGenes.remove(node);
				}
				genesChanged = true;
			} else if (type.equals(NodeType.MUTATION) || type.equals(NodeType.MULTIMUTATION)) {
				if (select) {
					selectedMutations.add(node);
				} else
					selectedMutations.remove(node);
			}
		}

		if (currentResultsPanel != null) {
			currentResultsPanel.update();
		}

		if (autoAnnotate && genesChanged) {
			updateChimera(true);
		}
	}

	public Set<CyNode> getSelectedGenes() { return selectedGenes; }
	public Set<CyNode> getSelectedMutations() { return selectedMutations; }

	public void loadPDB(String pdbPath, String extraCommands) {
		Map<String, Object> args = new HashMap<>();
		if (pdbPath != null) {
			args.put("structureFile", pdbPath);
			pdbFile = new File(pdbPath);
		} else {
			args.put("pdbID", getPDB());
		}

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
			args.put("command", "style sphere; "+extraCommands);
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

	public void colorSpheres(Map<Color, Set<String>> colorMap) {
		String command = null;
		for (Color color: colorMap.keySet()) {
			// System.out.println("Looking to color "+colorMap.get(color)+" "+color);
			// System.out.println("Residues: "+colorMap.get(color));
			Set<String> residues = colorMap.get(color);
			if (residues == null || residues.size() == 0)
				continue;

			String comm = colorResidues(color, colorMap.get(color));
			// System.out.println("Command = "+comm);
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
	public String colorResidues(Color color, Set<String> residues) {
		// Residue is the Cytoscape-style string of the form "nnn.a,nnn.b,...".  We need to convert
		// it to a ChimeraX list
		String xResidues = convertResiduesToX(residues.toArray(new String[1]));
		int r = color.getRed();
		int g = color.getGreen();
		int b = color.getBlue();
		String command = "color "+xResidues+" rgb("+r+","+g+","+b+") atoms";
		// System.out.println("Command: "+command);
		return command;
	}

	public void showSpheres(List<String> residues) {
		// System.out.println("showSpheres: "+residues.size()+" residues");
		// System.out.println("First residue = "+residues.get(0));
		if (lastResidues != null) {
			chimeraCommand("hide "+lastResidues);
		}
	
		/*
		// Make it a comma separated list
		String command = null;
		for (String r: residues) {
			if (command == null)
				command = r;
			else
				command += ","+r;
		}
		*/

		if (residues == null || residues.size() == 0) {
			lastResidues = null;
			return;
		}

		// System.out.println("showSpheres: converting to string ");
		try {
		lastResidues = convertResiduesToX(residues.toArray(new String[1]));
		} catch (Exception e) {
			e.printStackTrace();
		}
		// System.out.println("lastResidues = "+lastResidues);

		// System.out.println("Sending command: sel "+lastResidues);
		// It may be redundant, but select the residues (hopefully again)
		// chimeraCommand("sel "+lastResidues);
		// System.out.println("chimera: sel "+lastResidues);

		// Change to sphere
		// System.out.println("Sending command: disp "+lastResidues);
		chimeraCommand("disp "+lastResidues);
		// System.out.println("Sending command: repr sphere sel");
		// chimeraCommand("style "+lastResidues+" sphere");
	}

	public String convertResiduesToX(String residue) {
		String[] resArray = residue.split(",");
		return convertResiduesToX(resArray);
	}

	public String convertResiduesToX(String[] residues) {
		String xResidues = null;
		Map<String, TreeSet<Integer>> chainMap = new HashMap<>();
		for (String res: residues) {
			String[] resChain = res.split("\\.");
			if (!chainMap.containsKey(resChain[1]))
				chainMap.put(resChain[1], new TreeSet<Integer>());
			chainMap.get(resChain[1]).add(Integer.valueOf(resChain[0]));
		}

		xResidues = null;
		// Sort the residues
		for (String chain: chainMap.keySet()) {
			if (xResidues == null) {
				xResidues = "#"+modelNumber+"/"+chain;
			} else {
				xResidues += "|/"+chain;
			}

			int residueNumber = -1;
			int residueEnd = -1;
			if (chainMap.get(chain) != null && chainMap.get(chain).size() > 0) {
				xResidues += ":";
				for (Integer resNum: chainMap.get(chain)) {
					// Are we accumulating a range?
					if (residueEnd > 0) {
						// Yes
						if (resNum == residueEnd+1) {
							residueEnd = resNum;
							continue;
						} else {
							xResidues = endRange(xResidues, residueEnd);
							residueEnd = -1;
						}
					} else if (residueNumber != -1 && resNum == residueNumber+1) {
						residueEnd = resNum;
						continue;
					}

					if (residueNumber < 0)
						xResidues += resNum;
					else
						xResidues += ","+resNum;
					residueNumber = resNum;
				}
				xResidues = endRange(xResidues, residueEnd);
			}
		}
		return xResidues;
	}

	private String endRange(String spec, int residueEnd) {
		if (spec == null || residueEnd < 0) return spec;
		spec += "-"+residueEnd;
		return spec;
	}

	// Called form ResultsPanel to update color of spheres
	public void updateSpheres() {
		if (lastResidues != null)
			updateChimera(false);
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

		if (availableCommands == null)
			availableCommands = getService(AvailableCommands.class);

		if (taskManager == null)
			taskManager = getService(SynchronousTaskManager.class);

		if (availableCommands.getCommands(namespace) == null ||
				availableCommands.getCommands(namespace).size() == 0)
			return;
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

	public boolean autoAnnotate() { return autoAnnotate; }
	public void setAutoAnnotate(boolean auto) { this.autoAnnotate = auto; }

	public boolean ignoreMultiples() { return ignoreMultiples; }
	public void setIgnoreMultiples(boolean ignore) { this.ignoreMultiples = ignore; }

	public boolean useComplexColoring() { return useComplexColoring; }
	public void setUseComplexColoring(boolean complexColoring) { 
		this.useComplexColoring = complexColoring; 
	}

	public double getScale() { return scale; }
	public void setScale(double scale) { this.scale = scale; }

	public double getMinWeight() { return minWeight; }
	public void setMinWeight(double min) { minWeight = min; }

	public double getMaxWeight() { return maxWeight; }
	public void setMaxWeight(double max) { maxWeight = max; }

	public void setMinMax(double min, double max) { 
		minWeight = min; maxWeight = max; 
		ModelUtils.createColumn(mergedNetwork.getDefaultNetworkTable(), ModelUtils.MIN_WEIGHT_COLUMN, Double.class);
		ModelUtils.createColumn(mergedNetwork.getDefaultNetworkTable(), ModelUtils.MAX_WEIGHT_COLUMN, Double.class);
		mergedNetwork.getRow(mergedNetwork).set(ModelUtils.MIN_WEIGHT_COLUMN, min);
		mergedNetwork.getRow(mergedNetwork).set(ModelUtils.MAX_WEIGHT_COLUMN, max);
	}

	public ColorScale getSColorScale() {
		if (sColor == null) {
			sColor = new ColorScale(0.0, Math.abs(minWeight), ZERO_COLOR, ZERO_COLOR, MIN_COLOR, ZERO_COLOR);
		}
		return sColor;
	}

	public ColorScale getEColorScale() {
		if (eColor == null) {
			eColor = new ColorScale(0.0, maxWeight, ZERO_COLOR, ZERO_COLOR, MAX_COLOR, ZERO_COLOR);
		}
		return eColor;
	}

	public ColorScale getMColorScale() {
		if (mColor == null) {
			mColor = new ColorScale(0.0, (maxWeight+Math.abs(minWeight))/2.0, ZERO_COLOR, ZERO_COLOR, MIXED_COLOR, ZERO_COLOR);
		}
		return mColor;
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

	private void updateChimera(boolean show) {
		Map<Color, Set<String>> cm = new HashMap<>();
		List<String> residues = new ArrayList<>();
		double[] valueRange = { Double.MAX_VALUE, -Double.MAX_VALUE, Double.MAX_VALUE, Double.MIN_VALUE};

		// Check to see if we're using complex coloring.  If we are, we need to do the coloring
		// in one shot, not node by node
		boolean complexColoring = false;
		if (useComplexColoring()) {
			complexColoring = true;
			// Make sure we've only selected GENEs
			for (CyNode node: selectedGenes) {
				if (ModelUtils.getNodeType(mergedNetwork, node) == NodeType.GENE)
					continue;
				complexColoring = false;
				break;
			}
		}

		Color[] colorRange = null;
		Map<Color, Set<String>> resCol;
		if (!complexColoring) {
			colorRange = heatMapRange;
			for (CyNode node: selectedGenes) {
				resCol = StructureUtils.getResiduesAndColors(this, mergedNetworkView, node);

				for (Color color: resCol.keySet()) {
					if (cm.containsKey(color)) {
						cm.get(color).addAll(resCol.get(color));
					} else {
						cm.put(color, resCol.get(color));
					}
					residues.addAll(resCol.get(color));
				}
			}
		} else {
			colorRange = mixedMapRange;
			// System.out.println("Using complex coloring");
			resCol = MutationStats.getComplexResiduesAndColors(this, mergedNetworkView, 
							                                           new ArrayList<CyNode>(selectedGenes), scale);
			// System.out.println("resCol has "+resCol.size()+" colors");
			for (Color color: resCol.keySet()) {
				if (cm.containsKey(color)) {
					cm.get(color).addAll(resCol.get(color));
				} else {
					cm.put(color, resCol.get(color));
				}
				residues.addAll(resCol.get(color));
			}
		}

		// System.out.println("residues has "+residues.size()+" residues");
		if (show)
			showSpheres(residues);

		if (cm != null && cm.size() > 0) {
			try {
			ColorUtils.resolveDuplicates(cm);
			Map<Color, Set<String>> newMap = ColorUtils.compressMap(cm, colorRange);
			colorSpheres(newMap);
			} catch (Exception e) { e.printStackTrace(); }
		}
	}

}
