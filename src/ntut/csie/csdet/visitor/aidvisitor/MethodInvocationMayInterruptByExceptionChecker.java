package ntut.csie.csdet.visitor.aidvisitor;

import java.util.Iterator;
import java.util.List;

import ntut.csie.csdet.visitor.FirstLevelChildStatementCollectVisitor;
import ntut.csie.jdt.util.NodeUtils;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.Statement;

public class MethodInvocationMayInterruptByExceptionChecker {

	private CompilationUnit root;
	private MethodDeclaration methodDeclaration;
	private int beginningPosition;

	/**
	 * This instance can be used only for this compilation unit
	 */
	public MethodInvocationMayInterruptByExceptionChecker(CompilationUnit root) {
		this.root = root;
	}

	public boolean isMayInterruptByException(MethodInvocation methodInvocation) {
		initialize(methodInvocation);
		
		if (isThisMethodInvocationUnsafeOnParent(methodInvocation)) {
			return true;
		}
		
		ASTNode parentNode = methodInvocation.getParent();
		while(parentNode.getStartPosition() <= beginningPosition) {
			if(isParentUnsafeOnParent(parentNode)) {
				return true;
			}
			parentNode = parentNode.getParent();
		}
		return false;
	}

	/**
	 * Set up the information needed when detecting
	 */
	private void initialize(MethodInvocation methodInvocation) {
		methodDeclaration = (MethodDeclaration) NodeUtils
				.getSpecifiedParentNode(methodInvocation,ASTNode.METHOD_DECLARATION);
		
		beginningPosition = getVariableStartPositionForMethodDeclaration(methodInvocation);
	}

	/**
	 * If the variable is not start on this MethodDeclaration, then will
	 * return the start position of this MethodDeclaration
	 */
	private int getVariableStartPositionForMethodDeclaration(
			MethodInvocation methodInvocation) {
		ASTNode variableDeclaration = getVariableDeclaration(methodInvocation);
		int declarationPosition = variableDeclaration.getStartPosition();
		
		/*
		 * If the variable is not start on this MethodDeclaration, return the
		 * position of methodDeclaration instead.
		 */
		if (declarationPosition < methodDeclaration.getStartPosition()
				|| declarationPosition > methodInvocation.getStartPosition()) {
			return methodDeclaration.getStartPosition();
		} else {
			return variableDeclaration.getStartPosition();
		}
	}
	
	private ASTNode getVariableDeclaration(MethodInvocation methodInvocation) {
		SimpleName variableName = NodeUtils
				.getMethodInvocationBindingVariableSimpleName(methodInvocation
						.getExpression());
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
	 * Tell if it is unsafe from it's parent's view only
	 * (Won't check if it's parent is safe or not)
	 */
	private boolean isParentUnsafeOnParent(ASTNode node) {
		if (node instanceof Statement && isUnsafeStatement((Statement) node)) {
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
		FirstLevelChildStatementCollectVisitor firstLevelChildCollector = new FirstLevelChildStatementCollectVisitor(
				parentNode);
		parentNode.accept(firstLevelChildCollector);
		List<Statement> allStatements = firstLevelChildCollector.getChildrens();

		/*
		 * If any statement satisfy both "unsafe" and "between", return true.
		 * Otherwise, return false.
		 */
		Iterator<Statement> iter = allStatements.iterator();
		int endingPosition = executedNode.getStartPosition();
		while (iter.hasNext()) {
			Statement statement = iter.next();
			if (isUnsafeStatement(statement)
					&& isNodeBetweenBeginningAndEnding(statement, endingPosition)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * For the statements that won't make the statements behind unexecute.
	 */
	private boolean isUnsafeStatement(Statement Statement) {
		int nodeType = Statement.getNodeType();
		if (nodeType == ASTNode.EMPTY_STATEMENT || nodeType == ASTNode.BLOCK
				|| isTryBlock(Statement)) {
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
		return (nodePosition < beginningPosition)
				&& (nodePosition > endingPosition);
	}

}
