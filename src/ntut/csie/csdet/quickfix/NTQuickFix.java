package ntut.csie.csdet.quickfix;

import ntut.csie.rleht.builder.RLMarkerAttribute;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ui.IMarkerResolution;

public class NTQuickFix implements IMarkerResolution{

	private String label;
	
	public NTQuickFix(String label){
		this.label = label;
	}
	
	@Override
	public String getLabel() {
		return label;
	}

	@Override
	public void run(IMarker marker) {
		try {
			String problem = (String) marker.getAttribute(RLMarkerAttribute.RL_MARKER_TYPE);
			if(problem != null && problem.equals(RLMarkerAttribute.CS_NESTED_TRY_BLOCK)){
				// 因為無法直接使用Eclipse refactor - Extract Method,所以沒有任何解法
//				ExtractMethodAction action = new ExtractMethodAction(null);
//				IEditorPart editor = EditorUtils.getActiveEditor();
				
				//String methodIdx = (String) marker.getAttribute(RLMarkerAttribute.RL_METHOD_INDEX);
//				IResource resource = marker.getResource();
				//findMethodNode(marker.getResource(),Integer.parseInt(methodIdx));
//				if (resource instanceof IFile && resource.getName().endsWith(".java")) {
//					String content =(String)marker.getAttribute(RLMarkerAttribute.RL_INFO_EXCEPTION);
//					int selectionStart =Integer.valueOf(marker.getAttribute(RLMarkerAttribute.RL_INFO_SRC_POS).toString());
//					IJavaElement javaElement = JavaCore.create(resource);
//					ICompilationUnit icu = (ICompilationUnit)javaElement;
//					ExtractMethodRefactoring refactoring= new ExtractMethodRefactoring(icu, selectionStart, content.length());
//					new RefactoringStarter().activate(refactoring, new ExtractMethodWizard(refactoring), new Shell(), RefactoringMessages.ExtractMethodAction_dialog_title, RefactoringSaveHelper.SAVE_NOTHING);
//				}
			}			
		} catch (CoreException e) {		
			e.printStackTrace();
		}	
	}
	
}
