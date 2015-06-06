package com.camptocamp.sbb;

import static com.camptocamp.sbb.GeoserverRest.GEOSERVER_URL;
import static com.camptocamp.sbb.GeoserverRest.execRestInputStream;

import java.io.IOException;
import java.io.InputStream;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.styling.SLD;
import org.geotools.styling.SLDParser;
import org.geotools.styling.Style;
import org.geotools.styling.StyledLayerDescriptor;
import org.locationtech.udig.project.EditManagerEvent;
import org.locationtech.udig.project.IEditManagerListener;
import org.locationtech.udig.project.ILayer;
import org.locationtech.udig.project.internal.Layer;
import org.locationtech.udig.project.internal.LayerFactory;
import org.locationtech.udig.project.internal.Map;
import org.locationtech.udig.project.ui.ApplicationGIS;
import org.locationtech.udig.project.ui.tool.AbstractActionTool;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.filter.FilterFactory2;

import com.camptocamp.sbb.views.BranchTypeView;
import com.google.common.base.Function;
import com.vividsolutions.jts.geom.Point;

public class AddPOI extends AbstractActionTool {
	private static final String LAYER_NAME = "SBB Points of Interest";
	private IEditManagerListener businessRulesApplicator = new IEditManagerListener() {

		@Override
		public void changed(EditManagerEvent event) {
			ILayer editLayer = event.getSource().getEditLayer();
			if (event.getType() == EditManagerEvent.PRE_COMMIT && editLayer.getName().equals(LAYER_NAME)) {
				try {
					SimpleFeatureStore fs = editLayer.getResource(SimpleFeatureStore.class, new NullProgressMonitor());
					SimpleFeatureCollection features = fs.getFeatures();
					FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2();
					SimpleFeatureIterator iter = features.features();
					try {
						while (iter.hasNext()) {
							SimpleFeature feature = (SimpleFeature) iter.next();
							if ("info".equals(feature.getAttribute("type"))) {
								SimpleFeature station = snapFeature(feature, features);
								if (station != null) {
									fs.modifyFeatures(new String[]{"geometry", "type"}, new Object[]{station.getDefaultGeometry(), "hidden"}, 
											ff.id(ff.featureId(feature.getID())));
									fs.modifyFeatures("type", "station_info", ff.id(ff.featureId(station.getID())));
								}
							}
						}
					} finally {
						iter.close();
					}
				} catch (IOException e) {
					e.printStackTrace();
				}

			}
		}

		private SimpleFeature snapFeature(SimpleFeature feature, SimpleFeatureCollection features) {
			Point geom = (Point) feature.getDefaultGeometry();
			SimpleFeatureIterator iter = features.features();
			try {
				while (iter.hasNext()) {
					SimpleFeature next = (SimpleFeature) iter.next();
					if ("station".equals(next.getAttribute("type"))) {
						Point nextGeom = (Point) next.getDefaultGeometry();
						boolean withinSnapping = nextGeom.distance(geom) < 50;
						if (withinSnapping) {
							return next;
						}
					}
				}
			} finally {
				iter.close();
			}
			return null;
		}
	};

	public AddPOI() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public void run() {
		Map activeMap = (Map) ApplicationGIS.getActiveMap();
		LayerFactory layerFactory = activeMap.getLayerFactory();
		try {

			Layer layer = null;
			NullProgressMonitor monitor = new NullProgressMonitor();
			for (Layer l : activeMap.getLayersInternal()) {
				SimpleFeatureSource resource = l.getResource(SimpleFeatureSource.class, monitor);
				if (resource != null && resource.getSchema().getName().getLocalPart().equals("sbb_poi")) {
					layer = l;
					break;
				}
			}
			if (layer == null) {
				layer = getLayer(layerFactory);
			}

			activeMap.getLayersInternal().add(layer);
			activeMap.getEditManagerInternal().setSelectedLayer(layer);

			SimpleFeatureStore resource = layer.getResource(SimpleFeatureStore.class, monitor);
			FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2();
			resource.removeFeatures(ff.not(ff.id(ff.featureId("sbb_poi.3"), ff.featureId("sbb_poi.1"))));
			resource.modifyFeatures("type", "station", ff.id(ff.featureId("sbb_poi.3")));
			resource.modifyFeatures("type", "parking", ff.id(ff.featureId("sbb_poi.1")));
			activeMap.getEditManagerInternal().commitTransaction();

			BranchTypeView.zoom(null, activeMap);
			addCommitListener(activeMap);
		} catch (IOException e) {
			e.printStackTrace();
		}
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

	private Layer getLayer(LayerFactory layerFactory) throws IOException {
		final Layer layer = layerFactory.createLayer(Startup.sbbPOIResource);
		layer.setName(LAYER_NAME);

		execRestInputStream(GEOSERVER_URL + "rest/styles/sbb_poi_style.sld", new Function<InputStream, Boolean>() {

			@Override
			public Boolean apply(InputStream inputStream) {

				SLDParser stylereader = new SLDParser(CommonFactoryFinder.getStyleFactory(), inputStream);
				StyledLayerDescriptor sld = stylereader.parseSLD();
				Style[] style = SLD.styles(sld);
				if (style.length > 0) {
					layer.getStyleBlackboard().put("org.locationtech.udig.style.sld", style[0]);
				}
				return true;
			}

		});
		return layer;
	}

	@Override
	public void dispose() {

	}

}
