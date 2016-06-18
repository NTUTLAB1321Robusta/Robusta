package ntut.csie.aspect;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodInvocation;

public class FindAllMethodInvocationVisitor extends ASTVisitor {
	List<MethodInvocation> allInvocation = new ArrayList<MethodInvocation>();

	public FindAllMethodInvocationVisitor() {
	}

	@Override
	public boolean visit(MethodInvocation invocation) {
		allInvocation.add(invocation);
		return false;
	}

	public List<MethodInvocation> getMethodInvocations() {
		if (!allInvocation.isEmpty()) {
			return allInvocation;
		}
		return null;
	}
}
