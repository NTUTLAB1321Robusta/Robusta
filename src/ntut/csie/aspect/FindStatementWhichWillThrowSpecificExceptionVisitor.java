package ntut.csie.aspect;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.IAnnotationBinding;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.VariableDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;

public class FindStatementWhichWillThrowSpecificExceptionVisitor extends
		ASTVisitor {
	private String exceptionType = "";
	private MethodInvocation methodInvocation;

	public FindStatementWhichWillThrowSpecificExceptionVisitor(String exception) {
		exceptionType = exception;
	}

	@Override
	public boolean visit(MethodInvocation invocation) {
		// get method signature from a node of constructor call
		IMethodBinding methodBinding = invocation.resolveMethodBinding();
		List<ITypeBinding> exceptions = new ArrayList<ITypeBinding>();
		if (methodBinding != null) {
			exceptions.addAll(Arrays.asList(methodBinding.getExceptionTypes()));
		}
		for (ITypeBinding exception : exceptions) {
			if (exception.toString().contains(exceptionType)) {
				methodInvocation = invocation;
				return false;
			}
		}
		return true;
	}
	
	public MethodInvocation getMethodInvocationWhichWiThrowException(){
		return methodInvocation;
	}
}
