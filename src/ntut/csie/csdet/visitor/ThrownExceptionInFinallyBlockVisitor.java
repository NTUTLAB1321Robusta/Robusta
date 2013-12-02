package ntut.csie.csdet.visitor;

import java.util.ArrayList;
import java.util.List;

import ntut.csie.csdet.data.MarkerInfo;
import ntut.csie.csdet.visitor.aidvisitor.ThrownExceptionBeCaughtDetector;
import ntut.csie.jdt.util.NodeUtils;
import ntut.csie.rleht.builder.RLMarkerAttribute;

import org.apache.commons.lang.NullArgumentException;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.dom.ThrowStatement;
import org.eclipse.jdt.core.dom.TryStatement;

public class ThrownExceptionInFinallyBlockVisitor extends ASTVisitor {
	private CompilationUnit root; // For getting bad smell info
	private List<MarkerInfo> thrownInFinallyList;
	private int finallyStack = 0; // To check if in any finally block
	private Block outermostFinallyBlock = null;

	public ThrownExceptionInFinallyBlockVisitor(CompilationUnit compilationUnit) {
		super();
		thrownInFinallyList = new ArrayList<MarkerInfo>();
		root = compilationUnit;
	}

	/**
	 * For finally block, to plus the count of finally in stack Record the outer
	 * finally block
	 * 
	 * @author pig
	 */
	public boolean visit(Block node) {
		if (isFinallyBlock(node)) {
			++finallyStack;
			if (finallyStack == 1) {
				outermostFinallyBlock = node;
			}
		}
		return true;
	}

	/**
	 * For finally block, to minus the count of finally in stack
	 * 
	 * @author pig
	 */
	public void endVisit(Block node) {
		if (isFinallyBlock(node)) {
			--finallyStack;
			if (finallyStack == 0) {
				outermostFinallyBlock = null;
			}
		}
	}

	/**
	 * For each exception be thrown in finally, mark it as a bad smell
	 */
	public boolean visit(MethodInvocation node) {
		if (isInAnyFinallyBlock()) {
			ThrownExceptionBeCaughtDetector detector = new ThrownExceptionBeCaughtDetector(
					outermostFinallyBlock);
			if (detector.isAnyDeclaredExceptionBeenThrowOut(node)) {
				addMarkerInfo(node);
			}
		}
		return true;
	}

	/**
	 * For each exception be thrown in finally, mark it as a bad smell
	 */
	public boolean visit(SuperMethodInvocation node) {
		if (isInAnyFinallyBlock()) {
			ThrownExceptionBeCaughtDetector detector = new ThrownExceptionBeCaughtDetector(
					outermostFinallyBlock);
			if (detector.isAnyDeclaredExceptionBeenThrowOut(node)) {
				addMarkerInfo(node);
			}
		}
		return true;
	}

	/**
	 * For the exception be thrown in finally, mark it as a bad smell
	 */
	public boolean visit(ThrowStatement node) {
		if (isInAnyFinallyBlock()) {
			ThrownExceptionBeCaughtDetector detector = new ThrownExceptionBeCaughtDetector(
					outermostFinallyBlock);
			if (detector.isAnyDeclaredExceptionBeenThrowOut(node)) {
				addMarkerInfo(node);
			}
		}
		// Otherwise, "throw new Exception()" will be detected twice
		return false;
	}

	/**
	 * For the exception be thrown in finally, mark it as a bad smell
	 */
	public boolean visit(ClassInstanceCreation node) {
		if (isInAnyFinallyBlock()) {
			ThrownExceptionBeCaughtDetector detector = new ThrownExceptionBeCaughtDetector(
					outermostFinallyBlock);
			if (detector.isAnyDeclaredExceptionBeenThrowOut(node)) {
				addMarkerInfo(node);
			}
		}
		return true;
	}

	/**
	 * Check if the node input is a finally block
	 * @author pig
	 */
	private boolean isFinallyBlock(Block node) {
		Block finallyBlockOfParent = getFinallyBlockOfFirstParentOfTryStatement(node);
		if (finallyBlockOfParent == null || node == null) {
			return false;
		}
		return NodeUtils.isTwoASTNodeAreTheSame(finallyBlockOfParent, node);
	}

	/**
	 * @return null - if there is no any parent which type is try statement, or
	 *         there is no finally block on that try statement
	 * @author pig
	 */
	private Block getFinallyBlockOfFirstParentOfTryStatement(Block node)
			throws NullPointerException {
		TryStatement trySatatmentParentNode = (TryStatement) NodeUtils
				.getSpecifiedParentNode(node, ASTNode.TRY_STATEMENT);

		if (trySatatmentParentNode == null) {
			return null;
		}
		return trySatatmentParentNode.getFinally();
	}

	private boolean isInAnyFinallyBlock() {
		return finallyStack > 0;
	}

	private void addMarkerInfo(MethodInvocation node) {
		ITypeBinding typeBinding = NodeUtils.getExpressionBinding(node);
		MarkerInfo markerInfo = createThrownInFinallyMarkerInfo(node,
				typeBinding);
		markerInfo.setExceptionsMethodThrown(node.resolveMethodBinding()
				.getExceptionTypes());

		thrownInFinallyList.add(markerInfo);
	}

	private void addMarkerInfo(SuperMethodInvocation node) {
		ITypeBinding typeBinding = null;
		MarkerInfo markerInfo = createThrownInFinallyMarkerInfo(node,
				typeBinding);
		markerInfo.setExceptionsMethodThrown(node.resolveMethodBinding()
				.getExceptionTypes());

		thrownInFinallyList.add(markerInfo);
	}

	private void addMarkerInfo(ThrowStatement node) {
		ITypeBinding typeBinding = NodeUtils.getExpressionBinding(node);
		MarkerInfo markerInfo = createThrownInFinallyMarkerInfo(node,
				typeBinding);

		thrownInFinallyList.add(markerInfo);
	}

	private void addMarkerInfo(ClassInstanceCreation node) {
		ITypeBinding typeBinding = null;
		MarkerInfo markerInfo = createThrownInFinallyMarkerInfo(node,
				typeBinding);

		thrownInFinallyList.add(markerInfo);
	}

	/**
	 * Create MarkerInfo of ThrownExceptionInFinallyBlock
	 * 
	 * @author pig
	 */
	private MarkerInfo createThrownInFinallyMarkerInfo(ASTNode node,
			ITypeBinding typeBinding) {
		return new MarkerInfo(
				RLMarkerAttribute.CS_THROWN_EXCEPTION_IN_FINALLY_BLOCK,
				typeBinding, node.toString(), node.getStartPosition(),
				root.getLineNumber(node.getStartPosition()), null);
	}

	public List<MarkerInfo> getThrownInFinallyList() {
		return thrownInFinallyList;
	}
}