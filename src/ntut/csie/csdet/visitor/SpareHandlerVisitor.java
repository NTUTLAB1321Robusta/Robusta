package ntut.csie.csdet.visitor;

import java.util.List;

import ntut.csie.rleht.common.RLBaseVisitor;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.TryStatement;

/**
 * ��M�M�פ��Ҧ���spare handler
 * @author chewei
 */
public class SpareHandlerVisitor extends RLBaseVisitor {
	
	private boolean result = false;	
	/** �ƹ��ϥտ�쪺�`�I */
	private ASTNode selectNode = null;
	
	public SpareHandlerVisitor(ASTNode node) {
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
	 * �M��n�Qrefactor��try statement
	 * @param node
	 */
	private void processTryStatement(ASTNode node) {
		//�u�n�bcatch block�����٦��@��try-catch,�h����spare handler
		TryStatement ts = (TryStatement)node;
		List<?> catchList = ts.catchClauses();
		if(catchList != null && selectNode != null) {
			if(ts.getStartPosition() == selectNode.getStartPosition()) {
				//��쨺��try���`�I�N�]�w��true				
				result = true;
			}				
		}
	}
	
	/**
	 * �Q�Φ����G�ӱo���O�_�����n�Qrefactor���`�I
	 * @return
	 */
	public boolean getResult() {
		return this.result;
	}
}