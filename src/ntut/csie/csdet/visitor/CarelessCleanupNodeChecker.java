package ntut.csie.csdet.visitor;

import java.util.Iterator;
import java.util.List;

import ntut.csie.csdet.visitor.aidvisitor.ClassInstanceCreationVisitor;
import ntut.csie.jdt.util.NodeUtils;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.MethodInvocation;

public class CarelessCleanupNodeChecker {
	
	private MethodInvocation closeNode;

	public CarelessCleanupNodeChecker(MethodInvocation closeNode) {
		super();
		this.closeNode = closeNode;
	}
	
	public boolean isCarelessCleanup() {
		return isCarelessRecursive(closeNode);
	}
	
	private boolean isCarelessRecursive(ASTNode targetNode) {
		if(targetNode.getNodeType() == ASTNode.METHOD_DECLARATION) return false;
		ASTNode parentNode = targetNode.getParent();
		
		//Collect all first level child node in the parent node
		FirstLevelChildCollector firstLevelChildCollector = new FirstLevelChildCollector(parentNode, targetNode);
		parentNode.accept(firstLevelChildCollector);
		List<ASTNode> allStatements = firstLevelChildCollector.getChildrens();

		//Remove all the nodes that not between Declaration and Close
		Iterator<ASTNode> it = allStatements.iterator();
		while (it.hasNext()) {
		  ASTNode node = it.next();
		  if (!isNodeBetweenDeclarationAndClose(node)) {
		    it.remove();
		  }
		}
		
		//If there is any statement between declaration and close, it is careless cleanup
		if(allStatements.size() > 0) {
			return true;
		}
		return isCarelessRecursive(parentNode);
	}
	private boolean isNodeBetweenDeclarationAndClose(ASTNode node) {
		int declarationNodeStartPosition = -1;
		int closeNodeStartPosition = closeNode.getStartPosition();
		int astNodeStartPosition = node.getStartPosition();

		VariableDeclarationVisitor vdVisitor = new VariableDeclarationVisitor(closeNode);
		ASTNode methodDeclaration = NodeUtils.getSpecifiedParentNode(closeNode, ASTNode.METHOD_DECLARATION);
		methodDeclaration.accept(vdVisitor);
		ASTNode declarationNode = vdVisitor.getVariableDeclaration();
		if(declarationNode != null) {
			declarationNodeStartPosition = declarationNode.getStartPosition();
		}
		
		if ((astNodeStartPosition > declarationNodeStartPosition) &&
			(astNodeStartPosition < closeNodeStartPosition)) {
			return true;
		}
		
		return false;
	}
}
