package ntut.csie.analyzer.careless;

import java.util.Iterator;
import java.util.List;

import ntut.csie.analyzer.ThrownExceptionBeCaughtDetector;
import ntut.csie.util.BoundaryChecker;
import ntut.csie.util.NodeUtils;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.dom.ThrowStatement;
import org.eclipse.jdt.core.dom.TryStatement;

/**
 * It will check if any exception been declared and non-caught before
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
	 * Tell if there is any declared exception between checkingNode and it's parent.
	 */
	private boolean isNodeUnsafeInParent(ASTNode checkingNode) {
		// Collect all statements in this block
		StatementsInBlockCollectingVisitor statementCollector = new StatementsInBlockCollectingVisitor();
		Block parentBlock = (Block) NodeUtils.getSpecifiedParentNode(checkingNode, ASTNode.BLOCK);
		parentBlock.accept(statementCollector);
		List<Statement> allStatements = statementCollector.getStatementsInBlock();
		
		// Set the area of detection for class StatementDeclaredChecker
		ThrownExceptionBeCaughtDetector thrownExceptionDetector = 
				new ThrownExceptionBeCaughtDetector(parentBlock);
		BoundaryChecker boundChecker = new BoundaryChecker(beginningPosition,
				checkingNode.getStartPosition());
		
		// Return is there any declared exception in any statement
		Iterator<Statement> iter = allStatements.iterator();
		while (iter.hasNext()) {
			Statement statement = iter.next();
			StatementDeclaredChecker declaredChecher = 
					new StatementDeclaredChecker(thrownExceptionDetector, boundChecker);
			statement.accept(declaredChecher);
			if (declaredChecher.isAnyExceptionOut()) {
				return true;
			}
		}
		return false;
	}

	private class StatementDeclaredChecker extends ASTVisitor {

		private boolean isAnyExceptionOut = false;
		ThrownExceptionBeCaughtDetector thrownExceptionDetector;
		BoundaryChecker boundChecker;

		public StatementDeclaredChecker(
				ThrownExceptionBeCaughtDetector exceptionDetector,
				BoundaryChecker boundChecker) {
			thrownExceptionDetector = exceptionDetector;
			this.boundChecker = boundChecker;
		}

		public boolean isAnyExceptionOut() {
			return isAnyExceptionOut;
		}

		/**
		 * Stop when already found an exception been thrown or already out of
		 * the area we care.
		 */
		public boolean preVisit2(ASTNode node) {
			preVisit(node);
			return (!isAnyExceptionOut && isNodeInBoundary(node));
		}

		public boolean visit(MethodInvocation node) {
			if (thrownExceptionDetector
					.isAnyDeclaredExceptionBeenThrowOut(node)) {
				isAnyExceptionOut = true;
			}
			return true;
		}

		public boolean visit(SuperMethodInvocation node) {
			if (thrownExceptionDetector
					.isAnyDeclaredExceptionBeenThrowOut(node)) {
				isAnyExceptionOut = true;
			}
			return true;
		}
		
		public boolean visit(ThrowStatement node) {
			if (thrownExceptionDetector
					.isAnyDeclaredExceptionBeenThrowOut(node)) {
				isAnyExceptionOut = true;
			}
			return true;
		}

		public boolean visit(ClassInstanceCreation node) {
			if (thrownExceptionDetector
					.isAnyDeclaredExceptionBeenThrowOut(node)) {
				isAnyExceptionOut = true;
			}
			return true;
		}

		private boolean isNodeInBoundary(ASTNode node) {
			return boundChecker.isInOpenInterval(node);
		}
	}
}
