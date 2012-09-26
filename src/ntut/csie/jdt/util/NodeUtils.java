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
	 * 判斷指定的class是否為特定interface的實作。
	 * @param ASTNode上的ITypeBinding，有可能是Class或是Interface
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
	 * 檢查MethodInvocation是否在finally裡面
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
	 * 判斷MethodInvocation傳入的參數是否有實作指定的介面
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
	 * 從輸入的節點開始，尋找特定的父節點。
	 * 如果找不到特定父節點，則回傳null
	 * @param startNode
	 * @param nodeType
	 * @return
	 */
	public static ASTNode getSpecifiedParentNode(ASTNode startNode, int nodeType) {
		ASTNode resultNode = null;
		if(startNode == null)
			return startNode;
		ASTNode parentNode = startNode.getParent();
		// 如果parentNode是null，表示傳進來的node已經是rootNode(CompilationUnit)
		if(parentNode != null) {
			while(parentNode.getNodeType() != nodeType) {
				parentNode = parentNode.getParent();
				// 無窮迴圈終止條件 - 已經沒有parentNode
				if (parentNode == null) {
					break;
				}
			}
			resultNode = parentNode; 
		}
		return resultNode;
	}
}
