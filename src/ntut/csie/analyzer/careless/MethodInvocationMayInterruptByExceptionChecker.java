package ntut.csie.analyzer.careless;

import java.util.Iterator;
import java.util.List;

import ntut.csie.util.BoundaryChecker;
import ntut.csie.util.NodeUtils;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Statement;

/**
 * It will check if any exception may been thrown before
 */
public class MethodInvocationMayInterruptByExceptionChecker {

	private int beginningPosition;

	public boolean isMayInterruptByException(MethodInvocation methodInvocation) {
		findOutBeginningPosition(methodInvocation);

		ASTNode checkingNode = methodInvocation;
		// The condition is checking whether any statement didn't be checked.
		while (beginningPosition < checkingNode.getStartPosition()) {
			if (!isAlwaysSafeInParent(checkingNode) && isNodeUnsafeInParent(checkingNode)) {
				return true;
			}
			checkingNode = checkingNode.getParent();
		}
		
		return false;
	}

	private void findOutBeginningPosition(MethodInvocation methodInvocation) {
		beginningPosition = new ClosingResourceBeginningPositionFinder()
				.findPosition(methodInvocation);
	}

	private boolean isAlwaysSafeInParent(ASTNode node) {
		int parentType = node.getParent().getNodeType();
		/*
		 * Case: 
		 * 1. finally block and catch clause
		 * 2. catch clause's body
		 */
		return (parentType == ASTNode.TRY_STATEMENT || parentType == ASTNode.CATCH_CLAUSE);
	}

	/**
	 * Tell if there is any may-thrown-exception between checkingNode and it's parent.
	 */
	private boolean isNodeUnsafeInParent(ASTNode checkingNode) {
		// Set the area of detection for checkingNode
		BoundaryChecker boundChecker = new BoundaryChecker(beginningPosition,
				checkingNode.getStartPosition());
		
		// Collect all statements in parent block
		Block parentBlock = (Block) NodeUtils.getSpecifiedParentNode(checkingNode, ASTNode.BLOCK);
		List<Statement> allStatements = parentBlock.statements();
		
		// Return is there any may-thrown-statement between
		Iterator<Statement> iter = allStatements.iterator();
		while (iter.hasNext()) {
			Statement statement = iter.next();
			if (boundChecker.isInOpenInterval(statement.getStartPosition()) &&
					isUnsafeBrotherStatement(statement)) {
				return true;
			} 
		}
		return false;
	}

	/**
	 * Return false only if the statements will not throw any exception in 100%. 
	 */
	private boolean isUnsafeBrotherStatement(Statement statement) {
		int nodeType = statement.getNodeType();
		if (nodeType == ASTNode.EMPTY_STATEMENT) {
			return false;
		}
		return true;
	}
}
