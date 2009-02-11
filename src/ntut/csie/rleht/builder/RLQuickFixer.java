package ntut.csie.rleht.builder;

import ntut.csie.csdet.quickfix.CSQuickFix;
import ntut.csie.csdet.quickfix.DHQuickFix;
import ntut.csie.csdet.quickfix.UMQuickFix;
import ntut.csie.csdet.refactor.RethrowUncheckExAction;
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
					markerFixer[i - RLData.LEVEL_MIN] = new RLQuickFix("變更成level=" + i + " (" + exception + ")", i);
					logger.debug("變更成level=" + i + " (" + exception + ")");
				}
				return markerFixer;
			} else if (problem.equals(RLMarkerAttribute.ERR_NO_RL)) {
				if (!RLData.validLevel(RLUtils.str2int(level, -1))) {
					level = String.valueOf(RL.LEVEL_1_ERR_REPORTING);
				}
				return new IMarkerResolution[] { new RLQuickFix("新增@RL (level=" + level + ",exception=" + exception + ")") };
			} else if (problem.equals(RLMarkerAttribute.ERR_RL_DUPLICATE)) {
				return new IMarkerResolution[] { new RLQuickFix("移除首次出現之@RL (" + exception + ")") };
			} else if (problem.equals(RLMarkerAttribute.ERR_RL_INSTANCE)) {
				return new IMarkerResolution[] { new RLQuickFix("@RL順序對調(" + marker.getAttribute(IMarker.MESSAGE) + ")") };
				// 碰到Ignore Exception的Quick fix and refactoring方法
			} else if(problem.equals(RLMarkerAttribute.CS_INGNORE_EXCEPTION)){
				return new IMarkerResolution[] { new CSQuickFix("Quick Fix==>Rethrow Unhandled Excetpion"),
						new RethrowUncheckExAction("Refactor==>Rethrow Unhandled Excetpion")};
				// 碰到Dummy Handler的Quick fix
			} else if(problem.equals(RLMarkerAttribute.CS_DUMMY_HANDLER)){
				return new IMarkerResolution[] { new DHQuickFix("Quick Fix==>Rethrow Unhandled Excetpion"),
						new RethrowUncheckExAction("Refactor==>Rethrow Unhandled Excetpion")};
				// 碰到Nested Try block的Quick fix
			} else if(problem.equals(RLMarkerAttribute.CS_NESTED_TRY_BLOCK)){
				return new IMarkerResolution[] { new DHQuickFix("Quick Fix Code Smell==>????????") };
				// 碰到Unprotected Main program的Quick fix
			} else if(problem.equals(RLMarkerAttribute.CS_UNPROTECTED_MAIN)){
				return new IMarkerResolution[] { new UMQuickFix("Quick Fix==>Big outer try block") };
			} else if(problem.equals(RLMarkerAttribute.CS_SPARE_HANDLER)){
				return new IMarkerResolution[] { new DHQuickFix("Quick Fix Code Smell==>????????") };
			}

			return null;
		} catch (CoreException ex) {
			logger.error("[getResolutions] EXCEPTION ",ex);
			return new IMarkerResolution[0];
		}

	}

}
