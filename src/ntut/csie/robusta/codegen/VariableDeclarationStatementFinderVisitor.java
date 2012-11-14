package ntut.csie.robusta.codegen;

import ntut.csie.jdt.util.NodeUtils;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;

public class VariableDeclarationStatementFinderVisitor extends ASTVisitor {
	/**	�N��쪺���G�s�b�o�� */
	private VariableDeclarationStatement foundVariableDeclarationStatement;
	
	/** �Q�n�q�o��method invocation��X�ŧi�L��variable declaration statement�C */
	private MethodInvocation comparisingMethodInvocation;
	
	/** �u�q�o��TryStatement�h�M�� */
	private TryStatement specifiedSearchingNode; 
	
	public VariableDeclarationStatementFinderVisitor(MethodInvocation methodInvocation) {
		foundVariableDeclarationStatement = null;
		comparisingMethodInvocation = methodInvocation;
		specifiedSearchingNode = null;
	}

	public boolean visit(VariableDeclarationFragment node) {
		// method invocation���ܼƥi�H��ܪ��A�ڭ̥u���Ĥ@�ӡC
		SimpleName methodInvocationFirstVariableSimpleName = (SimpleName)comparisingMethodInvocation.getExpression();

		// �p�G�S��SimpleName�A��ܥL���Oinstance.method�o�ؼˤl�C�ڰ��˥��Oclose(instance)���ˤl
		if(methodInvocationFirstVariableSimpleName == null) {
			return false;
		}
		
		// �p�G�ܼƦW�r���@�ˡA�N���}�a
		if (!node.getName().toString().equals(
				methodInvocationFirstVariableSimpleName.getFullyQualifiedName())) {		
			return true;
		}
		
		ASTNode possibleVariableDeclarationStatement = node.getParent();

		// �p�Gnode���W�h���OASTNode.VARIABLE_DECLARATION_STATEMENT�A�]���}�a
		if (possibleVariableDeclarationStatement.getNodeType() != ASTNode.VARIABLE_DECLARATION_STATEMENT) {
			return true;
		}
		
		ASTNode possibleVariableDeclarationStatementMD = NodeUtils.getSpecifiedParentNode(possibleVariableDeclarationStatement, ASTNode.METHOD_DECLARATION);
		ASTNode comparisingMethodInvocationMD = NodeUtils.getSpecifiedParentNode(comparisingMethodInvocation, ASTNode.METHOD_DECLARATION);
		
		// �p�Gnode��possibleVariableDeclarationStatement�b���Pmethod declaration�̭��A�]���}�a
		if(possibleVariableDeclarationStatementMD.equals(comparisingMethodInvocationMD)) {
			foundVariableDeclarationStatement = (VariableDeclarationStatement) possibleVariableDeclarationStatement;
			return false;
		}
		
		return true;
	}
	
	public void setSearchingSpecifiedTryStatement(TryStatement tryStatement) {
		specifiedSearchingNode = tryStatement;
	}
	
	/**
	 * �p�G�����wTryStatement�A�N�|����̪�StartPosition�O�_�۵��A�۵��N�~�򩹤U��VariableDeclaration�C
	 * �p�G�S�����wTryStatement�A�N�|�C��TryStatement�����U��C
	 */
	public boolean visit(TryStatement node) {
		if(specifiedSearchingNode == null) {
			return true;
		} else if(specifiedSearchingNode.getStartPosition() == node.getStartPosition()) {
			return true;
		}
		return false;
	}
	
	public VariableDeclarationStatement getFoundVariableDeclarationStatement() {
		return foundVariableDeclarationStatement;
	}
}
