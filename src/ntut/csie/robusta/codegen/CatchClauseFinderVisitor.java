package ntut.csie.robusta.codegen;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.TryStatement;

public class CatchClauseFinderVisitor extends ASTVisitor {
	/**	將找到的CatchClause存在這裡 */
	private CatchClause foundCatchClause;
	/** 要找的CatchClause所在位置 */
	private int ccStartPosition;
	
	public CatchClauseFinderVisitor(int startPosition) {
		foundCatchClause = null;
		ccStartPosition = startPosition;
	}
	
	public boolean visit(MethodDeclaration node) {
		if(node.getStartPosition() > ccStartPosition) {
			return false;
		}
		
		if((node.getStartPosition() + node.getLength()) < ccStartPosition) {
			return false;
		}
		
		return true;
	}
	
	public boolean visit(TryStatement node) {
		if(node.getStartPosition() > ccStartPosition) {
			return false;
		}
		
		if((node.getStartPosition() + node.getLength()) < ccStartPosition) {
			return false;
		}
		
		return true;
	}
	
	public boolean visit(CatchClause node) {
		if(node.getStartPosition() == ccStartPosition) {
			foundCatchClause = node;
		}
		return false; 
	}
	
	public CatchClause getFoundCatchClause() {
		return foundCatchClause;
	}
}
