package ntut.csie.analyzer.careless;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Statement;

public class FirstLevelChildStatementCollectingVisitor extends ASTVisitor {

	private List<Statement> children;
	private boolean isVisitedParentNode;

	public List<Statement> getChildren() {
		return children;
	}

	public FirstLevelChildStatementCollectingVisitor() {
		children = new ArrayList<Statement>();
		isVisitedParentNode = false;
	}

	@Override
	public boolean preVisit2(ASTNode node) {
		if(!isVisitedParentNode) {
			isVisitedParentNode = true;
			return true;
		} else {
			if (node instanceof Statement) {
				children.add((Statement) node);
			}
			return false;
		}
	}

}
