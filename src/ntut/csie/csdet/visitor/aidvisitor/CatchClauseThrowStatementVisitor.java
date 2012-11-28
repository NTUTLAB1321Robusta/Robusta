package ntut.csie.csdet.visitor.aidvisitor;

import ntut.csie.jdt.util.NodeUtils;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.ThrowStatement;

/**
 * 檢查CatchClause裡面有沒有ThrowStatement
 * @author charles
 *
 */
public class CatchClauseThrowStatementVisitor extends ASTVisitor {
	private boolean isThrowStatementInCatch;
	public CatchClauseThrowStatementVisitor() {
		super();
		isThrowStatementInCatch = false;
	}

	public boolean visit(ThrowStatement node) {
		if(NodeUtils.getSpecifiedParentNode(node, ASTNode.CATCH_CLAUSE) != null) {
			isThrowStatementInCatch = true;
		}
		return false;
	}
	
	public boolean isThrowStatementInCatch() {
		return isThrowStatementInCatch;
	}
}
