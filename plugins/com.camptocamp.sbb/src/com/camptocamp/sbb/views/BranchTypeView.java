package com.camptocamp.sbb.views;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;
import org.locationtech.geogig.api.GeoGIG;
import org.locationtech.geogig.api.NodeRef;
import org.locationtech.geogig.api.Ref;
import org.locationtech.geogig.api.data.FindFeatureTypeTrees;
import org.locationtech.geogig.api.porcelain.BranchCreateOp;
import org.locationtech.geogig.api.porcelain.BranchDeleteOp;
import org.locationtech.geogig.api.porcelain.BranchListOp;
import org.locationtech.geogig.api.porcelain.CheckoutOp;
import org.locationtech.geogig.api.porcelain.ResetOp;
import org.locationtech.geogig.api.porcelain.ResetOp.ResetMode;
import org.locationtech.udig.project.IMap;
import org.locationtech.udig.project.command.Command;
import org.locationtech.udig.project.internal.command.navigation.AbstractNavCommand;
import org.locationtech.udig.project.internal.command.navigation.NavComposite;
import org.locationtech.udig.project.internal.command.navigation.SetViewportCenterCommand;
import org.locationtech.udig.project.internal.commands.SetScaleCommand;
import org.locationtech.udig.project.ui.ApplicationGIS;

import com.camptocamp.sbb.Activator;
import com.camptocamp.sbb.BranchMerge;
import com.camptocamp.sbb.LayerToAdd;
import com.camptocamp.sbb.Styling;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.vividsolutions.jts.geom.Coordinate;

/**
 * This sample class demonstrates how to plug-in a new workbench view. The view
 * shows data obtained from the model. The sample creates a dummy model on the
 * fly, but a real implementation would connect to the model available either in
 * this or another plug-in (e.g. the workspace). The view is connected to the
 * model using a content provider.
 * <p>
 * The view uses a label provider to define how model objects should be
 * presented in the view. Each view can present the same model objects using
 * different labels and icons, if needed. Alternatively, a single label provider
 * can be shared between views in order to ensure that objects of the same type
 * are presented in the same way everywhere.
 * <p>
 */

public class BranchTypeView extends ViewPart {

	/**
	 * The ID of the view as specified by the extension.
	 */

	public static final String ID = "com.camptocamp.sbb.views.BranchView";

	public TreeViewer viewer;
	private Action showInMap;
	private Action createBranch;
	private Action doubleClickAction;
	private Multimap<Ref, NodeRef> data = LinkedHashMultimap.create();

	private Action addToMap;

	private Action deleteBranch;

	private Action openLog;

	private Action merge;
	private BranchMerge currentMerge;


	class ViewContentProvider implements IStructuredContentProvider,
			ITreeContentProvider {

		public void inputChanged(Viewer v, Object oldInput, Object newInput) {
		}

		public void dispose() {
		}

		public Object[] getElements(Object parent) {
			if (parent.equals(getViewSite())) {
				Set<Ref> branches = data.keySet();
				return branches.toArray(new Ref[branches.size()]);
			}
			return getChildren(parent);
		}

		public Object getParent(Object child) {
			for (Ref ref : data.keySet()) {
				for (NodeRef nRef : data.get(ref)) {
					if (nRef == child) {
						return ref;
					}
				}
			}
			return null;
		}

		public Object[] getChildren(Object parent) {
			if (parent instanceof Ref) {
				Collection<NodeRef> list = data.get((Ref) parent);
				return list.toArray(new NodeRef[list.size()]);
			}
			return new Object[0];
		}

		public boolean hasChildren(Object parent) {
			if (parent instanceof Ref)
				return true;
			return false;
		}
	}

	class ViewLabelProvider extends LabelProvider {

		public String getText(Object obj) {
			if (obj instanceof Ref) {
				Ref ref = (Ref) obj;
				return ref.localName();
			} else if (obj instanceof NodeRef) {
				NodeRef ref = (NodeRef) obj;
				return ref.name();
			}
			return obj.toString();
		}

		public Image getImage(Object obj) {
			String imageKey = ISharedImages.IMG_OBJ_ELEMENT;
			if (obj instanceof Ref)
				imageKey = ISharedImages.IMG_OBJ_FOLDER;
			return PlatformUI.getWorkbench().getSharedImages()
					.getImage(imageKey);
		}
	}

	class NameSorter extends ViewerSorter {
	}

	/**
	 * The constructor.
	 */
	public BranchTypeView() {
	}

	/**
	 * This is a callback that will allow us to create the viewer and initialize
	 * it.
	 */
	public void createPartControl(Composite parent) {

		warmCache();

		viewer = new TreeViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
		viewer.setContentProvider(new ViewContentProvider());
		viewer.setLabelProvider(new ViewLabelProvider());
		viewer.setSorter(new NameSorter());
		viewer.setInput(getViewSite());

		// Create the help context id for the viewer's control
		PlatformUI.getWorkbench().getHelpSystem()
				.setHelp(viewer.getControl(), "aaaaaa.viewer");
		makeActions();
		hookContextMenu();
		hookDoubleClickAction();
		contributeToActionBars();
		getViewSite().setSelectionProvider(viewer);
	}

