package ntut.csie.robusta.codegen.markerresolution;

import ntut.csie.rleht.builder.RLMarkerAttribute;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ui.IMarkerResolution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RemoveCallChainOverLoggingStatementsMarkerResolution implements IMarkerResolution {
	private static Logger logger = LoggerFactory.getLogger(RemoveCallChainOverLoggingStatementsMarkerResolution.class);
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
