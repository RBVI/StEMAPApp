package edu.ucsf.rbvi.stEMAP.internal.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import org.cytoscape.application.CyUserLog;
import org.cytoscape.model.CyIdentifiable;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyRow;
import org.cytoscape.model.events.RowSetRecord;
import org.cytoscape.model.events.RowsSetEvent;
import org.cytoscape.model.events.RowsSetListener;

import edu.ucsf.rbvi.stEMAP.internal.utils.ModelUtils.NodeType;

public class SelectionListener implements RowsSetListener {
	StEMAPManager manager;
	final Logger logger = Logger.getLogger(CyUserLog.NAME);
	public SelectionListener(StEMAPManager manager) {
		this.manager = manager;
	}

	public void handleEvent(RowsSetEvent e) {
		CyNetwork mergedNetwork = manager.getMergedNetwork();

		if (!e.containsColumn(CyNetwork.SELECTED))
			return;

		if (!e.getSource().equals(mergedNetwork.getTable(CyNode.class, CyNetwork.LOCAL_ATTRS)))
			return;

		// OK, it's our network and the node table
		for (RowSetRecord rowSet: e.getColumnRecords(CyNetwork.SELECTED)) {
			CyRow row = rowSet.getRow();
			Boolean selected = row.get(CyNetwork.SELECTED, Boolean.class);

			// Get the node
			CyNode node = mergedNetwork.getNode(row.get(CyIdentifiable.SUID, Long.class));
			manager.selectGeneOrMutation(node, selected);
		}
		if (manager.getResultsPanel() != null)
			manager.getResultsPanel().update();
	}
}
