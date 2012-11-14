package ntut.csie.robusta.codegen.markerresolution;

import ntut.csie.rleht.builder.RLMarkerAttribute;
import ntut.csie.robusta.codegen.refactoringui.CodeSmellRefactoringWizard;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ltk.ui.refactoring.RefactoringWizardOpenOperation;
import org.eclipse.ui.IMarkerResolution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RemoveCallChainOverLoggingStatementsMarkerResulotion implements IMarkerResolution {
	private static Logger logger = LoggerFactory.getLogger(RemoveCallChainOverLoggingStatementsMarkerResulotion.class);
	private String label;
	
	@Override
	public String getLabel() {
		return label;
	}

	@Override
	public void run(IMarker marker) {
		String problem;
		try {
			problem = (String) marker.getAttribute(RLMarkerAttribute.RL_MARKER_TYPE);
			if ((problem == null) || (!problem.equals(RLMarkerAttribute.CS_OVER_LOGGING))){
				return;
			}
			
//			RefactoringWizardOpenOperation operation = new RefactoringWizardOpenOperation(new )
//			CodeSmellRefactoringWizard csRefactoringWizard = new CodeSmellRefactoringWizard(refactoring);
		} catch (CoreException e) {
			logger.error("");
			throw new RuntimeException(e);
		}

	}
}
