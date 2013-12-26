package ntut.csie.csdet.visitor;

import java.util.ArrayList;
import java.util.List;

import ntut.csie.csdet.visitor.aidvisitor.ClassInstanceCreationVisitor;
import ntut.csie.jdt.util.NodeUtils;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.TryStatement;

public class FirstLevelChildCollector extends ASTVisitor {

	private ASTNode targetNode;
	private ASTNode parentNode;
	private List<ASTNode> childrens;
	private boolean stop;
	
	public List<ASTNode> getChildrens() {
		return childrens;
	}

	public FirstLevelChildCollector(ASTNode parentNode, ASTNode targetNode) {
		super();
		this.targetNode = targetNode;
		this.parentNode = parentNode;
		childrens = new ArrayList<ASTNode>();
		stop = false;
	}

	@Override
	public boolean preVisit2(ASTNode node) {
		if(isInSameTryStatement())
			return false;
		if(stop) return false;
		if(node == parentNode)
			return true;
		if(node == targetNode)
		{
			stop = true;
			return false;
		}
		if(node.getNodeType() == ASTNode.BLOCK)
			return true;
		if(Statement.class.isInstance(node)) {
			childrens.add(node);
			return false;
		}
		return true;
	}
	
	private boolean isInSameTryStatement() {
		if(parentNode == null) return false;
		if(parentNode.getNodeType() == ASTNode.TRY_STATEMENT) {
			TryStatement tryStatement = (TryStatement)parentNode;
			if( tryStatement.getFinally() == null) return false;
			int startPositionFinallyBlock = tryStatement.getFinally().getStartPosition();
			int endPositionFinallyBlock = tryStatement.getFinally().getStartPosition() + tryStatement.getFinally().getLength();
			int targetPosition = targetNode.getStartPosition();
			if(startPositionFinallyBlock <= targetPosition && targetPosition <= endPositionFinallyBlock)
				return true;
		}
		return false;
	}	
}
