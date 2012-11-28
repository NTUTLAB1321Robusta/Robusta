package ntut.csie.csdet.visitor;

import java.util.ArrayList;
import java.util.List;

import ntut.csie.csdet.data.MarkerInfo;
import ntut.csie.csdet.preference.SmellSettings;
import ntut.csie.csdet.visitor.aidvisitor.CarelessCleanupToleranceVisitor;
import ntut.csie.csdet.visitor.aidvisitor.CarelessCleanupOnlyInFinallyVisitor;
import ntut.csie.csdet.visitor.aidvisitor.CarelessClenupRaisedExceptionNotInTryCausedVisitor;
import ntut.csie.jdt.util.NodeUtils;
import ntut.csie.rleht.builder.RLMarkerAttribute;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.TryStatement;

/**
 * Careless Cleanup���Ĥ@���ˬd�覡�C
 * �p�G�����귽���{���X���Afinally�̭�����A�h�@�ߵ���Careless Cleanup�C
 * @author charles
 *
 */
public class CarelessCleanupVisitor2 extends ASTVisitor {
	/** AST��Root�A�ΨӨ�line number�Pstart position�� */
	private CompilationUnit root;
	
	/** �x�s��쪺�ҥ~�B�z�a���D�{���X�Ҧb����ƥH�ε{���X���q...�� */
	private List<MarkerInfo> carelessCleanupList;
	
	/** �ھڳ]�w�ɡA�M�w�O�_�n�������a���D */
	private boolean isDetectingCarelessCleanupSmell;
	
	public CarelessCleanupVisitor2(CompilationUnit compilationUnit) {
		super();
		this.root = compilationUnit;
		carelessCleanupList = new ArrayList<MarkerInfo>();
		SmellSettings smellSettings = new SmellSettings(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
		isDetectingCarelessCleanupSmell = smellSettings.isDetectingSmell(SmellSettings.SMELL_CARELESSCLEANUP);
	}
	
	/**
	 * �ھڳ]�w�ɪ���T�A�M�w�n���n���X��ʾ�C
	 */
	public boolean visit(MethodDeclaration node) {
		CarelessCleanupToleranceVisitor cctv = new CarelessCleanupToleranceVisitor();
		node.accept(cctv);
		return((!cctv.isTolerable()) && isDetectingCarelessCleanupSmell);
	}
	
	public List<MarkerInfo> getCarelessCleanupList() {
		return carelessCleanupList;
	}
	
	public boolean visit(MethodInvocation node) {
		// �bfinally�̭���close�ʧ@�A�N���|�i��o�̳B�z
		if(NodeUtils.isMethodInvocationInFinally(node)) {
			return false;
		}
		
		if(NodeUtils.isCloseResourceMethodInvocation(root, node)) {
			collectSmell(node);
		}
		
		return false;
	}
	
	/**
	 * �S�O�Ψӻ`��FinallyBlock�̭��|�y���ҥ~���`�I�C
	 */
	public boolean visit(Block node) {
		TryStatement tryStatement = (TryStatement)NodeUtils.getSpecifiedParentNode(node, ASTNode.TRY_STATEMENT);
		if(tryStatement != null) {
			Block finallyBlock = tryStatement.getFinally();
			// �o��Block�OFinally
			if ((finallyBlock != null) && (finallyBlock.equals(node))) {
				
				// ���`��Finally�̭���Careless Cleanup
				CarelessCleanupOnlyInFinallyVisitor ccoifv = new CarelessCleanupOnlyInFinallyVisitor(root);
				node.accept(ccoifv);
				for(MethodInvocation mi : ccoifv.getCarelessCleanupNodes()) {
					collectSmell(mi);
				}
				
				// �A�ӻ`��������y��instance�A�bTry�~������B�|�ߨҥ~�����p
				CarelessClenupRaisedExceptionNotInTryCausedVisitor ccrenitcv = 
					new CarelessClenupRaisedExceptionNotInTryCausedVisitor(ccoifv.getfineCleanupNodes());
				ASTNode md = NodeUtils.getSpecifiedParentNode(node, ASTNode.METHOD_DECLARATION);
				md.accept(ccrenitcv);
				for(MethodInvocation mi : ccrenitcv.getCarelessCleanupNodes()) {
					collectSmell(mi);
				}

				// �`�����N���n�A�~��visit�U�h��~
				return false;
			}
		}
		return true;
	}
	
	/**
	 * �N�o��node���Jbad smell�M�椤�C
	 * @param node
	 */
	private void collectSmell(MethodInvocation node) {
		StringBuilder exceptions = new StringBuilder();
		ITypeBinding[] exceptionTypes = NodeUtils.getMethodInvocationThrownCheckedExceptions(node);
		if (exceptionTypes != null) {
			for (ITypeBinding itb : exceptionTypes) {
				exceptions.append(itb.toString());
				exceptions.append(",");
			}
		}
		ASTNode parentNode = NodeUtils.getSpecifiedParentNode(node, ASTNode.TRY_STATEMENT);
		MarkerInfo markerInfo = new MarkerInfo(RLMarkerAttribute.CS_CARELESS_CLEANUP,
			(node.getExpression() != null)? node.getExpression().resolveTypeBinding() : null,
			node.toString(), node.getStartPosition(),
			root.getLineNumber(node.getStartPosition()),
			(exceptionTypes != null)? exceptions.toString() : null);
		markerInfo.setIsInTry((parentNode != null)? true:false);
		carelessCleanupList.add(markerInfo);
	}
}