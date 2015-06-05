package com.camptocamp.sbb;

import static com.camptocamp.sbb.GeoserverRest.GEOSERVER_URL;
import static com.camptocamp.sbb.GeoserverRest.execRestInputStream;

import java.io.IOException;
import java.io.InputStream;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.styling.SLD;
import org.geotools.styling.SLDParser;
import org.geotools.styling.Style;
import org.geotools.styling.StyledLayerDescriptor;
import org.locationtech.udig.project.internal.Layer;
import org.locationtech.udig.project.internal.LayerFactory;
import org.locationtech.udig.project.internal.Map;
import org.locationtech.udig.project.ui.ApplicationGIS;
import org.locationtech.udig.project.ui.tool.AbstractActionTool;
import org.opengis.filter.FilterFactory2;

import com.camptocamp.sbb.views.BranchTypeView;
import com.google.common.base.Function;

public class AddPOI extends AbstractActionTool {

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
			activeMap.getEditManagerInternal().commitTransaction();

			BranchTypeView.zoom(null, activeMap);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private Layer getLayer(LayerFactory layerFactory) throws IOException {
		final Layer layer = layerFactory.createLayer(Startup.sbbPOIResource);
		layer.setName("SBB Points of Interest");
		
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
