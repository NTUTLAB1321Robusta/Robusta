package ntut.csie.robusta.codegen.markerresolution;

import ntut.csie.rleht.builder.RLMarkerAttribute;
import ntut.csie.robusta.codegen.refactoring.ExtractMethodRefactoring;
import ntut.csie.robusta.codegen.refactoringui.CodeSmellRefactoringWizard;
import ntut.csie.robusta.codegen.refactoringui.ExtractMethodInputPage;

import org.eclipse.core.resources.IMarker;
import org.eclipse.ltk.ui.refactoring.RefactoringWizardOpenOperation;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IMarkerResolution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExtractMethodMarkerResolution implements IMarkerResolution {
	private static Logger logger = LoggerFactory.getLogger(ExtractMethodMarkerResolution.class);
	private String label;

	public ExtractMethodMarkerResolution(String label) {
		this.label = label;
	}
	
	@Override
	public String getLabel() {
		return label;
	}

	@Override
	public void run(IMarker marker) {
		String problem;
		try {
			problem = (String) marker.getAttribute(RLMarkerAttribute.RL_MARKER_TYPE);
			
			if(problem == null)
				return;
			
			ExtractMethodRefactoring refactoring = new ExtractMethodRefactoring(marker);
			CodeSmellRefactoringWizard csRefactoringWizard = new CodeSmellRefactoringWizard(refactoring);
			csRefactoringWizard.setUserInputPage(new ExtractMethodInputPage("It's my way"));
			csRefactoringWizard.setDefaultPageTitle("Extract Method");
			RefactoringWizardOpenOperation operation = new RefactoringWizardOpenOperation(csRefactoringWizard);
			operation.run(new Shell(), "It's my way");
		} catch(Exception e) {
			logger.error(e.getMessage());
			throw new RuntimeException(e);
		}
	}
}
