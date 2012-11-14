package ntut.csie.robusta.codegen;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.MethodDeclaration;

/**
 * �Q�Φ渹��MExpressionStatement
 * @author Charles
 *
 */
public class ExpressionStatementLineNumberFinderVisitor extends ASTVisitor {

	/**	�N��쪺���G�s�b�o�� */
	private ExpressionStatement foundExpressionStatement;
	
	/** �Q�M�䪺�{���X�渹 */
	private int findingStatementLineNumber;
	
	/** �O�_�~��Visit���Tree */
	private boolean isKeepVisiting;
	
	/** �o��ExpressionStatement���ݪ�CompilationUnit�C��Line Number�ݭn�C */
	private CompilationUnit belongingCompilationUnit;
	
	public ExpressionStatementLineNumberFinderVisitor(CompilationUnit compilationUnit, int statementLineNumber) {
		foundExpressionStatement = null;
		findingStatementLineNumber = statementLineNumber;
		isKeepVisiting = true;
		belongingCompilationUnit = compilationUnit;
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
		if(findingStatementLineNumber == belongingCompilationUnit.getLineNumber(node.getStartPosition())) {
			foundExpressionStatement = node;
			isKeepVisiting = false;
		}
		return false;
	}
	
	public ExpressionStatement getExpressionStatement() {
		return foundExpressionStatement;
	}
}
