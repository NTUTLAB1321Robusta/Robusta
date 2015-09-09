package ntut.csie.analyzer.careless;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


import ntut.csie.util.BoundaryChecker;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.ParenthesizedExpression;
import org.eclipse.jdt.core.dom.PrefixExpression;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.SynchronizedStatement;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;

/**
 * It will check if any exception may been thrown before
 */
public class CloseInvocationExecutionChecker {

	private int startPosition;
	private List<ASTNode> nodesThatMayThrowException = new ArrayList<ASTNode>();

	public List<ASTNode> getASTNodesThatMayThrowExceptionBeforeCloseInvocation(MethodInvocation closeInvocation) {
		startPosition = findStartPosition(closeInvocation);
		ASTNode checkingNode = closeInvocation;
		
		while (startPosition < checkingNode.getStartPosition()) {
			ASTNode unsafeParentNode = getParentNodeThatMayThrowException(checkingNode);
			if(unsafeParentNode != null)
				nodesThatMayThrowException.add(unsafeParentNode);
			
			List<ASTNode> unsafeSiblingNodes = getSiblingNodeThatMayThrowException(checkingNode);
			if(unsafeSiblingNodes.size() != 0)
				nodesThatMayThrowException.addAll(unsafeSiblingNodes);
			
			checkingNode = checkingNode.getParent();
		}
		return nodesThatMayThrowException;
	}

	private List<ASTNode> getSiblingNodeThatMayThrowException(ASTNode checkingNode) {
		List<ASTNode> unsafeSiblingNodeList = new ArrayList<ASTNode>();
		
		// Set the detection area for checkingNode
		BoundaryChecker boundChecker = new BoundaryChecker(startPosition, checkingNode.getStartPosition());

		ASTNode parentNode = checkingNode.getParent();
		if (parentNode.getNodeType() == ASTNode.BLOCK) {
			List<Statement> siblingStatements = ((Block) parentNode).statements();
			
			for(Statement s : siblingStatements) {
				if(boundChecker.isInOpenInterval(s.getStartPosition()) && isUnsafeSiblingStatement(s))
					unsafeSiblingNodeList.add(s);
			}
		}

		return unsafeSiblingNodeList;
	}


	private ASTNode getParentNodeThatMayThrowException(ASTNode checkingNode) {
		if (checkingNode instanceof Statement) {
			ASTNode parent = checkingNode.getParent();
			int parentType = parent.getNodeType();
			boolean isParentBlock = (parentType == ASTNode.BLOCK);
			boolean isParentFinallBlockOrCatchClause = (parentType == ASTNode.TRY_STATEMENT);
			boolean isParentCatchBlock = (parentType == ASTNode.CATCH_CLAUSE);
			boolean isParentSafeIfSatement = isSafeIfStaementExpression(parent);
			boolean isParentSafeSynchronizedStatement = isSynchronizedStatement(parent);

			if(!(isParentBlock || isParentFinallBlockOrCatchClause
					|| isParentCatchBlock || isParentSafeIfSatement || isParentSafeSynchronizedStatement))
				return parent;
		}
		return null;
	}


	private int findStartPosition(MethodInvocation methodInvocation) {
		return new ClosingResourceBeginningPositionFinder().findPosition(methodInvocation);
	}

	private boolean isExtendOperandElementSafe(
			List<Boolean> checkExtendOperandSafe) {
		Iterator<Boolean> iter = checkExtendOperandSafe.iterator();
		while (iter.hasNext()) {
			boolean statementsituation = iter.next();
			if (!statementsituation) {
				return false;
			}
		}
		return true;
	}

	private boolean isSafeIfStaementExpression(ASTNode parent) {
		IfStatement ifStatement = null;
		Expression expression = null;
		if (parent.getNodeType() == ASTNode.IF_STATEMENT) {
			ifStatement = ((IfStatement) parent);
			expression = ifStatement.getExpression();
		}
		if (expression == null) {
			return false;
		}
		if (expression.getNodeType() == ASTNode.SIMPLE_NAME) {
			return true;
		}
		if (expression.getNodeType() == ASTNode.QUALIFIED_NAME) {
			return true;
		}
		if (expression.getNodeType() == ASTNode.PREFIX_EXPRESSION) {
			return isSafePrefixExpressionInIfstatement(ifStatement
					.getExpression());
		}
		if (expression.getNodeType() == ASTNode.INFIX_EXPRESSION) {
			return isSafeInfixExpressionInIfstatement(expression);
		}
		return false;
	}

	private boolean isSynchronizedStatement(ASTNode parent) {
		if (parent.getNodeType() == ASTNode.SYNCHRONIZED_STATEMENT) {
			SynchronizedStatement synchronizedStatement = ((SynchronizedStatement) parent);
			Expression expression = synchronizedStatement.getExpression();
			return expression.getNodeType() == ASTNode.SIMPLE_NAME;
		}
		return false;
	}

