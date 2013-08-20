package robusta.specificNodeCounter;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;

/**
 * @author pig
 */
public class SpecificNodeVisitor extends ASTVisitor {

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
