package ntut.csie.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodInvocation;

public class CheckedExceptionCollectorVisitor extends ASTVisitor {
	private final ArrayList<ITypeBinding> exceptions = new ArrayList<ITypeBinding>();

	// method invocation
	@Override
	public boolean visit(final MethodInvocation methodInvocation) {
		// get method signature from a node of constructor call
		IMethodBinding mb = methodInvocation.resolveMethodBinding();
		
		if(mb != null) {
			for(ITypeBinding itb : mb.getExceptionTypes()) {
				// if any exception listed on the signature then add to the list
				exceptions.add(itb);
			}
		}
		
		return super.visit(methodInvocation);
	}

	// constructor
	@Override
	public boolean visit(final ClassInstanceCreation classInstanceCreation) {
		// get method signature from a node of constructor call
		IMethodBinding mb = classInstanceCreation.resolveConstructorBinding();
		
		if(mb != null) {
			for(ITypeBinding itb : mb.getExceptionTypes()) {
				// if any exception listed on the signature then add to the list
				exceptions.add(itb);
			}
		}
		
		return super.visit(classInstanceCreation);
	}
	
	// what else should we visit??

	public List<ITypeBinding> getException() {
		return Collections.unmodifiableList(exceptions);
	}
}
