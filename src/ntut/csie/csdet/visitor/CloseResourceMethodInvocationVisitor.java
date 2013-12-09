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
	private Map<MethodDeclaration, List<MethodInvocation>> closeMethodInvocationMap;
	private List<MethodInvocation> closeMethodInvocationInMethodDeclaration;
	private CompilationUnit root;
	
	public CloseResourceMethodInvocationVisitor(CompilationUnit node) {
		super();
		this.root = node;
		closeMethodInvocationMap = new HashMap<MethodDeclaration, List<MethodInvocation>>();
	}

	@Override
	public boolean visit(MethodDeclaration node) {
		closeMethodInvocationInMethodDeclaration = new ArrayList<MethodInvocation>();
		return true;
	}

	@Override
	public boolean visit(MethodInvocation node) {
		if(NodeUtils.isCloseResourceMethodInvocation(root, node)) {
			closeMethodInvocationInMethodDeclaration.add(node);
		}
		return true;
	}

	@Override
	public void endVisit(MethodDeclaration node) {
		if(closeMethodInvocationInMethodDeclaration.size() > 0) {
			closeMethodInvocationMap.put(node, closeMethodInvocationInMethodDeclaration);
		}
	}
	
	public Map<MethodDeclaration, List<MethodInvocation>> getCloseMethodInvocationMap() {
		return closeMethodInvocationMap;
	}
	
	public List<MethodInvocation> getCloseMethodInvocationList() {
		List<MethodInvocation> closeMethods = new ArrayList<MethodInvocation>();
		for(Map.Entry<MethodDeclaration, List<MethodInvocation>> item : closeMethodInvocationMap.entrySet()) {
			closeMethods.addAll(item.getValue());
		}
		return closeMethods;
	}
	
}
