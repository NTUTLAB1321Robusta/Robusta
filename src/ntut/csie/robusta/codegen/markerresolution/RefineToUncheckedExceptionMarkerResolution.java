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
 * add refactoring(Rethrow Unchecked Exception) feature to the quick fix of marker
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
		//user click the marker of empty catch block or dummy handler will invoke specified refactor feature
		String problem;
		try {
			problem = (String) marker.getAttribute(RLMarkerAttribute.RL_MARKER_TYPE);
			/*
			 * actually, each warning marker will be attached specified refactoring or quick fix resolution.
			 * so, we don't need to check the bad smell type again. 
			 */
			if (((problem == null) || (!problem.equals(RLMarkerAttribute.CS_EMPTY_CATCH_BLOCK))) &&
				((problem == null) || (!problem.equals(RLMarkerAttribute.CS_DUMMY_HANDLER)))){
				return;
			}

			RefineExceptionRefactoring refactoring = new RefineExceptionRefactoring(marker);

			/*
			 * 1. pop up refactor dialog (there is a bug because user can select to throw checked exception)
			 * 2. if user decide to throw some kind of unchecked exception will cause error. 
			 *    we guess the reason may be the constructor of some kind of exception doesn't implement throwable. 
			 */
			CodeSmellRefactoringWizard csRefactoringWizard = new CodeSmellRefactoringWizard(refactoring);
			csRefactoringWizard.setUserInputPage(new RethrowExInputPage("Rethrow Unchecked Exception"));
			csRefactoringWizard.setDefaultPageTitle("Refine to Unchecked Exception");
			RefactoringWizardOpenOperation operation = 
				new RefactoringWizardOpenOperation(csRefactoringWizard);
			operation.run(new Shell(), "Rethrow Unchecked Exception");
		} catch (CoreException e) {
			logger.error("");
			throw new RuntimeException(e);
		} catch (InterruptedException e) {
			logger.error("");
			throw new RuntimeException(e);
		}
	}
}
