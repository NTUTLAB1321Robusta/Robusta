package ntut.csie.robusta.codegen.refactoringui;

import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.ui.refactoring.RefactoringWizard;
import org.eclipse.ltk.ui.refactoring.UserInputWizardPage;

/**
 * �ھڧ�Ū�{���X���o�{�A���ڭ�Robusta�u�ݭn�@��RefactoringWizard�N���F....XD
 * �ڭ̨èS���ݭn�h�ˤƪ�RefactoringWizard�A�ҥH�ڷ|���L���屼�C
 * @author charles
 *
 */
public class CodeSmellRefactoringWizard extends RefactoringWizard {
	private UserInputWizardPage userInputPage;
	
	public CodeSmellRefactoringWizard(Refactoring refactoring) {
		super(refactoring, DIALOG_BASED_USER_INTERFACE | PREVIEW_EXPAND_FIRST_NODE);
		userInputPage = null;
	}

	/**
	 * �]�wUserInputWizardPage
	 * @param userInputPage
	 */
	public void setUserInputPage(UserInputWizardPage userInputPage) {
		this.userInputPage = userInputPage;
	}
	
	@Override
	protected void addUserInputPages() {
		// �p�G�S���]�wuser input page�A�N�|�ߥX�ҥ~ĵ�i
		if(userInputPage == null) {
			throw new RuntimeException("UserInputPageIsNotSet");
		}
		addPage(userInputPage);
	}

}
