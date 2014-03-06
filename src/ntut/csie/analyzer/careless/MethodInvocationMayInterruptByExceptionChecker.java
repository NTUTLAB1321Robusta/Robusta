package ntut.csie.analyzer.careless;

import java.util.Iterator;
import java.util.List;

import ntut.csie.util.BoundaryChecker;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Statement;

public class MethodInvocationMayInterruptByExceptionChecker {

	private int beginningPosition;

	public boolean isMayInterruptByException(MethodInvocation methodInvocation) {
		beginningPosition = new ClosingResourceBeginningPositionFinder()
				.findPosition(methodInvocation);

		try {
			if (isThisMethodInvocationUnsafeOnParent(methodInvocation)) {
				return true;
			}
			
			ASTNode parentNode = methodInvocation.getParent();
			while(beginningPosition <= parentNode.getStartPosition()) {
				if(isParentUnsafeOnParent(parentNode)) {
					return true;
				}
				parentNode = parentNode.getParent();
			}
			return false;
		} catch(Exception e) {
			/*
			 * Any exception means it is not a ASTNode we can handle now, so we
			 * can't say that it is a bad smell
			 */
			return false;
		}
	}

	/**
	 * Tell if it is unsafe from it's parent's view only
	 * (Won't check if it's parent is safe or not)
	 */
	private boolean isThisMethodInvocationUnsafeOnParent(MethodInvocation methodInvocation) {
		return isAnyUnsafeStatementBefore(methodInvocation);
	}

	/**
	 * Tell if it is unsafe from it's parent's view only (Won't check if it's
	 * parent is safe or not)
	 */
	private boolean isParentUnsafeOnParent(ASTNode node) {
		if (node instanceof Statement
				&& isUnsafeParentStatement((Statement) node)) {
			return true;
		}
		return isAnyUnsafeStatementBefore(node);
	}

	/**
	 * Tell if there is any unsafe statement before the executedNode from it's
	 * parent's view
	 */
	private boolean isAnyUnsafeStatementBefore(ASTNode executedNode) {
		// Collect all first level child node in the parent node
		ASTNode parentNode = executedNode.getParent();
		FirstLevelChildStatementCollectingVisitor firstLevelChildCollector = new FirstLevelChildStatementCollectingVisitor();
		parentNode.accept(firstLevelChildCollector);
		List<Statement> allStatements = firstLevelChildCollector.getChildren();

		BoundaryChecker boundChecker = new BoundaryChecker(beginningPosition,
				executedNode.getStartPosition());
		/*
		 * If any statement satisfy both "between" and "unsafe", return true.
		 * Otherwise, return false.
		 */
		Iterator<Statement> iter = allStatements.iterator();
		while (iter.hasNext()) {
			Statement statement = iter.next();
			if (boundChecker.isInInterval(statement.getStartPosition())
					&& isUnsafeBrotherStatement(statement)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * For the parent statement that will make the statements inside unexecute.
	 */
	private boolean isUnsafeParentStatement(Statement Statement) {
		int nodeType = Statement.getNodeType();
		if (nodeType == ASTNode.BLOCK
				|| nodeType == ASTNode.EXPRESSION_STATEMENT
				|| nodeType == ASTNode.TRY_STATEMENT) {
			return false;
		}
		return true;
	}

	/**
	 * For the statements that will make the statements behind unexecute.
	 */
	private boolean isUnsafeBrotherStatement(Statement Statement) {
		int nodeType = Statement.getNodeType();
		if (nodeType == ASTNode.EMPTY_STATEMENT || isTryBlock(Statement)) {
			return false;
		}
		return true;
	}

	private boolean isTryBlock(Statement Statement) {
		return (Statement.getParent().getNodeType() == ASTNode.TRY_STATEMENT);
	}
}
