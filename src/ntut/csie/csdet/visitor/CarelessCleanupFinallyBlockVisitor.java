package ntut.csie.csdet.visitor;

import java.util.List;

import ntut.csie.jdt.util.NodeUtils;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.TryStatement;

public class CarelessCleanupFinallyBlockVisitor extends ASTVisitor {
	private Block finallyBlock;
	private MethodInvocation closeResource;
	/** �O�_�n�ˬdFinallyBlock */
	private boolean isVisit;
	
	private boolean isCloseResourceCarelessCleanup;
	
	public CarelessCleanupFinallyBlockVisitor(Block currentFinally, List<MethodInvocation> closeResourceList) {
		finallyBlock = currentFinally;
		isVisit = false;
//		/*
//		 * �p�Gclose resource method invocation�S��expression�A
//		 * �N���~�򰻴��C
//		 * 
//		 * TODO Close(fis)�����Ҽ{
//		 */
//		if(isMethodInvocationWithExpression()) {
//			isVisit = true;
//		}
		isCloseResourceCarelessCleanup = false;
	}
	
//	/**
//	 * CloseResource�o��method invocation�O�_�֦�expression�C
//	 * fis.close(); (�䤤fis�s��expression�Aclose�s��name)
//	 * 
//	 * @return
//	 */
//	private boolean isMethodInvocationWithExpression() {
//		if(closeResource.getExpression() == null)
//			return false;
//		return true;
//	}
	
	public boolean visit(MethodInvocation node) {
		// �p�G�����귽���{���X�w�g�Q�{�w��careless cleanup�A�N���ΦA������
		if(isCloseResourceCarelessCleanup) {
			return false;
		}
		
		TryStatement nodeBelongsTryStatement = null;
		// �p�G�o��node��TryStatement
		if(NodeUtils.getSpecifiedParentNode(node, ASTNode.TRY_STATEMENT) != null) {
			nodeBelongsTryStatement = (TryStatement) NodeUtils.getSpecifiedParentNode(node, ASTNode.TRY_STATEMENT);
			
			// �ӥB�o��TryStatement�֦���Finally��close method��Finally�ۦP
			if(nodeBelongsTryStatement.getFinally().equals(finallyBlock)) {
				// ���N���Ocareless cleanup
				isCloseResourceCarelessCleanup = false;
				return false;			
			}
		}
		
		SimpleName nodeExpression = (SimpleName)node.getExpression();
		SimpleName closeResourceExpression = (SimpleName)closeResource.getExpression();
		// �p�G�����귽��method invocation�P�{�b�o��method invocation�O�P�@��instance
		if(nodeExpression.getIdentifier().equals(closeResourceExpression.getIdentifier())) {
			// �ӥB�{�b�o��method invocatino�|�ߥX�ҥ~
			if(node.resolveMethodBinding().getExceptionTypes().length > 0) {
				isCloseResourceCarelessCleanup = true;
			}
		}
		return false;
	}
	
	/**
	 * �غc�l�ǤJ��close resource method invocation�O���O�@��careless cleanup�C
	 * @return
	 */
	public boolean isCloseResourceCareless() {
		return isCloseResourceCarelessCleanup;
	}
}
