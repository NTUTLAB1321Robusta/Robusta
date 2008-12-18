package ntut.csie.rleht.builder;

import ntut.csie.csdet.quickfix.CSQuickFix;
import ntut.csie.csdet.quickfix.DHQuickFix;
import ntut.csie.rleht.common.RLUtils;
import ntut.csie.rleht.views.RLData;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.IMarkerResolutionGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import agile.exception.RL;

public class RLQuickFixer implements IMarkerResolutionGenerator {
	private static Logger logger = LoggerFactory.getLogger(RLQuickFixer.class);

	public IMarkerResolution[] getResolutions(IMarker marker) {
		try {
			
			String problem = (String) marker.getAttribute(RLMarkerAttribute.RL_MARKER_TYPE);
			if (problem == null) {
				return null;
			}

			String exception = (String) marker.getAttribute(RLMarkerAttribute.RL_INFO_EXCEPTION);
			String level = (String) marker.getAttribute(RLMarkerAttribute.RL_INFO_LEVEL);

			if (problem.equals(RLMarkerAttribute.ERR_RL_LEVEL)) {

				IMarkerResolution[] markerFixer = new IMarkerResolution[RLData.levelSize()];
				for (int i = RLData.LEVEL_MIN; i <= RLData.LEVEL_MAX; i++) {
					markerFixer[i - RLData.LEVEL_MIN] = new RLQuickFix("�ܧ�level=" + i + " (" + exception + ")", i);
					logger.debug("�ܧ�level=" + i + " (" + exception + ")");
				}
				return markerFixer;
			} else if (problem.equals(RLMarkerAttribute.ERR_NO_RL)) {
				if (!RLData.validLevel(RLUtils.str2int(level, -1))) {
					level = String.valueOf(RL.LEVEL_1_ERR_REPORTING);
				}
				return new IMarkerResolution[] { new RLQuickFix("�s�W@RL (level=" + level + ",exception=" + exception + ")") };
			} else if (problem.equals(RLMarkerAttribute.ERR_RL_DUPLICATE)) {
				return new IMarkerResolution[] { new RLQuickFix("���������X�{��@RL (" + exception + ")") };
			} else if (problem.equals(RLMarkerAttribute.ERR_RL_INSTANCE)) {
				return new IMarkerResolution[] { new RLQuickFix("@RL���ǹ��(" + marker.getAttribute(IMarker.MESSAGE) + ")") };
				// �I��Ignore Exception��Quick fix
			} else if(problem.equals(RLMarkerAttribute.CS_INGNORE_EXCEPTION)){
				return new IMarkerResolution[] { new CSQuickFix("�ץ�Code Smell==>Rethrow Unhandled Excetpion") };
				// �I��Dummy Handler��Quick fix
			} else if(problem.equals(RLMarkerAttribute.CS_DUMMY_HANDLER)){
				return new IMarkerResolution[] { new DHQuickFix("�ץ�Code Smell==>Rethrow Unhandled Excetpion") };
			} else if(problem.equals(RLMarkerAttribute.CS_Nested_Try_Block)){
				return new IMarkerResolution[] { new DHQuickFix("�ץ�Code Smell==>????????") };
			}

			return null;
		} catch (CoreException ex) {
			logger.error("[getResolutions] EXCEPTION ",ex);
			return new IMarkerResolution[0];
		}

	}

}
