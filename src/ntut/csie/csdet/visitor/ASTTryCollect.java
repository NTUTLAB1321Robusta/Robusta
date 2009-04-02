package ntut.csie.csdet.visitor;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.dom.ASTNode;

import ntut.csie.rleht.common.RLBaseVisitor;

public class ASTTryCollect extends RLBaseVisitor{
	private List<ASTNode> tryList;
	
	public ASTTryCollect(){
		super(true);
		tryList = new ArrayList<ASTNode>();
	}
	
	protected boolean visitNode(ASTNode node) {
		try {
			switch (node.getNodeType()) {
			case ASTNode.TRY_STATEMENT:
				this.tryList.add(node);
//				System.out.println("¡iNode Content¡j===>"+node.toString());
				return true;
			default:
				return true;

			}
		} catch (Exception e) {
			return false;
		}
	}
	
	public List<ASTNode> getTryList() {
		return tryList;
	}
}
