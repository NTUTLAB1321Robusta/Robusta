package ntut.csie.csdet.refactor.ui;

import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.ui.refactoring.RefactoringWizard;

public class NoAltRetryWizard  extends RefactoringWizard{

	public NoAltRetryWizard(Refactoring refactoring, int flags) {
		super(refactoring, DIALOG_BASED_USER_INTERFACE | PREVIEW_EXPAND_FIRST_NODE);
		setDefaultPageTitle("No Alternative Retry");
	}

	@Override
	protected void addUserInputPages() {
		addPage(new NoAltRetryInputPage("No Alternative Retry"));
	}

}
