package ntut.csie.csdet.refactor.ui;

import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.ui.refactoring.RefactoringWizard;

/**
 * there is a input page owned by Careless Cleanup Refactoring Wizard
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
		//add my extract method page
		addPage(new ExtractCleanupMethodInputPage("Extract Cleanup Method"));
		
	}
}
