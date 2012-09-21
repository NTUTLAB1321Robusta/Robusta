package ntut.csie.csdet.visitor;

import java.util.List;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.TryStatement;

/**
 * ��M�M�פ��Ҧ���spare handler
 * @author chewei
 */
public class SpareHandlerVisitor extends ASTVisitor {
	private boolean result = false;	
	/** �ƹ��ϥտ�쪺�`�I */
	private ASTNode selectNode = null;
	
	public SpareHandlerVisitor(ASTNode node) {
		super(true);
		selectNode = node;
	}
	
	public boolean visit(TryStatement node) {
		processTryStatement(node);
		return false;
	}
	
	/**
	 * �M��n�Qrefactor��try statement
	 * @param node
	 */
	private void processTryStatement(ASTNode node) {
		/* 
		 * �S�w�`�I�p�G�����H�U����A�h�{�w�ϥΪ̻{�w����spare handler�A
		 * �åB�Q�n�Χڭ̴��Ѫ�Spare Handler ���c����C
		 * 1. �u�nCatch Clause�̭����O�Ū�
		 * 2. Catch Clause�̭����Ҧ��{���X���Q���
		 * FIXME: 
		 *  1. �L�u�|�h�P�_TryStatement���S����ӳQ��_�ӡA�Ӥ��|�h�ݳo��TryStatement�O���O�bCatch Clause�U��
		 *  2. �p�G���OTryStatement�A�ӬO���Catch Clause�̭����{���X�Q����A�]���ӭn�i�H���c 
		 */
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
		return result;
	}
}