package ntut.csie.analyzer;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CatchClause;

/**
 * collect all catch clauses in project
 * @author chewei
 */

public class ASTCatchCollect extends ASTVisitor {
	private List<CatchClause> methodList = new ArrayList<CatchClause>();
	
	public boolean visit(CatchClause catchClause) {
		methodList.add(catchClause);
		return true;
	}
	
	public List<CatchClause> getMethodList() {
		return methodList;
	}
}
