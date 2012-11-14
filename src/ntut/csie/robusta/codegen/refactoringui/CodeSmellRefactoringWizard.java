package ntut.csie.robusta.codegen.refactoringui;

import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.ui.refactoring.RefactoringWizard;
import org.eclipse.ltk.ui.refactoring.UserInputWizardPage;

/**
 * 根據我讀程式碼的發現，其實我們Robusta只需要一個RefactoringWizard就夠了....XD
 * 我們並沒有需要多樣化的RefactoringWizard，所以我會把其他的砍掉。
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
	 * 設定UserInputWizardPage
	 * @param userInputPage
	 */
	public void setUserInputPage(UserInputWizardPage userInputPage) {
		this.userInputPage = userInputPage;
	}
	
	@Override
	protected void addUserInputPages() {
		// 如果沒有設定user input page，就會拋出例外警告
		if(userInputPage == null) {
			throw new RuntimeException("UserInputPageIsNotSet");
		}
		addPage(userInputPage);
	}

}
