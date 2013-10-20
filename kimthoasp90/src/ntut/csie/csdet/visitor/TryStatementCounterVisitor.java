/**
 * 
 */
package ntut.csie.csdet.visitor;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.TryStatement;

/**
 * @author Reverof
 *
 */
public class TryStatementCounterVisitor extends ASTVisitor{

	private int tryCounter = 0;
	private int catchCount = 0;
	private int finallyCount = 0;
	
	public boolean visit(TryStatement node) {
		tryCounter++;
		if(node.getFinally() != null) {
			finallyCount++;
		}
		return true;
	}
	
	public boolean visit(CatchClause node) {
		catchCount++;
		return true;
	}
	
	public int getTryCount() {
		return tryCounter;
	}
	
	public int getCatchCount() {
		return catchCount;
	}
	
	public int getFinallyCount() {
		return finallyCount;
	}
}
