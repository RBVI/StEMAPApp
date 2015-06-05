package edu.ucsf.rbvi.stEMAP.internal;

import static org.cytoscape.work.ServiceProperties.COMMAND;
import static org.cytoscape.work.ServiceProperties.COMMAND_DESCRIPTION;
import static org.cytoscape.work.ServiceProperties.COMMAND_NAMESPACE;
import static org.cytoscape.work.ServiceProperties.ENABLE_FOR;
import static org.cytoscape.work.ServiceProperties.ID;
import static org.cytoscape.work.ServiceProperties.IN_MENU_BAR;
import static org.cytoscape.work.ServiceProperties.INSERT_SEPARATOR_BEFORE;
import static org.cytoscape.work.ServiceProperties.MENU_GRAVITY;
import static org.cytoscape.work.ServiceProperties.PREFERRED_MENU;
import static org.cytoscape.work.ServiceProperties.TITLE;

import java.util.Properties;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.model.CyNetworkFactory;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.model.subnetwork.CyRootNetworkManager;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.cytoscape.task.NetworkTaskFactory;
import org.cytoscape.work.TaskFactory;

import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.service.util.AbstractCyActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import edu.ucsf.rbvi.stEMAP.internal.model.StEMAPManager;
import edu.ucsf.rbvi.stEMAP.internal.tasks.CreateRINTaskFactory;
import edu.ucsf.rbvi.stEMAP.internal.tasks.MergeTaskFactory;
import edu.ucsf.rbvi.stEMAP.internal.tasks.SplitResiduesTaskFactory;

public class CyActivator extends AbstractCyActivator {

	public CyActivator() {
		super();
	}

	public void start(BundleContext bc) {
		final CyServiceRegistrar serviceRegistrar = getService(bc, CyServiceRegistrar.class);
		final StEMAPManager manager = new StEMAPManager(serviceRegistrar);

		{
			Properties splitResiduesProps = new Properties();
			SplitResiduesTaskFactory splitResiduesTaskFactory = new SplitResiduesTaskFactory(manager);

			splitResiduesProps.setProperty(PREFERRED_MENU, "Apps.stEMAP");
			splitResiduesProps.setProperty(TITLE, "Split Residues");
			splitResiduesProps.setProperty(MENU_GRAVITY, "1.0");
			splitResiduesProps.setProperty(ENABLE_FOR, "network");
			registerService(bc, splitResiduesTaskFactory, NetworkTaskFactory.class, splitResiduesProps);
		}
		
		{
			Properties createRINProps = new Properties();
			CreateRINTaskFactory createRINTaskFactory = new CreateRINTaskFactory(manager);

			createRINProps.setProperty(PREFERRED_MENU, "Apps.stEMAP");
			createRINProps.setProperty(TITLE, "Create RIN");
			createRINProps.setProperty(MENU_GRAVITY, "2.0");
			registerService(bc, createRINTaskFactory, TaskFactory.class, createRINProps);
		}
		
		{
			Properties mergeProps = new Properties();
			MergeTaskFactory mergeTaskFactory = new MergeTaskFactory(manager);

			mergeProps.setProperty(PREFERRED_MENU, "Apps.stEMAP");
			mergeProps.setProperty(TITLE, "Merge networks");
			mergeProps.setProperty(MENU_GRAVITY, "3.0");
			registerService(bc, mergeTaskFactory, TaskFactory.class, mergeProps);
		}

		/*
		{
			Properties splitResiduesProps = new Properties();
			cdtImporterProps.setProperty(PREFERRED_MENU, "Apps.stEMAP");
			cdtImporterProps.setProperty(TITLE, "Load PDB File");
			registerService(bc, splitResiduesTaskFactory, TaskFactory.class, splitResiduesProps);
		}
		*/
	}
}
