package edu.ucsf.rbvi.stEMAP.internal.utils;

import java.awt.Color;
import java.awt.Paint;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.CyNetworkViewFactory;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.cytoscape.view.model.View;
import org.cytoscape.view.presentation.property.BasicVisualLexicon;
import org.cytoscape.view.vizmap.VisualMappingFunctionFactory;
import org.cytoscape.view.vizmap.VisualMappingManager;
import org.cytoscape.view.vizmap.VisualStyle;
import org.cytoscape.view.vizmap.VisualStyleFactory;
import org.cytoscape.view.vizmap.mappings.BoundaryRangeValues;
import org.cytoscape.view.vizmap.mappings.ContinuousMapping;
import org.cytoscape.view.vizmap.mappings.PassthroughMapping;

import edu.ucsf.rbvi.stEMAP.internal.model.StEMAPManager;

public class StyleHelper {
	final StEMAPManager manager;

	public StyleHelper(final StEMAPManager manager) {
		this.manager = manager;
	}

	public void createStyle(CyNetworkView netView, double minWeight, double maxWeight,
	                        double negativeCutoff, double positiveCutoff) {
		VisualMappingManager vmm = manager.getService(VisualMappingManager.class);

		// Get a style we can play with
		VisualStyle currentStyle = vmm.getCurrentVisualStyle();
		VisualStyle vs = manager.getService(VisualStyleFactory.class).createVisualStyle(currentStyle);
		vs.setTitle("StEMAP Style");

		VisualMappingFunctionFactory continuousFactory = 
						manager.getService(VisualMappingFunctionFactory.class, "(mapping.type=continuous)");
		VisualMappingFunctionFactory passthroughFactory = 
						manager.getService(VisualMappingFunctionFactory.class, "(mapping.type=passthrough)");

		// Set the node label to the name column
		PassthroughMapping<String,String> name = (PassthroughMapping<String,String>)
						passthroughFactory.createVisualMappingFunction(CyNetwork.NAME, 
						                                               String.class, 
																					                 BasicVisualLexicon.NODE_LABEL);
		vs.addVisualMappingFunction(name);

		// Set the label color to black
		vs.setDefaultValue(BasicVisualLexicon.NODE_LABEL_COLOR, Color.BLACK);

		// Set the edge color to a cyan/yellow gradient
		ContinuousMapping<Double, Paint> edgeColor = (ContinuousMapping<Double,Paint>)
						continuousFactory.createVisualMappingFunction("weight", 
						                                              Double.class, 
																					                BasicVisualLexicon.EDGE_STROKE_UNSELECTED_PAINT);
		edgeColor.addPoint(minWeight, new BoundaryRangeValues(Color.CYAN, Color.CYAN, Color.CYAN));
		edgeColor.addPoint(0.0, 
		                   new BoundaryRangeValues(Color.WHITE, Color.WHITE, Color.WHITE));
		//edgeColor.addPoint(positiveCutoff, 
		//                   new BoundaryRangeValues(Color.WHITE, Color.WHITE, Color.WHITE));
		edgeColor.addPoint(maxWeight, new BoundaryRangeValues(Color.YELLOW, Color.YELLOW, Color.YELLOW));
		vs.addVisualMappingFunction(edgeColor);
		vmm.addVisualStyle(vs);
		vmm.setCurrentVisualStyle(vs);
		vmm.setVisualStyle(vs, netView);
		vs.apply(netView);
	}

	public Point2D copyNodeStyle(View<CyNode> from, View<CyNode> to, int offset) {
		double shift = 30.0*(double)offset;
		double x = from.getVisualProperty(BasicVisualLexicon.NODE_X_LOCATION)+shift;
		double y = from.getVisualProperty(BasicVisualLexicon.NODE_Y_LOCATION)+shift;
		to.setVisualProperty(BasicVisualLexicon.NODE_X_LOCATION, x);
		to.setVisualProperty(BasicVisualLexicon.NODE_Y_LOCATION, y);
		// Force the fill color
		// to.setLockedValue(BasicVisualLexicon.NODE_FILL_COLOR,
		//                   from.getVisualProperty(BasicVisualLexicon.NODE_FILL_COLOR));
		return new Point2D.Double(x,y);
	}

	public void copyEdgeStyle(View<CyEdge> from, View<CyEdge> to) {
		to.setLockedValue(BasicVisualLexicon.EDGE_STROKE_UNSELECTED_PAINT,
		                  from.getVisualProperty(BasicVisualLexicon.EDGE_STROKE_UNSELECTED_PAINT));
		to.setLockedValue(BasicVisualLexicon.EDGE_WIDTH,
		                  from.getVisualProperty(BasicVisualLexicon.EDGE_WIDTH));

	}

	public void styleMultiEdge(CyNetworkView netView, CyEdge edge) {
		View<CyEdge> ev = netView.getEdgeView(edge);
		if (ev == null) {
			System.out.println("No view for edge: "+edge);
			return;
		}
		ev.setLockedValue(BasicVisualLexicon.EDGE_WIDTH, 3.0);
		ev.setLockedValue(BasicVisualLexicon.EDGE_STROKE_UNSELECTED_PAINT, Color.BLACK);
	}
}
