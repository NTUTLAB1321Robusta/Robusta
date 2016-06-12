package ntut.csie.aspect;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.MethodInvocation;

public class FindAllMethodInvocationVisitor extends ASTVisitor {
	String exceptionType;
	List<MethodInvocation> allInvocation = new ArrayList<MethodInvocation>();

	public FindAllMethodInvocationVisitor(String exception) {
		exceptionType = exception;
	}

	@Override
	public boolean visit(MethodInvocation invocation) {
		allInvocation.add(invocation);
		return false;
	}

	public MethodInvocation getTheFirstInvocatingMethodInvocation() {
		if (!allInvocation.isEmpty()) {
			return allInvocation.get(0);
		}
		return null;
	}
}
