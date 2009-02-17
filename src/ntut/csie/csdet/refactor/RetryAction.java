package ntut.csie.csdet.refactor;

import ntut.csie.csdet.refactor.ui.RetryWizard;
import ntut.csie.rleht.builder.RLMarkerAttribute;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ltk.ui.refactoring.RefactoringWizardOpenOperation;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IMarkerResolution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RetryAction implements IMarkerResolution{
	private static Logger logger = LoggerFactory.getLogger(RetryAction.class);
	private String label;
	
	public RetryAction(String label){
		this.label = label;
	}
	
	@Override
	public String getLabel() {
		return label;
	}

	@Override
	public void run(IMarker marker) {
		//�ϥΪ��I��spare handler��marker��,�|�h��M������Refactor��k
//		try {
//			String problem = (String) marker.getAttribute(RLMarkerAttribute.RL_MARKER_TYPE);
//			if (problem != null && problem.equals(RLMarkerAttribute.CS_SPARE_HANDLER)){
//				System.out.println("Maker~~~");
//				//�إ߾ާ@Refactor������,�ñNmarker�Ƕi�h�H�Q������ocode smell������T
//				RetryRefactoring refactoring = new RetryRefactoring();				
//				refactoring.setMarker(marker);
//				//�Ұ�Refactor dialog
//				RefactoringWizardOpenOperation operation = 
//					new RefactoringWizardOpenOperation(new RetryWizard(refactoring,0));
//				operation.run(new Shell(), "Introduce resourceful try clause");
//			}		
//		} catch (InterruptedException e) {
//			e.printStackTrace();
//			logger.error("[Refactor][Rethrow Exception] EXCEPTION ",e);
//		} catch (CoreException e) {
//			e.printStackTrace();
//			logger.error("[Refactor][Rethrow Exception] EXCEPTION ",e);
//		}
		
	}

}
