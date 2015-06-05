package com.camptocamp.sbb;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.List;

import org.eclipse.core.runtime.ISafeRunnable;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.geotools.data.FeatureStore;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.factory.CommonFactoryFinder;
import org.locationtech.geogig.api.ContextBuilder;
import org.locationtech.geogig.api.GeoGIG;
import org.locationtech.geogig.api.GlobalContextBuilder;
import org.locationtech.geogig.cli.CLIContextBuilder;
import org.locationtech.udig.catalog.CatalogPlugin;
import org.locationtech.udig.catalog.IGeoResource;
import org.locationtech.udig.catalog.IResolve;
import org.locationtech.udig.catalog.IService;
import org.locationtech.udig.project.EditManagerEvent;
import org.locationtech.udig.project.IEditManager;
import org.locationtech.udig.project.IEditManagerListener;
import org.locationtech.udig.project.ILayer;
import org.locationtech.udig.project.IMap;
import org.locationtech.udig.project.IMapListener;
import org.locationtech.udig.project.MapEvent;
import org.locationtech.udig.project.MapEvent.MapEventType;
import org.locationtech.udig.project.internal.Layer;
import org.locationtech.udig.project.internal.Map;
import org.locationtech.udig.project.internal.commands.CreateMapCommand;
import org.locationtech.udig.project.ui.ApplicationGIS;
import org.opengis.filter.FilterFactory2;
import org.osgi.framework.BundleContext;

import com.google.common.base.Function;
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
	public static final String SBB_DIR = "C:/SBB";
	public static final URL KANTONE_URL;
	public static final URL RAILWAY_URL;
	static {
		URL tmp;
		try {
			tmp = new File(SBB_DIR + "/Kantone.shp").toURI().toURL();
		} catch (IOException e) {
			tmp = null;
		}
		KANTONE_URL = tmp;
		try {
			tmp = new File(SBB_DIR + "/railways.shp").toURI().toURL();
		} catch (IOException e) {
			tmp = null;
		}
		RAILWAY_URL = tmp;
	}
	private IEditManagerListener businessRulesApplicator = new IEditManagerListener() {

		@Override
		public void changed(EditManagerEvent event) {
			ILayer editLayer = event.getSource().getEditLayer();
			if (event.getType() == EditManagerEvent.PRE_COMMIT && editLayer.getName().equals("sbb_poi")) {
				try {
					SimpleFeatureStore fs = editLayer.getResource(SimpleFeatureStore.class, new NullProgressMonitor());
					FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2();
					SimpleFeatureCollection features = fs.getFeatures(ff.equal(ff.property("processed"), ff.literal(true), false));
					System.out.println(features.size());
				} catch (IOException e) {
					e.printStackTrace();
				}
				
			}
		}
	};

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

		geoGig = new GeoGIG(new File(SBB_DIR));
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

	public void showInMap(IWorkbenchSite site, String mapName, LayerToAdd... layersToAdd) {
		createAndOpenMap(site, mapName, null, layersToAdd);
	}

	public void showInMap(IWorkbenchSite site, String mapName, Function<Map, Void> postOpenAction, LayerToAdd... layersToAdd) {
		createAndOpenMap(site, mapName, postOpenAction, layersToAdd);
	}

	private void createAndOpenMap(final IWorkbenchSite site, final String name, final Function<Map, Void> postOpenAction, final LayerToAdd... layersToAdd) {
		ApplicationGIS.run(new ISafeRunnable() {

			@Override
			public void run() throws Exception {
				List<IGeoResource> background = getBackgroundResources();

				CreateMapCommand command = new CreateMapCommand(null, background, null);
				ApplicationGIS.getActiveProject().sendSync(command);
				ApplicationGIS.openMap(command.getCreatedMap(), true);
				org.locationtech.udig.project.internal.Map map = (org.locationtech.udig.project.internal.Map) ApplicationGIS.getActiveMap();
				map.setName(name);

				List<Layer> layers = toLayers(map, layersToAdd);
				map.getLayersInternal().addAll(layers);

				Layer layerToEdit = layers.get(layers.size() - 1);
				map.getEditManagerInternal().setSelectedLayer(layerToEdit);

				if (postOpenAction != null) {
					postOpenAction.apply(map);
				}
				map.addMapListener(new IMapListener() {
					
					@Override
					public void changed(MapEvent event) {
						if (event.getType() == MapEventType.EDIT_MANAGER) {
							((IEditManager)event.getNewValue()).addListener(businessRulesApplicator);
						}
					}
				});
				addCommitListener(map);
				if (site != null) {
					site.getPage().activate(site.getPage().getActiveEditor());
				}
			}

			@Override
			public void handleException(Throwable exception) {
				exception.printStackTrace();
			}
		});
	}

	// HACK. In actual system this should be in Geoserver.
	// This is to simulate business rules being executed on a commit
	// In actual system there should be a pre or post commit listener in
	// Geoserver that executes the business
	// rules.
	protected void addCommitListener(Map map) {
		if (!map.getEditManagerInternal().containsListener(businessRulesApplicator)) {
			map.getEditManager().addListener(businessRulesApplicator);
		}
	}

	private List<Layer> toLayers(org.locationtech.udig.project.internal.Map map, final LayerToAdd... layersToAdd) {
		List<Layer> layers = Lists.newArrayList();
		for (LayerToAdd layerToAdd : layersToAdd) {
			layers.add(layerToAdd.createLayer(map.getLayerFactory()));
		}
		return layers;
	}

	public void addToMap(IWorkbenchSite site, String mapName, LayerToAdd... layersToAdd) {

		Collection<? extends IMap> visibleMap = ApplicationGIS
				.getVisibleMaps();
		if (visibleMap.isEmpty()) {
			createAndOpenMap(site, mapName, null, layersToAdd);
		} else {
			Map activeMap = (Map) ApplicationGIS.getActiveMap();
			activeMap.getLayersInternal().addAll(toLayers(activeMap, layersToAdd));
		}
	}

	private List<IGeoResource> getBackgroundResources() {
		try {
			NullProgressMonitor monitor = new NullProgressMonitor();
			IService service = CatalogPlugin
					.getDefault()
					.getLocalCatalog()
					.acquire(KANTONE_URL,
							monitor);
			List<IGeoResource> resources = Lists.newArrayList();
			for (IResolve resolve : service.members(monitor)) {
				resources.add((IGeoResource) resolve);
			}
			return resources;
		} catch (IOException e) {
			e.printStackTrace();
			return Lists.newArrayList();
		}
	}

	public static String openInputDialog(Shell parent, String text, String message) {
		InputDialog inputDialog = new InputDialog(parent, text, message,
				"BranchA", null);
		if (inputDialog.open() != Window.OK) {
			return null;
		}
		return inputDialog.getValue();
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
