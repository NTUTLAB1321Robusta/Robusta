package ntut.csie.csdet.visitor.aidvisitor;

import java.util.List;

import ntut.csie.jdt.util.NodeUtils;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

/**
 * �bMethodDeclaration�̭��A�M��Y�@��method invocation��class instance creation node�C
 * (new FileInputStream(""))
 * @author charles
 *
 */
public class ClassInstanceCreationVisitor extends ASTVisitor {
	/** �M��إ߳o��Instance��node (new FileInputStream("")) */
	private ClassInstanceCreation cic = null;
	private SimpleName declaringVariable = null;
	private List<Expression> argumentsOfMethodInvocation = null;
	
	public ClassInstanceCreationVisitor(MethodInvocation methodInvocation) {
		/*
		 * ��X�o��MethodInvocation�ŧi���ܼ�
		 */
		declaringVariable = NodeUtils.getMethodInvocationBindingVariableSimpleName(methodInvocation.getExpression());
		argumentsOfMethodInvocation = methodInvocation.arguments();
	}
	
//	public boolean visit(VariableDeclarationFragment node) {
//		if(declaringVariable == null) {
//			return false;
//		}
//		
//		// �p�G�o��fis = new FileInputStream("")
//		if(node.resolveBinding().equals(declaringVariable.resolveBinding())){
//			classInstanceCreation = (ClassInstanceCreation) node.getInitializer();
//			return false;
//		}
//		return true;
//	}
	
	public boolean visit(ClassInstanceCreation node) {
		int parentNodeType = node.getParent().getNodeType();
		switch(parentNodeType) {
			case ASTNode.VARIABLE_DECLARATION_FRAGMENT:
				if(declaringVariable == null) {
					return false;
				}
				VariableDeclarationFragment vdf = (VariableDeclarationFragment)node.getParent();
				// �p�G�o��fis = new FileInputStream("")
				if(vdf.resolveBinding().equals(declaringVariable.resolveBinding())){
					cic = node;
					return false;
				}
				break;
			case ASTNode.ASSIGNMENT:
				Assignment assignment = (Assignment) node.getParent();
				Expression leftHandside = assignment.getLeftHandSide();
				SimpleName leftSimpleName = null;
				if (leftHandside.getNodeType() == ASTNode.SIMPLE_NAME) {
					leftSimpleName = (SimpleName) leftHandside;
				}
				if (leftSimpleName == null) {
					return true;
				}
				if (leftSimpleName.resolveBinding().equals(
						declaringVariable.resolveBinding())) {
					cic = node;
				}
				if (argumentsOfMethodInvocation != null) {
					for(Expression expression : argumentsOfMethodInvocation ) {
						if(leftSimpleName.toString().equals(expression.toString())) {
							cic = node;
						}
					}
				}
				break;
			default:
				return true;
		}
		return false;
	}
	
	/**
	 * ���ŧi���a��
	 * @return
	 */
	public ClassInstanceCreation getClassInstanceCreation() {
		return cic;
	}
}
