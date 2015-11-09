package ntut.csie.robusta.codegen.markerresolution;

import ntut.csie.analyzer.UserDefinedMethodAnalyzer;
import ntut.csie.csdet.preference.SmellSettings;
import ntut.csie.rleht.builder.RLMarkerAttribute;
import ntut.csie.robusta.agile.exception.Robustness;
import ntut.csie.robusta.agile.exception.RTag;
import ntut.csie.robusta.codegen.CatchClauseFinderVisitor;
import ntut.csie.robusta.codegen.QuickFixCore;
import ntut.csie.robusta.codegen.QuickFixUtils;
import ntut.csie.util.NodeUtils;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.ui.IMarkerResolution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



/**
 * 將Catch到的例外直接拋出，並且移除System.out.println或e.printStackTrace等資訊
 * @author Charles
 */
public class ThrowCheckedExceptionQuickFix implements IMarkerResolution{
	private static Logger logger = LoggerFactory.getLogger(ThrowCheckedExceptionQuickFix.class);

	private QuickFixCore quickFixCore;
	
	private String label;

	private String problem;

	private String srcPos;
	
	private SmellSettings smellSettings;

	public ThrowCheckedExceptionQuickFix(String label) {
		this.label = label;
		quickFixCore = new QuickFixCore();
		smellSettings = new SmellSettings(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
	}
	
	@Override
	public String getLabel() {
		return label;
	}

	@Override
	@Robustness(value = { @RTag(level = 2, exception = java.lang.RuntimeException.class) })
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
		
		CatchClauseFinderVisitor catchClauseFinder = new CatchClauseFinderVisitor(Integer.parseInt(srcPos));
		compilationUnit.accept(catchClauseFinder);
		CatchClause exactlyCatchClause = catchClauseFinder.getFoundCatchClause();
		if(exactlyCatchClause == null) {
			return;
		}
		
		Class<?> exceptionType = NodeUtils.getClassFromCatchClause(exactlyCatchClause);

		if(smellSettings.isAddingRobustnessAnnotation()) {
			quickFixCore.generateRobustnessLevelAnnotation(methodDeclaration, 1, exceptionType);
		}
		quickFixCore.generateThrowExceptionOnMethodSignature(methodDeclaration, exceptionType);
		quickFixCore.removeNodeInCatchClause(exactlyCatchClause, ".printStackTrace()", "System.out.print");
		quickFixCore.addThrowExceptionInCatchClause(exactlyCatchClause);

		quickFixCore.applyChange();
	}

	/**
	 * collect marker information and return the index of methodDelcaration 
	 * @param marker
	 * @return
	 * @throws CoreException
	 */
	private String collectMarkerInfo(IMarker marker) throws CoreException {
		String methodIdx = "";
		try {
			problem = (String) marker.getAttribute(RLMarkerAttribute.RL_MARKER_TYPE);
			if(problem == null) {
				return methodIdx;
			}
			if(!(problem.equals(RLMarkerAttribute.CS_DUMMY_HANDLER)) && 
			  !(problem.equals(RLMarkerAttribute.CS_EMPTY_CATCH_BLOCK))) {
				return methodIdx;
			}

			methodIdx = (String) marker.getAttribute(RLMarkerAttribute.RL_METHOD_INDEX);
			//save the start line position of statement which invoke QuickFix  
			srcPos = marker.getAttribute(RLMarkerAttribute.RL_INFO_SRC_POS).toString();
			
		} catch (CoreException e) {
			logger.error(e.getMessage());
			throw e;
		}
		return methodIdx;
	}
}
