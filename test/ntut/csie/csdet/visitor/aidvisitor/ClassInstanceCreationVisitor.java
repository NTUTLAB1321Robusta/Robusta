package ntut.csie.csdet.visitor.aidvisitor;

import ntut.csie.jdt.util.NodeUtils;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
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
	private ClassInstanceCreation classInstanceCreation;
	private SimpleName declaringVariable; 
	public ClassInstanceCreationVisitor(MethodInvocation methodInvocation) {
		classInstanceCreation = null;
		declaringVariable = null;
		/*
		 * 找出這個MethodInvocation宣告的變數
		 */
		declaringVariable = NodeUtils.getMethodInvocationBindingVariableSimpleName(methodInvocation.getExpression());
	}
	
	public boolean visit(VariableDeclarationFragment node) {
		if(declaringVariable == null) {
			return false;
		}
		
		// 如果這個fis = new FileInputStream("")
		if(node.resolveBinding().equals(declaringVariable.resolveBinding())){
			classInstanceCreation = (ClassInstanceCreation) node.getInitializer();
			return false;
		}
		return true;
	}
	
	/**
	 * 找到宣告的地方
	 * @return
	 */
	public ClassInstanceCreation getClassInstanceCreation() {
		return classInstanceCreation;
	}
}
