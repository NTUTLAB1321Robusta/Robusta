package ntut.csie.analyzer.careless;

import java.util.ArrayList;
import java.util.List;

import ntut.csie.csdet.data.MarkerInfo;
import ntut.csie.rleht.builder.RLMarkerAttribute;
import ntut.csie.robusta.marker.AnnotationInfo;
import ntut.csie.util.AbstractBadSmellVisitor;
import ntut.csie.util.NodeUtils;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;

public class CarelessCleanupVisitor extends AbstractBadSmellVisitor {
	CompilationUnit root;
	boolean isOnlyDetectingInTry;
	private List<MarkerInfo> carelessCleanupList;

	public CarelessCleanupVisitor(CompilationUnit root, boolean outOfTry) {
		this.root = root;
		carelessCleanupList = new ArrayList<MarkerInfo>();
		isOnlyDetectingInTry = !outOfTry;
	}

	@Override
	public boolean visit(MethodDeclaration node) {
		// Get all close statements
		List<MethodInvocation> closeInvocations = collectCloseStamentNode(node);
		Closenumber.closeNum = Closenumber.closeNum + closeInvocations.size();
		// User can decide whether to check or not to check the close invocations which are not in a try statement
		if (isOnlyDetectingInTry) {
			removeCloseInvocationNotInTry(closeInvocations);
		} 
		
		// Create MarkerInfo for close invocations that might not be executed due to an exception
		for (MethodInvocation closeInvocation : closeInvocations) {
			CloseInvocationExecutionChecker cieChecker = new CloseInvocationExecutionChecker();
			List<ASTNode> astNodesThatMayThrowException = cieChecker.getASTNodesThatMayThrowExceptionBeforeCloseInvocation(closeInvocation);
			if (astNodesThatMayThrowException.size() != 0) {
				collectSmell(closeInvocation, astNodesThatMayThrowException);
			}
		}
		return false;
	}

	private List<MethodInvocation> collectCloseStamentNode(MethodDeclaration methodDeclaration) {
		CloseResourceMethodInvocationVisitor crmiVisitor = new CloseResourceMethodInvocationVisitor(root);
		methodDeclaration.accept(crmiVisitor);
		return crmiVisitor.getCloseMethodInvocations();
	}
	
	private void removeCloseInvocationNotInTry(List<MethodInvocation> closeInvocations) {
		List<MethodInvocation> closeInvocationNotInTry = new ArrayList<MethodInvocation>();
		for(MethodInvocation closeInvocation : closeInvocations) {
			if (NodeUtils.getSpecifiedParentNode(closeInvocation, ASTNode.TRY_STATEMENT) == null) {
				closeInvocationNotInTry.add(closeInvocation);
			}
		}
		for(MethodInvocation mi : closeInvocationNotInTry) {
			closeInvocations.remove(mi);
		}
	}
	
	private void collectSmell(MethodInvocation closeInvocation, List<ASTNode> astNodesThatMayThrowException) { //TODO add annotation for nodes in the second parameter
		List<AnnotationInfo> annotationList = new ArrayList<AnnotationInfo>();
		for(ASTNode node : astNodesThatMayThrowException) {
			AnnotationInfo ai = new AnnotationInfo(root.getLineNumber(node.getStartPosition()), 
					node.getStartPosition(), 
					node.getLength(), 
					"May Cause Closing Resource Not To Be Executed!");
			annotationList.add(ai);
		}
		
		MarkerInfo markerInfo = new MarkerInfo(
			RLMarkerAttribute.CS_CARELESS_CLEANUP,
			(closeInvocation.getExpression() != null)? closeInvocation.getExpression().resolveTypeBinding() : null,
			((CompilationUnit)closeInvocation.getRoot()).getJavaElement().getElementName(), // class name
			closeInvocation.toString(), 
			closeInvocation.getStartPosition(), 
			root.getLineNumber(closeInvocation.getStartPosition()), 
			null,
			annotationList);
		carelessCleanupList.add(markerInfo);
	}

	public List<MarkerInfo> getCarelessCleanupList() {
		return carelessCleanupList;
	}

	@Override
	public List<MarkerInfo> getBadSmellCollected() {
		return getCarelessCleanupList();
	}
}
