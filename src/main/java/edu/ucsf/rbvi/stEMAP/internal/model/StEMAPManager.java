package edu.ucsf.rbvi.stEMAP.internal.model;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.cytoscape.model.CyNetwork;

import org.cytoscape.event.CyEventHelper;
import org.cytoscape.service.util.CyServiceRegistrar;

public class StEMAPManager {
	final CyServiceRegistrar serviceRegistrar;
	final CyEventHelper eventHelper;
	StructureMap map = null;
	CyNetwork rinNetwork = null;

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


}
