package ntut.csie.csdet.refactor;

import ntut.csie.csdet.refactor.ui.RethrowExWizard;
import ntut.csie.rleht.builder.RLMarkerAttribute;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ltk.ui.refactoring.RefactoringWizardOpenOperation;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IMarkerResolution;

/**
 * 在Marker上面的Quick Fix中加入Refactoring(Rethrow Unhandled Exceptionb)的功能
 * @author chewei
 */

public class RethrowUncheckExAction implements IMarkerResolution{

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
		try {
			String problem = (String) marker.getAttribute(RLMarkerAttribute.RL_MARKER_TYPE);
			if (problem != null && problem.equals(RLMarkerAttribute.CS_INGNORE_EXCEPTION)){

				RethrowExRefactoring refactoring = new RethrowExRefactoring();				
				refactoring.setMarker(marker);			
				RefactoringWizardOpenOperation operation = new RefactoringWizardOpenOperation(new RethrowExWizard(refactoring,0));
				operation.run(new Shell(), "Rethrow Unhnadle Exception");
			}
			

		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (CoreException e) {
			e.printStackTrace();
		}
	}

	


}
