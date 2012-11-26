package ntut.csie.csdet.visitor.aidvisitor;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.TryStatement;

/**
 * �ˬdFinally block�̭������귽��Method Invocation��instane�A
 * �O���O�bTry�~���A�åB�|�ߥX�ҥ~�C
 * 
 * ��md�haccept�A�åB�ǤJ�w�g���D��close�M��i�ӡC
 * mi�A�h���A�ݬO���O��match��instance
 * @author charles
 *
 */
public class CarelessClenupRaisedExceptionNotInTryCausedVisitor extends	ASTVisitor {

	private List<MethodInvocation> closeResources;
	
	private List<MethodInvocation> carelessCleanupMethod;
	
	/** MethodDeclaration�Ҧ������귽��method invocation��Binding��instance�C*/
	private List<IBinding> closeResourcesInstanceBinding;

	public CarelessClenupRaisedExceptionNotInTryCausedVisitor(List<MethodInvocation> closeResources) {
		this.closeResources = closeResources;
		carelessCleanupMethod = new ArrayList<MethodInvocation>();
		closeResourcesInstanceBinding = new ArrayList<IBinding>();
		for(MethodInvocation closeResource : closeResources) {
			SimpleName closeResourceDeclaredInstance = getDeclaredInstanceSimpleName(closeResource.getExpression());
			if(closeResourceDeclaredInstance != null) {
				closeResourcesInstanceBinding.add(closeResourceDeclaredInstance.resolveBinding());
			}
		}
	}
	
	/**
	 * �p�G�Oxx.close()���Φ��A�h�i�H�qxx��SimpleName���oBinding��instance�C
	 * @param expression
	 * @return
	 */
	private SimpleName getDeclaredInstanceSimpleName(Expression expression) {
		// �p�G�Oclose(xxx)���Φ��A�h�Ƕi�Ӫ�expression��null
		if(expression == null) {
			return null;
		}
		
		if(expression.getNodeType() == ASTNode.METHOD_INVOCATION) {
			MethodInvocation expressionChild = (MethodInvocation) expression;
			return getDeclaredInstanceSimpleName(expressionChild.getExpression());
		} else if (expression.getNodeType() == ASTNode.SIMPLE_NAME) {
			return (SimpleName) expression;
		}
		
		return null;
	}

	/**
	 * ����TryStatement���ˬd�C
	 */
	public boolean visit(TryStatement node) {
		return false;
	}
	
	public boolean visit(MethodInvocation node) {
		SimpleName nodeVariable = getDeclaredInstanceSimpleName(node.getExpression());
		// System.out.println(fis.toString()); �o��Node�i��N�|NULL
		if(nodeVariable == null) {
			return true;
		}
		int checkedExceptionLength = node.resolveMethodBinding().getExceptionTypes().length;
		for(int i = 0; i<closeResourcesInstanceBinding.size(); i++) {
			if((nodeVariable.resolveBinding().equals(closeResourcesInstanceBinding.get(i))) &&
			   (checkedExceptionLength != 0)) {
				// �Nclose���ʧ@�[�Jcareless cleanup �M��
				carelessCleanupMethod.add(closeResources.get(i));
				
				/* �N�[�L��close�ʧ@������ */
				closeResourcesInstanceBinding.remove(i);
				closeResources.remove(i);
				break;
			}
		}
		return true;
	}
	
	public List<MethodInvocation> getCarelessCleanupNodes() {
		return carelessCleanupMethod;
	}
}
