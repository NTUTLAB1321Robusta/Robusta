package ntut.csie.csdet.refactor.ui;

import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.ui.refactoring.RefactoringWizard;

/**
 * Careless CleanUp Refactoring��Wizard,wizard���U�|��page
 * @author Min
 */
public class ExtractCleanUpMethodWizard  extends RefactoringWizard{
	
	public ExtractCleanUpMethodWizard(Refactoring refactoring, int flags) {
		super(refactoring, DIALOG_BASED_USER_INTERFACE | PREVIEW_EXPAND_FIRST_NODE);
		setDefaultPageTitle("Extract CleanUp Method");
	}
	
	@Override
	protected void addUserInputPages() {
		//�[�JMy Extract Method��Page
		addPage(new ExtractCleanUpMethodInputPage("Extract CleanUp Method"));
		
	}
}
