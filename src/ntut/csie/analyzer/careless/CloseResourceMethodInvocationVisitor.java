package ntut.csie.analyzer.careless;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.List;

import ntut.csie.analyzer.UserDefinedMethodAnalyzer;
import ntut.csie.csdet.preference.SmellSettings;
import ntut.csie.util.NodeUtils;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;

public class CloseResourceMethodInvocationVisitor extends ASTVisitor {
	private List<MethodInvocation> closeMethodInvocations;
	private CompilationUnit root;
	
	public CloseResourceMethodInvocationVisitor(CompilationUnit node) {
		root = node;
		closeMethodInvocations = new ArrayList<MethodInvocation>();
	}

	@Override
	public boolean visit(MethodInvocation node) {
		if(isCloseResourceMethodInvocation(root, node)) {
			closeMethodInvocations.add(node);
		}
		return true;
	}

	public List<MethodInvocation> getCloseMethodInvocations() {
		return closeMethodInvocations;
	}

	/**
	 * Check whether the method invocation is a close invocation or not
	 * @param root the java file of the method invocation
	 */
	private static boolean isCloseResourceMethodInvocation(CompilationUnit root,
			MethodInvocation node) {
		boolean userDefinedLibResult = false;
		boolean userDefinedResult = false;
		boolean userDefinedExtraRule = false;
		boolean defaultResult = false;

		UserDefinedMethodAnalyzer userDefinedMethodAnalyzer = new UserDefinedMethodAnalyzer(
				SmellSettings.SMELL_CARELESSCLEANUP);
		if (userDefinedMethodAnalyzer.analyzeLibrary(node)) {
			userDefinedLibResult = true;
		}

		if (userDefinedMethodAnalyzer.analyzeMethods(node)) {
			userDefinedResult = true;
		}

		if (userDefinedMethodAnalyzer.analyzeExtraRule(node, root)) {
			userDefinedExtraRule = true;
		}

		if (userDefinedMethodAnalyzer.getEnable()) {
			defaultResult = isNodeACloseCodeAndImplementedCloseable(node);
		}

		return (userDefinedLibResult || userDefinedResult
				|| userDefinedExtraRule || defaultResult);
	}

	/**
	 * If this node implemented Closeable and named "close", return true.
	 * Otherwise, return false.
	 */
	private static boolean isNodeACloseCodeAndImplementedCloseable(
			MethodInvocation node) {
		return isSimpleNameClose(node.getName())
				&& isIMethodBindingImplementedCloseable(node
						.resolveMethodBinding());
	}

	/**
	 * If this node implemented Closeable and named "close", return true.
	 * Otherwise, return false.
	 * // TODO Haven't be implement
	 */
	private static boolean isNodeACloseCodeAndImplementedCloseable(
			SuperMethodInvocation node) {
		return isSimpleNameClose(node.getName())
				&& isIMethodBindingImplementedCloseable(node
						.resolveMethodBinding());
	}

	private static boolean isSimpleNameClose(SimpleName name) {
		return name.toString().equals("close");
	}

	private static boolean isIMethodBindingImplementedCloseable(
			IMethodBinding methodBinding) {
		return NodeUtils.isITypeBindingImplemented(
				methodBinding.getDeclaringClass(), Closeable.class);
	}
}
