package ntut.csie.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.MethodInvocation;

public class MethodInvocationCollectorVisitor extends ASTVisitor{
	private final List <MethodInvocation> methodInvocations = new ArrayList <MethodInvocation> ();
	private final List <MethodInvocation> firstInv = new ArrayList <MethodInvocation> ();

	 int count = 0;
	  @Override
	  public boolean visit (final MethodInvocation methodInvocation) {
		  if(count==0){
			  firstInv.add(methodInvocation);
		  }
		  count++;
		  methodInvocations.add (methodInvocation);
	    return super.visit (methodInvocation);
	  }

	  public List<MethodInvocation> getMethodInvocations () {
	    return Collections.unmodifiableList (methodInvocations);
	  }
	  
	  public List<MethodInvocation> getFirstInvocations () {
	    return Collections.unmodifiableList (firstInv);
	  }
	  
	  public void resetFirstInv(){
		  count = 0;
	  }
}
