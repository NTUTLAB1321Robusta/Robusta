package ntut.csie.csdet.visitor;

import java.util.ArrayList;
import java.util.List;

import ntut.csie.csdet.data.MarkerInfo;
import ntut.csie.csdet.preference.SmellSettings;
import ntut.csie.rleht.builder.RLMarkerAttribute;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;

public class NewCarelessCleanupVisitor extends ASTVisitor {
	CompilationUnit root;
	MethodDeclaration methodDeclaration;
	private List<MarkerInfo> carelessCleanupList;
	
	public NewCarelessCleanupVisitor(CompilationUnit root) {
		super();
		this.root = root;
		carelessCleanupList = new ArrayList<MarkerInfo>();
	}
	
	@Override
	public boolean visit(MethodDeclaration node) {
		List<MethodInvocation> suspectedNode = collectSuspectedNode(node);
		
		for (MethodInvocation methodInvocation : suspectedNode) {
			CarelessCleanupNodeChecker ccChecker = new CarelessCleanupNodeChecker(methodInvocation);
			if(ccChecker.isCarelessCleanup()) {
				collectSmell(methodInvocation);
			}
		}
		return false;
	}

	private List<MethodInvocation> collectSuspectedNode(MethodDeclaration methodDeclaration) {
		CloseResourceMethodInvocationVisitor crmiVisitor = new CloseResourceMethodInvocationVisitor(root);
		methodDeclaration.accept(crmiVisitor);
		return crmiVisitor.getCloseMethodInvocationList();
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
