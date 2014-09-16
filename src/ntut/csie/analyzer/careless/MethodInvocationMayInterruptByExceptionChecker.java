package ntut.csie.analyzer.careless;

import java.util.Iterator;
import java.util.List;

import ntut.csie.util.BoundaryChecker;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.InfixExpression;
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
			if (isParentUnsafe(checkingNode) || isThereUnsafeBrother(checkingNode)) {
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

	private boolean isParentUnsafe(ASTNode node) {
		/*
		 * true: some situation will always safe, check it
		 * false: this node is a element of statement, it is always safe
		 */
		if (node instanceof Statement) {
			ASTNode parent = node.getParent();
			int parentType = parent.getNodeType();
			
			boolean isParentBlock = (parentType == ASTNode.BLOCK);
			boolean isFinallBlockOrCatchClause = (parentType == ASTNode.TRY_STATEMENT);
			boolean isCatchBlock = (parentType == ASTNode.CATCH_CLAUSE);
			
			// Check if the parent is a simple non-null checking expression
			boolean isParentSimpleNonNullChecking = false;
			try {
				InfixExpression infixExpression = ((InfixExpression) ((IfStatement) parent).getExpression());
				isParentSimpleNonNullChecking = isCheckingSimpleNonNull(infixExpression);
			} catch (ClassCastException e) {
				// This empty catch block is inevitable,
				// the castings also act as if statements
				// It is not a simple non-null checking expression, keep isSimpleNonnullChecking false
			}

			return !(isParentBlock || isFinallBlockOrCatchClause
					|| isCatchBlock || isParentSimpleNonNullChecking);
		} else {
			return false;
		}
	}

	/**
	 * Tell if it is one side NULL_LITERAL and other side SIMPLE_NAME
	 */
	private boolean isCheckingSimpleNonNull(InfixExpression expression) {
		int rightType = expression.getRightOperand().getNodeType();
		int leftType = expression.getLeftOperand().getNodeType();
		
		// TODO should check one of them is the resource to be closed
		if (rightType == ASTNode.NULL_LITERAL || leftType == ASTNode.NULL_LITERAL) {
			if (rightType == ASTNode.SIMPLE_NAME || leftType == ASTNode.SIMPLE_NAME) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Tell if there is any may-thrown-exception between checkingNode and it's parent.
	 */
	private boolean isThereUnsafeBrother(ASTNode checkingNode) {
		// Set the area of detection for checkingNode
		BoundaryChecker boundChecker = new BoundaryChecker(beginningPosition,
				checkingNode.getStartPosition());
		
		// Collect all brother statements, and return if there is any may-thrown-statement between
		ASTNode parent = checkingNode.getParent();
		if (parent.getNodeType() == ASTNode.BLOCK) {
			List<Statement> allStatements = ((Block) parent).statements();

			// Return is there any may-thrown-statement between
			Iterator<Statement> iter = allStatements.iterator();
			while (iter.hasNext()) {
				Statement statement = iter.next();
				if (boundChecker.isInOpenInterval(statement.getStartPosition()) &&
						isUnsafeBrotherStatement(statement)) {
					return true;
				} 
			}
		}
		return false;
	}

	/**
	 * Return false only if the statements will not throw any exception in 100%.
	 */
	private boolean isUnsafeBrotherStatement(Statement statement) {
		boolean isEmptyStatement = (statement.getNodeType() == ASTNode.EMPTY_STATEMENT);
		boolean isTryBlock = (statement.getParent().getNodeType() == ASTNode.TRY_STATEMENT);
		
		if (isEmptyStatement || isTryBlock) {
			return false;
		}
		return true;
	}
}
