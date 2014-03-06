package ntut.csie.analyzer.careless;

import java.util.List;

import ntut.csie.util.BoundaryChecker;
import ntut.csie.util.NodeUtils;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SimpleName;

public class ClosingResourceBeginningPositionFinder {

	private int lowerBound;
	BoundaryChecker boundaryChecker;
	private int beginningPosition;
	
	public int findPosition(MethodInvocation methodInvocation) {
		// initialized
		lowerBound = getStartPositionOfMethodDeclaration(methodInvocation);
		boundaryChecker = new BoundaryChecker(lowerBound,
				methodInvocation.getStartPosition());

		return findResourceStartPositionForMethodDeclaration(methodInvocation);
	}

	private int getStartPositionOfMethodDeclaration(
			MethodInvocation methodInvocation) {
		MethodDeclaration methodDeclaration = (MethodDeclaration) NodeUtils
				.getSpecifiedParentNode(methodInvocation,
						ASTNode.METHOD_DECLARATION);
		return methodDeclaration.getStartPosition();
	}

	/**
	 * If the resource is not start on this MethodDeclaration, then will set the
	 * start position at the start position of this MethodDeclaration
	 */
	private int findResourceStartPositionForMethodDeclaration(
			MethodInvocation methodInvocation) {
		try {
			if (!methodInvocation.arguments().isEmpty()) {
				setStartPositionByTheFurthestArgument(methodInvocation);
			} else {
				setStartPositionByTheExpressionOfMethodInvocation(methodInvocation);
			}
		} catch (Exception e) {
			/*
			 * Any exception means we can't find the proper position, so we have
			 * to beware of any interrupted from the beginning of MethodDeclaration
			 */
			beginningPosition = lowerBound;
		}
		return beginningPosition;
	}

	/**
	 *  Find the furthest beginningPosition if the arguments are SimpleName
	 *  @exception RuntimeException if any of them doesn't
	 */
	private void setStartPositionByTheFurthestArgument(
			MethodInvocation methodInvocation) {
		// Start from the nearest position
		beginningPosition = methodInvocation.getStartPosition();

		List<Expression> arguments = methodInvocation.arguments();
		for (Expression eachArgument : arguments) {
			if (eachArgument instanceof SimpleName) {
				updateArgumentsPosition(getVariableDeclarationPosition(eachArgument));
			} else {
				throw new RuntimeException();
			}
		}
	}

	private void updateArgumentsPosition(int newPosition) {
		if (boundaryChecker.isInInterval(newPosition)) {
			if (newPosition < beginningPosition) {
				beginningPosition = newPosition;
			}
		} else {
			throw new RuntimeException();
		}
	}

	/**
	 *  Set beginningPosition if the methodInvocation forms "instance.method()"
	 *  @exception RuntimeException if it doesn't
	 */
	private void setStartPositionByTheExpressionOfMethodInvocation(MethodInvocation methodInvocation) {
		Expression expression = methodInvocation.getExpression();
		if (expression instanceof SimpleName) {
			beginningPosition = getVariableDeclarationPosition(expression);

			/*
			 * If the beginningPosition is Not in this MethodDeclaration, change
			 * it to the beginning of this methodDeclaration instead.
			 */
			if (!boundaryChecker.isInInterval(beginningPosition)) {
				beginningPosition = lowerBound;
			}
		} else {
			throw new RuntimeException();
		}
	}

	private int getVariableDeclarationPosition(Expression expression) {
		ASTNode variableDeclaration = getVariableDeclaration(expression);
		return variableDeclaration.getStartPosition();
	}

	private ASTNode getVariableDeclaration(Expression expression) {
		SimpleName variableName = NodeUtils
				.getSimpleNameFromExpression(expression);
		
		CompilationUnit root = (CompilationUnit) expression.getRoot();
		return root.findDeclaringNode(variableName.resolveBinding());
	}
}
