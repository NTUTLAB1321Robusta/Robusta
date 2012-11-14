package ntut.csie.csdet.quickfix;

import ntut.csie.rleht.builder.RLMarkerAttribute;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.dialogs.MessageDialog;
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
		MessageDialog.openInformation(null, "Nested TryStatement QuickFix Notification", "Please use the refactoring function provided by eclipse to eliminate this code smell.");
		try {
			String problem = (String) marker.getAttribute(RLMarkerAttribute.RL_MARKER_TYPE);
			if(problem != null && problem.equals(RLMarkerAttribute.CS_NESTED_TRY_BLOCK)){
				// 因為無法直接使用Eclipse refactor - Extract Method,所以沒有任何解法
				//在這邊只會提示使用者使用Eclipse平台的重構功能
			}
		} catch (CoreException e) {		
			logger.error("[NTQuickFix] EXCEPTION ",e);
		}	
	}
	
	
}
