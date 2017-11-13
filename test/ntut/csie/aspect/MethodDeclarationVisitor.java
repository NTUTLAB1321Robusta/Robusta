package ntut.csie.aspect;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;

public class MethodDeclarationVisitor extends ASTVisitor {
	private List<Integer> methodList;
	private CompilationUnit compilationUnit;

	public MethodDeclarationVisitor(CompilationUnit root) {
		super(true);
		compilationUnit = root;
		methodList = new ArrayList<Integer>();
	}
	
	public boolean visit(CatchClause node) {
		methodList.add(compilationUnit.getLineNumber(node.getStartPosition()));
		return false;
	}

	public List<Integer> getCatchClauseLineNumberList() {
		return methodList;
	}

}
