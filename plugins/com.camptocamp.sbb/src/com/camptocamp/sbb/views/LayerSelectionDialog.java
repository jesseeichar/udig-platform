package com.camptocamp.sbb.views;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

public class LayerSelectionDialog extends Dialog {

	TableViewer viewer;
	private String featureType;
	private String srs;
	private Text text;
	private Composite databaseComp;
	private Composite wfsComp;
	private Composite uploadComp;
	private Combo table;
	protected LayerSelectionDialog(Shell parentShell) {
		super(parentShell);
	}

	@Override
	protected Point getInitialSize() {
		return new Point(600, 600);
	}
	@Override
	protected Control createDialogArea(Composite parent) {
		final Composite comp = (Composite) super.createDialogArea(parent);
		((GridLayout)comp.getLayout()).numColumns = 2;

		getShell().setText("Processor Properties");

		addLabel(comp, "Output SRS:");
		this.text = new org.eclipse.swt.widgets.Text(comp, SWT.BORDER);
		text.setText("4326");
		text.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

		addLabel(comp, "Input Type:");
		final Combo inputType = new Combo(comp, SWT.BORDER | SWT.READ_ONLY);
		inputType.setItems(new String[]{"Upload Data", "Web Feature Server", "Database"});
		inputType.select(2);
		final Composite contentPanel = new Composite (comp, SWT.NONE);
		final StackLayout layout = new StackLayout ();
		inputType.addSelectionListener(new SelectionListener() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				switch(inputType.getSelectionIndex()){
				case 0:
					layout.topControl = uploadComp;
					break;
				case 1:
					layout.topControl = wfsComp;
					break;
				default:
					layout.topControl = databaseComp;
				}
				contentPanel.layout ();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});
		
		contentPanel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
		contentPanel.setLayout (layout);
		addUploadComp(contentPanel);
		addWFSComp(contentPanel);
		addDatabaseComp(contentPanel);
		
		layout.topControl = databaseComp;
		return comp;
	}

	private void addUploadComp(Composite comp) {
		this.uploadComp = createContainerComp(comp);
		
		addLabel(uploadComp, "Feature Type:");
		viewer = new TableViewer(uploadComp);
		viewer.setContentProvider(new ArrayContentProvider());
		viewer.setLabelProvider(new LabelProvider());
		String kantone = "Kantone";
		viewer.setInput(new String[]{kantone, "Railways"});
		viewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		viewer.setSelection(new StructuredSelection(new String[]{kantone}));
	}
	private void addWFSComp(Composite comp) {
		this.wfsComp = createContainerComp(comp);
		addLabel(wfsComp, "Server URL:");
		Text tmp = new org.eclipse.swt.widgets.Text(wfsComp, SWT.BORDER);
		tmp.setText("http://...");
		tmp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		addLabel(wfsComp, "Feature Type:");
		viewer = new TableViewer(wfsComp);
		viewer.setContentProvider(new ArrayContentProvider());
		viewer.setLabelProvider(new LabelProvider());
		viewer.setInput(new String[]{});
		viewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
	}
	private void addDatabaseComp(Composite comp) {
		this.databaseComp = createContainerComp(comp);

		addLabel(databaseComp, "Type:");
		Combo tmp = new Combo(databaseComp, SWT.BORDER);
		tmp.setItems(new String[]{"DB2", "MySQL", "PostGIS", "Oracle", "Sql Server"});
		tmp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		tmp.select(2);
		
		addLabel(databaseComp, "Server:");
		Text server = new org.eclipse.swt.widgets.Text(databaseComp, SWT.BORDER);
		server.setText("localhost");
		server.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		addLabel(databaseComp, "Port:");
		Text port = new org.eclipse.swt.widgets.Text(databaseComp, SWT.BORDER);
		port.setText("5432");
		port.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		addLabel(databaseComp, "Database:");
		Text database = new org.eclipse.swt.widgets.Text(databaseComp, SWT.BORDER);
		database.setText("sbb_demo");
		database.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		addLabel(databaseComp, "Table:");
		this.table = new Combo(databaseComp, SWT.BORDER | SWT.READ_ONLY);
		table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		table.setItems(new String[]{"Countries", "Gemeinden", "Kantone"});
		table.select(2);
		
	    Button auth = new Button(databaseComp, SWT.CHECK);
	    auth.setText("Authentication");
	    auth.setSelection(true);
		auth.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
	    
	    addLabel(databaseComp, "Username:");
		Text username = new org.eclipse.swt.widgets.Text(databaseComp, SWT.BORDER);
		username.setText("www-data");
		username.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		addLabel(databaseComp, "Username:");
		Text password = new org.eclipse.swt.widgets.Text(databaseComp, SWT.BORDER | SWT.PASSWORD);
		password.setText("www-data");
		password.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
	}

	private Composite createContainerComp(Composite comp) {
		Composite uploadComp = new Composite(comp, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		layout.verticalSpacing = convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_SPACING);
		layout.horizontalSpacing = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_SPACING);
		layout.numColumns = 2;
		uploadComp.setLayout(layout);
		return uploadComp;
	}

	private void addLabel(Composite comp, String textLabel) {
		Label label = new Label(comp, SWT.NONE);
		label.setText(textLabel);
		label.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
	}
	
	@Override
	protected void okPressed() {
		this.featureType = this.table.getItem(this.table.getSelectionIndex());
		this.srs = text.getText();
		
		close();
	}

	public String getFeatureType() {
		return this.featureType;
	}

	public String getSrs() {
		return this.srs;
	}
}
