package ntut.csie.robusta.codegen.refactoringui;

import org.eclipse.ltk.ui.refactoring.UserInputWizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

public class DisplayMessageUserInputWizardPage extends UserInputWizardPage {

	public DisplayMessageUserInputWizardPage(String name) {
		super(name);
	}

	@Override
	public void createControl(Composite parent) {
		Composite result= new Composite(parent, SWT.NONE);		
		setControl(result);

		Label label= new Label(result, SWT.NONE);
		label.setText("Press preview to see result.");
	}
}
