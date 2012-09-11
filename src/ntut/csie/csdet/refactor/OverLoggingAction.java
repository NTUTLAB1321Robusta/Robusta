package ntut.csie.csdet.refactor;

import ntut.csie.rleht.builder.RLMarkerAttribute;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ui.IMarkerResolution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OverLoggingAction implements IMarkerResolution {
	private static Logger logger = LoggerFactory.getLogger(OverLoggingAction.class);
	// code smell的訊息
	private String label;
	
	public OverLoggingAction(String label) {
		this.label = label;
	}
	
	@Override
	public String getLabel() {
		return label;
	}

	@Override
	public void run(IMarker marker) {
		try {
			// 觸發Marker是否為OverLogging
			String problem = (String) marker.getAttribute(RLMarkerAttribute.RL_MARKER_TYPE);
			if(problem != null && (problem.equals(RLMarkerAttribute.CS_OVER_LOGGING))) {
				OverLoggingRefactor refactor = new OverLoggingRefactor();
				refactor.refator(marker);
			}
		} catch(CoreException e) {
			logger.error("[OLQuickFix] EXCEPTION ", e);
		}
	}
}
