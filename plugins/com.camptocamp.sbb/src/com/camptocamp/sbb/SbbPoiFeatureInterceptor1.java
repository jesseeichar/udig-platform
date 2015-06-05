package com.camptocamp.sbb;

import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;
import org.locationtech.udig.project.interceptor.FeatureInterceptor;
import org.locationtech.udig.tools.edit.EditBlackboardUtil;
import org.locationtech.udig.tools.edit.support.EditBlackboard;
import org.opengis.feature.Feature;
import org.opengis.feature.simple.SimpleFeature;

public class SbbPoiFeatureInterceptor1 implements FeatureInterceptor {

	public SbbPoiFeatureInterceptor1() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public void run(Feature newFeature) {

        // SBB Demo Hack
        SBBFeatureEditor sbbFeatureEditor = new SBBFeatureEditor(Display.getDefault().getActiveShell());
        if (sbbFeatureEditor.open() == Window.CANCEL) {
        	return;
        } else {
        	((SimpleFeature)newFeature).setAttribute("name", sbbFeatureEditor.name);
        	((SimpleFeature)newFeature).setAttribute("type", sbbFeatureEditor.type);
        	
        }
	}

}
