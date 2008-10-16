package ntut.csie.csdet.visitor;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.dom.ASTNode;

import ntut.csie.rleht.common.RLBaseVisitor;

public class ASTCatchCollect extends RLBaseVisitor{
	private List<ASTNode> methodList;
	
	public ASTCatchCollect(){
		super(true);
		methodList = new ArrayList<ASTNode>();
	}
	
	protected boolean visitNode(ASTNode node) {
		try {
			switch (node.getNodeType()) {
			case ASTNode.CATCH_CLAUSE:
				this.methodList.add(node);
				return true;
			default:
				return true;

			}
		} catch (Exception e) {
			return false;
		}
	}
	
	public List<ASTNode> getMethodList() {
		return methodList;
	}
}
