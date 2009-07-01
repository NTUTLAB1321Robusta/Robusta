package ntut.csie.csdet.refactor;

import ntut.csie.csdet.refactor.ui.RetryWizard;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ���ѥt�@�ح��զP�@��Ʊ�����Retry template�b�k����
 * Retry with original values
 * @author chewei
 */

public class NoAlternativeRetryAction implements IEditorActionDelegate{
	private static Logger logger = LoggerFactory.getLogger(NoAlternativeRetryAction.class);
	
	private IEditorPart editor;
	
	private String retry_type = "Retry_with_original";
		
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
				//���o�ϥΪ̩ҿ������r
				ITextSelection textSelection = (ITextSelection) selection;
				IProject project = EditorUtils.getProject(editor);
				IJavaProject javaProject = JavaCore.create(project);
				IEditorInput input = editor.getEditorInput();
				IFile file = (IFile) input.getAdapter(IFile.class);
				IJavaElement javaElement = JavaCore.create(file);
				try {
					RetryRefactoring refactoring = new RetryRefactoring(javaProject,javaElement,textSelection,retry_type);
					//�Ұ�Refactor dialog
					RefactoringWizardOpenOperation operation = 

						new RefactoringWizardOpenOperation(new RetryWizard(refactoring,0));
					operation.run(new Shell(), "No Alternative Retry");
				} catch (InterruptedException e) {
					logger.error("[Retry with original values] EXCEPTION ",e);
				}
			}
		}
	}

	@Override
	public void selectionChanged(IAction action, ISelection selection) {		
	}

}
