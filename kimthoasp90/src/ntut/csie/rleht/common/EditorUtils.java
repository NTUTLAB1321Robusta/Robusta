package ntut.csie.rleht.common;

import ntut.csie.rleht.RLEHTPlugin;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IOpenable;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.util.Assert;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.ITextEditor;

public class EditorUtils {
	private EditorUtils() {
		super();
	}

	public static IEditorPart getActiveEditor() {
		IWorkbenchWindow window = RLEHTPlugin.getDefault().getWorkbench().getActiveWorkbenchWindow();
		if (window != null) {
			IWorkbenchPage page = window.getActivePage();
			if (page != null) {
				return page.getActiveEditor();
			}
		}
		return null;
	}

	public static IProject getProject(IEditorPart part) {
		IEditorInput editorInput = part.getEditorInput();
		if (editorInput instanceof IFileEditorInput) {
			IFile file = ((IFileEditorInput) editorInput).getFile();
			return file.getProject();
		} else {
			return null;
		}
	}

	public static IOpenable getJavaInput(IEditorPart part) {
		IEditorInput editorInput = part.getEditorInput();
		if (editorInput != null) {
			IJavaElement input = javaUIgetEditorInputJavaElement(editorInput);
			if (input instanceof IOpenable) {
				return (IOpenable) input;
			}
		}
		return null;
	}

	private static IJavaElement javaUIgetEditorInputJavaElement(IEditorInput editorInput) {
		Assert.isNotNull(editorInput);
		IJavaElement je = JavaUI.getWorkingCopyManager().getWorkingCopy(editorInput);
		if (je != null)
			return je;

		return (IJavaElement) editorInput.getAdapter(IJavaElement.class);
	}

	public static void selectInEditor(ITextEditor editor, int offset, int length) {
		IEditorPart active = getActiveEditor();
		if (active != editor) {
			editor.getSite().getPage().activate(editor);
		}
		editor.selectAndReveal(offset, length);
	}
	
	public static void showMessage(String message){
		MessageDialog.openInformation(
				PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(),
				"Warnning", 
				message);
	}
}
