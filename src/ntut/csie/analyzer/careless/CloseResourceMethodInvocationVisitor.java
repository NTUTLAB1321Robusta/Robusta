package ntut.csie.analyzer.careless;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.List;

import ntut.csie.analyzer.UserDefinedMethodAnalyzer;
import ntut.csie.csdet.preference.SmellSettings;
import ntut.csie.util.NodeUtils;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;

public class CloseResourceMethodInvocationVisitor extends ASTVisitor {
	private List<MethodInvocation> closeMethodInvocations;
	private CompilationUnit root;
	protected static UserDefinedMethodAnalyzer userDefinedMethodAnalyzer;
	
	public CloseResourceMethodInvocationVisitor(CompilationUnit node) {
		root = node;
		closeMethodInvocations = new ArrayList<MethodInvocation>();
		userDefinedMethodAnalyzer = new UserDefinedMethodAnalyzer(
				SmellSettings.SMELL_CARELESSCLEANUP);
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
	 * Checks the method invocation whether if it's a close invocation or not
	 * @param root 
	 * 			the java file of the method invocation
	 */
	private static boolean isCloseResourceMethodInvocation(CompilationUnit root,
			MethodInvocation node) {
		boolean userDefinedLibResult = false;
		boolean userDefinedResult = false;
		boolean userDefinedExtraRule = false;
		boolean defaultResult = false;

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
			defaultResult = isNodeACloseCodeAndImplementedCloseable(node) && !isExpressionOfCloseCodeAMethodInvocation(node);
		}

		return (userDefinedLibResult || userDefinedResult
				|| userDefinedExtraRule || defaultResult);
	}

	private static boolean isExpressionOfCloseCodeAMethodInvocation(MethodInvocation node) {
		if(node.getExpression().getNodeType() == ASTNode.METHOD_INVOCATION){
			return true;
		}
		return false;
	}

	/**
	 * If this node implemented Closeable and named "close", return true.
	 * Otherwise, return false.
	 */
	private static boolean isNodeACloseCodeAndImplementedCloseable(MethodInvocation node) {
	
		if(node.resolveMethodBinding() == null){
			return false;
		}
		
		return isSimpleNameClose(node.getName())
				&& isIMethodBindingImplementedCloseable(node
						.resolveMethodBinding());
		/*取得close invocation之後，把invocation的expression記錄下來，用來當作往回比對掃終點的依據*/
	}

	/**
	 * If this node implemented Closeable and named "close", return true.
	 * Otherwise, return false.
	 * // TODO Hasn't been implemented
	 */
	private static boolean isNodeACloseCodeAndImplementedCloseable(SuperMethodInvocation node) {
		return isSimpleNameClose(node.getName()) && isIMethodBindingImplementedCloseable(node.resolveMethodBinding());
	}

	private static boolean isSimpleNameClose(SimpleName name) {
		return name.toString().equals("close");
	}

	private static boolean isIMethodBindingImplementedCloseable(
			IMethodBinding methodBinding) {
		
		return NodeUtils.isITypeBindingImplemented(
				methodBinding.getDeclaringClass(), Closeable.class) || NodeUtils.isITypeBindingImplemented(
						methodBinding.getDeclaringClass(), AutoCloseable.class);
	}
}
