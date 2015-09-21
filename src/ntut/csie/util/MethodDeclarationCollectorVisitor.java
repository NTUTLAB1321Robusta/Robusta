package ntut.csie.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.MethodDeclaration;

public class MethodDeclarationCollectorVisitor extends ASTVisitor {
	private final List <MethodDeclaration> methods = new ArrayList <MethodDeclaration> ();

	  @Override
	  public boolean visit (final MethodDeclaration method) {
	    methods.add (method);
	    return super.visit (method);
	  }

	  /**
	   * @return an immutable list view of the methods discovered by this visitor
	   */
	  public List <MethodDeclaration> getMethods () {
	    return Collections.unmodifiableList (methods);
	  }
}
