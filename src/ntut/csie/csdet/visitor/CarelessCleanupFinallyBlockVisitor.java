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
	/** 是否要檢查FinallyBlock */
	private boolean isVisit;
	
	private boolean isCloseResourceCarelessCleanup;
	
	public CarelessCleanupFinallyBlockVisitor(Block currentFinally, List<MethodInvocation> closeResourceList) {
		finallyBlock = currentFinally;
		isVisit = false;
//		/*
//		 * 如果close resource method invocation沒有expression，
//		 * 就不繼續偵測。
//		 * 
//		 * TODO Close(fis)先不考慮
//		 */
//		if(isMethodInvocationWithExpression()) {
//			isVisit = true;
//		}
		isCloseResourceCarelessCleanup = false;
	}
	
//	/**
//	 * CloseResource這個method invocation是否擁有expression。
//	 * fis.close(); (其中fis叫做expression，close叫做name)
//	 * 
//	 * @return
//	 */
//	private boolean isMethodInvocationWithExpression() {
//		if(closeResource.getExpression() == null)
//			return false;
//		return true;
//	}
	
	public boolean visit(MethodInvocation node) {
		// 如果關閉資源的程式碼已經被認定為careless cleanup，就不用再偵測啦
		if(isCloseResourceCarelessCleanup) {
			return false;
		}
		
		TryStatement nodeBelongsTryStatement = null;
		// 如果這個node有TryStatement
		if(NodeUtils.getSpecifiedParentNode(node, ASTNode.TRY_STATEMENT) != null) {
			nodeBelongsTryStatement = (TryStatement) NodeUtils.getSpecifiedParentNode(node, ASTNode.TRY_STATEMENT);
			
			// 而且這個TryStatement擁有的Finally跟close method的Finally相同
			if(nodeBelongsTryStatement.getFinally().equals(finallyBlock)) {
				// 那就不是careless cleanup
				isCloseResourceCarelessCleanup = false;
				return false;			
			}
		}
		
		SimpleName nodeExpression = (SimpleName)node.getExpression();
		SimpleName closeResourceExpression = (SimpleName)closeResource.getExpression();
		// 如果關閉資源的method invocation與現在這個method invocation是同一個instance
		if(nodeExpression.getIdentifier().equals(closeResourceExpression.getIdentifier())) {
			// 而且現在這個method invocatino會拋出例外
			if(node.resolveMethodBinding().getExceptionTypes().length > 0) {
				isCloseResourceCarelessCleanup = true;
			}
		}
		return false;
	}
	
	/**
	 * 建構子傳入的close resource method invocation是不是一個careless cleanup。
	 * @return
	 */
	public boolean isCloseResourceCareless() {
		return isCloseResourceCarelessCleanup;
	}
}
