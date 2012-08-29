package ntut.csie.csdet.visitor;

import java.util.ArrayList;
import java.util.List;
import ntut.csie.csdet.data.MarkerInfo;
import ntut.csie.csdet.preference.SmellSettings;
import ntut.csie.jdt.util.NodeUtils;
import ntut.csie.rleht.builder.RLMarkerAttribute;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.TryStatement;

public class NestedTryStatementVisitor extends ASTVisitor {
	private CompilationUnit compilationUnit;
	private List<MarkerInfo> nestedTryStatementList;
	private boolean isDetectingNestedTryStatementmell;
	
	public NestedTryStatementVisitor(CompilationUnit compilationUnit) {
		super();
		this.compilationUnit = compilationUnit;
		nestedTryStatementList = new ArrayList<MarkerInfo>();
		SmellSettings sms = new SmellSettings(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
		isDetectingNestedTryStatementmell = sms.isDetectingSmell(SmellSettings.SMELL_NESTEDTRYBLOCK);
	}
	
	/**
	 * Get the type of bad smell list of NestedTryStatemnt.
	 * @return
	 */
	public List<MarkerInfo> getNestedTryStatementList() {
		return nestedTryStatementList;
	}
	
	@Override
	public boolean visit(TryStatement node) {
		ASTNode parentTryStatement = NodeUtils.getSpecifiedParentNode(node, ASTNode.TRY_STATEMENT);
		if (parentTryStatement != null && isDetectingNestedTryStatementmell) {
			collectSmell(node);
		}
		return true;
	}
	
	private void collectSmell(TryStatement node) {
		MarkerInfo markerInfo = new MarkerInfo(RLMarkerAttribute.CS_NESTED_TRY_BLOCK,
				null, node.toString(), node.getStartPosition(),
				compilationUnit.getLineNumber(node.getStartPosition()),	null);
		nestedTryStatementList.add(markerInfo);
	}
}
