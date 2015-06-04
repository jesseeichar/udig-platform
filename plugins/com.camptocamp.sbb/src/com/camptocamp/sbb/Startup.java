package com.camptocamp.sbb;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.ui.IStartup;
import org.locationtech.udig.catalog.CatalogPlugin;

public class Startup implements IStartup {

	@Override
	public void earlyStartup() {
		try {
			NullProgressMonitor monitor = new NullProgressMonitor();
			CatalogPlugin.getDefault().getLocalCatalog().acquire(Activator.RAILWAY_URL, monitor);
			
			Activator.getDefault().showInMap(null, "Railways (master)", new LayerToAdd("master", "railroads", Styling.createTrackStyle()));
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

}
