package ntut.csie.csdet.visitor;

import java.util.List;

import ntut.csie.jdt.util.NodeUtils;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

public class VariableDeclarationVisitor extends ASTVisitor {
	private ASTNode variableDeclaration = null;
	private SimpleName declaringVariable = null;
	private List<Expression> argumentsOfMethodInvocation = null;
	
	public VariableDeclarationVisitor(MethodInvocation methodInvocation) {
		declaringVariable = NodeUtils.getMethodInvocationBindingVariableSimpleName(methodInvocation.getExpression());
		argumentsOfMethodInvocation = methodInvocation.arguments();
	}
	
	public boolean visit(VariableDeclarationFragment node) {
		if(declaringVariable != null) {
			if(node.resolveBinding().equals(declaringVariable.resolveBinding())){
				variableDeclaration = node;
				return false;
			}
		} 
		for(Expression name : argumentsOfMethodInvocation) {
			if(((SimpleName)name).resolveBinding().equals(node.resolveBinding())) {
				variableDeclaration = node;
				return false;
			}
		}
		return false;
	}
	
	public ASTNode getVariableDeclaration() {
		return variableDeclaration;
	}
}
