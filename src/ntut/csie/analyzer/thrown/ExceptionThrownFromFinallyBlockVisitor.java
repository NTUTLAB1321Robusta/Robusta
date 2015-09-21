package ntut.csie.analyzer.thrown;

import java.util.ArrayList;
import java.util.List;

import ntut.csie.csdet.data.MarkerInfo;
import ntut.csie.rleht.builder.RLMarkerAttribute;
import ntut.csie.robusta.marker.AnnotationInfo;
import ntut.csie.util.AbstractBadSmellVisitor;
import ntut.csie.util.NodeUtils;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.dom.ThrowStatement;
import org.eclipse.jdt.core.dom.TryStatement;

public class ExceptionThrownFromFinallyBlockVisitor extends AbstractBadSmellVisitor {
	private CompilationUnit root;
	private List<MarkerInfo> thrownInFinallyList;
	private int finallyStack = 0; // To check if in any finally block
	private Block outermostFinallyBlock = null;

	public ExceptionThrownFromFinallyBlockVisitor(CompilationUnit compilationUnit) {
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

	private boolean isInAnyFinallyBlock() {
		return finallyStack > 0;
	}

	private void addMarkerInfo(MethodInvocation node) {
		ITypeBinding typeBinding = NodeUtils.getExpressionBinding(node);
		MarkerInfo markerInfo = createThrownInFinallyMarkerInfo(node,
				typeBinding);
		markerInfo.setExceptionsMethodThrown(node.resolveMethodBinding()
				.getExceptionTypes());
		
		ITypeBinding returnType = (ITypeBinding)node.resolveMethodBinding().getReturnType();
		if(returnType.getQualifiedName().equals("void")) {
			markerInfo.setSupportRefactoring(true);
		} else {
			markerInfo.setSupportRefactoring(false);
		}
		
		thrownInFinallyList.add(markerInfo);
	}

	private void addMarkerInfo(SuperMethodInvocation node) {
		ITypeBinding typeBinding = null;
		MarkerInfo markerInfo = createThrownInFinallyMarkerInfo(node,
				typeBinding);
		markerInfo.setExceptionsMethodThrown(node.resolveMethodBinding()
				.getExceptionTypes());
		
		ITypeBinding returnType = (ITypeBinding)node.resolveMethodBinding().getReturnType();
		if(returnType.getQualifiedName().equals("void")) {
			markerInfo.setSupportRefactoring(true);
		} else {
			markerInfo.setSupportRefactoring(false);
		}
		
		thrownInFinallyList.add(markerInfo);
	}

	private void addMarkerInfo(ThrowStatement node) {
		ITypeBinding typeBinding = NodeUtils.getExpressionBinding(node);
		MarkerInfo markerInfo = createThrownInFinallyMarkerInfo(node,
				typeBinding);
		markerInfo.setSupportRefactoring(false);
		thrownInFinallyList.add(markerInfo);
	}

	private void addMarkerInfo(ClassInstanceCreation node) {
		ITypeBinding typeBinding = null;
		MarkerInfo markerInfo = createThrownInFinallyMarkerInfo(node,
				typeBinding);
		markerInfo.setSupportRefactoring(false);
		thrownInFinallyList.add(markerInfo);
	}

	/**
	 * Create MarkerInfo of ExceptionThrownFromFinallyBlock
	 * 
	 * @author pig
	 */
	private MarkerInfo createThrownInFinallyMarkerInfo(ASTNode node,
			ITypeBinding typeBinding) {
		ArrayList<AnnotationInfo> annotationList = new ArrayList<AnnotationInfo>(2);
		AnnotationInfo ai = new AnnotationInfo(root.getLineNumber(node.getStartPosition()), 
				node.getStartPosition(), 
				node.getLength(), 
				"May throw exception in this finally block and swallow exception thrown in associated try block");
		annotationList.add(ai);
		
		return new MarkerInfo(
				RLMarkerAttribute.CS_EXCEPTION_THROWN_FROM_FINALLY_BLOCK,
				typeBinding, 
				((CompilationUnit)node.getRoot()).getJavaElement().getElementName(), // class name
				node.toString(), 
				node.getStartPosition(),
				root.getLineNumber(node.getStartPosition()), 
				null,
				annotationList);
	}

	public List<MarkerInfo> getThrownInFinallyList() {
		return thrownInFinallyList;
	}

	/**
	 * Check if the node input is a finally block
	 * @author pig
	 */
	private boolean isFinallyBlock(ASTNode checkingNode) {
		if (checkingNode == null || !(checkingNode instanceof Block)) {
			return false;
		}

		TryStatement trySatatmentParentNode = (TryStatement) NodeUtils
				.getSpecifiedParentNode(checkingNode, ASTNode.TRY_STATEMENT);
		if (trySatatmentParentNode != null) {
			Block finallyBlock = trySatatmentParentNode.getFinally();
			return (finallyBlock!=null && NodeUtils.isTwoASTNodeAreTheSame(finallyBlock, checkingNode));
		}

		return false;
	}

	@Override
	public List<MarkerInfo> getBadSmellCollected() {
		return getThrownInFinallyList();
	}
}