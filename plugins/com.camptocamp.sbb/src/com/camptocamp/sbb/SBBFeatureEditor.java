package com.camptocamp.sbb;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

public class SBBFeatureEditor extends Dialog {

	private Text nameText;
	private Combo typeCombo;
	String name, type;

	protected SBBFeatureEditor(Shell parentShell) {
		super(parentShell);
		// TODO Auto-generated constructor stub
	}
	
	@Override
	protected Control createDialogArea(Composite parent) {
		Composite comp = (Composite) super.createDialogArea(parent);
		((GridLayout)comp.getLayout()).numColumns = 2;

		addLabel(comp, "Name:");
		this.nameText = new org.eclipse.swt.widgets.Text(comp, SWT.BORDER);
		nameText.setText("Sagliains ");
		nameText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		addLabel(comp, "Type:");
		this.typeCombo = new Combo(comp, SWT.BORDER | SWT.READ_ONLY);
		this.typeCombo.setItems(new String[]{"Station", "Park and Ride", "Information"});
		this.typeCombo.select(2);
		
		return comp;
	}
	@Override
	protected void okPressed() {
		this.name = nameText.getText();
		switch (this.typeCombo.getSelectionIndex()) {
		case 0:
			this.type = "station";
			break;
		case 1:
			this.type = "parking";
			break;

		default:
			this.type = "info";
			break;
		}
		super.okPressed();
	}
//	@Override
//	protected Point getInitialSize() {
//		return new Point(800,800);
//	}

	private void addLabel(Composite comp, String textLabel) {
		Label label = new Label(comp, SWT.NONE);
		label.setText(textLabel);
		label.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
	}
}
