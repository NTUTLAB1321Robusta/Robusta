package ntut.csie.csdet.visitor;

import java.util.ArrayList;
import java.util.List;

import ntut.csie.csdet.data.CSMessage;
import ntut.csie.rleht.common.RLBaseVisitor;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.TryStatement;

/**
 * ��M�M�פ��Ҧ���spare handler
 * @author chewei
 */
public class SpareHandlerAnalyzer extends RLBaseVisitor{
	
	private boolean result = false;	
	private ASTNode selectNode = null;
	// AST tree��root(�ɮצW��)
	private CompilationUnit root;
	
	// �x�s�ҧ�쪺dummy handler
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
	 * �M��n�Qrefactor��try statement
	 * @param node
	 */
	private void processTryStatement(ASTNode node){
		//�u�n�bcatch block�����٦��@��try-catch,�h����spare handler
		TryStatement ts = (TryStatement)node;
		List catchList = ts.catchClauses();
		if(catchList != null){
			
			if(selectNode != null){
				if(ts.getStartPosition() == selectNode.getStartPosition()){
					//��쨺��try���`�I�N�]�w��true				
					result = true;
				}				
			}	
		}
	}
	
//	/**
//	 * �ھ�startPosition�Ө��o���
//	 */
//	private int getLineNumber(int pos) {
//		return root.getLineNumber(pos);
//	}
	
	/**
	 * �Q�Φ����G�ӱo���O�_�����n�Qrefactor���`�I
	 * @return
	 */
	public boolean getResult(){
		return this.result;
	}
	
	/**
	 * ���ospare Handler��List
	 */
	public List<CSMessage> getSpareHandlerList(){
		return spareHandlerList;
	}
}
