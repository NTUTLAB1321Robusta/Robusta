package ntut.csie.robusta.codegen;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.TryStatement;

public class CatchClauseFinderVisitor extends ASTVisitor {
	/**	�N��쪺CatchClause�s�b�o�� */
	private CatchClause foundCatchClause;
	/** �n�䪺CatchClause�Ҧb��m */
	private int ccStartPosition;
	
	public CatchClauseFinderVisitor(int startPosition) {
		foundCatchClause = null;
		ccStartPosition = startPosition;
	}
	
	public boolean visit(MethodDeclaration node) {
		if(node.getStartPosition() > ccStartPosition) {
			return false;
		}
		
		if((node.getStartPosition() + node.getLength()) < ccStartPosition) {
			return false;
		}
		
		return true;
	}
	
	public boolean visit(TryStatement node) {
		if(node.getStartPosition() > ccStartPosition) {
			return false;
		}
		
		if((node.getStartPosition() + node.getLength()) < ccStartPosition) {
			return false;
		}
		
		return true;
	}
	
	public boolean visit(CatchClause node) {
		if(node.getStartPosition() == ccStartPosition) {
			foundCatchClause = node;
		}
		return false; 
	}
	
	public CatchClause getFoundCatchClause() {
		return foundCatchClause;
	}
}
