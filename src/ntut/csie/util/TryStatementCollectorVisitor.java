package ntut.csie.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.TryStatement;

public class TryStatementCollectorVisitor extends ASTVisitor{
	private List<TryStatement> tryStatements = new ArrayList<TryStatement> ();
	
	public boolean visit (final TryStatement tryStatement) {
		tryStatements.add (tryStatement);
	    return super.visit (tryStatement);
	  }
	
	  public List<TryStatement> getTryStatements () {
	    return Collections.unmodifiableList (tryStatements);
	  }
}
