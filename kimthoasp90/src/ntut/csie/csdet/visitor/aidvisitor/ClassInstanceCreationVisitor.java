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
 * 在MethodDeclaration裡面，尋找某一個method invocation的class instance creation node。
 * (new FileInputStream(""))
 * @author charles
 *
 */
public class ClassInstanceCreationVisitor extends ASTVisitor {
	/** 尋找建立這個Instance的node (new FileInputStream("")) */
	private ClassInstanceCreation cic = null;
	private SimpleName declaringVariable = null;
	private List<Expression> argumentsOfMethodInvocation = null;
	
	public ClassInstanceCreationVisitor(MethodInvocation methodInvocation) {
		/*
		 * 找出這個MethodInvocation宣告的變數
		 */
		declaringVariable = NodeUtils.getMethodInvocationBindingVariableSimpleName(methodInvocation.getExpression());
		argumentsOfMethodInvocation = methodInvocation.arguments();
	}
	
//	public boolean visit(VariableDeclarationFragment node) {
//		if(declaringVariable == null) {
//			return false;
//		}
//		
//		// 如果這個fis = new FileInputStream("")
//		if(node.resolveBinding().equals(declaringVariable.resolveBinding())){
//			classInstanceCreation = (ClassInstanceCreation) node.getInitializer();
//			return false;
//		}
//		return true;
//	}
	
	public boolean visit(ClassInstanceCreation node) {
		int parentNodeType = node.getParent().getNodeType();
		switch(parentNodeType) {
			// case: 宣告物件，並 create instance 指定給某個參考
			case ASTNode.VARIABLE_DECLARATION_FRAGMENT:
				if(declaringVariable == null) {
					return false;
				}
				VariableDeclarationFragment vdf = (VariableDeclarationFragment)node.getParent();
				// 如果這個fis = new FileInputStream("")
				if(vdf.resolveBinding().equals(declaringVariable.resolveBinding())){
					cic = node;
					return false;
				} else {
					for(Expression name : argumentsOfMethodInvocation) {
						if(name.toString().equalsIgnoreCase(vdf.getName().toString())) {
							cic = node;
							return false;
						}
					}
				}
				break;
			// case: create instance 並指定給某個參考
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
				if(declaringVariable == null) {
					return false;
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
	 * 找到宣告的地方
	 * @return
	 */
	public ClassInstanceCreation getClassInstanceCreation() {
		return cic;
	}
}
