package ntut.csie.csdet.visitor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ntut.csie.jdt.util.NodeUtils;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;

public class CloseResourceMethodInvocationVisitor extends ASTVisitor {
	private List<MethodInvocation> closeMethodInvocations;
	private CompilationUnit root;
	
	public CloseResourceMethodInvocationVisitor(CompilationUnit node) {
		root = node;
		closeMethodInvocations = new ArrayList<MethodInvocation>();
	}

	@Override
	public boolean visit(MethodInvocation node) {
		if(NodeUtils.isCloseResourceMethodInvocation(root, node)) {
			closeMethodInvocations.add(node);
		}
		return true;
	}

	public List<MethodInvocation> getCloseMethodInvocations() {
		return closeMethodInvocations;
	}
	
}
