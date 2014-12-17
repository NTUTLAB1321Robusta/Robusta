package ntut.csie.analyzer.careless;

import java.util.Iterator;
import java.util.List;

import ntut.csie.util.BoundaryChecker;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;

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
			if (isParentUnsafe(checkingNode)
					|| isThereUnsafeBrother(checkingNode)) {
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
		 * true: some situation will always safe, check it false: this node is a
		 * element of statement, it is always safe
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
				InfixExpression infixExpression = ((InfixExpression) ((IfStatement) parent)
						.getExpression());

				isParentSimpleNonNullChecking = isCheckingSimpleNonNull(infixExpression);
			} catch (ClassCastException e) {
				// This empty catch block is inevitable,
				// the castings also act as if statements
				// It is not a simple non-null checking expression, keep
				// isSimpleNonnullChecking false
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
		if (rightType == ASTNode.NULL_LITERAL
				|| leftType == ASTNode.NULL_LITERAL) {
			if (rightType == ASTNode.SIMPLE_NAME
					|| leftType == ASTNode.SIMPLE_NAME) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Tell if there is any may-thrown-exception between checkingNode and it's
	 * parent.
	 */
	private boolean isThereUnsafeBrother(ASTNode checkingNode) {
		// Set the area of detection for checkingNode
		BoundaryChecker boundChecker = new BoundaryChecker(beginningPosition,
				checkingNode.getStartPosition());

		// Collect all brother statements, and return if there is any
		// may-thrown-statement between
		ASTNode parent = checkingNode.getParent();
		if (parent.getNodeType() == ASTNode.BLOCK) {
			List<Statement> allStatements = ((Block) parent).statements();

			// Return is there any may-thrown-statement between
			Iterator<Statement> iter = allStatements.iterator();
			while (iter.hasNext()) {
				Statement statement = iter.next();
				if (boundChecker.isInOpenInterval(statement.getStartPosition())
						&& isUnsafeBrotherStatement(statement)) {
					return true;
				}
			}
		}

		return false;
	}

	private boolean isVariableDelarcation(Statement statement) {
		// 判斷node是否為變數宣告
		if (statement.getNodeType() == ASTNode.VARIABLE_DECLARATION_STATEMENT) {
			VariableDeclarationStatement variableDeclarationStatement = (VariableDeclarationStatement) statement;
			// 把變數宣告內所有的fragment一一比對
			for (int i = 0; i < variableDeclarationStatement.fragments().size(); i++) {
				VariableDeclarationFragment fragment = (VariableDeclarationFragment) variableDeclarationStatement
						.fragments().get(i);
				// 變數宣告等號的右邊不為null才比對
				if (fragment.getInitializer() == null) {
					// 變數宣告等號的右邊為null，此為一般變數宣告ex int a;
					return true;//node是VariableDelarcation，安全
				}else{// 變數宣告等號的右邊為不null，此為一般變數宣告ex int a=1;
					if ((fragment.getInitializer().getClass() != null) && 
							(fragment.getInitializer().getClass().getName().endsWith("Literal"))) {
						return true;//node是VariableDelarcation，安全
					}
				}
			}
			return false;//node是VariableDelarcationstatement，但不是單純VariableDelare，危險
		}
		//node不是VariableDelarcationstatement
		return false;
	}

	private boolean isVariableAssignment(Statement statement) {
		// 判斷node是否為expression
		if (statement.getNodeType() == ASTNode.EXPRESSION_STATEMENT) {
			ExpressionStatement expressionstatement = (ExpressionStatement) statement;
			Expression expression = expressionstatement.getExpression();
			// 判斷node屬性是否為assignment
			if (expression.getNodeType() == ASTNode.ASSIGNMENT) {
				Assignment assignment = (Assignment) expression;
				// node assignment的結果是否為literal類型
				if (assignment.getRightHandSide() != null) {
					if ((assignment.getRightHandSide().getClass() != null)
							&& assignment.getRightHandSide().getClass()
									.getName().endsWith("Literal")) {
						return true;
					}
				}
			}
			return false;
		}
		return false;
	}

	/**
	 * Return false only if the statements will not throw any exception in 100%.
	 */
	private boolean isUnsafeBrotherStatement(Statement statement) {
		if(isVariableDelarcation(statement)){
			return false;
		}
		if(isVariableAssignment(statement)){
			return false;
		}
		boolean isEmptyStatement = (statement.getNodeType() == ASTNode.EMPTY_STATEMENT);
		boolean isTryBlock = (statement.getParent().getNodeType() == ASTNode.TRY_STATEMENT);
		if (isEmptyStatement || isTryBlock) {
			return false;
		}
		return true;
	}
}
