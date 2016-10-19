package edu.ucsf.rbvi.stEMAP.internal.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;

import edu.ucsf.rbvi.stEMAP.internal.utils.ModelUtils;

public class GeneStats { 
	final static double MAD_SCALE = 1.4826;
	final public static String GENE_CNT = "Int Count";
	final public static String GENE_MEAN = "Int Mean";
	final public static String GENE_SD = "Int Stdev";
	final public static String GENE_MEDIAN = "Int Median";
	final public static String GENE_MAD = "Int MAD";

	public static void calculateGeneStats(CyNetwork cdtNetwork, CyNetwork mergedNetwork) {
		// Create the columns in the mergedNetwork, if necessary
		ModelUtils.createColumn(mergedNetwork.getDefaultNodeTable(), GENE_CNT, Double.class);
		ModelUtils.createColumn(mergedNetwork.getDefaultNodeTable(), GENE_MEAN, Double.class);
		ModelUtils.createColumn(mergedNetwork.getDefaultNodeTable(), GENE_SD, Double.class);
		ModelUtils.createColumn(mergedNetwork.getDefaultNodeTable(), GENE_MEDIAN, Double.class);
		ModelUtils.createColumn(mergedNetwork.getDefaultNodeTable(), GENE_MAD, Double.class);

		// Look at possibly redoing this for efficiency
		List<String> geneNames = cdtNetwork.getRow(cdtNetwork).getList("__nodeOrder",String.class);
		List<String> arrayNames = cdtNetwork.getRow(cdtNetwork).getList("__arrayOrder",String.class);

		Map<CyNode, List<Double>> fValMap = new HashMap<>();
		for (CyEdge edge: cdtNetwork.getEdgeList()) {
			Double w = cdtNetwork.getRow(edge).get("weight", Double.class);
			if (w == null) continue;
			CyNode source = edge.getSource();
			String sourceName = cdtNetwork.getRow(source).get(CyNetwork.NAME, String.class);
			CyNode target = edge.getTarget();
			String targetName = cdtNetwork.getRow(source).get(CyNetwork.NAME, String.class);
			CyNode node = null;
			if (geneNames.contains(sourceName)) {
				node = source;
			} else if (geneNames.contains(targetName)) {
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
			mergedNetwork.getRow(node).set(GENE_CNT, cnt);
			mergedNetwork.getRow(node).set(GENE_MEAN, mymean);
			mergedNetwork.getRow(node).set(GENE_SD, mysd);
			mergedNetwork.getRow(node).set(GENE_MEDIAN, mymedian);
			mergedNetwork.getRow(node).set(GENE_MAD, mymad);
		}
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