	private boolean isSafeOperand(ASTNode operand) {
		if (operand.getNodeType() == ASTNode.INFIX_EXPRESSION) {
			return isSafeInInFixExpressionInOperand(operand);
		}
		if (operand.getNodeType() == ASTNode.PREFIX_EXPRESSION) {
			return isSafePrefixExpressionInIfstatement(operand);
		}
		if (operand.getNodeType() == ASTNode.SIMPLE_NAME) {
			return true;
		}
		if (operand.getClass().getName().endsWith("Literal")) {
			return true;
		}
		if (operand.getNodeType() == ASTNode.QUALIFIED_NAME) {
			return true;
		}
		if (operand.getNodeType() == ASTNode.PARENTHESIZED_EXPRESSION) {
			ParenthesizedExpression parenthesizedExpression = (ParenthesizedExpression) operand;
			Expression expressionOfParenthesizedExpression = parenthesizedExpression
					.getExpression();
			return isExpressionSafe(expressionOfParenthesizedExpression);
		}
		return false;
	}

	private boolean isSafeInInFixExpressionInOperand(ASTNode parent) {
		InfixExpression infix = (InfixExpression) parent;
		ASTNode rightOperand = infix.getRightOperand();
		ASTNode leftOperand = infix.getLeftOperand();
		return (isSafeOperand(rightOperand) && isSafeOperand(leftOperand));
	}

	private boolean isSafeInfixExpressionInIfstatement(Expression expression) {
		InfixExpression infix = (InfixExpression) expression;
		ASTNode rightOperand = infix.getRightOperand();
		ASTNode leftOperand = infix.getLeftOperand();
		List<ASTNode> extendOperand = infix.extendedOperands();
		List<Boolean> checkExtendOperandSafe = checkExtendOperandInInFixStatement(extendOperand);
		return (isSafeOperand(rightOperand) && isSafeOperand(leftOperand) && isExtendOperandElementSafe(checkExtendOperandSafe));
	}

	private boolean isSafePrefixExpressionInIfstatement(ASTNode expression) {
		PrefixExpression prefix = (PrefixExpression) expression;
		ASTNode operand = prefix.getOperand();
		return isSafeOperand(operand);
	}

	private List<Boolean> checkExtendOperandInInFixStatement(
			List<ASTNode> extendOperand) {
		List<Boolean> checkExtendOperandSafe = new ArrayList<Boolean>();
		Iterator<ASTNode> iter = extendOperand.iterator();
		while (iter.hasNext()) {
			ASTNode ExtendOperandElement = iter.next();
			checkExtendOperandSafe.add(isSafeOperand(ExtendOperandElement));
		}
		return checkExtendOperandSafe;
	}

	private boolean isSafeVariableDelarcation(Statement statement) {
		if (statement.getNodeType() == ASTNode.VARIABLE_DECLARATION_STATEMENT) {
			VariableDeclarationStatement variableDeclarationStatement = (VariableDeclarationStatement) statement;
			List<VariableDeclarationFragment> allStatements = variableDeclarationStatement
					.fragments();
			for (VariableDeclarationFragment fragment : allStatements) {
				if (fragment.getInitializer() == null) {
					return true;
				}
				if (fragment.getInitializer().getClass().getName()
						.endsWith("Literal")) {
					return true;
				}
			}
		}
		return false;
	}

	private boolean isLiteralReturnStatement(Statement statement) {
		if (statement.getNodeType() == ASTNode.RETURN_STATEMENT) {
			ReturnStatement returnStatememt = (ReturnStatement) statement;
			if (returnStatememt.getExpression() == null) {
				return true;
			}
			if (returnStatememt.getExpression().getClass().getName()
					.endsWith("Literal")) {
				return true;
			}
		}
		return false;
	}

	private boolean isSafeInStatement(List<Boolean> checkChildStatementSafe) {
		Iterator<Boolean> iter = checkChildStatementSafe.iterator();
		while (iter.hasNext()) {
			boolean statementsituation = iter.next();
			if (statementsituation) {
				return false;
			}
		}
		return true;
	}

	private List<Boolean> checkStatementSafe(ASTNode ifBodyStatement) {
		List<Boolean> checkChildStatementSafe = new ArrayList<Boolean>();
		if (ifBodyStatement.getNodeType() == ASTNode.BLOCK) {
			List<Statement> allStatements = ((Block) ifBodyStatement)
					.statements();
			Iterator<Statement> iter = allStatements.iterator();
			while (iter.hasNext()) {
				Statement statementInIfBody = iter.next();
				checkChildStatementSafe
						.add(isUnsafeSiblingStatement(statementInIfBody));
			}
		}
		return checkChildStatementSafe;
	}

	private boolean isSafeThenStatement(IfStatement statement) {
		ASTNode thenStatement = statement.getThenStatement();
		List<Boolean> checkStatementSafe = checkStatementSafe(thenStatement);
		if (isSafeInStatement(checkStatementSafe)) {
			return true;
		}
		return false;
	}

	private boolean isSafeElseStatement(IfStatement statement) {
		ASTNode elseStatement = statement.getElseStatement();
		if (elseStatement != null) {
			List<Boolean> checkStatementSafe = checkStatementSafe(elseStatement);
			if (isSafeInStatement(checkStatementSafe)) {
				return true;
			} else {
				return false;
			}
		}
		// if statement 不一定都有 else
		return true;
	}

