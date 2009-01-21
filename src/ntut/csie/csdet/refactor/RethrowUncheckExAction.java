package ntut.csie.csdet.refactor;

import ntut.csie.csdet.refactor.ui.RethrowExWizard;
import ntut.csie.rleht.builder.RLMarkerAttribute;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ltk.ui.refactoring.RefactoringWizardOpenOperation;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IMarkerResolution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * �bMarker�W����Quick Fix���[�JRefactoring(Rethrow Unhandled Exceptionb)���\��
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
		//�ϥΪ��I��ignore ex �Ϊ�dummy handler��marker��,�|�h��M������Refactor��k
		try {
			String problem = (String) marker.getAttribute(RLMarkerAttribute.RL_MARKER_TYPE);
			if ((problem != null && problem.equals(RLMarkerAttribute.CS_INGNORE_EXCEPTION)) ||
					(problem != null && problem.equals(RLMarkerAttribute.CS_DUMMY_HANDLER))){
				//�إ߾ާ@Refactor������,�ñNmarker�Ƕi�h�H�Q������ocode smell������T
				RethrowExRefactoring refactoring = new RethrowExRefactoring();				
				refactoring.setMarker(marker);
				//�Ұ�Refactor dialog
				RefactoringWizardOpenOperation operation = 
					new RefactoringWizardOpenOperation(new RethrowExWizard(refactoring,0));
				operation.run(new Shell(), "Rethrow Unhnadle Exception");
			}
			

		} catch (InterruptedException e) {
			logger.error("[Refactor][Rethrow Exception] EXCEPTION ",e);
		} catch (CoreException e) {
			logger.error("[Refactor][Rethrow Exception] EXCEPTION ",e);
		}
	}

	


}
