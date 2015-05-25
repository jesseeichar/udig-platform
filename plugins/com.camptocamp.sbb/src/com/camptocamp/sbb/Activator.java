package com.camptocamp.sbb;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.ISafeRunnable;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.geotools.data.simple.SimpleFeatureSource;
import org.locationtech.geogig.api.ContextBuilder;
import org.locationtech.geogig.api.GeoGIG;
import org.locationtech.geogig.api.GlobalContextBuilder;
import org.locationtech.geogig.cli.CLIContextBuilder;
import org.locationtech.geogig.geotools.data.GeoGigDataStoreFactory;
import org.locationtech.udig.catalog.CatalogPlugin;
import org.locationtech.udig.catalog.ICatalog;
import org.locationtech.udig.catalog.ID;
import org.locationtech.udig.catalog.IGeoResource;
import org.locationtech.udig.catalog.IResolve;
import org.locationtech.udig.catalog.IService;
import org.locationtech.udig.catalog.geotools.data.DataStoreServiceExtension;
import org.locationtech.udig.project.IMap;
import org.locationtech.udig.project.ui.ApplicationGIS;
import org.locationtech.udig.project.ui.internal.tool.display.ToolManager;
import org.locationtech.udig.project.ui.tool.ModalTool;
import org.osgi.framework.BundleContext;

import com.google.common.collect.Lists;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin {

	static {
		if (GlobalContextBuilder.builder == null
				|| GlobalContextBuilder.builder.getClass().equals(
						ContextBuilder.class)) {
			GlobalContextBuilder.builder = new CLIContextBuilder();
		}
	}

	private GeoGIG geoGig;
	// The plug-in ID
	public static final String PLUGIN_ID = "com.camptocamp.sbb"; //$NON-NLS-1$

	// The shared instance
	private static Activator plugin;

	/**
	 * The constructor
	 */
	public Activator() {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext
	 * )
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;

		geoGig = new GeoGIG(new File("E:\\SBB"));
		geoGig.getRepository();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext
	 * )
	 */
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		geoGig.close();
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static Activator getDefault() {
		return plugin;
	}

	public GeoGIG getGeoGig() {
		return geoGig;
	}

	public void showInMap(IWorkbenchSite site, String commit, String ftName) {
		IGeoResource resource = null;

		resource = getResource(commit, ftName, resource);

		if (resource != null) {
			createAndOpenMap(resource);
		}
	}

	private void createAndOpenMap(final IGeoResource resource) {
		ApplicationGIS.run(new ISafeRunnable() {
			
			@Override
			public void run() throws Exception {
				List<IGeoResource> layers = getBackgroundResources();
				layers.add(resource);
				ApplicationGIS.createAndOpenMap(layers);
			}
			
			@Override
			public void handleException(Throwable exception) {
				exception.printStackTrace();
			}
		});
	}
	
	public void addToMap(IWorkbenchSite site, String commit, String ftName) {
		IGeoResource resource = null;
		resource = getResource(commit, ftName, resource);
		
		if (resource != null) {
			Collection<? extends IMap> activeMap = ApplicationGIS.getVisibleMaps();
			if (activeMap.isEmpty()) {
				createAndOpenMap(resource);
			} else {
				ApplicationGIS.addLayersToMap(activeMap.iterator().next(), Collections.singletonList(resource), 0);
			}
		}
	}

	private List<IGeoResource> getBackgroundResources() {
		try {
			NullProgressMonitor monitor = new NullProgressMonitor();
			IService service = CatalogPlugin.getDefault().getLocalCatalog().acquire(new File("E:/SBB/Kantone.shp").toURI().toURL(), monitor);
			List<IGeoResource> resources = Lists.newArrayList();
			for (IResolve resolve: service.members(monitor)) {
				resources.add((IGeoResource) resolve);
			}
			return resources;
		} catch (IOException e) {
			e.printStackTrace();
			return Lists.newArrayList();
		}
	}

	private IGeoResource getResource(String commit, String ftName, IGeoResource resource) {
		try {
			Map<String, Serializable> params = new HashMap<String, Serializable>();
			params.put(GeoGigDataStoreFactory.HEAD.getName(), commit);
			params.put(GeoGigDataStoreFactory.REPOSITORY.getName(), new File(geoGig.getRepository().getLocation().toURI()).toString());
			URL url = new URL("http://geogig.com/" + commit + "/" + ftName);
			params.put("ID", url);

			DataStoreServiceExtension ext = new DataStoreServiceExtension();
			ID id = DataStoreServiceExtension.createID(url, new GeoGigDataStoreFactory(), params);
			NullProgressMonitor monitor = new NullProgressMonitor();
			ICatalog localCatalog = CatalogPlugin.getDefault().getLocalCatalog();
			IService service = localCatalog.getById(IService.class, id, monitor);
			if (service == null) {
				service = ext.createService(url, params);
				localCatalog.add(service);
			}
			
			List<? extends IGeoResource> resources = service.resources(monitor);
			for (IGeoResource iGeoResource : resources) {
				SimpleFeatureSource resolve = iGeoResource.resolve(SimpleFeatureSource.class, monitor);
				if (ftName.equals(resolve.getSchema().getName().getLocalPart())) {
					resource = iGeoResource;
					break;
				}
			}

		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return resource;
	}

	/**
	 * Returns an image descriptor for the image file at the given plug-in
	 * relative path
	 *
	 * @param path
	 *            the path
	 * @return the image descriptor
	 */
	public static ImageDescriptor getImageDescriptor(String path) {
		return imageDescriptorFromPlugin(PLUGIN_ID, path);
	}
}
