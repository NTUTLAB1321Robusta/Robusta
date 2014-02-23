package ntut.csie.analyzer.careless;

import java.util.Iterator;
import java.util.List;

import ntut.csie.util.BoundaryChecker;
import ntut.csie.util.NodeUtils;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.Statement;

public class MethodInvocationMayInterruptByExceptionChecker {

	private CompilationUnit root;
	private MethodDeclaration methodDeclaration;
	private int beginningPosition;
	BoundaryChecker boundaryChecker;

	/**
	 * This instance can be used only for this compilation unit
	 */
	public MethodInvocationMayInterruptByExceptionChecker(CompilationUnit root) {
		this.root = root;
	}

	public boolean isMayInterruptByException(MethodInvocation methodInvocation) {
		initialize(methodInvocation);
		
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
	 * Set up the information needed when detecting
	 */
	private void initialize(MethodInvocation methodInvocation) {
		methodDeclaration = (MethodDeclaration) NodeUtils
				.getSpecifiedParentNode(methodInvocation,
						ASTNode.METHOD_DECLARATION);

		boundaryChecker = new BoundaryChecker(
				methodDeclaration.getStartPosition(),
				methodInvocation.getStartPosition());

		setResourceStartPositionForMethodDeclaration(methodInvocation);
	}

	/**
	 * If the resource is not start on this MethodDeclaration, then will set the
	 * start position at the start position of this MethodDeclaration
	 */
	private void setResourceStartPositionForMethodDeclaration(
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
			 * to beware any interrupted from the beginning
			 */
			beginningPosition = methodDeclaration.getStartPosition();
		}
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
	private void setStartPositionByTheExpressionOfMethodInvocation (MethodInvocation methodInvocation) {
		Expression expression = methodInvocation.getExpression();
		if (expression instanceof SimpleName) {
			beginningPosition = getVariableDeclarationPosition(expression);

			/*
			 * If the beginningPosition is Not in this MethodDeclaration, change
			 * it to the beginning of this methodDeclaration instead.
			 */
			if (!boundaryChecker.isInInterval(beginningPosition)) {
				beginningPosition = methodDeclaration.getStartPosition();
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
		return root.findDeclaringNode(variableName.resolveBinding());
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

		/*
		 * If any statement satisfy both "between" and "unsafe", return true.
		 * Otherwise, return false.
		 */
		Iterator<Statement> iter = allStatements.iterator();
		int endingPosition = executedNode.getStartPosition();
		while (iter.hasNext()) {
			Statement statement = iter.next();
			if (isNodeBetweenBeginningAndEnding(statement, endingPosition)
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

	/**
	 * Tell if this node is between the beginningPosition and the given
	 * endingPosition.
	 */
	private boolean isNodeBetweenBeginningAndEnding(ASTNode node,
			int endingPosition) {
		int nodePosition = node.getStartPosition();
		return (beginningPosition <= nodePosition)
				&& (endingPosition > nodePosition);
	}

}
