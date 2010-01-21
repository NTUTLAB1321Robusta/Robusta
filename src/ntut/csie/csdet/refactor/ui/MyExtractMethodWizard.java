package ntut.csie.csdet.refactor.ui;

import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.ui.refactoring.RefactoringWizard;

/**
 * Careless CleanUp Refactoring的wizard,wizard之下會有page
 * @author Min
 */
public class MyExtractMethodWizard  extends RefactoringWizard{
	
	public MyExtractMethodWizard(Refactoring refactoring, int flags) {
		super(refactoring, DIALOG_BASED_USER_INTERFACE | PREVIEW_EXPAND_FIRST_NODE);
		setDefaultPageTitle("My Extract Method");
	}
	
	@Override
	protected void addUserInputPages() {
		//加入My Extract Method的Page
		addPage(new MyExtractMethodInputPage("My Extract Method"));
		
	}
}
