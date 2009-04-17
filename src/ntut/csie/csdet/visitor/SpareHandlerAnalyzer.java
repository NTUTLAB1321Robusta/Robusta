package ntut.csie.csdet.visitor;

import java.util.ArrayList;
import java.util.List;

import ntut.csie.csdet.data.CSMessage;
import ntut.csie.rleht.builder.RLMarkerAttribute;
import ntut.csie.rleht.common.RLBaseVisitor;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.TryStatement;

/**
 * 找尋專案中所有的spare handler
 * @author chewei
 */
public class SpareHandlerAnalyzer extends RLBaseVisitor{
	
	private boolean result = false;	
	private ASTNode selectNode = null;
	// AST tree的root(檔案名稱)
	private CompilationUnit root;
	
	// 儲存所找到的dummy handler
	private List<CSMessage> spareHandlerList;
	
	public SpareHandlerAnalyzer(ASTNode node,CompilationUnit root){
		super(true);		
		this.selectNode = node;
		this.root = root;
		spareHandlerList = new ArrayList<CSMessage>();
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
		//只要在catch block之中還有一個try-catch,則視為spare handler
		TryStatement ts = (TryStatement)node;
		List catchList = ts.catchClauses();
		if(catchList != null){
			for(int i=0; i< catchList.size(); i++){
				CatchClause cc = (CatchClause)catchList.get(i);
				List catchStat = cc.getBody().statements();
				if(catchStat != null){
					for(int x = 0 ; x< catchStat.size(); x++){
						if(catchStat.get(i) instanceof TryStatement){
							TryStatement temp = (TryStatement)catchStat.get(i);
							CSMessage csmsg = new CSMessage(RLMarkerAttribute.CS_SPARE_HANDLER,
									null,											
									ts.toString(),temp.getStartPosition(),
									this.getLineNumber(temp.getStartPosition()),null);
							spareHandlerList.add(csmsg);			
						}
					}
				}
			}
			if(selectNode != null){
				if(ts.getStartPosition() == selectNode.getStartPosition()){
					//找到那個try的節點就設定為true				
					result = true;
				}				
			}			
		}
	}
	
	/**
	 * 根據startPosition來取得行數
	 */
	private int getLineNumber(int pos) {
		return root.getLineNumber(pos);
	}
	
	/**
	 * 利用此結果來得知是否有找到要被refactor的節點
	 * @return
	 */
	public boolean getResult(){
		return this.result;
	}
	
	/**
	 * 取得spare Handler的List
	 */
	public List<CSMessage> getSpareHandlerList(){
		return spareHandlerList;
	}
}
