package ntut.csie.aspect;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.TryStatement;

public class FindAllTryStatementVisitor extends ASTVisitor {
	List<TryStatement> tryStatements = new ArrayList<TryStatement>();

	@Override
	public boolean visit(TryStatement tryStatement) {
		tryStatements.add(tryStatement);
		return true;
	}

	public List<TryStatement> getTryStatementsList() {
		return tryStatements;
	}
}
