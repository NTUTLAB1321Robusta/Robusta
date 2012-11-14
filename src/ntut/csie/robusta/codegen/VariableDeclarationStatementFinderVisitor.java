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
	/**	將找到的結果存在這裡 */
	private VariableDeclarationStatement foundVariableDeclarationStatement;
	
	/** 想要從這個method invocation找出宣告他的variable declaration statement。 */
	private MethodInvocation comparisingMethodInvocation;
	
	/** 只從這個TryStatement去尋找 */
	private TryStatement specifiedSearchingNode; 
	
	public VariableDeclarationStatementFinderVisitor(MethodInvocation methodInvocation) {
		foundVariableDeclarationStatement = null;
		comparisingMethodInvocation = methodInvocation;
		specifiedSearchingNode = null;
	}

	public boolean visit(VariableDeclarationFragment node) {
		// method invocation的變數可以串很長，我們只取第一個。
		SimpleName methodInvocationFirstVariableSimpleName = (SimpleName)comparisingMethodInvocation.getExpression();

		// 如果沒有SimpleName，表示他不是instance.method這種樣子。我假裝它是close(instance)的樣子
		if(methodInvocationFirstVariableSimpleName == null) {
			return false;
		}
		
		// 如果變數名字不一樣，就離開吧
		if (!node.getName().toString().equals(
				methodInvocationFirstVariableSimpleName.getFullyQualifiedName())) {		
			return true;
		}
		
		ASTNode possibleVariableDeclarationStatement = node.getParent();

		// 如果node的上層不是ASTNode.VARIABLE_DECLARATION_STATEMENT，也離開吧
		if (possibleVariableDeclarationStatement.getNodeType() != ASTNode.VARIABLE_DECLARATION_STATEMENT) {
			return true;
		}
		
		ASTNode possibleVariableDeclarationStatementMD = NodeUtils.getSpecifiedParentNode(possibleVariableDeclarationStatement, ASTNode.METHOD_DECLARATION);
		ASTNode comparisingMethodInvocationMD = NodeUtils.getSpecifiedParentNode(comparisingMethodInvocation, ASTNode.METHOD_DECLARATION);
		
		// 如果node跟possibleVariableDeclarationStatement在不同method declaration裡面，也離開吧
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
	 * 如果有指定TryStatement，就會比對兩者的StartPosition是否相等，相等就繼續往下找VariableDeclaration。
	 * 如果沒有指定TryStatement，就會每個TryStatement都往下找。
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
