package ntut.csie.robusta.codegen;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.TryStatement;

public class CatchClauseFinderVisitor extends ASTVisitor {
	private CatchClause catchClauseHasBeenVisited;
	private int startPositionOfCatchClauseHasBeenVisited;
	
	public CatchClauseFinderVisitor(int startPosition) {
		catchClauseHasBeenVisited = null;
		startPositionOfCatchClauseHasBeenVisited = startPosition;
	}
	
	public boolean visit(MethodDeclaration node) {
		if(node.getStartPosition() > startPositionOfCatchClauseHasBeenVisited) {
			return false;
		}
		
		if((node.getStartPosition() + node.getLength()) < startPositionOfCatchClauseHasBeenVisited) {
			return false;
		}
		
		return true;
	}
	
	public boolean visit(TryStatement node) {
		if(node.getStartPosition() > startPositionOfCatchClauseHasBeenVisited) {
			return false;
		}
		
		if((node.getStartPosition() + node.getLength()) < startPositionOfCatchClauseHasBeenVisited) {
			return false;
		}
		
		return true;
	}
	
	public boolean visit(CatchClause node) {
		if(node.getStartPosition() == startPositionOfCatchClauseHasBeenVisited) {
			catchClauseHasBeenVisited = node;
		}
		return false; 
	}
	
	public CatchClause getFoundCatchClause() {
		return catchClauseHasBeenVisited;
	}
}
