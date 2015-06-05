package com.camptocamp.sbb;

import java.io.Serializable;
import java.net.URL;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.ui.IStartup;
import org.geotools.data.wfs.WFSDataStoreFactory;
import org.locationtech.udig.catalog.CatalogPlugin;
import org.locationtech.udig.catalog.IGeoResource;
import org.locationtech.udig.catalog.IResolve;
import org.locationtech.udig.catalog.IService;

import com.google.common.collect.Maps;

public class Startup implements IStartup {

	static IGeoResource sbbPOIResource;

	@Override
	public void earlyStartup() {
		try {
			NullProgressMonitor monitor = new NullProgressMonitor();
			CatalogPlugin.getDefault().getLocalCatalog().acquire(Activator.RAILWAY_URL, monitor);
			
			Activator.getDefault().showInMap(null, "Railways (master)", new LayerToAdd("master", "railroads", Styling.createTrackStyle()));
			
			addWFS();
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

	private void addWFS() throws Throwable {
		Map<String, Serializable> params = Maps.newHashMap();
		params.put(WFSDataStoreFactory.URL.key, new URL(GeoserverRest.GEOSERVER_URL + "wfs"));
		params.put(WFSDataStoreFactory.LENIENT.key, true);
		params.put(WFSDataStoreFactory.TRY_GZIP.key, true);
		params.put(WFSDataStoreFactory.WFS_STRATEGY.key, "geoserver");
		params.put(WFSDataStoreFactory.WFS_STRATEGY.key, "geoserver");

		NullProgressMonitor monitor = new NullProgressMonitor();
		IService wfs = CatalogPlugin.getDefault().getLocalCatalog().acquire(params, monitor);
		List<IResolve> members = wfs.members(monitor);
		for (IResolve iResolve : members) {
			if (iResolve.getIdentifier().toString().contains("sbb_poi")) {
				sbbPOIResource = (IGeoResource) iResolve;
				break;
			}
		}
	}

}
