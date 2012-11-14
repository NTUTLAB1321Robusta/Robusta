package ntut.csie.robusta.codegen.markerresolution;

import java.util.List;

import ntut.csie.csdet.data.MarkerInfo;
import ntut.csie.csdet.visitor.OverLoggingDetector;
import ntut.csie.rleht.builder.RLMarkerAttribute;
import ntut.csie.robusta.codegen.CatchClauseFinderVisitor;
import ntut.csie.robusta.codegen.ExpressionStatementLineNumberFinderVisitor;
import ntut.csie.robusta.codegen.QuickFixCore;
import ntut.csie.robusta.codegen.QuickFixUtils;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.ui.IMarkerResolution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 被加上over logging marker的程式碼，可以利用這個Class來移除壞味道。
 * 如果開發人員有兩行一模一樣的程式碼屬於over logging marker的壞味道，這邊一次只會移除一行。
 * @author Charles
 *
 */
public class RemoveOverLoggingStatementQuickFix implements IMarkerResolution {
	private static Logger logger = LoggerFactory.getLogger(RemoveOverLoggingStatementQuickFix.class);
	
	private String problem;

	private QuickFixCore quickFixCore;
	
	private String label;
	
	public RemoveOverLoggingStatementQuickFix(String label) {
		this.label = label;
		quickFixCore = new QuickFixCore();
	}
	
	@Override
	public String getLabel() {
		return label;
	}

	@Override
	public void run(IMarker marker) {
		String[] markerContents;
		String methodIdx="";
		String msgIdx = "";
		String ccStartPosition = "";
		
		try {
			markerContents = collectMarkerInfo(marker);
		} catch (CoreException e) {
			throw new RuntimeException("Failed to resolve marker attribute.", e);
		}
		methodIdx = markerContents[0];
		msgIdx = markerContents[1];
		ccStartPosition = markerContents[2];
		
		quickFixCore.setJavaFileModifiable(marker.getResource());
		CompilationUnit compilationUnit = quickFixCore.getCompilationUnit(); 
		MethodDeclaration methodDeclaration = QuickFixUtils.getMethodDeclaration(compilationUnit, Integer.parseInt(methodIdx));
		if(methodDeclaration == null) {
			return;
		}
		
		CatchClauseFinderVisitor catchClauseFinder = new CatchClauseFinderVisitor(Integer.parseInt(ccStartPosition));
		compilationUnit.accept(catchClauseFinder);
		CatchClause exactlyCatchClause = catchClauseFinder.getFoundCatchClause();
		if(exactlyCatchClause == null) {
			return;
		}
		
		/*
		 * 因為Over Logging的資訊只有log該行的Line Number，
		 * 所以這邊是透過行號來尋找該行程式碼的內容。
		 */
		int overLoggingCodeLineNumber = findOverLoggingCodeLineNumber(methodDeclaration, Integer.parseInt(msgIdx));
		ExpressionStatementLineNumberFinderVisitor expressionStatementLineNumberFinderVisitor = new ExpressionStatementLineNumberFinderVisitor(compilationUnit, overLoggingCodeLineNumber);
		compilationUnit.accept(expressionStatementLineNumberFinderVisitor);
		String overLoggingStatement = expressionStatementLineNumberFinderVisitor.getExpressionStatement().getExpression().toString();
		
		/*
		 * 找到程式碼內容以後，再把該行刪掉。
		 */
		quickFixCore.removeNodeInCatchClause(exactlyCatchClause, overLoggingStatement);
		
		quickFixCore.applyChange();
	}
	
	/**
	 * 從IMaker蒐集壞味道的相關資訊
	 * @param marker
	 * @return
	 * @throws CoreException
	 */
	private String[] collectMarkerInfo(IMarker marker) throws CoreException {
		String methodIdx = "";
		String msgIdx = "";
		String ccStartPosition = "";
		try {
			problem = (String) marker.getAttribute(RLMarkerAttribute.RL_MARKER_TYPE);
			if(problem == null) {
				return null;
			}
			if(!(problem.equals(RLMarkerAttribute.CS_OVER_LOGGING))) {
				return null;
			}
			methodIdx = (String) marker.getAttribute(RLMarkerAttribute.RL_METHOD_INDEX);
			msgIdx = (String) marker.getAttribute(RLMarkerAttribute.RL_MSG_INDEX); 
			// 不用懷疑，當時紀錄Marker是紀錄Catch clause 的 Start position
			ccStartPosition = marker.getAttribute(RLMarkerAttribute.RL_INFO_SRC_POS).toString();
		} catch (CoreException e) {
			logger.error(e.getMessage());
			throw e;
		}
		String[] result = new String[3];
		result[0] = methodIdx;
		result[1] = msgIdx;
		result[2] = ccStartPosition;
		return result;
	}
	
	/**
	 * 取得有Over Logging壞味道該行程式碼的行號
	 * @param currentMethodNode
	 * @param markerInfoIndex
	 * @return
	 */
	private int findOverLoggingCodeLineNumber(MethodDeclaration currentMethodNode, int markerInfoIndex) {
		List<MarkerInfo> loggingMarkers = null;
		if (currentMethodNode != null) {
			// 尋找該method內的OverLogging
			OverLoggingDetector loggingDetector = new OverLoggingDetector(quickFixCore.getCompilationUnit(), currentMethodNode);
			loggingDetector.detect();
			// 取得專案中OverLogging
			loggingMarkers = loggingDetector.getOverLoggingList();
		}
		MarkerInfo markerInfo = loggingMarkers.get(markerInfoIndex);
		return markerInfo.getLineNumber();
	}
}