	private boolean isSafeElseIfStatement(IfStatement statement) {
		ASTNode elseStatement = statement.getElseStatement();
		if (elseStatement != null) {
			if (elseStatement.getNodeType() == ASTNode.IF_STATEMENT) {
				IfStatement elseIfstatement = (IfStatement) elseStatement;
				if (isSafeThenStatement(elseIfstatement)
						&& isSafeElseStatement(elseIfstatement)) {
					return true;
				} else {
					return false;
				}
			}
		}
		// if statement 不一定都有 else if
		return true;
	}

	private boolean isSafeIfStatement(Statement statement) {
		ASTNode sibilingStatement = (ASTNode) statement;
		if (isSafeIfStaementExpression(sibilingStatement)) {
			IfStatement ifstatement = (IfStatement) sibilingStatement;
			if (isSafeThenStatement(ifstatement)
					&& isSafeElseStatement(ifstatement)
					&& isSafeElseIfStatement(ifstatement)) {
				return true;
			}
		}
		return false;
	}

	private boolean isSafeTryCatchStatement(Statement statement) {
		ASTNode sibilingStatement = (ASTNode) statement;
		if (sibilingStatement.getNodeType() == ASTNode.TRY_STATEMENT) {
			TryStatement tryStatement = (TryStatement) sibilingStatement;
			List<CatchClause> catchClauseList = tryStatement.catchClauses();
			for (CatchClause catchClause : catchClauseList) {
				if (catchClause.getException().getType().toString().equals("Exception")
						|| catchClause.getException().getType().toString().equals("Throwable")) {
					List<Statement> StatementList = catchClause.getBody().statements();
					boolean allInBlockStatementSafe = true;
					if (StatementList.isEmpty()) {
						return true;
					}
					for (Statement suspectStatement : StatementList) {
						if(isUnsafeSiblingStatement(suspectStatement)){
							allInBlockStatementSafe = false;
						}
					}
					return allInBlockStatementSafe;
				}
			}
		}
		return false;
	}

	private boolean isSafeExpressionStatement(Statement statement) {
		ASTNode sibilingStatement = (ASTNode) statement;
		if (sibilingStatement.getNodeType() == ASTNode.EXPRESSION_STATEMENT) {
			ExpressionStatement ExpressionExpression = (ExpressionStatement) sibilingStatement;
			Expression expression = ExpressionExpression.getExpression();
			if (expression.getNodeType() == ASTNode.ASSIGNMENT) {
				Assignment assignment = (Assignment) expression;
				return isExpressionSafe(assignment.getLeftHandSide())
						&& isExpressionSafe(assignment.getRightHandSide());
			}
			if (expression.getNodeType() == ASTNode.POSTFIX_EXPRESSION) {
				return true;
			}
			if (expression.getNodeType() == ASTNode.PREFIX_EXPRESSION) {
				return true;
			}
		}
		return false;
	}

	private boolean isExpressionSafe(Expression expression) {
		if (expression.getNodeType() == ASTNode.SIMPLE_NAME) {
			return true;
		}
		if (expression.getNodeType() == ASTNode.QUALIFIED_NAME) {
			return true;
		}
		if (expression.getClass().getName().endsWith("Literal")) {
			return true;
		}
		if (expression.getNodeType() == ASTNode.ASSIGNMENT) {
			Assignment assignment = (Assignment) expression;
			return isExpressionSafe(assignment.getLeftHandSide())
					&& isExpressionSafe(assignment.getRightHandSide());
		}
		if (expression.getNodeType() == ASTNode.INFIX_EXPRESSION) {
			return isSafeInfixExpressionInIfstatement(expression);
		}
		if (expression.getNodeType() == ASTNode.POSTFIX_EXPRESSION) {
			return true;
		}
		if (expression.getNodeType() == ASTNode.PREFIX_EXPRESSION) {
			return true;
		}
		if (expression.getNodeType() == ASTNode.PARENTHESIZED_EXPRESSION) {
			ParenthesizedExpression parenthesizedExpression = (ParenthesizedExpression) expression;
			Expression expressionOfParenthesizedExpression = parenthesizedExpression
					.getExpression();
			return isExpressionSafe(expressionOfParenthesizedExpression);
		}
		return false;
	}

	/**
	 * Return false only if the statements will not throw any exception in 100%.
	 */
	private boolean isUnsafeSiblingStatement(Statement statement) {
		if (statement.getNodeType() == ASTNode.EMPTY_STATEMENT) {
			return false;
		}
		if (statement.getParent().getNodeType() == ASTNode.TRY_STATEMENT) {
			return false;
		}
		if (isSafeVariableDelarcation(statement)) {
			return false;
		}
		if (isLiteralReturnStatement(statement)) {
			return false;
		}
		if (isSafeExpressionStatement(statement)) {
			return false;
		}
		if (isSafeIfStatement(statement)) {
			return false;
		}
		if (isSafeTryCatchStatement(statement)) {
			return false;
		}
		return true;
	}
}
