package com.camptocamp.sbb;

import org.eclipse.jface.action.ICoolBarManager;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.ui.IWorkbenchWindow;
import org.locationtech.udig.ui.MenuBuilder;

public class MenuBuilder1 implements MenuBuilder {

	@Override
	public void fillMenuBar(IMenuManager menuBar, IWorkbenchWindow window) {
		System.out.println("Build menu");

	}

	@Override
	public void fillCoolBar(ICoolBarManager coolBar, IWorkbenchWindow window) {
		System.out.println("Build menu");

	}

}
