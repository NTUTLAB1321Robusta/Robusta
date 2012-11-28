package ntut.csie.csdet.visitor.aidvisitor;

import ntut.csie.jdt.util.NodeUtils;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
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
	private ClassInstanceCreation classInstanceCreation;
	private SimpleName declaringVariable; 
	public ClassInstanceCreationVisitor(MethodInvocation methodInvocation) {
		classInstanceCreation = null;
		declaringVariable = null;
		/*
		 * ��X�o��MethodInvocation�ŧi���ܼ�
		 */
		declaringVariable = NodeUtils.getMethodInvocationBindingVariableSimpleName(methodInvocation.getExpression());
	}
	
	public boolean visit(VariableDeclarationFragment node) {
		if(declaringVariable == null) {
			return false;
		}
		
		// �p�G�o��fis = new FileInputStream("")
		if(node.resolveBinding().equals(declaringVariable.resolveBinding())){
			classInstanceCreation = (ClassInstanceCreation) node.getInitializer();
			return false;
		}
		return true;
	}
	
	/**
	 * ���ŧi���a��
	 * @return
	 */
	public ClassInstanceCreation getClassInstanceCreation() {
		return classInstanceCreation;
	}
}
