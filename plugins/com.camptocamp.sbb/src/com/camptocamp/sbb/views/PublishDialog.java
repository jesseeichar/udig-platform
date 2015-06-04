package com.camptocamp.sbb.views;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

public class PublishDialog extends Dialog {

	private String name;
	private Text text;
	protected PublishDialog(Shell parentShell) {
		super(parentShell);
	}

	@Override
	protected Point getInitialSize() {
		return new Point(600, 600);
	}
	@Override
	protected Control createDialogArea(Composite parent) {
		Composite comp = (Composite) super.createDialogArea(parent);
		((GridLayout)comp.getLayout()).numColumns = 2;

		getShell().setText("Processor Properties");
		addLabel(comp, "Published Layer Name:");
		this.text = new org.eclipse.swt.widgets.Text(comp, SWT.BORDER);
		this.text.setText("wps_output");
		text.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

		return comp;
	}

	private void addLabel(Composite comp, String textLabel) {
		Label label = new Label(comp, SWT.NONE);
		label.setText(textLabel);
		label.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
	}
	
	@Override
	protected void okPressed() {
		
		this.name = text.getText();
		
		close();
	}

	public String getFeatureTypeName() {
		return this.name;
	}
}
