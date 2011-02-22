package ntut.csie.rleht.builder;

import java.util.ArrayList;
import java.util.List;

import ntut.csie.csdet.quickfix.CCUQuickFix;
import ntut.csie.csdet.quickfix.DHQuickFix;
import ntut.csie.csdet.quickfix.NTQuickFix;
import ntut.csie.csdet.quickfix.OLQuickFix;
import ntut.csie.csdet.quickfix.TEQuickFix;
import ntut.csie.csdet.quickfix.UMQuickFix;
import ntut.csie.csdet.refactor.CarelessCleanUpAction;
import ntut.csie.csdet.refactor.OLRefactoring;
import ntut.csie.csdet.refactor.RethrowUncheckExAction;
import ntut.csie.rleht.common.RLUtils;
import ntut.csie.rleht.rlAdvice.AchieveRL1QuickFix;
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
			String errMsg = (String) marker.getAttribute(IMarker.MESSAGE);
			String exceptionType = (String) marker.getAttribute(RLMarkerAttribute.MI_WITH_Ex);

			List<IMarkerResolution> markerList = new ArrayList<IMarkerResolution>();

			if (problem.equals(RLMarkerAttribute.ERR_RL_LEVEL)) {
				for (int i = RLData.LEVEL_MIN; i <= RLData.LEVEL_MAX; i++) {
					markerList.add(new RLQuickFix("變更成level=" + i + " (" + exception + ")", i, errMsg));
					logger.debug("變更成level=" + i + " (" + exception + ")");
				}
			} else if (problem.equals(RLMarkerAttribute.ERR_NO_RL)) {
				if (!RLData.validLevel(RLUtils.str2int(level, -1))) {
					level = String.valueOf(RL.LEVEL_1_ERR_REPORTING);
				}
				markerList.add(new RLQuickFix("新增@RL (level=" + level + ",exception=" + exception + ")",errMsg));
			} else if (problem.equals(RLMarkerAttribute.ERR_RL_DUPLICATE)) {
				markerList.add(new RLQuickFix("移除首次出現之@RL (" + exception + ")",errMsg));
			} else if (problem.equals(RLMarkerAttribute.ERR_RL_INSTANCE)) {
				markerList.add(new RLQuickFix("@RL順序對調(" + marker.getAttribute(IMarker.MESSAGE) + ")",errMsg));
				// SuppressSmell內沒有名稱
			} else if (problem.equals(RLMarkerAttribute.ERR_SS_NO_SMELL)) {
				boolean inCatch = Boolean.valueOf((String)marker.getAttribute(RLMarkerAttribute.SS_IN_CATCH));
				String[] smellList;
				if (inCatch)	//若Marker位於Catch內
					smellList = RLMarkerAttribute.CS_CATCH_TYPE;
				else			//若Marker位於Method上
					smellList = RLMarkerAttribute.CS_TOTAL_TYPE;

				for (int i= 0; i < smellList.length; i++) {
					String type = smellList[i];
					markerList.add(new CSQuickFix("新增 Smell Type:" + type, type, inCatch));
				}
				// SuppressSmell內Smell名稱錯誤
			} else if (problem.equals(RLMarkerAttribute.ERR_SS_FAULT_NAME)) {
				boolean inCatch = Boolean.valueOf((String)marker.getAttribute(RLMarkerAttribute.SS_IN_CATCH));
				String faultName = (String) marker.getAttribute(RLMarkerAttribute.ERR_SS_FAULT_NAME);

				String[] smellList;
				if (inCatch)	//若Marker位於Catch內
					smellList = RLMarkerAttribute.CS_CATCH_TYPE;
				else			//若Marker位於Method上
					smellList = RLMarkerAttribute.CS_TOTAL_TYPE;

				for (String type : smellList) {
					boolean isDetect  = Boolean.valueOf((String) marker.getAttribute(type));
					if (!isDetect)
						markerList.add(new CSQuickFix("修改" + faultName + "為" + type, type, inCatch));
				}
				// 碰到Ignore Exception的Quick fix and refactor方法
			} else if(problem.equals(RLMarkerAttribute.CS_INGNORE_EXCEPTION)) {
				markerList.add(new DHQuickFix("Quick Fix==>Rethrow Unchecked Exception"));
				markerList.add(new RethrowUncheckExAction("Refactor==>Rethrow Unchecked Excetpion"));
				markerList.add(new TEQuickFix("Quick Fix==>Throw Checked Exception"));
				markerList.add(new CSQuickFix("新增 @SuppressSmell '" + problem + "' on Method", false));
				markerList.add(new CSQuickFix("新增 @SuppressSmell '" + problem + "' on Catch", true));
				// 碰到Dummy Handler的Quick fix and refactor方法
			} else if(problem.equals(RLMarkerAttribute.CS_DUMMY_HANDLER)) {
				markerList.add(new DHQuickFix("Quick Fix==>Rethrow Unchecked Exception"));
				markerList.add(new RethrowUncheckExAction("Refactor==>Rethrow Unchecked Excetpion"));
				markerList.add(new TEQuickFix("Quick Fix==>Throw Checked Exception"));
				markerList.add(new CSQuickFix("新增 @SuppressSmell '" + problem + "' on Method", false));
				markerList.add(new CSQuickFix("新增 @SuppressSmell '" + problem + "' on Catch", true));
				// 碰到Nested Try block的refactor
			} else if(problem.equals(RLMarkerAttribute.CS_NESTED_TRY_BLOCK)) {
				markerList.add(new NTQuickFix("Please use Eclipse refactor==>Extract Method"));
				markerList.add(new CSQuickFix("新增 @SuppressSmell '" + problem + "' on Method", false));
				// 碰到Unprotected Main program的Quick fix
			} else if(problem.equals(RLMarkerAttribute.CS_UNPROTECTED_MAIN)) {
				markerList.add(new UMQuickFix("Quick Fix==>Add Big outer try block"));
				markerList.add(new CSQuickFix("新增 @SuppressSmell '" + problem + "' on Method", false));
				// 碰到Careless CleanUp的Quick fix and refactor方法
			} else if(problem.equals(RLMarkerAttribute.CS_CARELESS_CLEANUP)){
				//只有CCMessage才會有這個，所以只能在這邊get
				int withTryBlock = 0;
				if(marker.getAttribute(RLMarkerAttribute.CCU_WITH_TRY) != null){
					withTryBlock = (Integer) marker.getAttribute(RLMarkerAttribute.CCU_WITH_TRY);
				}
				
				//MethodInv. won't throw exceptions will only offer "quick fix"
				if(exceptionType == null){
					markerList.add(new CCUQuickFix("Quick Fix==>Move code to finally block"));
				}
				//MethodInv. not in try block, should use "Three steps to refactor"
				if(withTryBlock == 0){
					markerList.add(new CarelessCleanUpAction("Refactor==>Use Three Steps"));
				
				//MethodInv. will throw exceptions and in try block
				}else if(exceptionType != null && withTryBlock != 0){
					markerList.add(new CarelessCleanUpAction("Refactor==>Use Extract Method"));
				}
				markerList.add(new CSQuickFix("新增 @SuppressSmell '" + problem + "' on Method", false));
				// 碰到OverLogging的Quick fix and refactor方法
			}else if(problem.equals(RLMarkerAttribute.CS_OVER_LOGGING)){
				markerList.add(new OLQuickFix("Quick Fix==>Remove Logging"));
				markerList.add(new OLRefactoring("Refactor==>Remove Reference Logging"));
				markerList.add(new CSQuickFix("新增 @SuppressSmell '" + problem + "' on Method", false));
				markerList.add(new CSQuickFix("新增 @SuppressSmell '" + problem + "' on Catch", true));
				//遇到可以建議的方法
			}else if(problem.equals(RLMarkerAttribute.CS_EXCEPTION_RLADVICE)){
				String advice = (String) marker.getAttribute(IMarker.MESSAGE);
				//有RL annotation，才是有拋出這個例外(我有偷偷幫throw e的都硬上RL)
				if(advice.contains("RL")){
					markerList.add(new AchieveRL1QuickFix("RL1 quick gene ==> Rethrow Unckecked Exception"));
				}
			}
			//List轉Array
			IMarkerResolution[] markerArray = markerList.toArray(new IMarkerResolution[markerList.size()]);
			return markerArray;
		} catch (CoreException ex) {
			logger.error("[getResolutions] EXCEPTION ",ex);
			return new IMarkerResolution[0];
		}

	}
}
