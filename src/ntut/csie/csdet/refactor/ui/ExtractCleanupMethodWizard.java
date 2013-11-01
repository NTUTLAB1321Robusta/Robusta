package ntut.csie.csdet.refactor.ui;

import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.ui.refactoring.RefactoringWizard;

/**
 * Careless Cleanup Refactoring的Wizard,wizard之下會有page
 * @author Min
 */
public class ExtractCleanupMethodWizard  extends RefactoringWizard{
	
	public ExtractCleanupMethodWizard(Refactoring refactoring, int flags) {
		super(refactoring, DIALOG_BASED_USER_INTERFACE | PREVIEW_EXPAND_FIRST_NODE);
		setDefaultPageTitle("Extract Cleanup Method");
		//setDialogSettings(JavaPlugin.getDefault().getDialogSettings());
	}
	
	@Override
	protected void addUserInputPages() {
		//加入My Extract Method的Page
		addPage(new ExtractCleanupMethodInputPage("Extract Cleanup Method"));
		
	}
}
