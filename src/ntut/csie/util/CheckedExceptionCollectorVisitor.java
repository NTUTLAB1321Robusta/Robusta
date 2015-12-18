package ntut.csie.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.dom.ThrowStatement;

public class CheckedExceptionCollectorVisitor extends ASTVisitor {
	private final HashSet<ITypeBinding> exceptions = new HashSet<ITypeBinding>();

	// method invocation
	public boolean visit(final MethodInvocation methodInvocation) {
		// get method signature from a node of constructor call
		IMethodBinding mb = methodInvocation.resolveMethodBinding();
		
		if(mb != null) {
			exceptions.addAll(Arrays.asList(mb.getExceptionTypes()));
		}
		
		return true;
	}

	// constructor
	public boolean visit(final ClassInstanceCreation classInstanceCreation) {
		// get method signature from a node of constructor call
		IMethodBinding mb = classInstanceCreation.resolveConstructorBinding();
		
		if(mb != null) {
			exceptions.addAll(Arrays.asList(mb.getExceptionTypes()));
		}
		
		return true;
	}
	
	// super.method()
	public boolean visit(final SuperMethodInvocation superMethodInvocation) {
		// get method signature from a node of constructor call
		IMethodBinding mb = superMethodInvocation.resolveMethodBinding();
		
		if(mb != null) {
			exceptions.addAll(Arrays.asList(mb.getExceptionTypes()));
		}
		
		return true;
	}
	
	// throw statement
	public boolean visit(final ThrowStatement throwStatement) {
		// get method signature from a node of constructor call
		ITypeBinding exceptionType = throwStatement.getExpression().resolveTypeBinding();

		if(exceptionType != null) {
			exceptions.addAll(Arrays.asList(exceptionType));
		}
		
		return false;
	}
	
	public List<ITypeBinding> getException() {
		return Collections.unmodifiableList(new ArrayList<ITypeBinding>(exceptions));
	}
}
