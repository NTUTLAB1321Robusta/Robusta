package ntut.csie.csdet.visitor.aidvisitor;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodInvocation;

public class MethodInvocationMayInterruptByExceptionChecker extends ASTVisitor {

	private CompilationUnit root;
	
	public MethodInvocationMayInterruptByExceptionChecker(CompilationUnit root) {
		this.root = root;
	}
	
	public boolean isInterruptByException(MethodInvocation methodInvocation) {
		ASTNode variableDeclaration = getVariableDeclaration(methodInvocation);
		return true;
	}
	
	private ASTNode getVariableDeclaration(MethodInvocation methodInvocation) {
		// root.findDeclaringNode(node.resolveMethodBinding().getMethodDeclaration())
		return null;
	}
}
