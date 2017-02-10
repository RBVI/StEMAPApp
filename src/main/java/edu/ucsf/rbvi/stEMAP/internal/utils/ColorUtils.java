package edu.ucsf.rbvi.stEMAP.internal.utils;

import java.awt.Color;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ColorUtils {
	// Quantize the colors a little to compress the map
	static int COLORS = 5;
	public static Map<Color, Set<String>> compressMap(Map<Color, Set<String>> cmap, Color[] colorRange) {
		Map<Color, Set<String>> newMap = new HashMap<>();
		List<Color> colorList = new ArrayList<Color>();
		makeRange(colorList, colorRange[0], colorRange[1], COLORS);
		makeRange(colorList, colorRange[2], colorRange[3], COLORS);
		if (colorRange.length == 6)
			makeRange(colorList, colorRange[4], colorRange[5], COLORS);
		for (Color clr: colorList) {
			newMap.put(clr, new HashSet<String>());
		}

		for (Color clr: cmap.keySet()) {
			addBinnedColor(newMap, colorList, clr, cmap.get(clr));
		}

		return newMap;
	}

	public static void resolveDuplicates(Map<Color, Set<String>>cmap) {
		// Make an inverse map
		Map<String, Set<Color>> reverseMap = new HashMap<>();
		for (Color clr: cmap.keySet()) {
			for (String res: cmap.get(clr)) {
				if (!reverseMap.containsKey(res))
					reverseMap.put(res, new HashSet<Color>());
				reverseMap.get(res).add(clr);
			}
		}

		// Find all of the duplicates and create average colors for them
		for (String res: reverseMap.keySet()) {
			Set<Color> colors = reverseMap.get(res);
			if (colors.size() > 1) {
				for (Color color: colors) {
					cmap.get(color).remove(res); // Remove the residues from their previous colors
					if (cmap.get(color).size() == 0)
						cmap.remove(color);
				}
				Color avgColor = adjustedColor(colors);
				if (cmap.containsKey(avgColor)) {
					cmap.get(avgColor).add(res);
				} else {
					cmap.put(avgColor, new HashSet<String>());
					cmap.get(avgColor).add(res);
				}
			}
		}
	}

	private static void makeRange(List<Color> colorList, Color lowColor, Color highColor, int bins) {
		float rStep = (float)(highColor.getRed() - lowColor.getRed())/(bins-1);
		float gStep = (float)(highColor.getGreen() - lowColor.getGreen())/(bins-1);
		float bStep = (float)(highColor.getBlue() - lowColor.getBlue())/(bins-1);

		int rLow = lowColor.getRed();
		int gLow = lowColor.getGreen();
		int bLow = lowColor.getBlue();
		for (int i = 1; i < bins; i++) {
			int r = rLow + (int)(i*rStep);
			int g = gLow + (int)(i*gStep);
			int b = bLow + (int)(i*bStep);
			colorList.add(new Color(r, g, b));
		}
	}

	private static void addBinnedColor(Map<Color, Set<String>> map, List<Color> colorList, Color color, Set<String>residues) {
		double distance = Double.MAX_VALUE;
		Color bestColor = colorList.get(0);

		for (Color c: colorList) {
			double d = colorDist(c, color);
			if (d < distance) {
				bestColor = c;
				distance = d;
			}
		}
		map.get(bestColor).addAll(residues);
	}

	private static double colorDist(Color c1, Color c2) {
		int r1 = c1.getRed();
		int g1 = c1.getGreen();
		int b1 = c1.getBlue();
		int r2 = c2.getRed();
		int g2 = c2.getGreen();
		int b2 = c2.getBlue();
		double d = Math.sqrt(Math.pow((r2-r1),2) + Math.pow((g2-g1),2) + Math.pow((b2-b1),2));
		return d;
	}

	// Currently, this is a very simple approach to this
	private static Color adjustedColor(Set<Color> colors) {
		int r = 0, g = 0, b = 0;
		for (Color c: colors) {
			r += c.getRed();
			g += c.getGreen();
			b += c.getBlue();
		}
		return new Color(r/colors.size(), g/colors.size(), b/colors.size());
	}
}
