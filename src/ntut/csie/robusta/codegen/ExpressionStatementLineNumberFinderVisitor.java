package ntut.csie.robusta.codegen;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.MethodDeclaration;

/**
 * use line number to find ExpressionStatement
 * @author Charles
 *
 */
public class ExpressionStatementLineNumberFinderVisitor extends ASTVisitor {

	private ExpressionStatement expressionStatementHasBeenVisited;
	
	private int lineNumberOfFindingStatement;
	
	private boolean isKeepVisiting;
	
	/** use this CompilationUnit to calculate line number */
	private CompilationUnit compilationUnitOfExpressionStatementHasBeenVisited;
	
	public ExpressionStatementLineNumberFinderVisitor(CompilationUnit compilationUnit, int statementLineNumber) {
		expressionStatementHasBeenVisited = null;
		lineNumberOfFindingStatement = statementLineNumber;
		isKeepVisiting = true;
		compilationUnitOfExpressionStatementHasBeenVisited = compilationUnit;
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
		if(lineNumberOfFindingStatement == compilationUnitOfExpressionStatementHasBeenVisited.getLineNumber(node.getStartPosition())) {
			expressionStatementHasBeenVisited = node;
			isKeepVisiting = false;
		}
		return false;
	}
	
	public ExpressionStatement getExpressionStatement() {
		return expressionStatementHasBeenVisited;
	}
}
