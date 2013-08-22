package robusta.specificNodeCounter;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;

/**
 * 根據建構時的設定偵測檔案中特定節點的個數
 * @author pig
 */
public class SpecificNodeVisitor extends ASTVisitor {

	/*
	 * 目前SpecificNodeCounter無法利用此class，
	 * 要使用時可以先利用 RLBuilder，
	 * 加入以下程式碼
		private int number = 0;
		SpecificNodeVisitor snVisitor = new SpecificNodeVisitor(ASTNode.CATCH_CLAUSE);
		root.accept(snVisitor);
		number += snVisitor.getCount();  
	 */
	
	private int specificNodeType;
	private int counter;
	
	/**
	 * 將指定的節點在 ASTNode 中的 type 設定好
	 * @param index
	 */
	public SpecificNodeVisitor(int type) {
		specificNodeType = type;
		counter = 0;
	}

	public void preVisit(ASTNode node) {
		if(node.getNodeType() == specificNodeType) {
			counter++;
		}
	}

	public int getCount() {
		return counter;
	}
}
