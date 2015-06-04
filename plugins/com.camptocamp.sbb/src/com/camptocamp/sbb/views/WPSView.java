package com.camptocamp.sbb.views;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;
import org.locationtech.udig.ui.PlatformGIS;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.camptocamp.sbb.GeoserverRest;
import com.camptocamp.sbb.process.WpsProcess;
import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

public class WPSView extends ViewPart {

	enum ViewerType {
		CHAIN, PROCESS_LIST
	}

	/**
	 * The ID of the view as specified by the extension.
	 */
	public static final String ID = "com.camptocamp.sbb.views.WPS";

	private TableViewer processList;
	private TableViewer chainViewer;

	private WpsProcess[] processes;
	private List<WpsProcess> chain = Lists.newArrayList();

	private LayerSelectionDialog layerSelectionDialog;

	private PublishDialog publishDialog;

	/*
	 * The content provider class is responsible for providing objects to the
	 * view. It can wrap existing objects in adapters or simply return objects
	 * as-is. These objects may be sensitive to the current input of the view,
	 * or ignore it and always show the same content (like Task List, for
	 * example).
	 */

	class ViewContentProvider implements IStructuredContentProvider {
		public void inputChanged(Viewer v, Object oldInput, Object newInput) {
		}

		public void dispose() {
		}

		public Object[] getElements(Object parent) {
			switch ((ViewerType) parent) {
			case CHAIN:
				return chain.toArray(new WpsProcess[chain.size()]);
			default:
				return processes;
			}
		}
	}

	class ViewLabelProvider extends LabelProvider implements ITableLabelProvider {
		public String getColumnText(Object obj, int index) {
			return obj.toString();
		}

		public Image getColumnImage(Object obj, int index) {
			return getImage(obj);
		}

