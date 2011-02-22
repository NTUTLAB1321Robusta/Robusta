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
					markerList.add(new RLQuickFix("�ܧ�level=" + i + " (" + exception + ")", i, errMsg));
					logger.debug("�ܧ�level=" + i + " (" + exception + ")");
				}
			} else if (problem.equals(RLMarkerAttribute.ERR_NO_RL)) {
				if (!RLData.validLevel(RLUtils.str2int(level, -1))) {
					level = String.valueOf(RL.LEVEL_1_ERR_REPORTING);
				}
				markerList.add(new RLQuickFix("�s�W@RL (level=" + level + ",exception=" + exception + ")",errMsg));
			} else if (problem.equals(RLMarkerAttribute.ERR_RL_DUPLICATE)) {
				markerList.add(new RLQuickFix("���������X�{��@RL (" + exception + ")",errMsg));
			} else if (problem.equals(RLMarkerAttribute.ERR_RL_INSTANCE)) {
				markerList.add(new RLQuickFix("@RL���ǹ��(" + marker.getAttribute(IMarker.MESSAGE) + ")",errMsg));
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
					markerList.add(new CSQuickFix("�s�W Smell Type:" + type, type, inCatch));
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
						markerList.add(new CSQuickFix("�ק�" + faultName + "��" + type, type, inCatch));
				}
				// �I��Ignore Exception��Quick fix and refactor��k
			} else if(problem.equals(RLMarkerAttribute.CS_INGNORE_EXCEPTION)) {
				markerList.add(new DHQuickFix("Quick Fix==>Rethrow Unchecked Exception"));
				markerList.add(new RethrowUncheckExAction("Refactor==>Rethrow Unchecked Excetpion"));
				markerList.add(new TEQuickFix("Quick Fix==>Throw Checked Exception"));
				markerList.add(new CSQuickFix("�s�W @SuppressSmell '" + problem + "' on Method", false));
				markerList.add(new CSQuickFix("�s�W @SuppressSmell '" + problem + "' on Catch", true));
				// �I��Dummy Handler��Quick fix and refactor��k
			} else if(problem.equals(RLMarkerAttribute.CS_DUMMY_HANDLER)) {
				markerList.add(new DHQuickFix("Quick Fix==>Rethrow Unchecked Exception"));
				markerList.add(new RethrowUncheckExAction("Refactor==>Rethrow Unchecked Excetpion"));
				markerList.add(new TEQuickFix("Quick Fix==>Throw Checked Exception"));
				markerList.add(new CSQuickFix("�s�W @SuppressSmell '" + problem + "' on Method", false));
				markerList.add(new CSQuickFix("�s�W @SuppressSmell '" + problem + "' on Catch", true));
				// �I��Nested Try block��refactor
			} else if(problem.equals(RLMarkerAttribute.CS_NESTED_TRY_BLOCK)) {
				markerList.add(new NTQuickFix("Please use Eclipse refactor==>Extract Method"));
				markerList.add(new CSQuickFix("�s�W @SuppressSmell '" + problem + "' on Method", false));
				// �I��Unprotected Main program��Quick fix
			} else if(problem.equals(RLMarkerAttribute.CS_UNPROTECTED_MAIN)) {
				markerList.add(new UMQuickFix("Quick Fix==>Add Big outer try block"));
				markerList.add(new CSQuickFix("�s�W @SuppressSmell '" + problem + "' on Method", false));
				// �I��Careless CleanUp��Quick fix and refactor��k
			} else if(problem.equals(RLMarkerAttribute.CS_CARELESS_CLEANUP)){
				//�u��CCMessage�~�|���o�ӡA�ҥH�u��b�o��get
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
				markerList.add(new CSQuickFix("�s�W @SuppressSmell '" + problem + "' on Method", false));
				// �I��OverLogging��Quick fix and refactor��k
			}else if(problem.equals(RLMarkerAttribute.CS_OVER_LOGGING)){
				markerList.add(new OLQuickFix("Quick Fix==>Remove Logging"));
				markerList.add(new OLRefactoring("Refactor==>Remove Reference Logging"));
				markerList.add(new CSQuickFix("�s�W @SuppressSmell '" + problem + "' on Method", false));
				markerList.add(new CSQuickFix("�s�W @SuppressSmell '" + problem + "' on Catch", true));
				//�J��i�H��ĳ����k
			}else if(problem.equals(RLMarkerAttribute.CS_EXCEPTION_RLADVICE)){
				String advice = (String) marker.getAttribute(IMarker.MESSAGE);
				//��RL annotation�A�~�O���ߥX�o�Өҥ~(�ڦ�������throw e�����w�WRL)
				if(advice.contains("RL")){
					markerList.add(new AchieveRL1QuickFix("RL1 quick gene ==> Rethrow Unckecked Exception"));
				}
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
