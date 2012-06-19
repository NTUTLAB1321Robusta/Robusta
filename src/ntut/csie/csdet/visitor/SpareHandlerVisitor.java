package ntut.csie.csdet.visitor;

import java.util.List;

import ntut.csie.rleht.common.RLBaseVisitor;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.TryStatement;

/**
 * 找尋專案中所有的spare handler
 * @author chewei
 */
public class SpareHandlerVisitor extends RLBaseVisitor {
	
	private boolean result = false;	
	/** 滑鼠反白選到的節點 */
	private ASTNode selectNode = null;
	
	public SpareHandlerVisitor(ASTNode node) {
		super(true);
		this.selectNode = node;
	}
	
	/*
	 * (non-Javadoc)
	 * @see ntut.csie.rleht.views.ASTBaseVisitor#visitNode(org.eclipse.jdt.core.dom.ASTNode)
	 */
	protected boolean visitNode(ASTNode node) {
		switch (node.getNodeType()) {
			case ASTNode.TRY_STATEMENT:
				processTryStatement(node);
				return false;
			default:
				return true;
		}
	}
	
	/**
	 * 尋找要被refactor的try statement
	 * @param node
	 */
	private void processTryStatement(ASTNode node) {
		//只要在catch block之中還有一個try-catch,則視為spare handler
		TryStatement ts = (TryStatement)node;
		List<?> catchList = ts.catchClauses();
		if(catchList != null && selectNode != null) {
			if(ts.getStartPosition() == selectNode.getStartPosition()) {
				//找到那個try的節點就設定為true				
				result = true;
			}				
		}
	}
	
	/**
	 * 利用此結果來得知是否有找到要被refactor的節點
	 * @return
	 */
	public boolean getResult() {
		return this.result;
	}
}