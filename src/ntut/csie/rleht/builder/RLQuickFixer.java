package ntut.csie.rleht.builder;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

import ntut.csie.aspect.AddAspectsMarkerResoluation;
import ntut.csie.csdet.refactor.NTMarkerResolution;
import ntut.csie.csdet.refactor.RethrowUncheckExAction;
import ntut.csie.rleht.common.RLUtils;
import ntut.csie.rleht.rlAdvice.AchieveRL1QuickFix;
import ntut.csie.rleht.views.RLData;
import ntut.csie.robusta.agile.exception.RTag;
import ntut.csie.robusta.codegen.markerresolution.TEFBExtractMethodMarkerResolution;
import ntut.csie.robusta.codegen.markerresolution.MoveCodeIntoBigOuterTryQuickFix;
import ntut.csie.robusta.codegen.markerresolution.RefineRuntimeExceptionQuickFix;
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
	
	private String ECBThrowRuntimeExceptionQuickFixDescription = "Quick Fix==>Throw RuntimeException";
	private String ECBThrowCheckedExceptionQuickFixDescription = "Quick Fix==>Throw Checked Exception";
	private String ECBThrowUncheckedExceptionRefactoringDescription = "Refactor==>Throw Unchecked Excetpion";
	
	private String DHThrowRuntimeExceptionQuickFixDescription = "Quick Fix==>Throw RuntimeException";
	private String DHBThrowCheckedExceptionQuickFixDescription = "Quick Fix==>Throw Checked Exception";
	private String DHBThrowUncheckedExceptionRefactoringDescription = "Refactor==>Throw Unchecked Excetpion";
	
	// should be quick fix
	private String NTExtractMethodRefactoringDescription = "Refactor==>Extract Method";

	// should be refactoring
	private String UMEncloseAllStatementInTryRefactoringDescription = "Quick Fix==>Enclose everything in a try block";
	
	// should be quick fix
	private String TEFFBExtractMethodRefactoringDescription = "Refactor==>Extract Method";
	
	public IMarkerResolution[] getResolutions(IMarker marker) {
		try {
			String problem = (String) marker.getAttribute(RLMarkerAttribute.RL_MARKER_TYPE);
			if (problem == null) {
				return null;
			}

			String exception = (String) marker.getAttribute(RLMarkerAttribute.RL_INFO_EXCEPTION);
			String level = (String) marker.getAttribute(RLMarkerAttribute.RL_INFO_LEVEL);
			String errMsg = (String) marker.getAttribute(IMarker.MESSAGE);
			List<IMarkerResolution> markerList = new ArrayList<IMarkerResolution>();

			/*
			 *  Add refactoring or quickfix for bad smells
			 */
			if(problem.equals(RLMarkerAttribute.CS_EMPTY_CATCH_BLOCK)) {
				markerList.add(new RefineRuntimeExceptionQuickFix(ECBThrowRuntimeExceptionQuickFixDescription));
				markerList.add(new RethrowUncheckExAction(ECBThrowCheckedExceptionQuickFixDescription));
				markerList.add(new ThrowCheckedExceptionQuickFix(ECBThrowUncheckedExceptionRefactoringDescription));
			} else if(problem.equals(RLMarkerAttribute.CS_DUMMY_HANDLER)) {
				String methodIdx = (String) marker.getAttribute(RLMarkerAttribute.RL_METHOD_INDEX);
				if(!methodIdx.equals("-1")) {
					markerList.add(new RefineRuntimeExceptionQuickFix(DHThrowRuntimeExceptionQuickFixDescription));
					markerList.add(new RethrowUncheckExAction(DHBThrowCheckedExceptionQuickFixDescription));
					markerList.add(new ThrowCheckedExceptionQuickFix(DHBThrowUncheckedExceptionRefactoringDescription));
					markerList.add(new AddAspectsMarkerResoluation("add Adspect"));
				}
			} else if(problem.equals(RLMarkerAttribute.CS_NESTED_TRY_STATEMENT)) {
				markerList.add(new NTMarkerResolution(NTExtractMethodRefactoringDescription));
			} else if(problem.equals(RLMarkerAttribute.CS_UNPROTECTED_MAIN)) {
				markerList.add(new MoveCodeIntoBigOuterTryQuickFix(UMEncloseAllStatementInTryRefactoringDescription));
			} else if(problem.equals(RLMarkerAttribute.CS_CARELESS_CLEANUP)){
				// not going to provide resolution for now.
			} else if(problem.equals(RLMarkerAttribute.CS_EXCEPTION_THROWN_FROM_FINALLY_BLOCK)) {
				boolean isSupportRefactoring = (Boolean)marker.getAttribute(RLMarkerAttribute.RL_INFO_SUPPORT_REFACTORING);
				if(isSupportRefactoring)
					markerList.add(new TEFBExtractMethodMarkerResolution(TEFFBExtractMethodRefactoringDescription));
			}
			
			/*
			 *  add refactoring or quickfix for RL annotation
			 */
			if (problem.equals(RLMarkerAttribute.ERR_RL_LEVEL)) {
				for (int i = RLData.LEVEL_MIN; i <= RLData.LEVEL_MAX; i++) {
					markerList.add(new RLQuickFix(resource.getString("err.rl.level") + i + " (" + exception + ")", i, errMsg));
					logger.debug("change robustness level to =" + i + " (" + exception + ")");
				}
			} else if (problem.equals(RLMarkerAttribute.ERR_NO_RL)) {
				if (!RLData.validLevel(RLUtils.str2int(level, -1)))
					level = String.valueOf(RTag.LEVEL_1_ERR_REPORTING);
				markerList.add(new RLQuickFix(resource.getString("err.no.rl") + level + resource.getString("tag.level2") + exception + ")",errMsg));
			} else if (problem.equals(RLMarkerAttribute.ERR_RL_DUPLICATE)) {
				markerList.add(new RLQuickFix(resource.getString("err.rl.duplicate") + exception + ")",errMsg));
			} else if (problem.equals(RLMarkerAttribute.ERR_RL_INSTANCE)) {
				markerList.add(new RLQuickFix(resource.getString("err.rl.instance") + marker.getAttribute(IMarker.MESSAGE) + ")",errMsg));
				// there is not any smell name in SuppressSmell
			} else if (problem.equals(RLMarkerAttribute.ERR_SS_NO_SMELL)) {
				boolean inCatch = Boolean.valueOf((String)marker.getAttribute(RLMarkerAttribute.SS_IN_CATCH));
				String[] smellList;
				if (inCatch)	
					smellList = RLMarkerAttribute.CS_CATCH_TYPE;
				else			
					smellList = RLMarkerAttribute.CS_TOTAL_TYPE;

				for (int i= 0; i < smellList.length; i++) {
					String type = smellList[i];
					markerList.add(new CSQuickFix(resource.getString("err.ss.no.smell") + type, type, inCatch));
				}
				// there is a wrong smell name in SuppressSmell
			} else if (problem.equals(RLMarkerAttribute.ERR_SS_FAULT_NAME)) {
				boolean inCatch = Boolean.valueOf((String)marker.getAttribute(RLMarkerAttribute.SS_IN_CATCH));
				String faultName = (String) marker.getAttribute(RLMarkerAttribute.ERR_SS_FAULT_NAME);

				String[] smellList;
				if (inCatch)	
					smellList = RLMarkerAttribute.CS_CATCH_TYPE;
				else			
					smellList = RLMarkerAttribute.CS_TOTAL_TYPE;

				for (String type : smellList) {
					boolean isDetect  = Boolean.valueOf((String) marker.getAttribute(type));
					if (!isDetect)
						markerList.add(new CSQuickFix(resource.getString("err.ss.fault.name1") + " " + faultName + " " + resource.getString("err.ss.fault.name2") + " " + type, type, inCatch));
				}
			} else if(problem.equals(RLMarkerAttribute.CS_EXCEPTION_RLADVICE)){
				String advice = (String) marker.getAttribute(IMarker.MESSAGE);
				if(advice.contains(RTag.class.getSimpleName())){
					markerList.add(new AchieveRL1QuickFix("RL1 quick gene ==> Rethrow Unckecked Exception"));
				}
			}
			IMarkerResolution[] markerArray = markerList.toArray(new IMarkerResolution[markerList.size()]);
			return markerArray;
		} catch (CoreException ex) {
			logger.error("[getResolutions] EXCEPTION ",ex);
			return new IMarkerResolution[0];
		}
	}
}
