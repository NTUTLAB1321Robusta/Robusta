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
 * ��M�M�פ��Ҧ���spare handler
 * @author chewei
 */
public class SpareHandlerAnalyzer extends RLBaseVisitor{

	// AST tree��root(�ɮצW��)
	private CompilationUnit root;
    
	// �x�s�ҧ�쪺spare handler 
	private List<CSMessage> spareHandlerList;
	
	
	public SpareHandlerAnalyzer(CompilationUnit root){
		super(true);
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
	 * �M��spare handler��code smell
	 * @param node
	 */
	private void processTryStatement(ASTNode node){
		//�u�ncatch block�����٦��@��try,�N�|�Q����spare handler
		TryStatement ts = (TryStatement)node;
		List catchList = ts.catchClauses();
		for(int i=0;i<catchList.size();i++){
			CatchClause cc = (CatchClause)catchList.get(i);
			List catchStat = cc.getBody().statements();
			for(int x=0;x<catchStat.size();x++){
				if(catchStat.get(x) instanceof TryStatement){
					//marker����m��ܼЦb�Ĥ@�h��try
//					CSMessage csmsg = new CSMessage(RLMarkerAttribute.CS_SPARE_HANDLER,null,											
//							ts.toString(),ts.getStartPosition(),
//							this.getLineNumber(ts.getStartPosition()),null);
//					this.spareHandlerList.add(csmsg);
					break;
				}					
			}
		}
	}
	
	/**
	 * �ھ�startPosition�Ө��o���
	 */
	private int getLineNumber(int pos) {
		return root.getLineNumber(pos);
	}
	
	/**
	 * ���ospare handler��list 
	 */
	public List<CSMessage> getSpareHandler(){
		return spareHandlerList;
	}
}
