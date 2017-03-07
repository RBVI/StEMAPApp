package edu.ucsf.rbvi.stEMAP.internal.model;

import java.awt.Color;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.view.model.CyNetworkView;

import edu.ucsf.rbvi.stEMAP.internal.utils.ModelUtils;
import edu.ucsf.rbvi.stEMAP.internal.utils.StructureUtils;
import edu.ucsf.rbvi.stEMAP.internal.view.ColorScale;

public class MutationStats { 
	final static double MAD_SCALE = 1.4826;
	final public static String MUT_CNT = "Int Count";
	final public static String MUT_MEAN = "Int Mean";
	final public static String MUT_SD = "Int Stdev";
	final public static String MUT_MEDIAN = "Int Median";
	final public static String MUT_MAD = "Int MAD";

	public static void calculateMutationStats(CyNetwork cdtNetwork, CyNetwork mergedNetwork) {
		// Create the columns in the mergedNetwork, if necessary
		ModelUtils.createColumn(mergedNetwork.getDefaultNodeTable(), MUT_CNT, Double.class);
		ModelUtils.createColumn(mergedNetwork.getDefaultNodeTable(), MUT_MEAN, Double.class);
		ModelUtils.createColumn(mergedNetwork.getDefaultNodeTable(), MUT_SD, Double.class);
		ModelUtils.createColumn(mergedNetwork.getDefaultNodeTable(), MUT_MEDIAN, Double.class);
		ModelUtils.createColumn(mergedNetwork.getDefaultNodeTable(), MUT_MAD, Double.class);

		// Look at possibly redoing this for efficiency
		List<String> mutationNames = cdtNetwork.getRow(cdtNetwork).getList("__nodeOrder",String.class);
		List<String> geneNames = cdtNetwork.getRow(cdtNetwork).getList("__arrayOrder",String.class);

		Map<CyNode, List<Double>> fValMap = new HashMap<>();
		for (CyEdge edge: cdtNetwork.getEdgeList()) {
			Double w = cdtNetwork.getRow(edge).get(ModelUtils.WEIGHT_COLUMN, Double.class);
			if (w == null) continue;
			CyNode source = edge.getSource();
			String sourceName = cdtNetwork.getRow(source).get(CyNetwork.NAME, String.class);
			CyNode target = edge.getTarget();
			String targetName = cdtNetwork.getRow(source).get(CyNetwork.NAME, String.class);
			CyNode node = null;
			if (mutationNames.contains(sourceName)) {
				node = source;
			} else if (mutationNames.contains(targetName)) {
				node = target;
			} else
				continue;

			if (!fValMap.containsKey(node)) {
				fValMap.put(node, new ArrayList<Double>());
			}
			fValMap.get(node).add(w);
		}

		for (CyNode node: fValMap.keySet()) {
			List<Double> fVals = fValMap.get(node);
			double cnt = fVals.size();
			double sum1 = sum(fVals);
			double sum2 = sumSq(fVals);
			double mymean = 0.0;
			double mymedian = 0.0;
			double mysd = 1.0;
			double mymad = 1.0;
			if (cnt > 1.0) {
				double var = (sum2 - (sum1 * sum1 / cnt))/(cnt - 1.0);
				mymean = sum1 / cnt;
				mysd = Math.sqrt(var);
				mymedian = median(fVals);
				List<Double> myabs = absdev(fVals, mymedian);
				mymad = MAD_SCALE * median(myabs);
			}
			mergedNetwork.getRow(node).set(MUT_CNT, cnt);
			mergedNetwork.getRow(node).set(MUT_MEAN, mymean);
			mergedNetwork.getRow(node).set(MUT_SD, mysd);
			mergedNetwork.getRow(node).set(MUT_MEDIAN, mymedian);
			mergedNetwork.getRow(node).set(MUT_MAD, mymad);
		}
	}

