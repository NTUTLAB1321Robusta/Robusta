package ntut.csie.csdet.quickfix;

import ntut.csie.rleht.builder.RLMarkerAttribute;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ui.IMarkerResolution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NTQuickFix implements IMarkerResolution{
	private static Logger logger = LoggerFactory.getLogger(NTQuickFix.class);
	
	private String label;
	
	public NTQuickFix(String label){
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
			if(problem != null && problem.equals(RLMarkerAttribute.CS_NESTED_TRY_BLOCK)){
				// �]���L�k�����ϥ�Eclipse refactor - Extract Method,�ҥH�S������Ѫk
				//�b�o��u�|���ܨϥΪ̨ϥ�Eclipse���x�����c�\��
			}			
		} catch (CoreException e) {		
			logger.error("[NTQuickFix] EXCEPTION ",e);
		}	
	}
	
	
}
