package ntut.csie.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.MethodInvocation;

public class MethodInvocationCollectorVisitor extends ASTVisitor{
	private final List <MethodInvocation> methodInvocations = new ArrayList <MethodInvocation> ();

	  @Override
	  public boolean visit (final MethodInvocation methodInvocation) {
		  methodInvocations.add (methodInvocation);
	    return super.visit (methodInvocation);
	  }

	  public List<MethodInvocation> getMethodInvocations () {
	    return Collections.unmodifiableList (methodInvocations);
	  }
}
