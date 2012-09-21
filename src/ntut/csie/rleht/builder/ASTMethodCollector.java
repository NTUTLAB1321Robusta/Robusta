package ntut.csie.rleht.builder;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.MethodDeclaration;

/**
 * 收集所有class中的method
 * @author allen
 */
public class ASTMethodCollector extends ASTVisitor {
	private List<MethodDeclaration> methodList;

	public ASTMethodCollector() {
		super(true);
		methodList = new ArrayList<MethodDeclaration>();
	}
	
	public boolean visit(MethodDeclaration node) {
		methodList.add(node);
		return false;
	}

	public List<MethodDeclaration> getMethodList() {
		return methodList;
	}
	
}
