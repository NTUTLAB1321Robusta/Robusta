package ntut.csie.csdet.refactor;

import ntut.csie.csdet.refactor.ui.NoAltRetryWizard;
import ntut.csie.rleht.common.EditorUtils;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ltk.ui.refactoring.RefactoringWizardOpenOperation;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorActionDelegate;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;

/**
 * 提供另一種沒有Alternative的Retry template在右鍵選單
 * No Alternative retry refactoring
 * @author chewei
 */

public class NoAlternativeRetryAction implements IEditorActionDelegate{
	private IEditorPart editor;
	
	private String retry_type = "No_Alt_Retry";
		
	@Override
	public void setActiveEditor(IAction action, IEditorPart targetEditor) {	
	}

	@Override
	public void run(IAction action) {
		editor = EditorUtils.getActiveEditor();
		if(editor == null){
			System.out.println("This editor is null!!!");
		}else{
			ISelection selection = editor.getEditorSite().getSelectionProvider().getSelection();
			if ((selection != null) && selection instanceof ITextSelection) {
				//取得使用者所選取的文字
				ITextSelection textSelection = (ITextSelection) selection;
				IProject project = EditorUtils.getProject(editor);
				IJavaProject javaProject = JavaCore.create(project);
				IEditorInput input = editor.getEditorInput();
				IFile file = (IFile) input.getAdapter(IFile.class);
				IJavaElement javaElement = JavaCore.create(file);
				try {
					NoAltRetryRefactoring refactoring = new NoAltRetryRefactoring(javaProject,javaElement,textSelection,retry_type);
//					RetryRefactoring refactoring = new RetryRefactoring(javaProject,javaElement,textSelection,retry_type);
					//啟動Refactor dialog
					RefactoringWizardOpenOperation operation = 
						new RefactoringWizardOpenOperation(new NoAltRetryWizard(refactoring,0));
					operation.run(new Shell(), "No Alternative Retry");
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}

	@Override
	public void selectionChanged(IAction action, ISelection selection) {		
	}

}
