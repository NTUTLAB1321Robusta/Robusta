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
 * �Q�[�Wover logging marker���{���X�A�i�H�Q�γo��Class�Ӳ����a���D�C
 * �p�G�}�o�H�������@�Ҥ@�˪��{���X�ݩ�over logging marker���a���D�A�o��@���u�|�����@��C
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
		 * �]��Over Logging����T�u��log�Ӧ檺Line Number�A
		 * �ҥH�o��O�z�L�渹�ӴM��Ӧ�{���X�����e�C
		 */
		int overLoggingCodeLineNumber = findOverLoggingCodeLineNumber(methodDeclaration, Integer.parseInt(msgIdx));
		ExpressionStatementLineNumberFinderVisitor expressionStatementLineNumberFinderVisitor = new ExpressionStatementLineNumberFinderVisitor(compilationUnit, overLoggingCodeLineNumber);
		compilationUnit.accept(expressionStatementLineNumberFinderVisitor);
		String overLoggingStatement = expressionStatementLineNumberFinderVisitor.getExpressionStatement().getExpression().toString();
		
		/*
		 * ���{���X���e�H��A�A��Ӧ�R���C
		 */
		quickFixCore.removeNodeInCatchClause(exactlyCatchClause, overLoggingStatement);
		
		quickFixCore.applyChange();
	}
	
	/**
	 * �qIMaker�`���a���D��������T
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
			// �����h�áA��ɬ���Marker�O����Catch clause �� Start position
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
	 * ���o��Over Logging�a���D�Ӧ�{���X���渹
	 * @param currentMethodNode
	 * @param markerInfoIndex
	 * @return
	 */
	private int findOverLoggingCodeLineNumber(MethodDeclaration currentMethodNode, int markerInfoIndex) {
		List<MarkerInfo> loggingMarkers = null;
		if (currentMethodNode != null) {
			// �M���method����OverLogging
			OverLoggingDetector loggingDetector = new OverLoggingDetector(quickFixCore.getCompilationUnit(), currentMethodNode);
			loggingDetector.detect();
			// ���o�M�פ�OverLogging
			loggingMarkers = loggingDetector.getOverLoggingList();
		}
		MarkerInfo markerInfo = loggingMarkers.get(markerInfoIndex);
		return markerInfo.getLineNumber();
	}
}
