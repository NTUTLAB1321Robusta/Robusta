package ntut.csie.csdet.refactor;

import ntut.csie.csdet.refactor.ui.RethrowExWizard;
import ntut.csie.rleht.builder.RLMarkerAttribute;

import org.eclipse.core.resources.IMarker;
import org.eclipse.ltk.ui.refactoring.RefactoringWizardOpenOperation;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IMarkerResolution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * add "Rethrow Unchecked Exception" feature to quick fix of the marker
 * @author chewei
 */

public class RethrowUncheckExAction implements IMarkerResolution{
	private static Logger logger = LoggerFactory.getLogger(RethrowUncheckExAction.class);
	private String label;
	
	public RethrowUncheckExAction(String label){
		this.label = label;
	}	
	
	@Override
	public String getLabel() {	
		return label;
	}

	@Override
	public void run(IMarker marker) {
		//it will invoke specified refactor feature if user select "empty catch block" or "dummy handler" marker. 
		try {
			String problem = (String) marker.getAttribute(RLMarkerAttribute.RL_MARKER_TYPE);
			if ((problem != null && problem.equals(RLMarkerAttribute.CS_EMPTY_CATCH_BLOCK)) ||
					(problem != null && problem.equals(RLMarkerAttribute.CS_DUMMY_HANDLER))){
				RethrowExRefactoring refactoring = new RethrowExRefactoring();				
				refactoring.setMarker(marker);
				RefactoringWizardOpenOperation operation = 
					new RefactoringWizardOpenOperation(new RethrowExWizard(refactoring, 0));
				operation.run(new Shell(), "Rethrow Unchecked Exception");
				
				//if annotation's order is not correct then swap the order 
				refactoring.swapTheIndexOfAnnotation();
			}
		} catch (Exception e) {
			//the exception type will be thrown is InterruptedException、CoreException
			logger.error("[Refactor][My Extract Method] EXCEPTION ", e);
		}
	}
}
