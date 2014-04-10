package ntut.csie.analyzer.careless;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.Statement;

public class StatementsInBlockCollectingVisitor extends ASTVisitor {

	private List<Statement> children;
	private boolean isParentNodeSet;

	public List<Statement> getStatementsInBlock() {
		return children;
	}

	public boolean preVisit2(ASTNode node) {
		preVisit(node);
		
		if (node instanceof Block) {
			startCollecting();
			return true;
		} else if (isParentNodeSet) {
			// This means statement in the block.
			children.add((Statement) node);
			return false;
		} else {
			// This means accepted by a non-block node.
			throw new IllegalArgumentException("Should accept by block.");
		}
	}

	/**
	 * Tear down
	 */
	public void endVisit(Block block) {
		isParentNodeSet = false;
	}
	
	/**
	 * Set up
	 */
	private void startCollecting() {
		children = new ArrayList<Statement>();
		isParentNodeSet = true;
	}

}
