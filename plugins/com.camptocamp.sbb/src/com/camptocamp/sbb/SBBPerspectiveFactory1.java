package com.camptocamp.sbb;

import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;
import org.locationtech.udig.project.ui.internal.LayersView;

import com.camptocamp.sbb.views.BranchTypeView;
import com.camptocamp.sbb.views.GeogigLogViewer;
import com.camptocamp.sbb.views.WPSView;

public class SBBPerspectiveFactory1 implements IPerspectiveFactory {
	public static final String ID_PERSPECTIVE = "com.camptocamp.sbb.perspective1"; //$NON-NLS-1$

	@Override
	public void createInitialLayout(IPageLayout layout) {
        String editorArea = layout.getEditorArea();
        layout.addView(LayersView.ID, IPageLayout.RIGHT, 0.8f, editorArea);
        
        IFolderLayout folder = layout.createFolder(ID_PERSPECTIVE + "geogig", IPageLayout.BOTTOM, 0.7f, editorArea);
        folder.addView(BranchTypeView.ID);
        folder.addView(GeogigLogViewer.ID);
        folder.addView(WPSView.ID);
	}

}
