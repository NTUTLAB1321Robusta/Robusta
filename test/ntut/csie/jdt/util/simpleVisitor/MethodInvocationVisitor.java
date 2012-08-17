package ntut.csie.jdt.util.simpleVisitor;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.MethodInvocation;

public class MethodInvocationVisitor extends ASTVisitor {
	private List<MethodInvocation> methodInvocationList;
	
	public MethodInvocationVisitor(){
		super();
		methodInvocationList = new ArrayList<MethodInvocation>();
	}
	
	public boolean visit(MethodInvocation node) {
		methodInvocationList.add(node);
		return false;
	}
	
	public MethodInvocation getMethodInvocation(int index) {
		return methodInvocationList.get(index);
	}
	
	public int countMethodInvocations() {
		return methodInvocationList.size();
	}
}
