package ntut.csie.analyzer.careless;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import ntut.csie.csdet.data.MarkerInfo;
import ntut.csie.rleht.builder.RLMarkerAttribute;
import ntut.csie.util.NodeUtils;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;

public class CarelessCleanupVisitor extends ASTVisitor {
	CompilationUnit root;
	MethodDeclaration methodDeclaration;
	MethodInvocationMayInterruptByExceptionChecker invocationChecker;
	boolean isOnlyDetectingInTry;
	private List<MarkerInfo> carelessCleanupList;

	public CarelessCleanupVisitor(CompilationUnit root, boolean outOfTry) {
		this.root = root;
		carelessCleanupList = new ArrayList<MarkerInfo>();
		invocationChecker = new MethodInvocationMayInterruptByExceptionChecker();
		isOnlyDetectingInTry = !outOfTry;
	}

	@Override
	public boolean visit(MethodDeclaration node) {
		// Get all close actions
		List<MethodInvocation> suspectedNodes = collectSuspectedNode(node);

		// Is use want to detect it out of try?
		if (isOnlyDetectingInTry) {
			removeSuspectsAreNotInTry(suspectedNodes);
		}
		
		// Is it may interrupt by declared exceptions
		for (MethodInvocation eachSuspect : suspectedNodes) {
			if (invocationChecker.isMayInterruptByException(eachSuspect)) {
				collectSmell(eachSuspect);
			}
		}
		return false;
	}

	private List<MethodInvocation> collectSuspectedNode(MethodDeclaration methodDeclaration) {
		CloseResourceMethodInvocationVisitor crmiVisitor = new CloseResourceMethodInvocationVisitor(root);
		methodDeclaration.accept(crmiVisitor);
		return crmiVisitor.getCloseMethodInvocations();
	}
	
	private void removeSuspectsAreNotInTry(List<MethodInvocation> suspects) {
		for (Iterator iterator = suspects.iterator(); iterator.hasNext();) {
			MethodInvocation methodInvocation = (MethodInvocation) iterator.next();
			if (null == NodeUtils.getSpecifiedParentNode(methodInvocation, ASTNode.TRY_STATEMENT)) {
				iterator.remove();
			}
		}
	}
	
	private void collectSmell(MethodInvocation node) {
		MarkerInfo markerInfo = new MarkerInfo(RLMarkerAttribute.CS_CARELESS_CLEANUP,
			(node.getExpression() != null)? node.getExpression().resolveTypeBinding() : null,
			node.toString(), node.getStartPosition(),
			root.getLineNumber(node.getStartPosition()),
			null);
		carelessCleanupList.add(markerInfo);
	}

	public List<MarkerInfo> getCarelessCleanupList() {
		return carelessCleanupList;
	}
}
