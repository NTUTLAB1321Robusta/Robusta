package ntut.csie.robusta.codegen;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.MethodDeclaration;

/**
 * �Q�ε{���X���e��MExpressionStatement
 * @author charles
 *
 */
public class ExpressionStatementStringFinderVisitor extends ASTVisitor {

	/**	�N��쪺���G�s�b�o�� */
	private ExpressionStatement foundExpressionStatement;
	
	/** �Q�M�䪺�{���X */
	private String comparisingStatement;
	
	/** �O�_�~��Visit���Tree */
	private boolean isKeepVisiting;
	
	public ExpressionStatementStringFinderVisitor(String statement) {
		foundExpressionStatement = null;
		comparisingStatement = statement;
		isKeepVisiting = true;
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
		if(node.toString().contains(comparisingStatement)) {
			foundExpressionStatement = node;
			isKeepVisiting = false;
		}
		return false;
	}
	
	public ExpressionStatement getFoundExpressionStatement() {
		return foundExpressionStatement;
	}
}
