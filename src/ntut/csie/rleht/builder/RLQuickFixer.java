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
					logger.debug("�ܧ�level=" + i + " (" + exception + ")");
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
				// SuppressSmell���S���W��
			} else if (problem.equals(RLMarkerAttribute.ERR_SS_NO_SMELL)) {
				boolean inCatch = Boolean.valueOf((String)marker.getAttribute(RLMarkerAttribute.SS_IN_CATCH));
				String[] smellList;
				if (inCatch)	//�YMarker���Catch��
					smellList = RLMarkerAttribute.CS_CATCH_TYPE;
				else			//�YMarker���Method�W
					smellList = RLMarkerAttribute.CS_TOTAL_TYPE;

				for (int i= 0; i < smellList.length; i++) {
					String type = smellList[i];
					markerList.add(new CSQuickFix(resource.getString("err.ss.no.smell") + type, type, inCatch));
				}
				// SuppressSmell��Smell�W�ٿ��~
			} else if (problem.equals(RLMarkerAttribute.ERR_SS_FAULT_NAME)) {
				boolean inCatch = Boolean.valueOf((String)marker.getAttribute(RLMarkerAttribute.SS_IN_CATCH));
				String faultName = (String) marker.getAttribute(RLMarkerAttribute.ERR_SS_FAULT_NAME);

				String[] smellList;
				if (inCatch)	//�YMarker���Catch��
					smellList = RLMarkerAttribute.CS_CATCH_TYPE;
				else			//�YMarker���Method�W
					smellList = RLMarkerAttribute.CS_TOTAL_TYPE;

				for (String type : smellList) {
					boolean isDetect  = Boolean.valueOf((String) marker.getAttribute(type));
					if (!isDetect)
						markerList.add(new CSQuickFix(resource.getString("err.ss.fault.name1") + " " + faultName + " " + resource.getString("err.ss.fault.name2") + " " + type, type, inCatch));
				}
				// �I��Ignore Exception��Quick fix and refactor��k
			} else if(problem.equals(RLMarkerAttribute.CS_EMPTY_CATCH_BLOCK)) {
				markerList.add(new RefineRuntimeExceptionQuickFix("Quick Fix==>Rethrow RuntimeException"));
				markerList.add(new RethrowUncheckExAction("Refactor==>Rethrow Unchecked Excetpion"));
				markerList.add(new ThrowCheckedExceptionQuickFix("Quick Fix==>Throw Checked Exception"));
				// �I��Dummy Handler��Quick fix and refactor��k
			} else if(problem.equals(RLMarkerAttribute.CS_DUMMY_HANDLER)) {
				markerList.add(new RefineRuntimeExceptionQuickFix("Quick Fix==>Refine to RuntimeException"));
				markerList.add(new RethrowUncheckExAction("Refactor==>Rethrow Unchecked Excetpion"));
				markerList.add(new ThrowCheckedExceptionQuickFix("Quick Fix==>Throw Checked Exception"));
				// �I��Nested Try block��refactor
			} else if(problem.equals(RLMarkerAttribute.CS_NESTED_TRY_BLOCK)) {
				markerList.add(new NTQuickFix("Please use Eclipse refactor==>Extract Method"));
				// �I��Unprotected Main program��Quick fix
			} else if(problem.equals(RLMarkerAttribute.CS_UNPROTECTED_MAIN)) {
				markerList.add(new MoveCodeIntoBigOuterTryQuickFix("Quick Fix==>Add Big outer try block"));
				// �I��Careless CleanUp��Quick fix and refactor��k
			} else if(problem.equals(RLMarkerAttribute.CS_CARELESS_CLEANUP)){
				//�u��CCMessage�~�|���o�ӡA�ҥH�u��b�o��get
				boolean withTryBlock = false;
				if(marker.getAttribute(RLMarkerAttribute.CCU_WITH_TRY) != null){
					withTryBlock = (Boolean) marker.getAttribute(RLMarkerAttribute.CCU_WITH_TRY);
				}
				
				
				//MethodInv. won't throw exceptions will only offer "quick fix"
				if ((withTryBlock) && (exceptionType == null)) {
					markerList.add(new MoveCloseResouceFromTryCatchToFinallyBlockQuickFix("Quick Fix==>Move code to finally block"));

				// FIXME - MethodInv. not in try block, should use "Three steps to refactor" ���仡�����쥻�c�Q�A�ثe������k�������ѥ���\��
				} else if ((!withTryBlock) && (exceptionType == null)) {
					//  Surround MethodDeclaration Body with big outer Try and close in finally

				//MethodInv. will throw exceptions and in try block
				} else if ((withTryBlock) && (exceptionType != null)) {
					markerList.add(new CarelessCleanUpAction("Refactor==>Use Extract Method"));
				} else {
					// �ݭn����Refactoring���\��
				}
					
				// �I��OverLogging��Quick fix and refactor��k
			}else if(problem.equals(RLMarkerAttribute.CS_OVER_LOGGING)){
				markerList.add(new RemoveOverLoggingStatementQuickFix("Quick Fix==>Remove Logging"));
//				markerList.add(new OLRefactoring("Refactor==>Remove Reference Logging"));
				markerList.add(new OverLoggingAction("Refactor==>Remove Reference Logging"));
				//�J��i�H��ĳ����k
			}else if(problem.equals(RLMarkerAttribute.CS_EXCEPTION_RLADVICE)){
				String advice = (String) marker.getAttribute(IMarker.MESSAGE);
				//��RL annotation�A�~�O���ߥX�o�Өҥ~(�ڦ�������throw e�����w�WRL)
				if(advice.contains(RTag.class.getSimpleName())){
					markerList.add(new AchieveRL1QuickFix("RL1 quick gene ==> Rethrow Unckecked Exception"));
				}
			} else if(problem.equals(RLMarkerAttribute.CS_OVERWRITTEN_LEAD_EXCEPTION)) {
				markerList.add(new ExtractMethodMarkerResolution("Refactor==>Use Extract Method"));
			}
			//List��Array
			IMarkerResolution[] markerArray = markerList.toArray(new IMarkerResolution[markerList.size()]);
			return markerArray;
		} catch (CoreException ex) {
			logger.error("[getResolutions] EXCEPTION ",ex);
			return new IMarkerResolution[0];
		}

	}
}
