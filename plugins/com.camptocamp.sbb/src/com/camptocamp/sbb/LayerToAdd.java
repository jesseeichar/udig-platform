package com.camptocamp.sbb;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.styling.Style;
import org.locationtech.geogig.geotools.data.GeoGigDataStoreFactory;
import org.locationtech.udig.catalog.CatalogPlugin;
import org.locationtech.udig.catalog.ICatalog;
import org.locationtech.udig.catalog.ID;
import org.locationtech.udig.catalog.IGeoResource;
import org.locationtech.udig.catalog.IService;
import org.locationtech.udig.catalog.geotools.data.DataStoreServiceExtension;
import org.locationtech.udig.project.internal.Layer;
import org.locationtech.udig.project.internal.LayerFactory;

public class LayerToAdd {
	public static final String GEOGIG_MARKER = "isGeogig";
	public final String commit, featureType;
	public final Style style;
	public final String layerName;
	public LayerToAdd(String commit, String featureType, Style style) {
		this(commit, featureType, style, null);
	}
	public LayerToAdd(String commit, String featureType, Style style, String layerName) {
		super();
		this.commit = commit;
		this.featureType = featureType;
		this.style = style;
		this.layerName = layerName;
	}

	IGeoResource getResource() {
		IGeoResource resource = null;
		try {
			Map<String, Serializable> params = new HashMap<String, Serializable>();
			params.put(GeoGigDataStoreFactory.HEAD.getName(), commit);
			params.put(GeoGigDataStoreFactory.REPOSITORY.getName(), new File(
					Activator.getDefault().getGeoGig().getRepository().getLocation().toURI()).toString());
			URL url = new URL("http://geogig.com/" + commit + "/" + featureType);
			params.put("ID", url);

			DataStoreServiceExtension ext = new DataStoreServiceExtension();
			ID id = DataStoreServiceExtension.createID(url,
					new GeoGigDataStoreFactory(), params);
			NullProgressMonitor monitor = new NullProgressMonitor();
			ICatalog localCatalog = CatalogPlugin.getDefault()
					.getLocalCatalog();
			IService service = localCatalog
					.getById(IService.class, id, monitor);
			if (service == null) {
				service = ext.createService(url, params);
				localCatalog.add(service);
			}

			List<? extends IGeoResource> resources = service.resources(monitor);
			for (IGeoResource iGeoResource : resources) {
				SimpleFeatureSource resolve = iGeoResource.resolve(
						SimpleFeatureSource.class, monitor);
				if (featureType.equals(resolve.getSchema().getName().getLocalPart())) {
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
	public Layer createLayer(LayerFactory layerFactory) {
		try {
			Layer layer = layerFactory.createLayer(getResource());
			if (layerName != null) {
				layer.setName(layerName);
			}
			if (style != null) {
				layer.getStyleBlackboard().put("org.locationtech.udig.style.sld", style);
			}
			layer.getBlackboard().put(GEOGIG_MARKER, true);
			return layer;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

}
