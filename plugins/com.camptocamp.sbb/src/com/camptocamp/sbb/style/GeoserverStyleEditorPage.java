package com.camptocamp.sbb.style;

import static com.camptocamp.sbb.GeoserverRest.GEOSERVER_URL;
import static com.camptocamp.sbb.GeoserverRest.execRestDom;
import static com.camptocamp.sbb.GeoserverRest.execRestInputStream;

import java.io.InputStream;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.styling.SLD;
import org.geotools.styling.SLDParser;
import org.geotools.styling.Style;
import org.geotools.styling.StyledLayerDescriptor;
import org.locationtech.geogig.api.NodeRef;
import org.locationtech.geogig.api.Ref;
import org.locationtech.udig.style.sld.editor.StyleEditor;
import org.locationtech.udig.style.sld.editor.StyleEditorPage;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.camptocamp.sbb.Activator;
import com.camptocamp.sbb.GeoserverRest;
import com.google.common.base.Function;

public class GeoserverStyleEditorPage extends StyleEditorPage {

	private TreeViewer viewer;
	private Object[] STYLES = null;

	public GeoserverStyleEditorPage() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public boolean okToLeave() {
		return true;
	}

	@Override
	public boolean performOk() {
		return performApply();
	}

	@Override
	public boolean performApply() {
		Boolean done = false;
		ISelection sel = viewer.getSelection();
		if (sel instanceof StructuredSelection) {
			StructuredSelection ss = (StructuredSelection) sel;
			if (ss.getFirstElement() != null) {
				Object layer = ss.getFirstElement();
				done = execRestInputStream(GEOSERVER_URL + "rest/styles/" + layer + ".sld", new Function<InputStream, Boolean>() {

					@Override
					public Boolean apply(InputStream inputStream) {

						SLDParser stylereader = new SLDParser(CommonFactoryFinder.getStyleFactory(), inputStream);
						StyledLayerDescriptor sld = stylereader.parseSLD();
						Style[] style = SLD.styles(sld);
						if (style.length > 0) {
							setStyle(style[0]);
						}
						return true;
					}

				});
			}
		}
		return done != null && done;
	}

	@Override
	public void refresh() {
		STYLES = null;
		viewer.refresh();
	}

	@Override
	public void createPageContent(Composite parent) {
		viewer = new TreeViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);

		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		viewer.getControl().setLayoutData(gd);
		viewer.setContentProvider(new ViewContentProvider());
		viewer.setLabelProvider(new ViewLabelProvider());
		viewer.setSorter(new NameSorter());
		viewer.setInput(this);

		hookContextMenu();
	}

	private void hookContextMenu() {
		MenuManager menuMgr = new MenuManager("#PopupMenu");
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(IMenuManager manager) {
				GeoserverStyleEditorPage.this.fillContextMenu(manager);
			}
		});
		Menu menu = menuMgr.createContextMenu(viewer.getControl());
		viewer.getControl().setMenu(menu);
	}

	protected void fillContextMenu(IMenuManager manager) {
		ISelection selection = viewer.getSelection();
		if (selection instanceof StructuredSelection) {
			StructuredSelection ss = (StructuredSelection) selection;
			final String selValue = (String) ss.getFirstElement();
			if (selValue != null) {
				final String sld = StyleEditor.styleToXML(getSLD());

				if (selValue.equals(GEOSERVER_URL)) {
					manager.add(new Action("Create New Style") {
						@Override
						public void run() {
							String newName = Activator.openInputDialog(getShell(), "Style Name", "Enter the name of the new style");
							String createXml = "<style><name>" + newName + "</name><filename>" + newName + ".sld</filename></style>";
							if (checkUpdate(GeoserverRest.post(GEOSERVER_URL + "rest/styles", "POST", "text/xml", createXml), false)) {
								doUpdate(newName, sld);
								refresh();
							}
						}
					});
				} else {
					manager.add(new Action("Download and apply") {
						@Override
						public void run() {
							performApply();
				            getSelectedLayer().apply();
						}
					});
					manager.add(new Action("Update Geoserver") {
						@Override
						public void run() {
							doUpdate(selValue, sld);
						}
					});
				}
			}
		}

	}

	boolean doUpdate(String name, String sld) {
		return checkUpdate(GeoserverRest.post(GEOSERVER_URL + "rest/styles/" + name, "PUT", "application/vnd.ogc.sld+xml", sld), true);
	}
	
	boolean checkUpdate(int responseCode, boolean showSuccess) {
		if (responseCode > 299) {
			MessageDialog.openWarning(getShell(), "Update Failure", "Style update failed with " + responseCode + " reponse code.");
			return false;
		}
		if (showSuccess) {
		MessageDialog.openInformation(getShell(), "Success", "Update success");
		}
		return true;
	}

	@Override
	public String getLabel() {
		return "Geoserver";
	}

	@Override
	public boolean performCancel() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String getErrorMessage() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void gotFocus() {
		// TODO Auto-generated method stub

	}

	@Override
	public void styleChanged(Object source) {
		// TODO Auto-generated method stub

	}

	class ViewContentProvider implements IStructuredContentProvider, ITreeContentProvider {

		public void inputChanged(Viewer v, Object oldInput, Object newInput) {
		}

		public void dispose() {
		}

		public Object[] getElements(Object parent) {
			if (parent.equals(GeoserverStyleEditorPage.this)) {
				return new String[] { GEOSERVER_URL };
			}
			return getChildren(parent);
		}

		public Object getParent(Object child) {
			return null;
		}

		private void loadStyles() {
			STYLES = execRestDom(GEOSERVER_URL + "rest/styles.xml", new Function<Document, String[]>() {

				@Override
				public String[] apply(Document doc) {
					NodeList nodes = doc.getElementsByTagName("name");

					String[] styles = new String[nodes.getLength()];
					for (int i = 0; i < nodes.getLength(); i++) {
						Element element = (Element) nodes.item(i);
						styles[i] = element.getTextContent();
					}
					return styles;
				}
			});
		}

		public Object[] getChildren(Object parent) {
			if (STYLES == null) {
				loadStyles();
			}
			return STYLES;
		}

		public boolean hasChildren(Object parent) {
			return parent.equals(GEOSERVER_URL);
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
			return PlatformUI.getWorkbench().getSharedImages().getImage(imageKey);
		}
	}

	class NameSorter extends ViewerSorter {
	}

}