	public void warmCache() {
		data.clear();
		BranchListOp branchListOp = getGeoGig().command(BranchListOp.class);
		Collection<Ref> branches = branchListOp.call();
		for (Ref ref : branches) {
			FindFeatureTypeTrees cmd = getGeoGig().command(
					FindFeatureTypeTrees.class).setRootTreeRef(ref.getName());
			List<NodeRef> list = cmd.call();
			data.putAll(ref, list);
		}
	}

	private GeoGIG getGeoGig() {
		return Activator.getDefault().getGeoGig();
	}

	private void hookContextMenu() {
		MenuManager menuMgr = new MenuManager("#PopupMenu");
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(IMenuManager manager) {
				BranchTypeView.this.fillContextMenu(manager);
			}
		});
		Menu menu = menuMgr.createContextMenu(viewer.getControl());
		viewer.getControl().setMenu(menu);
		getSite().registerContextMenu(menuMgr, viewer);
	}

	private void contributeToActionBars() {
		IActionBars bars = getViewSite().getActionBars();
		fillLocalPullDown(bars.getMenuManager());
		fillLocalToolBar(bars.getToolBarManager());
	}

	private void fillLocalPullDown(IMenuManager manager) {
	}

	private void fillContextMenu(IMenuManager manager) {
		ISelectionProvider selectionProvider = getViewSite()
				.getSelectionProvider();
		if (selectionProvider != null) {
			ISelection selection = selectionProvider.getSelection();
			if (selection instanceof StructuredSelection) {
				StructuredSelection ss = (StructuredSelection) selection;
				if (ss.getFirstElement() instanceof NodeRef) {
					manager.add(addToMap);
					manager.add(showInMap);
				} else if (ss.getFirstElement() instanceof Ref) {
					Ref ref = (Ref) ss.getFirstElement();
					manager.add(createBranch);
					manager.add(deleteBranch);
					manager.add(openLog);
					if (!ref.localName().equals("master")) {
						manager.add(merge);
					}
				}
			}
		}
	}

	private void fillLocalToolBar(IToolBarManager manager) {
		addZoomAction(getViewSite(), manager);
		addFinalizeMergeAction(manager);
	}

	private void addFinalizeMergeAction(IToolBarManager manager) {
		manager.add(new Action("Complete Merge") {
			@Override
			public void run() {
				if (currentMerge != null) {
					currentMerge.finishMerge();
					currentMerge = null;
				}
			}
		});
		
	}

	public static void addZoomAction(final IViewSite site, IToolBarManager manager) {
		manager.add(new Action("Zoom") {
			@Override
			public void run() {
				zoom(site, ApplicationGIS.getActiveMap());
			}

		});
	}
	public static void zoom(IViewSite site, IMap map) {
		SetViewportCenterCommand centerCmd = new SetViewportCenterCommand(new Coordinate(802826, 182297));
		AbstractNavCommand scale = new AbstractNavCommand() {
			
			@Override
			public String getName() {
				return "Set Scale";
			}
			
			@Override
			public Command copy() {
				return null;
			}
			
			@Override
			protected void runImpl(IProgressMonitor monitor) throws Exception {
				SetScaleCommand scale = new SetScaleCommand(8343);
				scale.setMap(getMap());
				scale.run(monitor);
			}
		};
		map.sendCommandASync(new NavComposite(Lists.newArrayList(centerCmd, scale)));
		if (site != null) {
			site.getPage().activate(site.getPage().getActiveEditor());
		}
	}

	private void makeActions() {
		createShowInMapAction();
		createAddToMapAction();
		createCreateBranchAction();
		createDeleteBranchAction();
		createOpenLog();
		createMergeAction();
		
		doubleClickAction = new Action() {
			public void run() {
				Object selEl = ((StructuredSelection) viewer.getSelection()).getFirstElement();
				if (selEl instanceof Ref) {
					Ref ref = (Ref) selEl;
					viewer.setExpandedState(ref, true);
				} else {
					showInMap.run();
				}
			}
		};
	}

	private void createMergeAction() {
		merge = new Action() {
			public void run() {
				Ref ref = (Ref) ((StructuredSelection) viewer.getSelection()).getFirstElement();
				currentMerge = new BranchMerge(ref, "railways", BranchTypeView.this);
				currentMerge.execute();
			}
		};
		merge.setText("Merge");
		merge
				.setImageDescriptor(PlatformUI
						.getWorkbench()
						.getSharedImages()
						.getImageDescriptor(
								org.locationtech.udig.project.ui.internal.ISharedImages.DELETE));
	}
	
	private void createDeleteBranchAction() {
		deleteBranch = new Action() {
			public void run() {
				CheckoutOp checkoutOp = getGeoGig().command(CheckoutOp.class);
				checkoutOp.setForce(true);
				checkoutOp.setSource("master");
				checkoutOp.call();
				
				Ref ref = (Ref) ((StructuredSelection) viewer.getSelection())
						.getFirstElement();
				BranchDeleteOp createOp = getGeoGig().command(
						BranchDeleteOp.class);
				createOp.setName(ref.getName());
				createOp.call();
				
				warmCache();
				viewer.refresh();
			}
		};
		deleteBranch.setText("Delete Branch");
		deleteBranch
		.setImageDescriptor(PlatformUI
				.getWorkbench()
				.getSharedImages()
				.getImageDescriptor(
						org.locationtech.udig.project.ui.internal.ISharedImages.DELETE));
	}

	private void createCreateBranchAction() {
		createBranch = new Action() {
			public void run() {
				String branchName = Activator.openInputDialog(viewer.getControl()
						.getShell(), "Create Branch", "Enter name of branch:");
				if (branchName != null) {
					Ref ref = (Ref) ((StructuredSelection) viewer
							.getSelection()).getFirstElement();

					getGeoGig().command(ResetOp.class).setMode(ResetMode.HARD).call();
					
					BranchCreateOp createOp = getGeoGig().command(
							BranchCreateOp.class);
					createOp.setForce(true);
					createOp.setName(branchName);
					createOp.setAutoCheckout(true);
					createOp.setSource(ref.getName());
					createOp.call();
					warmCache();
					viewer.refresh();
					Activator.getDefault().showInMap(getSite(), "railways (" + branchName + ")", 
							new LayerToAdd(branchName, "railways", Styling.createTrackStyle()));
				}
			}
		};
		createBranch.setText("Create Branch");
		createBranch
				.setImageDescriptor(PlatformUI
						.getWorkbench()
						.getSharedImages()
						.getImageDescriptor(
								org.locationtech.udig.project.ui.internal.ISharedImages.LINK));
	}

	private void createShowInMapAction() {
		showInMap = new Action() {
			public void run() {
				NodeRef firstElement = (NodeRef) ((StructuredSelection) viewer
						.getSelection()).getFirstElement();
				Ref ref = (Ref) ((ViewContentProvider) viewer.getContentProvider()).getParent(firstElement);
				String featureTypeName = firstElement.name();
				Activator.getDefault().showInMap(getViewSite(), featureTypeName + " (" + ref.localName() + ")", 
						new LayerToAdd(ref.getName(), featureTypeName, Styling.createTrackStyle()));
			}
		};
		showInMap.setText("Show in Map");
		showInMap
				.setImageDescriptor(PlatformUI
						.getWorkbench()
						.getSharedImages()
						.getImageDescriptor(
								org.locationtech.udig.project.ui.internal.ISharedImages.MAP_OBJ));
	}

	private void createAddToMapAction() {
		addToMap = new Action() {
			public void run() {
				NodeRef firstElement = (NodeRef) ((StructuredSelection) viewer
						.getSelection()).getFirstElement();
				Ref ref = (Ref) ((ViewContentProvider) viewer
						.getContentProvider()).getParent(firstElement);
				String featureTypeName = firstElement.name();
				Activator.getDefault().addToMap(getViewSite(), featureTypeName + " (" + ref.localName() + ")", 
						new LayerToAdd(ref.getName(), featureTypeName, Styling.createTrackStyle()));
			}
		};
		addToMap.setText("Add to Map");
		addToMap.setToolTipText("Add to current map or open map if no map is currently open");
		addToMap.setImageDescriptor(PlatformUI
				.getWorkbench()
				.getSharedImages()
				.getImageDescriptor(
						org.locationtech.udig.project.ui.internal.ISharedImages.LAYER_OBJ));
	}
	
	private void createOpenLog() {
		openLog = new Action() {
			public void run() {
				Ref ref = (Ref) ((StructuredSelection) viewer
						.getSelection()).getFirstElement();
				GeogigLogViewer view;
				try {
					view = (GeogigLogViewer) getSite().getPage().showView(GeogigLogViewer.ID);
				} catch (PartInitException e) {
					e.printStackTrace();
					return;
				}
				view.openLog(ref);
			}
		};

		openLog.setText("Show Log");
		openLog.setImageDescriptor(PlatformUI
				.getWorkbench()
				.getSharedImages()
				.getImageDescriptor(
						org.locationtech.udig.project.ui.internal.ISharedImages.LAYER_OBJ));
	}

	private void hookDoubleClickAction() {
		viewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {
				doubleClickAction.run();
			}
		});
	}

	/**
	 * Passing the focus request to the viewer's control.
	 */
	public void setFocus() {
		viewer.getControl().setFocus();
	}
}