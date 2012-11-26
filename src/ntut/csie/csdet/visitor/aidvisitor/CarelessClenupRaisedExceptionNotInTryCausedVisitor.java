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
 * 檢查Finally block裡面關閉資源之Method Invocation的instane，
 * 是不是在Try外面，並且會拋出例外。
 * 
 * 用md去accept，並且傳入已經知道的close清單進來。
 * mi再去比對，看是不是有match的instance
 * @author charles
 *
 */
public class CarelessClenupRaisedExceptionNotInTryCausedVisitor extends	ASTVisitor {

	private List<MethodInvocation> closeResources;
	
	private List<MethodInvocation> carelessCleanupMethod;
	
	/** MethodDeclaration所有關閉資源的method invocation所Binding的instance。*/
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
	 * 如果是xx.close()的形式，則可以從xx的SimpleName取得Binding的instance。
	 * @param expression
	 * @return
	 */
	private SimpleName getDeclaredInstanceSimpleName(Expression expression) {
		// 如果是close(xxx)的形式，則傳進來的expression為null
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
	 * 忽略TryStatement的檢查。
	 */
	public boolean visit(TryStatement node) {
		return false;
	}
	
	public boolean visit(MethodInvocation node) {
		SimpleName nodeVariable = getDeclaredInstanceSimpleName(node.getExpression());
		// System.out.println(fis.toString()); 這種Node可能就會NULL
		if(nodeVariable == null) {
			return true;
		}
		int checkedExceptionLength = node.resolveMethodBinding().getExceptionTypes().length;
		for(int i = 0; i<closeResourcesInstanceBinding.size(); i++) {
			if((nodeVariable.resolveBinding().equals(closeResourcesInstanceBinding.get(i))) &&
			   (checkedExceptionLength != 0)) {
				// 將close的動作加入careless cleanup 清單
				carelessCleanupMethod.add(closeResources.get(i));
				
				/* 將加過的close動作移除掉 */
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
