package ntut.csie.robusta.codegen;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.MethodDeclaration;

/**
 * use code statement to find ExpressionStatement
 * @author charles
 *
 */
public class ExpressionStatementStringFinderVisitor extends ASTVisitor {

	private ExpressionStatement expressionStatementHasBeenVisited;
	
	private String statementWantToBeFound;
	
	private boolean isKeepVisiting;
	
	public ExpressionStatementStringFinderVisitor(String statement) {
		expressionStatementHasBeenVisited = null;
		statementWantToBeFound = statement;
		isKeepVisiting = true;
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
		if(node.toString().contains(statementWantToBeFound)) {
			expressionStatementHasBeenVisited = node;
			isKeepVisiting = false;
		}
		return false;
	}
	
	public ExpressionStatement getFoundExpressionStatement() {
		return expressionStatementHasBeenVisited;
	}
}
