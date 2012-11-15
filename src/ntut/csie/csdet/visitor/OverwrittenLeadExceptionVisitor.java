package ntut.csie.csdet.visitor;

import java.util.ArrayList;
import java.util.List;

import ntut.csie.csdet.data.MarkerInfo;
import ntut.csie.csdet.preference.SmellSettings;
import ntut.csie.jdt.util.NodeUtils;
import ntut.csie.rleht.builder.RLMarkerAttribute;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.ThrowStatement;
import org.eclipse.jdt.core.dom.TryStatement;

public class OverwrittenLeadExceptionVisitor extends ASTVisitor {
	private CompilationUnit root;
	private List<MarkerInfo> overwrittenLeadList;
	private boolean isTarget;
	private boolean isDetectingOverwrittenLeadExceptionSmell;
	
	public OverwrittenLeadExceptionVisitor(CompilationUnit compilationUnit) {
		overwrittenLeadList = new ArrayList<MarkerInfo>();
		root = compilationUnit;
		isTarget = false;
		SmellSettings smellSettings = new SmellSettings(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
		isDetectingOverwrittenLeadExceptionSmell = smellSettings.isDetectingSmell(SmellSettings.SMELL_OVERWRITTENLEADEXCEPTION);
	}
	
	/**
	 * 根據設定檔的資訊，決定要不要拜訪整棵樹。
	 */
	public boolean visit(MethodDeclaration node) {
		return isDetectingOverwrittenLeadExceptionSmell;
	}
	
	public boolean visit(TryStatement node) {
		if(node.catchClauses().size() != 0)
			isTarget = false;
		if(NodeUtils.getSpecifiedParentNode(node, ASTNode.TRY_STATEMENT) == null && node.getFinally() == null)
			return false;
		return true;
	}
	
	public boolean visit(CatchClause node) {
		ASTNode tryParentNode = NodeUtils.getSpecifiedParentNode(node, ASTNode.TRY_STATEMENT);
		ASTNode tryAncestorNode = NodeUtils.getSpecifiedParentNode(tryParentNode, ASTNode.TRY_STATEMENT);
		if(tryParentNode != null && tryAncestorNode == null)
			return true;
		if(tryParentNode != null && tryAncestorNode != null && tryAncestorNode.getNodeType() == ASTNode.TRY_STATEMENT) {
			isTarget = true;
			return true;
		}
		isTarget = false;
		return false;
	}
	
	public boolean visit(ThrowStatement node) {
		if(isTarget)
			addMarkerInfo(node);
		return false;
	}
	
	public boolean visit(Block node) {
		ASTNode parentNode = NodeUtils.getSpecifiedParentNode(node, ASTNode.TRY_STATEMENT);
		if(parentNode != null && parentNode.getNodeType() == ASTNode.TRY_STATEMENT) {
			if(((TryStatement) parentNode).getFinally() != null && ((TryStatement) parentNode).getFinally().getStartPosition() == node.getStartPosition()) {
				isTarget = true;
			}
		} else
			isTarget = false;
		return true;
	}
	
	public boolean visit(MethodInvocation node) {
		if(isTarget) {
			ITypeBinding[] exTypes =  node.resolveMethodBinding().getExceptionTypes();
			if(exTypes.length > 0)
				addMarkerInfo(node);
		}
		return true; 
	}
	
	private void addMarkerInfo(MethodInvocation node) {
		MarkerInfo markerInfo = new MarkerInfo(RLMarkerAttribute.CS_OVERWRITTEN_LEAD_EXCEPTION,
				(node.getExpression() != null)? node.getExpression().resolveTypeBinding() : null,
						node.toString(), node.getStartPosition(),
				root.getLineNumber(node.getStartPosition()), null);
		markerInfo.setMethodThrownExceptions(node.resolveMethodBinding().getExceptionTypes());
		overwrittenLeadList.add(markerInfo);
	}
	
	private void addMarkerInfo(ThrowStatement node) {
		MarkerInfo markerInfo = new MarkerInfo(RLMarkerAttribute.CS_OVERWRITTEN_LEAD_EXCEPTION,
				(node.getExpression() != null)? node.getExpression().resolveTypeBinding() : null,
						node.toString(), node.getStartPosition(),
				root.getLineNumber(node.getStartPosition()), null);
		overwrittenLeadList.add(markerInfo);
	}
	
	public List<MarkerInfo> getOverwrittenList() {
		return overwrittenLeadList;
	}
}
