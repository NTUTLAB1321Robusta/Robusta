package ntut.csie.rleht.caller;

import ntut.csie.rleht.RLEHTPlugin;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorActionDelegate;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CallersEditorAction implements IEditorActionDelegate {
	private static Logger logger = LoggerFactory.getLogger(CallersEditorAction.class);

	private IEditorPart editor;

	public void run(IAction action) {
		try {
			System.out.println("=====Start To Call Hierarchy=====");
			IWorkbenchPage workbenchPage = editor.getSite().getPage();
			CallersView callersView = (CallersView) workbenchPage.showView(CallersView.ID);
			callersView.handleSelectionChanged4Editor();
		} catch (PartInitException e) {
			RLEHTPlugin.logError("無法切換至@Tag Call Hierarchy View!", e);
			logger.error("無法切換至@Tag Call Hierarchy View!", e);
		}
	}

	public void selectionChanged(IAction action, ISelection selection) {
	}

	public void setActiveEditor(IAction action, IEditorPart targetEditor) {
		this.editor = targetEditor;
	}

}
