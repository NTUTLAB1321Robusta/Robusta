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
	private boolean isTarget, inFinally;
	private boolean isDetectingOverwrittenLeadExceptionSmell;
	
	public OverwrittenLeadExceptionVisitor(CompilationUnit compilationUnit) {
		overwrittenLeadList = new ArrayList<MarkerInfo>();
		root = compilationUnit;
		isTarget = false;
		inFinally = false;
		SmellSettings smellSettings = new SmellSettings(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
		isDetectingOverwrittenLeadExceptionSmell = smellSettings.isDetectingSmell(SmellSettings.SMELL_OVERWRITTENLEADEXCEPTION);
	}
	
	/**
	 * �ھڳ]�w�ɪ���T�A�M�w�n���n���X��ʾ�C
	 */
	public boolean visit(MethodDeclaration node) {
		isTarget = false;
		inFinally = false;
		return isDetectingOverwrittenLeadExceptionSmell;
	}
	
	public boolean visit(TryStatement node) {
		// try statement �S�� catch clause�A�N�n����try block�A�o����O�ΨӧP�_�̭���try statement�Ϊ�
		if(node.catchClauses().size() != 0)
			isTarget = false;
		
		// try �~���S�� try ��ܥ��O�̥~�h�A�h��l�Ʊ��� 
		if(NodeUtils.getSpecifiedParentNode(node, ASTNode.TRY_STATEMENT) == null) {
			isTarget = false;
			inFinally = false;
		}
		return true;
	}
	
	public boolean visit(CatchClause node) {
		if(inFinally)
			isTarget = true;
		return true;
	}
	
	public boolean visit(ThrowStatement node) {
		if(isTarget)
			addMarkerInfo(node);
		return false;
	}
	
	public boolean visit(Block node) {
		ASTNode parentNode = NodeUtils.getSpecifiedParentNode(node, ASTNode.TRY_STATEMENT);
		// block�����ˬOtry statement
		if(parentNode != null && parentNode.getNodeType() == ASTNode.TRY_STATEMENT) {
			// ���˦�finally block�A�B���˪�finally block����o��block�A��ܳo�O�ڭ̭n�䪺finally block
			if(((TryStatement) parentNode).getFinally() != null && ((TryStatement) parentNode).getFinally().getStartPosition() == node.getStartPosition()) {
				inFinally = true;
				isTarget = true;
			}
		}
		
		return true;
	}
	
	public boolean visit(MethodInvocation node) {
		if(isTarget) {
			ITypeBinding[] exTypes =  node.resolveMethodBinding().getExceptionTypes();
			// method invocation�|��checked exception����
			if(exTypes.length > 0)
				addMarkerInfo(node);
		}
		return true; 
	}
	
	public void endVisit(Block node) {
		TryStatement parentNode = (node.getParent().getNodeType() == ASTNode.TRY_STATEMENT) ? (TryStatement)node.getParent() : null;
		if(parentNode != null && parentNode.getFinally() != null && parentNode.getFinally().getStartPosition() == node.getStartPosition()) {
			isTarget = false;
		}
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
