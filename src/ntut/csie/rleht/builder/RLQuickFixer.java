package ntut.csie.rleht.builder;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

import ntut.csie.csdet.quickfix.NTQuickFix;
import ntut.csie.csdet.refactor.CarelessCleanUpAction;
import ntut.csie.csdet.refactor.OverLoggingAction;
import ntut.csie.csdet.refactor.RethrowUncheckExAction;
import ntut.csie.rleht.common.RLUtils;
import ntut.csie.rleht.rlAdvice.AchieveRL1QuickFix;
import ntut.csie.rleht.views.RLData;
import ntut.csie.robusta.agile.exception.RTag;
import ntut.csie.robusta.codegen.markerresolution.ExtractMethodMarkerResolution;
import ntut.csie.robusta.codegen.markerresolution.MoveCloseResouceFromTryCatchToFinallyBlockQuickFix;
import ntut.csie.robusta.codegen.markerresolution.MoveCodeIntoBigOuterTryQuickFix;
import ntut.csie.robusta.codegen.markerresolution.RefineRuntimeExceptionQuickFix;
import ntut.csie.robusta.codegen.markerresolution.RemoveOverLoggingStatementQuickFix;
import ntut.csie.robusta.codegen.markerresolution.ThrowCheckedExceptionQuickFix;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.IMarkerResolutionGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class RLQuickFixer implements IMarkerResolutionGenerator {
	private static Logger logger = LoggerFactory.getLogger(RLQuickFixer.class);
	private ResourceBundle resource = ResourceBundle.getBundle("robusta", new Locale("en", "US"));

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
					markerList.add(new RLQuickFix(resource.getString("err.rl.level") + i + " (" + exception + ")", i, errMsg));
					logger.debug("變更成level=" + i + " (" + exception + ")");
				}
			} else if (problem.equals(RLMarkerAttribute.ERR_NO_RL)) {
				if (!RLData.validLevel(RLUtils.str2int(level, -1))) {
					level = String.valueOf(RTag.LEVEL_1_ERR_REPORTING);
				}
				markerList.add(new RLQuickFix(resource.getString("err.no.rl") + level + resource.getString("tag.level2") + exception + ")",errMsg));
			} else if (problem.equals(RLMarkerAttribute.ERR_RL_DUPLICATE)) {
				markerList.add(new RLQuickFix(resource.getString("err.rl.duplicate") + exception + ")",errMsg));
			} else if (problem.equals(RLMarkerAttribute.ERR_RL_INSTANCE)) {
				markerList.add(new RLQuickFix(resource.getString("err.rl.instance") + marker.getAttribute(IMarker.MESSAGE) + ")",errMsg));
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
					markerList.add(new CSQuickFix(resource.getString("err.ss.no.smell") + type, type, inCatch));
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
						markerList.add(new CSQuickFix(resource.getString("err.ss.fault.name1") + " " + faultName + " " + resource.getString("err.ss.fault.name2") + " " + type, type, inCatch));
				}
				// 碰到Ignore Exception的Quick fix and refactor方法
			} else if(problem.equals(RLMarkerAttribute.CS_INGNORE_EXCEPTION)) {
				markerList.add(new RefineRuntimeExceptionQuickFix("Quick Fix==>Rethrow RuntimeException"));
				markerList.add(new RethrowUncheckExAction("Refactor==>Rethrow Unchecked Excetpion"));
				markerList.add(new ThrowCheckedExceptionQuickFix("Quick Fix==>Throw Checked Exception"));
				// 碰到Dummy Handler的Quick fix and refactor方法
			} else if(problem.equals(RLMarkerAttribute.CS_DUMMY_HANDLER)) {
				markerList.add(new RefineRuntimeExceptionQuickFix("Quick Fix==>Refine to RuntimeException"));
				markerList.add(new RethrowUncheckExAction("Refactor==>Rethrow Unchecked Excetpion"));
				markerList.add(new ThrowCheckedExceptionQuickFix("Quick Fix==>Throw Checked Exception"));
				// 碰到Nested Try block的refactor
			} else if(problem.equals(RLMarkerAttribute.CS_NESTED_TRY_BLOCK)) {
				markerList.add(new NTQuickFix("Please use Eclipse refactor==>Extract Method"));
				// 碰到Unprotected Main program的Quick fix
			} else if(problem.equals(RLMarkerAttribute.CS_UNPROTECTED_MAIN)) {
				markerList.add(new MoveCodeIntoBigOuterTryQuickFix("Quick Fix==>Add Big outer try block"));
				// 碰到Careless CleanUp的Quick fix and refactor方法
			} else if(problem.equals(RLMarkerAttribute.CS_CARELESS_CLEANUP)){
				//只有CCMessage才會有這個，所以只能在這邊get
				boolean withTryBlock = false;
				if(marker.getAttribute(RLMarkerAttribute.CCU_WITH_TRY) != null){
					withTryBlock = (Boolean) marker.getAttribute(RLMarkerAttribute.CCU_WITH_TRY);
				}
				
				
				//MethodInv. won't throw exceptions will only offer "quick fix"
				if ((withTryBlock) && (exceptionType == null)) {
					markerList.add(new MoveCloseResouceFromTryCatchToFinallyBlockQuickFix("Quick Fix==>Move code to finally block"));

				// FIXME - MethodInv. not in try block, should use "Three steps to refactor" 左邊說明為原本構想，目前對應方法為不提供任何功能
				} else if ((!withTryBlock) && (exceptionType == null)) {
					//  Surround MethodDeclaration Body with big outer Try and close in finally

				//MethodInv. will throw exceptions and in try block
				} else if ((withTryBlock) && (exceptionType != null)) {
					markerList.add(new CarelessCleanUpAction("Refactor==>Use Extract Method"));
				} else {
					// 需要提供Refactoring的功能
				}
					
				// 碰到OverLogging的Quick fix and refactor方法
			}else if(problem.equals(RLMarkerAttribute.CS_OVER_LOGGING)){
				markerList.add(new RemoveOverLoggingStatementQuickFix("Quick Fix==>Remove Logging"));
//				markerList.add(new OLRefactoring("Refactor==>Remove Reference Logging"));
				markerList.add(new OverLoggingAction("Refactor==>Remove Reference Logging"));
				//遇到可以建議的方法
			}else if(problem.equals(RLMarkerAttribute.CS_EXCEPTION_RLADVICE)){
				String advice = (String) marker.getAttribute(IMarker.MESSAGE);
				//有RL annotation，才是有拋出這個例外(我有偷偷幫throw e的都硬上RL)
				if(advice.contains(RTag.class.getSimpleName())){
					markerList.add(new AchieveRL1QuickFix("RL1 quick gene ==> Rethrow Unckecked Exception"));
				}
			} else if(problem.equals(RLMarkerAttribute.CS_OVERWRITTEN_LEAD_EXCEPTION)) {
				markerList.add(new ExtractMethodMarkerResolution("Refactor==>Use Extract Method"));
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
