package ntut.csie.csdet.visitor;

import java.util.List;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.TryStatement;

/**
 * 找尋專案中所有的spare handler
 * @author chewei
 */
public class SpareHandlerVisitor extends ASTVisitor {
	private boolean result = false;	
	/** 滑鼠反白選到的節點 */
	private ASTNode selectNode = null;
	
	public SpareHandlerVisitor(ASTNode node) {
		super(true);
		selectNode = node;
	}
	
	public boolean visit(TryStatement node) {
		processTryStatement(node);
		return false;
	}
	
	/**
	 * 尋找要被refactor的try statement
	 * @param node
	 */
	private void processTryStatement(ASTNode node) {
		/* 
		 * 特定節點如果滿足以下條件，則認定使用者認定它為spare handler，
		 * 並且想要用我們提供的Spare Handler 重構機制。
		 * 1. 只要Catch Clause裡面不是空的
		 * 2. Catch Clause裡面的所有程式碼都被選取
		 * FIXME: 
		 *  1. 他只會去判斷TryStatement有沒有整個被選起來，而不會去看這個TryStatement是不是在Catch Clause下面
		 *  2. 如果不是TryStatement，而是整個Catch Clause裡面的程式碼被選取，也應該要可以重構 
		 */
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
		return result;
	}
}