	public static List<CyEdge> getSignificantPositions(CyNetwork network, List<CyNode> complex) {
		// Get the threshold
		double chisqThresh = 0.0;
		// Get all of our data
		List<CyEdge> gis = ModelUtils.getInteractions(network, complex);
		// Find all of the mutations
		List<CyNode> mutations = ModelUtils.getMutations(network, complex);
		int nMutations = mutations.size();
		List<CyEdge> significantEdges = new ArrayList<>();
		for (CyNode mutation: mutations) {
			List<CyEdge> mutEdges = new ArrayList<>();

			double mysum = 0.0;
			double count = 0.0;
			for (CyEdge interaction: gis) {
				if (interaction.getSource() == mutation || interaction.getTarget() == mutation) {
					mutEdges.add(interaction);
					double weight = network.getRow(interaction).get(ModelUtils.WEIGHT_COLUMN,Double.class);
					mysum += weight;
					count += 1.0;
				}
			}
			if (count == 0.0) continue;
			double mutation_mean = network.getRow(mutation).get(MUT_MEAN, Double.class);
			double mutation_sd = network.getRow(mutation).get(MUT_SD, Double.class);
			double myz = (mysum - count*mutation_mean) / Math.sqrt(count);
			myz = myz / mutation_sd;
			double mychisq = myz*myz;
			if (mychisq >= chisqThresh)
				significantEdges.addAll(mutEdges);
		}

		return significantEdges;
	}

	public static Map<Color, Set<String>> getComplexResiduesAndColors(StEMAPManager manager, 
	                                                                  CyNetworkView view, List<CyNode> complex, 
																																		List<CyNode> filteredMutations,
	                                                                  double scale) {
		CyNetwork net = view.getModel();
		// System.out.println("Getting significant edges");
		List<CyEdge> edges = getSignificantPositions(net, complex);
		// System.out.println("Found "+edges.size()+" significant edges");

		// Now we need to choose the color range based on whether we have all epistatic, all suppresive, or a mix
		Map<String, List<Double>> valueMap = new HashMap<>();
		Map<String, Integer> flagMap = new HashMap<>();

		for (CyEdge edge: edges) {
			// Get the residue
			CyNode resNode = edge.getSource();

			if (filteredMutations != null && !filteredMutations.contains(resNode))
				continue;

			List<String> resSet = StructureUtils.getResidue(manager, net, resNode);

			// Add the value
			Double weight = net.getRow(edge).get(ModelUtils.WEIGHT_COLUMN, Double.class);
			int flag = -1;
			if (weight > 0) flag = 1;
			for (String residue: resSet) {
				if (!valueMap.containsKey(residue)) {
					valueMap.put(residue, new ArrayList<Double>());
				}
				valueMap.get(residue).add(weight);

				// Set the flag
				// -1 all suppressive
				// 1 all epistatic
				// 0 mixed
				if (!flagMap.containsKey(residue)) {
					flagMap.put(residue, flag);

				// Only care if it's not already flagged as mixed
				} else if ((flagMap.get(residue) != 0) && (flagMap.get(residue) != flag)) {
					flagMap.put(residue, 0);
				}
			}
		}

		// System.out.println("Value map has "+valueMap.size()+" residues");

		// Now, we have two maps.  The flagMap tells us which color scheme to use, the absolute
		// value of the scores for each residue tell us the strength of the color
		Map<Color, Set<String>> resultMap = new HashMap<>();
		for (String residue: valueMap.keySet()) {
			ColorScale colorScale = null;
			if (flagMap.get(residue) == 0) {
				colorScale = manager.getMColorScale();
			} else if (flagMap.get(residue) < 0) {
				colorScale = manager.getSColorScale();
			} else if (flagMap.get(residue) > 0) {
				colorScale = manager.getEColorScale();
			}

			double score = 0.0;
			for (double weight: valueMap.get(residue)) {
				score += Math.abs(weight)/valueMap.get(residue).size();
			}

			// Get the color
			Color color = (Color) colorScale.getPaint(score*scale);
			if (!resultMap.containsKey(color))
				resultMap.put(color, new HashSet<>());

			resultMap.get(color).add(residue);
		}

		// System.out.println("ResultMap has "+resultMap.size()+" colors");
		return resultMap;
	}

	private static List<Double> absdev(List<Double> fVals, double med) {
		List<Double> abs = new ArrayList<>(fVals.size());
		for (Double f: fVals) {
			abs.add(Math.abs(f-med));
		}
		return abs;
	}

	private static double median(List<Double> l) {
		Collections.sort(l);
		int length = l.size();
		if (length % 2 != 0)
			return (l.get(length / 2) + l.get(length / 2 - 1)) / 2.0;
		return l.get(length / 2);

	}

	private static double sum(List<Double> l) {
		double s = 0.0;
		for (Double d: l)
			s += d;
		return s;
	}

	private static double sumSq(List<Double> l) {
		double s = 0.0;
		for (Double d: l)
			s += d*d;
		return s;
	}

}
