package ntut.csie.robusta.codegen.refactoring;

import ntut.csie.util.NodeUtils;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;

/**
 * For throw exception in finally block refactoring
 */
public class ExtractMethodAnalyzer {
	private ASTNode target;

	public ExtractMethodAnalyzer(ASTNode target) {
		super();
		this.target = target;
	}
	
	/*
	 * This method will return the enclosing node need to move to new method
	 * It should be MethodInvocation, SupperMethodInvocation or ClassInstanceCreation
	 * The throw statement is not included in refactoring
	 */
	public ASTNode getEnclosingNode() {
		switch (target.getNodeType()) {
		/*
		 * MethodInvocation will begin with a simple name: like "fos".close(); or "close"(xxxx);
		 * So, get the methodinvocation node
		 */
		case ASTNode.SIMPLE_NAME:
			return NodeUtils.getSpecifiedParentNode(target, ASTNode.METHOD_INVOCATION);
			
		case ASTNode.SUPER_METHOD_INVOCATION:
		case ASTNode.METHOD_INVOCATION:
			return target;
		default:
			return null;
		}
	}
	
	/*
	 * Get the return type to create new method
	 */
	public ITypeBinding getReturnType() {
		ASTNode node = getEnclosingNode();
		switch (node.getNodeType()) {
		case ASTNode.METHOD_INVOCATION:
			MethodInvocation mi = (MethodInvocation)node;
			return mi.resolveMethodBinding().getReturnType();
		case ASTNode.SUPER_METHOD_INVOCATION:
			SuperMethodInvocation smi = (SuperMethodInvocation)node;
			return smi.resolveMethodBinding().getReturnType();
		default:
			return null;
		}
	}
	
	/*
	 * Get Exception list
	 */
	public ITypeBinding[] getDeclaredExceptions() {
		ASTNode node = getEnclosingNode();
		switch (node.getNodeType()) {
		case ASTNode.METHOD_INVOCATION:
			MethodInvocation mi = (MethodInvocation)node;
			return NodeUtils.getDeclaredExceptions(mi);
		case ASTNode.SUPER_METHOD_INVOCATION:
			SuperMethodInvocation smi = (SuperMethodInvocation)node;
			return NodeUtils.getDeclaredExceptions(smi);
		default:
			return null;
		}
	}
}
