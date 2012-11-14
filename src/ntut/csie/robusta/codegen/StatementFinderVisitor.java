package ntut.csie.robusta.codegen;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.MethodDeclaration;

public class StatementFinderVisitor extends ASTVisitor {

	/**	�N��쪺���G�s�b�o�� */
	private ExpressionStatement foundExpressionStatement;
	
	/** �Q��諸startPosition */
	private int comparisingStartPosition;
	
	/** �O�_�~��Visit���Tree */
	private boolean isKeepVisiting;
	
	public StatementFinderVisitor(int startPosition) {
		foundExpressionStatement = null;
		isKeepVisiting = true;
		comparisingStartPosition = startPosition;
	}
	
	public boolean visit(MethodDeclaration methodDeclaration) {
		/*
		 * �p�G���~��visit���Tree�A�N�bMethodDeclaration���`�I�ױ��A
		 * ���~�򩹤l�`�I���X�A�[�ֵ������t�סC
		 * �o�ӬO�w��ϥΦ�Class��Caller�n�D���XCompilationUnit���ɭԤ~���@�ΡC
		 * ���y�ܻ��A�p�G�u�O���XIfStatement, TryStatement..., and so on�A�o�q�{���X�N�S�t�C
		 */
		return isKeepVisiting;
	}
	
	public boolean visit(ExpressionStatement node) {
		if(node.getStartPosition() == comparisingStartPosition) {
			foundExpressionStatement = node;
			isKeepVisiting = false;
		}
		return false;
	}
	
	public ExpressionStatement getFoundExpressionStatement() {
		return foundExpressionStatement;
	}
}
