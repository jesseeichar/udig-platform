package com.camptocamp.sbb;

import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;

import com.camptocamp.sbb.views.BranchView;

public class SBBPerspectiveFactory1 implements IPerspectiveFactory {
	public static final String ID_PERSPECTIVE = "com.camptocamp.sbb.perspective1"; //$NON-NLS-1$

	@Override
	public void createInitialLayout(IPageLayout layout) {
        String editorArea = layout.getEditorArea();
        layout.addView(BranchView.ID, IPageLayout.RIGHT, 0.7f, editorArea);
	}

}
