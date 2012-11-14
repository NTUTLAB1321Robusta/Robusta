package ntut.csie.robusta.codegen.markerresolution;

import ntut.csie.rleht.builder.RLMarkerAttribute;
import ntut.csie.robusta.codegen.refactoring.RefineExceptionRefactoring;
import ntut.csie.robusta.codegen.refactoringui.CodeSmellRefactoringWizard;
import ntut.csie.robusta.codegen.refactoringui.RethrowExInputPage;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ltk.ui.refactoring.RefactoringWizardOpenOperation;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IMarkerResolution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 在Marker上面的Quick Fix中加入Refactoring(Rethrow Unchecked Exception)的功能
 * @author chewei
 */

public class RefineToUncheckedExceptionMarkerResolution implements IMarkerResolution{
	private static Logger logger = LoggerFactory.getLogger(RefineToUncheckedExceptionMarkerResolution.class);
	private String label;
	
	public RefineToUncheckedExceptionMarkerResolution(String label){
		this.label = label;
	}	
	
	@Override
	public String getLabel() {	
		return label;
	}

	@Override
	public void run(IMarker marker) {
		//使用者點選ignore ex 或者dummy handler的marker時,會去找尋對應的Refactor方法
		String problem;
		try {
			problem = (String) marker.getAttribute(RLMarkerAttribute.RL_MARKER_TYPE);
			/*
			 * 其實在QuickFixer裡面，每個warning都已經決定會附上哪種resolution，
			 * 這裡其實不用特地再做一次壞味道類型的判斷才對，除非每個壞味道都會跑進來。
			 * 讓我們繼續試下去。
			 */
			if (((problem == null) || (!problem.equals(RLMarkerAttribute.CS_INGNORE_EXCEPTION))) &&
				((problem == null) || (!problem.equals(RLMarkerAttribute.CS_DUMMY_HANDLER)))){
				return;
			}

			// 建立操作Refactor的物件,並將marker傳進去以利之後取得code smell相關資訊
			RefineExceptionRefactoring refactoring = new RefineExceptionRefactoring(marker);

			/*
			 * 1. 啟動Refactor dialog (使用者還是可以要求拋出 Checked Exception，這邊算是Bug)
			 * 2. 使用者拋出某些Unchecked Exception會失敗，可能原因是那些例外類型的建構子沒有throwable
			 */
			CodeSmellRefactoringWizard csRefactoringWizard = new CodeSmellRefactoringWizard(refactoring);
			csRefactoringWizard.setUserInputPage(new RethrowExInputPage("Rethrow Unchecked Exception"));
			csRefactoringWizard.setDefaultPageTitle("Refine to Unchecked Exception");
			RefactoringWizardOpenOperation operation = 
				new RefactoringWizardOpenOperation(csRefactoringWizard);
			operation.run(new Shell(), "Rethrow Unchecked Exception");

//			//若Annotation順序不對，則交換順序。最後再定位
//			refactoring.changeAnnotation();
			
		} catch (CoreException e) {
			logger.error("");
			throw new RuntimeException(e);
		} catch (InterruptedException e) {
			logger.error("");
			throw new RuntimeException(e);
		}
	}
}
