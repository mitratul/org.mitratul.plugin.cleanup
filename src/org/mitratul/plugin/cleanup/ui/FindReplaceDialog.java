package org.mitratul.plugin.cleanup.ui;

import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.mitratul.plugin.cleanup.core.CleanupAction;

public class FindReplaceDialog extends TitleAreaDialog {

	private Text txtTarget;
	private Text txtReplacement;

	private String target;
	private String replacement;

	private boolean confirmationMode;

	public FindReplaceDialog(Shell parentShell) {
		super(parentShell);
	}

	@Override
	public void create() {
		super.create();
		setTitle("Provide the statement you want to clean-up / replace");

		// * initially show in input mode
		setInputMode();
		confirmationMode = false;
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite area = (Composite) super.createDialogArea(parent);
		Composite container = new Composite(area, SWT.NONE);
		container.setLayoutData(new GridData(GridData.FILL_BOTH));
		GridLayout layout = new GridLayout(2, false);
		container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		container.setLayout(layout);

		txtTarget = createTextInput(container, "Statement to find");
		txtReplacement = createTextInput(container, "Replace with");

		return area;
	}

	@Override
	protected void okPressed() {
		// * do nothing without a target string
		if (txtTarget.getText().trim().length() == 0) {
			return;
		}

		if (confirmationMode) {
			saveInput();
			super.okPressed();
		} else {
			confirmationMode = true;
			setConfirmationMode();
		}
	}

	@Override
	protected void cancelPressed() {
		if (confirmationMode) {
			confirmationMode = false;
			setInputMode();
		} else {
			super.cancelPressed();
		}
	}

	@Override
	protected boolean isResizable() {
		return true;
	}

	private void setInputMode() {
		txtTarget.setEditable(true);
		txtReplacement.setEditable(true);
		setMessage(
				"This will clean-up / replace all statements starting with your input",
				IMessageProvider.INFORMATION);
		this.getButton(OK).setText("OK");
		this.getButton(CANCEL).setText("Cancel");
	}

	private void setConfirmationMode() {
		txtTarget.setEditable(false);
		txtReplacement.setEditable(false);
		CleanupAction action = getActionFromInput(txtTarget.getText().trim(),
				txtReplacement.getText().trim());

		String target = txtTarget.getText().trim();
		String replacement = txtReplacement.getText().trim();
		StringBuilder sbMsg = new StringBuilder(
				"Are you sure you want to PARMANENTLY ");
		if (CleanupAction.COMMENT == action) {
			sbMsg.append("comment out "
					+ (target.length() > 48 ? target.substring(0, 48) + "..."
							: target));
		} else if (CleanupAction.REMOVE == action) {
			sbMsg.append("remove "
					+ (target.length() > 48 ? target.substring(0, 48) + "..."
							: target));
		} else if (CleanupAction.REPLACE == action) {
			sbMsg.append("replace "
					+ (target.length() > 24 ? target.substring(0, 24) + "..."
							: target)
					+ " with "
					+ (replacement.length() > 24 ? replacement.substring(0, 24)
							+ "..." : replacement));
		}
		sbMsg.append("?");

		setMessage(sbMsg.toString(), IMessageProvider.WARNING);
		this.getButton(OK).setText("Go ahead");
		this.getButton(CANCEL).setText("May be not");
	}

	private Text createTextInput(Composite container, String lblText) {
		Label label = new Label(container, SWT.NONE);
		label.setText(lblText);

		GridData gridData = new GridData();
		gridData.grabExcessHorizontalSpace = true;
		gridData.horizontalAlignment = GridData.FILL;

		Text text = new Text(container, SWT.BORDER);
		text.setLayoutData(gridData);
		return text;
	}

	/**
	 * Save content of the Text fields because they get disposed as soon as the
	 * Dialog closes
	 */
	private void saveInput() {
		target = txtTarget.getText();
		replacement = txtReplacement.getText();
	}

	public CleanupAction getAction() {
		return getActionFromInput(getTarget(), getReplacement());
	}

	private CleanupAction getActionFromInput(String target, String replacement) {
		CleanupAction action = CleanupAction.NONE;

		// * TODO: validate target string semantically
		// * IMPORTANT: use the getters to get trimmed string
		if (target.length() > 0) {
			if (replacement.length() == 0) {
				// * no replacement means removal
				action = CleanupAction.REMOVE;
			} else if (replacement.startsWith("/*")
					&& replacement.endsWith("*/")) {
				// * /* */ means commenting out
				action = CleanupAction.COMMENT;
			} else {
				action = CleanupAction.REPLACE;
			}
		} // * nothing can be done without target statement, defaults to NONE

		return action;
	}

	public String getTarget() {
		return target.trim();
	}

	public String getReplacement() {
		return replacement.trim();
	}
}