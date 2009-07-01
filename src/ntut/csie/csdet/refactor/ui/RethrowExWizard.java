package ntut.csie.csdet.refactor.ui;


import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.ui.refactoring.RefactoringWizard;

/**
 * Refactoring的wizard,wizard之下會有page
 * @author chewei
 */

public class RethrowExWizard extends RefactoringWizard {

	public RethrowExWizard(Refactoring refactoring, int flags) {
		super(refactoring, DIALOG_BASED_USER_INTERFACE | PREVIEW_EXPAND_FIRST_NODE);
		setDefaultPageTitle("Rethrow Unchecked Exception");
	}

	@Override
	protected void addUserInputPages() {
		//加入Rethrow exception的Page
		addPage(new RethrowExInputPage("Rethrow Unchecked Exception"));
		
	}


}
