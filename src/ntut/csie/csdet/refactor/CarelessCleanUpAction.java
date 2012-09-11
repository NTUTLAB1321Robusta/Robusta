package ntut.csie.csdet.refactor;


import ntut.csie.csdet.refactor.ui.ExtractCleanUpMethodWizard;
import ntut.csie.rleht.builder.RLMarkerAttribute;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ltk.ui.refactoring.RefactoringWizardOpenOperation;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IMarkerResolution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * �bMarker�W�[�JRefactoring���\��
 * @author Min
 */
public class CarelessCleanUpAction implements IMarkerResolution{
	private static Logger logger = LoggerFactory.getLogger(CarelessCleanUpAction.class);
	private String label;

	public CarelessCleanUpAction(String label){
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
			if ((problem != null && problem.equals(RLMarkerAttribute.CS_CARELESS_CLEANUP))){
				// �إ߾ާ@Refactor������,�ñNmarker�Ƕi�h�H�Q������ocode smell������T
				CarelessCleanUpRefactor refactoring = new CarelessCleanUpRefactor();				
				refactoring.setMarker(marker);
				// �Ұ�Refactor dialog
				RefactoringWizardOpenOperation operation = 
					new RefactoringWizardOpenOperation(new ExtractCleanUpMethodWizard(refactoring, 0));
				operation.run(new Shell(), "My Extract Method");
			}
		} catch (Exception e) {
			// �|�ߥX���Q�~����InterruptedException�BCoreException
			logger.error("[Refactor][My Extract Method] EXCEPTION ", e);
		}
	}
}



