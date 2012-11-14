package ntut.csie.robusta.codegen.markerresolution;

import ntut.csie.csdet.preference.SmellSettings;
import ntut.csie.csdet.visitor.UserDefinedMethodAnalyzer;
import ntut.csie.rleht.builder.RLMarkerAttribute;
import ntut.csie.robusta.agile.exception.Robustness;
import ntut.csie.robusta.agile.exception.RTag;
import ntut.csie.robusta.codegen.CatchClauseFinderVisitor;
import ntut.csie.robusta.codegen.QuickFixCore;
import ntut.csie.robusta.codegen.QuickFixUtils;

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
	//紀錄code smell的type
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
		
		SingleVariableDeclaration singleVariableDeclaration = exactlyCatchClause.getException();
		Class<?> exceptionType = null;
		try {
			exceptionType = Class.forName(singleVariableDeclaration.getType().resolveBinding().getQualifiedName());
		} catch (ClassNotFoundException e) {
			logger.debug(e.getLocalizedMessage());
			throw new RuntimeException("Failed to resolve the exception type in catch clause.", e);
		}

		if(smellSettings.isAddingRobustnessAnnotation()) {
			// 建立Robustness Annotation
			quickFixCore.generateRobustnessLevelAnnotation(methodDeclaration, 1, exceptionType);
		}
		// 在Method上宣告例外拋出
		quickFixCore.generateThrowExceptionOnMethodDeclaration(methodDeclaration, exceptionType);
		// 移除dummy handler的敘述
		quickFixCore.removeNodeInCatchClause(exactlyCatchClause, ".printStackTrace()", "System.out.print");
		// 加上拋出例外 throw e
		quickFixCore.addThrowExceptionInCatchClause(exactlyCatchClause);

		// 寫回Edit中
		quickFixCore.applyChange();
	}

	/**
	 * 蒐集marker資訊，並回傳methodDelcaration的index
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
			  !(problem.equals(RLMarkerAttribute.CS_INGNORE_EXCEPTION))) {
				return methodIdx;
			}

			methodIdx = (String) marker.getAttribute(RLMarkerAttribute.RL_METHOD_INDEX);
			//儲存按下QuickFix該行的程式起始位置
			srcPos = marker.getAttribute(RLMarkerAttribute.RL_INFO_SRC_POS).toString();
			
		} catch (CoreException e) {
			logger.error(e.getMessage());
			throw e;
		}
		return methodIdx;
	}
}
