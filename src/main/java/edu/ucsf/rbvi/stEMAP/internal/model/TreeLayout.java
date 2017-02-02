package edu.ucsf.rbvi.stEMAP.internal.model;

import java.awt.geom.Point2D;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.view.model.View;
import org.cytoscape.view.presentation.property.BasicVisualLexicon;


public class TreeLayout {
	TreeNode root = null;
	Map<String, TreeNode> treeMap;
	Map<String, View<CyNode>> nodeMap;

	public TreeLayout(CyNetwork network, List<View<CyNode>> nvList, 
	                  List<String> tree) {
		this.treeMap = new HashMap<>();
		this.nodeMap = new HashMap<>();
		for (View<CyNode> nv: nvList) {
			nodeMap.put(network.getRow(nv.getModel()).get(CyNetwork.NAME, String.class), nv);
		}

		for (String treeString: tree) {
			String[] parts = treeString.split("\t");
			double dist = Double.parseDouble(parts[3]);
			// System.out.println("Node: "+parts[0]+" dist: "+dist+" left: "+parts[1]+", right: "+parts[2]);
			TreeNode newNode;
		 	if (treeMap.containsKey(parts[0])) {
				newNode = treeMap.get(parts[0]);
			} else {
				newNode	= new TreeNode(parts[0], dist);
				treeMap.put(parts[0], newNode);
				root = newNode;
			}
			String left = parts[1];
			String right = parts[2];
			if (!treeMap.containsKey(left)) {
				TreeNode leftNode = new TreeNode(left, 0.0);
				newNode.addLeft(leftNode);
				treeMap.put(left, leftNode);
			} else {
				newNode.addLeft(treeMap.get(left));
			}

			if (!treeMap.containsKey(right)) {
				TreeNode rightNode = new TreeNode(right, 0.0);
				newNode.addRight(rightNode);
				treeMap.put(right, rightNode);
			} else {
				newNode.addRight(treeMap.get(right));
			}

		}
		// printTree(root);
	}

	public int getDepth() {
		return root.getDepth();
	}

	public int getMaxDepth() {
		return root.getMaxDepth();
	}

	public void printTree(TreeNode start) {
		System.out.println(start.toString());
	}

	public void layout(List<String> nodeOrder, double xMin, double yMax) {
		double size = 40.0;
		double spacing= 10.0;
		double yMin = yMax-getMaxDepth()*(size+spacing);
		double x = xMin;

		int lastLevel = -1;
		for (String nodeName: nodeOrder) {
			TreeNode tn = treeMap.get(nodeName);
			if (tn == null) continue;
			int level = tn.getDepth();
			double y = yMin+level*(size+spacing);
			if (level == lastLevel) {
				x = x+size+spacing;
			} else {
				x = x+(size+spacing)/2.0;
				lastLevel = level;
			}

			View<CyNode> nv = nodeMap.get(nodeName);
			if (nv == null) {
				System.out.println("Error: no view for "+nodeName);
			} else {
				nv.setVisualProperty(BasicVisualLexicon.NODE_X_LOCATION, x);
				nv.setVisualProperty(BasicVisualLexicon.NODE_Y_LOCATION, y);
			}
		}
	}

	class TreeNode {
		String name;
		double distance;
		TreeNode left;
		TreeNode right;
		TreeNode parent;
		int depth = 0;

		public TreeNode(String name, double distance) {
			this.name = name;
			this.distance = distance;
			this.left = null;
			this.right = null;
			this.parent = null;
		}

		public String toString() {
			String r = "TreeNode: "+name+" depth: "+depth;
			if (left != null)
				r += "\n "+name+" left: "+left.toString()+"\n";
			if (right != null)
				r += "\n "+name+" right: "+right.toString();
			return r;
		}

		public void addLeft(TreeNode ln) {
			this.left = ln;
			ln.setParent(this);
			ln.setDepth(this.depth);
		}
		public void addRight(TreeNode rn) {
			this.right = rn;
			rn.setParent(this);
			rn.setDepth(this.depth);
		}

		public String getName() { return name; }
		public TreeNode getLeft() { return left; }
		public TreeNode getRight() { return right; }
		public boolean isLeaf() { 
			if (left == null && right == null) 
				return true;
			return false;
		}
		public int getDepth() { return depth; }
		public void setParent(TreeNode p) { this.parent = p; }
		public void setDepth(int d) { 
			this.depth = d+1;
			if (left != null) left.setDepth(this.depth);
			if (right != null) right.setDepth(this.depth);
		}

		public int getMaxDepth() {
			int maxDepth = depth;
			if (left != null) {
				int md = left.getMaxDepth();
				if (md > maxDepth) maxDepth = md;
			}
			if (right != null) {
				int md = right.getMaxDepth();
				if (md > maxDepth) maxDepth = md;
			}
			return maxDepth;
		}
	}
}
