package ntut.csie.robusta.codegen;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.MethodDeclaration;

public class StatementFinderVisitor extends ASTVisitor {

	private ExpressionStatement expressionStatementHasBeenVisited;
	
	private int startPositionUsedToBeCompared;
	
	private boolean isKeepVisiting;
	
	public StatementFinderVisitor(int startPosition) {
		expressionStatementHasBeenVisited = null;
		isKeepVisiting = true;
		startPositionUsedToBeCompared = startPosition;
	}
	
	public boolean visit(MethodDeclaration methodDeclaration) {
		/*
		 * early return when visit MethodDeclaration statement that can speed up scanning velocity.
		 * this mechanism is only useful for visiting CompilationUnit.
		 * if visit IfStatement, TryStatement and so on, this mechanism is useless.
		 */
		return isKeepVisiting;
	}
	
	public boolean visit(ExpressionStatement node) {
		if(node.getStartPosition() == startPositionUsedToBeCompared) {
			expressionStatementHasBeenVisited = node;
			isKeepVisiting = false;
		}
		return false;
	}
	
	public ExpressionStatement getFoundExpressionStatement() {
		return expressionStatementHasBeenVisited;
	}
}
