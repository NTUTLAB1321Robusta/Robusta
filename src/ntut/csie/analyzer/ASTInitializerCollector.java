package ntut.csie.analyzer;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Initializer;

public class ASTInitializerCollector extends ASTVisitor {
	private List<Initializer> initializerList;

	public ASTInitializerCollector() {
		super(true);
		initializerList = new ArrayList<Initializer>();
	}
	
	public boolean visit(Initializer node) {
		initializerList.add(node);
		return false;
	}

	public List<Initializer> getInitializerList() {
		return initializerList;
	}
}
