package ntut.csie.csdet.visitor;

import java.util.List;

import ntut.csie.rleht.common.RLBaseVisitor;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.TryStatement;

/**
 * 找尋專案中所有的spare handler
 * @author chewei
 */
public class SpareHandlerAnalyzer extends RLBaseVisitor{
	
	private boolean result = false;
	
	private ASTNode selectNode = null;
	
	public SpareHandlerAnalyzer(ASTNode node){
		super(true);		
		this.selectNode = node;

	}
	
	/*
	 * (non-Javadoc) 
	 * @see ntut.csie.rleht.views.ASTBaseVisitor#visitNode(org.eclipse.jdt.core.dom.ASTNode)
	 */
	protected boolean visitNode(ASTNode node) {
		switch (node.getNodeType()) {
			case ASTNode.TRY_STATEMENT:
				processTryStatement(node);
				return false;
			default:
				return true;
		}		
	}
	
	/**
	 * 尋找要被refactor的try statement
	 * @param node
	 */
	private void processTryStatement(ASTNode node){
		TryStatement ts = (TryStatement)node;
		List catchList = ts.catchClauses();
		if(catchList != null){
			if(ts.getStartPosition() == selectNode.getStartPosition()){
				//找到那個try的節點就設定為true
				result = true;
			}
//			for(int i=0;i<catchList.size();i++){
//				CatchClause cc = (CatchClause)catchList.get(i);
//				List catchStat = cc.getBody().statements();
//				for(int x=0;x<catchStat.size();x++){
//					if(catchStat.get(i) instanceof TryStatement){
//						if(ts.getStartPosition() == selectNode.getStartPosition()){
//							//找到那個try的節點就設定為true
//							result = true;
//						}
//					}
//				}
//			}			
		}
	}
	
	/**
	 * 利用此結果來得知是否有找到要被refactor的節點
	 * @return
	 */
	public boolean getResult(){
		return this.result;
	}
}
