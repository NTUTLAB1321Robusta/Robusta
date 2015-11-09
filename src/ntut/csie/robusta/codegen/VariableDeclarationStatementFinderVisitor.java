package ntut.csie.robusta.codegen;

import ntut.csie.util.NodeUtils;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;

public class VariableDeclarationStatementFinderVisitor extends ASTVisitor {
	private VariableDeclarationStatement variableDeclarationStatementHasBeenVisited;
	
	private MethodInvocation methodInvocationWhichContainsVariableDeclaration;
	
	/** only scan this try statement */
	private TryStatement specifiedSearchingNode; 
	
	public VariableDeclarationStatementFinderVisitor(MethodInvocation methodInvocation) {
		variableDeclarationStatementHasBeenVisited = null;
		methodInvocationWhichContainsVariableDeclaration = methodInvocation;
		specifiedSearchingNode = null;
	}

	public boolean visit(VariableDeclarationFragment node) {
		// we only take the first parameter from method invocation。
		SimpleName methodInvocationFirstVariableSimpleName = (SimpleName)methodInvocationWhichContainsVariableDeclaration.getExpression();

		// if SimpleName is null, it means method invocation is not like "instance.method()". So, we suppose it would be like close(instance).
		if(methodInvocationFirstVariableSimpleName == null) {
			return false;
		}
		
		if (!node.getName().toString().equals(methodInvocationFirstVariableSimpleName.getFullyQualifiedName())) {		
			return true;
		}
		
		ASTNode possibleVariableDeclarationStatement = node.getParent();

		if (possibleVariableDeclarationStatement.getNodeType() != ASTNode.VARIABLE_DECLARATION_STATEMENT) {
			return true;
		}
		
		ASTNode possibleVariableDeclarationStatementMD = NodeUtils.getSpecifiedParentNode(possibleVariableDeclarationStatement, ASTNode.METHOD_DECLARATION);
		ASTNode comparisingMethodInvocationMD = NodeUtils.getSpecifiedParentNode(methodInvocationWhichContainsVariableDeclaration, ASTNode.METHOD_DECLARATION);
		
		if(possibleVariableDeclarationStatementMD.equals(comparisingMethodInvocationMD)) {
			variableDeclarationStatementHasBeenVisited = (VariableDeclarationStatement) possibleVariableDeclarationStatement;
			return false;
		}
		
		return true;
	}
	
	public void setSearchingSpecifiedTryStatement(TryStatement tryStatement) {
		specifiedSearchingNode = tryStatement;
	}
	
	public boolean visit(TryStatement node) {
		if(specifiedSearchingNode == null) {
			return true;
		} else if(specifiedSearchingNode.getStartPosition() == node.getStartPosition()) {
			return true;
		}
		return false;
	}
	
	public VariableDeclarationStatement getFoundVariableDeclarationStatement() {
		return variableDeclarationStatementHasBeenVisited;
	}
}
