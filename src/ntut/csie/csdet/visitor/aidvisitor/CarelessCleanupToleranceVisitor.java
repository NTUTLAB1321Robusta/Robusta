package ntut.csie.csdet.visitor.aidvisitor;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.TryStatement;

/**
 * 判斷某個MethodDeclaration是不是Careless Cleanup允許的例外狀況
 * @author charles
 *
 */
public class CarelessCleanupToleranceVisitor extends ASTVisitor {
	/** 是不是要忽略檢查這個MethodDeclaration */
	private boolean isIgnoringMethodDeclaration;
	public CarelessCleanupToleranceVisitor() {
		isIgnoringMethodDeclaration = false;
	}
	
	public boolean visit(MethodDeclaration node) {
		if (node.getBody().statements().size() == 1) {	
			return true;
		}
		return false;
	}
	
	public boolean visit(TryStatement node) {
		if(node.getBody().statements().size() == 1) {
			return true;
		}
		return false;
	}

	public boolean visit(IfStatement node) {
		if(node.getElseStatement() != null) {
			return false; 
		}
		
//		/*
//		 *  if 裡面只有一個try statement 的語法有兩種寫法。
//		 *  一個是沒有花括號的，你會直接抓到try statement。
//		 *  一個是有花括號的，你會在then statement抓到Block。
//		 */
//		Statement thenStatement = node.getThenStatement();
//		if ((thenStatement.getNodeType() == ASTNode.EXPRESSION_STATEMENT) || 
//			(thenStatement.getNodeType() == ASTNode.BLOCK)) {
//			return true;
//		}
//		
//		return false;
		return true;
	}
	
	public boolean visit(MethodInvocation node) {
//		if(node.resolveMethodBinding().getExceptionTypes().length == 0)
		isIgnoringMethodDeclaration = true;
		return false;
	}
	
	public boolean visit(Block node) {
		if(node.getParent().getNodeType() == ASTNode.METHOD_DECLARATION) {
			return true;
		}
		
		if (node.statements().size() != 1)
			return false;
		
		return true;
	}
	
	public boolean visit(CatchClause node) {
		CatchClauseThrowStatementVisitor cctv = new CatchClauseThrowStatementVisitor();
		node.accept(cctv);
		if(cctv.isThrowStatementInCatch())
			isIgnoringMethodDeclaration = false;
		return false;
	}
	
	public boolean isTolerable() {
		return isIgnoringMethodDeclaration;
	}
	
}
