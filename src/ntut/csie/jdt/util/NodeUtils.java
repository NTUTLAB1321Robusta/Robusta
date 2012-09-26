package ntut.csie.jdt.util;

import java.util.List;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.TryStatement;

public class NodeUtils {
	/**
	 * �P�_���w��class�O�_���S�winterface����@�C
	 * @param ASTNode�W��ITypeBinding�A���i��OClass�άOInterface
	 * @param looking4interface
	 * @return
	 */
	public static boolean isITypeBindingImplemented(ITypeBinding bindingClass, Class<?> looking4Interface) {
		if (bindingClass == null || bindingClass.getQualifiedName()
						.equals(Object.class.getName())) {
			return false;
		}
		
		ITypeBinding[] interfaces = bindingClass.getInterfaces();
		if(interfaces != null) {
			for(int i = 0; i<interfaces.length; i++) {
				if(interfaces[i].getName().equals(looking4Interface.getSimpleName())){
					return true;
				}
			}
		}
		return isITypeBindingImplemented(bindingClass.getSuperclass(), looking4Interface);
	}
	
	/**
	 * �ˬdMethodInvocation�O�_�bfinally�̭�
	 * @param node
	 * @return
	 */
	public static boolean isMethodInvocationInFinally(MethodInvocation node) {
		ASTNode astNode = getSpecifiedParentNode(node, ASTNode.TRY_STATEMENT);
		if(astNode != null) {
			TryStatement ts = (TryStatement)astNode;
			if(ts.getFinally() != null) {
				List<?> statements = ts.getFinally().statements();
				for(Object object : statements) {
					Statement statement = (Statement)object;
					if(statement.getStartPosition() == node.getStartPosition())
						return true;
				}
			}
		}
		return false;
	}
	
	/**
	 * �P�_MethodInvocation�ǤJ���ѼƬO�_����@���w������
	 * @param node
	 * @param looking4Interface
	 * @return
	 */
	public static boolean isParameterImplemented(MethodInvocation node, Class<?> looking4Interface) {
		List<?> arguments = node.arguments();
		for(Object object : arguments) {
			Expression argument = (Expression)object;
			if(NodeUtils.isITypeBindingImplemented(argument.resolveTypeBinding(), looking4Interface)) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * �q��J���`�I�}�l�A�M��S�w�����`�I�C
	 * �p�G�䤣��S�w���`�I�A�h�^��null
	 * @param startNode
	 * @param nodeType
	 * @return
	 */
	public static ASTNode getSpecifiedParentNode(ASTNode startNode, int nodeType) {
		ASTNode resultNode = null;
		if(startNode == null)
			return startNode;
		ASTNode parentNode = startNode.getParent();
		// �p�GparentNode�Onull�A��ܶǶi�Ӫ�node�w�g�OrootNode(CompilationUnit)
		if(parentNode != null) {
			while(parentNode.getNodeType() != nodeType) {
				parentNode = parentNode.getParent();
				// �L�a�j��פ���� - �w�g�S��parentNode
				if (parentNode == null) {
					break;
				}
			}
			resultNode = parentNode; 
		}
		return resultNode;
	}
}