		public Image getImage(Object obj) {
			return PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_ELEMENT);
		}

	}

	class NameSorter extends ViewerSorter {
	}

	/**
	 * The constructor.
	 */
	public WPSView() {
	}

	/**
	 * This is a callback that will allow us to create the viewer and initialize
	 * it.
	 */
	public void createPartControl(Composite parent) {
		loadProcesses();

		ISelectionChangedListener showAbstract = new ISelectionChangedListener() {

			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				String msg = "";
				if (event.getSelection() instanceof StructuredSelection && !event.getSelection().isEmpty()) {
					WpsProcess selection = (WpsProcess) ((StructuredSelection) event.getSelection()).getFirstElement();
					msg = selection.abstr;
				}
				getViewSite().getActionBars().getStatusLineManager().setMessage(msg);
			}
		};

		Composite composite = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		composite.setLayout(layout);
		processList = new TableViewer(composite, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
		processList.setContentProvider(new ViewContentProvider());
		processList.setLabelProvider(new ViewLabelProvider());
		// processList.setSorter(new NameSorter());
		processList.setInput(ViewerType.PROCESS_LIST);
		processList.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		processList.addSelectionChangedListener(showAbstract);

		chainViewer = new TableViewer(composite, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
		chainViewer.setContentProvider(new ViewContentProvider());
		chainViewer.setLabelProvider(new ViewLabelProvider());
		chainViewer.setInput(ViewerType.CHAIN);
		chainViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		chainViewer.addSelectionChangedListener(showAbstract);

		makeActions();
		hookContextMenu();
		hookDoubleClickAction(processList);
		hookDoubleClickAction(chainViewer);
		contributeToActionBars();
	}

	private void loadProcesses() {
		this.processes = GeoserverRest.execRestDom(GeoserverRest.GEOSERVER_URL + "ows?service=WPS&version=1.0.0&request=GetCapabilities",
				new Function<Document, WpsProcess[]>() {

					@Override
					public WpsProcess[] apply(Document doc) {
						NodeList operationEls = doc.getElementsByTagName("wps:Process");
						Set<WpsProcess> processes = Sets.newTreeSet();

						for (int i = 0; i < operationEls.getLength(); i++) {
							Element element = (Element) operationEls.item(i);
							String id = element.getElementsByTagName("ows:Identifier").item(0).getTextContent();
							String title = element.getElementsByTagName("ows:Title").item(0).getTextContent();
							String abstr = element.getElementsByTagName("ows:Abstract").item(0).getTextContent();
							processes.add(new WpsProcess(title, id, abstr));
						}
						return processes.toArray(new WpsProcess[processes.size()]);
					}

				});
	}

	private void hookContextMenu() {
		MenuManager menuMgr = new MenuManager("#PopupMenu");
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(IMenuManager manager) {
				WPSView.this.fillContextMenu(manager);
			}
		});
		Menu menu = menuMgr.createContextMenu(chainViewer.getControl());
		chainViewer.getControl().setMenu(menu);
		getSite().registerContextMenu(menuMgr, chainViewer);
	}

	private void contributeToActionBars() {
		IActionBars bars = getViewSite().getActionBars();
		fillLocalPullDown(bars.getMenuManager());
		fillLocalToolBar(bars.getToolBarManager());
	}

	private void fillLocalPullDown(IMenuManager manager) {
	}

	private void fillContextMenu(IMenuManager manager) {
		manager.add(new Action("Properties") {
			@Override
			public void run() {
				StructuredSelection sel = (StructuredSelection) chainViewer.getSelection();
				if (sel != null && !sel.isEmpty()) {
					WpsProcess process = (WpsProcess) sel.getFirstElement();
					if (process.id.toLowerCase().contains("reproject")) {
						openTransformPropertiesDialog();
					} else {
						openPublishPropertiesDialog();
					}
				}
			}
		});
	}

	private void fillLocalToolBar(IToolBarManager manager) {
		manager.add(new Action("Execute") {
			@Override
			public void run() {
				PlatformGIS.runInProgressDialog("Executing Process Chain", true, new IRunnableWithProgress() {

					@Override
					public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
						monitor.beginTask("Execute Process Chain", 4);
						monitor.worked(1);
						try {
							String postgisConfig = "<dataStore>" +
									"  <name>sbb_demo</name>\n" +
									"  <connectionParameters>\n" +
									"    <host>localhost</host>\n" +
									"    <port>5432</port>\n" +
									"    <database>sbb_demo</database>\n" +
									"    <user>www-data</user>\n" +
									"    <passwd>www-data</passwd>\n" +
									"    <dbtype>postgis</dbtype>\n" +
									"  </connectionParameters>\n" +
									"</dataStore>";
							GeoserverRest.exec(GeoserverRest.GEOSERVER_URL + "rest/workspaces/topp/datastores", "POST", "text/xml", postgisConfig);
							String inputLayer = layerSelectionDialog.getFeatureType().toLowerCase();
							String layerConfig = String.format("<featureType><name>%s</name></featureType>", inputLayer);
							GeoserverRest.exec(GeoserverRest.GEOSERVER_URL + "rest/workspaces/topp/datastores/sbb_demo/featuretypes", "POST", "text/xml", layerConfig);
							Boolean layerExists = GeoserverRest.execRestDom(GeoserverRest.GEOSERVER_URL + "rest/workspaces/topp/datastores/sbb_demo/featuretypes/" + inputLayer + ".xml", new Function<Document, Boolean>() {

								@Override
								public Boolean apply(Document doc) {
									return true;
								}
							});
							
							if (!layerExists) {
								System.out.println("Publishing layer failed");
							}

							String outputLayer = publishDialog.getFeatureTypeName().toLowerCase();
							GeoserverRest.exec(GeoserverRest.GEOSERVER_URL + "rest/workspaces/topp/datastores/processing/featuretypes/" + outputLayer, "DELETE", null, null);
							String srs = layerSelectionDialog.getSrs();
							monitor.worked(1);
							String template = IOUtils.toString(getClass().getResource("/wps.template.xml"));
							String request = template.replace("@@input_type@@", inputLayer).
									replace("@@layer_name@@", outputLayer).
									replace("@@SRS@@", srs);
							monitor.worked(1);
							GeoserverRest.exec(GeoserverRest.GEOSERVER_URL + "wps", "POST", "application/xml",
									request);
							monitor.worked(1);
							
							GeoserverRest.exec(GeoserverRest.GEOSERVER_URL + "rest/workspaces/topp/datastores/sbb_demo?recurse=true", "DELETE", null, null);
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						monitor.done();
					}
				}, true);
			}
		});
	}

	private void makeActions() {
	}

	private void hookDoubleClickAction(final TableViewer viewer) {
		viewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {

				if (viewer == chainViewer) {
					WpsProcess selection = getSelection(chainViewer, WpsProcess.class);
					if (selection != null) {
						chain.remove(selection);
					}
				} else {
					WpsProcess selection = getSelection(processList, WpsProcess.class);
					if (selection != null) {
						add(selection);
					}
				}
				chainViewer.refresh();
			}

			private <T> T getSelection(TableViewer chainViewer, Class<T> returnType) {
				ISelection selection = chainViewer.getSelection();
				if (selection != null && !selection.isEmpty() && selection instanceof StructuredSelection) {
					StructuredSelection ss = (StructuredSelection) selection;
					return returnType.cast(ss.getFirstElement());
				}
				return null;
			}
		});
	}

	protected void add(WpsProcess selection) {
		if (chain.isEmpty()) {
			openTransformPropertiesDialog();
			chain.add(selection);
		} else {
			openPublishPropertiesDialog();
			chain.add(selection);
		}
	}

	private void openPublishPropertiesDialog() {
		this.publishDialog = new PublishDialog(getSite().getShell());
		this.publishDialog.open();
	}

	private void openTransformPropertiesDialog() {
		this.layerSelectionDialog = new LayerSelectionDialog(getSite().getShell());
		this.layerSelectionDialog.open();
	}

	/**
	 * Passing the focus request to the viewer's control.
	 */
	public void setFocus() {
		processList.getControl().setFocus();
	}

}
