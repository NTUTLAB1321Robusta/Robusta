package ntut.csie.robusta.codegen.refactoringui;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.ui.refactoring.RefactoringWizard;
import org.eclipse.ltk.ui.refactoring.UserInputWizardPage;

/**
 * @author charles
 *
 */
public class CodeSmellRefactoringWizard extends RefactoringWizard {
	private UserInputWizardPage userInputPage;
	
	public CodeSmellRefactoringWizard(Refactoring refactoring) {
		super(refactoring, DIALOG_BASED_USER_INTERFACE | PREVIEW_EXPAND_FIRST_NODE);
		setDialogSettings(JavaPlugin.getDefault().getDialogSettings());
		userInputPage = null;
	}

	/**
	 * setup UserInputWizardPage
	 * @param userInputPage
	 */
	public void setUserInputPage(UserInputWizardPage userInputPage) {
		this.userInputPage = userInputPage;
	}
	
	@Override
	protected void addUserInputPages() {
		if(userInputPage == null) {
			throw new RuntimeException("UserInputPageIsNotSet");
		}
		addPage(userInputPage);
	}

}
