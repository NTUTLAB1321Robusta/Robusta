package ntut.csie.csdet.refactor.ui;

import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.ui.refactoring.RefactoringWizard;

public class RetryWizard extends RefactoringWizard{

	public RetryWizard(Refactoring refactoring, int flags) {
		super(refactoring, DIALOG_BASED_USER_INTERFACE | PREVIEW_EXPAND_FIRST_NODE);
		setDefaultPageTitle("Introduce resourceful try clause");

	}

	@Override
	protected void addUserInputPages() {
		addPage(new RetryInputPage("Introduce resourceful try clause"));
	}

}
