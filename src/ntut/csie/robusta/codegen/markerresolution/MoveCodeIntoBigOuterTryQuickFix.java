package ntut.csie.robusta.codegen.markerresolution;

import ntut.csie.csdet.preference.SmellSettings;
import ntut.csie.csdet.visitor.UserDefinedMethodAnalyzer;
import ntut.csie.rleht.builder.RLMarkerAttribute;
import ntut.csie.robusta.codegen.QuickFixCore;
import ntut.csie.robusta.codegen.QuickFixUtils;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.ui.IMarkerResolution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MoveCodeIntoBigOuterTryQuickFix implements IMarkerResolution{
	private static Logger logger = LoggerFactory.getLogger(MoveCodeIntoBigOuterTryQuickFix.class);

	private QuickFixCore quickFixCore;
	
	private String label;
	//¬ö¿ýcode smellªºtype
	private String problem;

	public MoveCodeIntoBigOuterTryQuickFix(String label) {
		this.label = label;
		quickFixCore = new QuickFixCore();
		new SmellSettings(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
	}
	
	@Override
	public String getLabel() {
		return label;
	}

	@Override
	public void run(IMarker marker) {
		String methodIdx="";
		
		try {
			methodIdx = collectMarkerInfo(marker);
		} catch (CoreException e) {
			throw new RuntimeException("Failed to resolve marker attribute.", e);
		}
		
		quickFixCore.setJavaFileModifiable(marker.getResource());
		CompilationUnit compilationUnit = quickFixCore.getCompilationUnit(); 
		MethodDeclaration methodDeclaration = QuickFixUtils.getMethodDeclaration(compilationUnit, Integer.parseInt(methodIdx));
		if(methodDeclaration == null) {
			return;
		}
		
		quickFixCore.generateBigOuterTryStatement(methodDeclaration);
		quickFixCore.applyChange();
	}
	
	private String collectMarkerInfo(IMarker marker) throws CoreException {
		String methodIdx = "";
		try {
			problem = (String) marker.getAttribute(RLMarkerAttribute.RL_MARKER_TYPE);
			if(problem == null || !(problem.equals(RLMarkerAttribute.CS_UNPROTECTED_MAIN))) {
				return methodIdx;
			}

			methodIdx = (String) marker.getAttribute(RLMarkerAttribute.RL_METHOD_INDEX);
			marker.getAttribute(RLMarkerAttribute.RL_INFO_SRC_POS).toString();
			
		} catch (CoreException e) {
			logger.error(e.getMessage());
			throw e;
		}
		return methodIdx;
	}

}
