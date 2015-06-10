package edu.ucsf.rbvi.stEMAP.internal.tasks;

import java.io.File;

import org.cytoscape.work.Tunable;

import edu.ucsf.rbvi.stEMAP.internal.model.StEMAPManager;

public class PDBFile {
	final StEMAPManager manager;

	// Tunable for RIN network
	@Tunable (description="PDBFile:", params="input=true", gravity=1.0)
	public File pdbFile = null;

	public PDBFile(final StEMAPManager manager) {
		this.manager = manager;
		if (manager.getPDBFile() != null)
			pdbFile = new File(manager.getPDBFile());
	}

	public File getPDBFile() { return pdbFile; }

}
