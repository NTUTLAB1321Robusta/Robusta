package ntut.csie.aspect;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.TypeDeclaration;

public class FindExpressionObjectOfMethodInvocationVisitor extends
		ASTVisitor {

	String objectName = ""; 
	String objectPackage = "";
	@Override
	public boolean visit(MethodInvocation invocation) {
		Expression expression = invocation.getExpression();
		if (expression != null) {
			if (expression.getNodeType() == ASTNode.METHOD_INVOCATION) {
				return true;
			} else {
				objectPackage = expression.resolveTypeBinding().getBinaryName();
				objectName = expression.resolveTypeBinding().getName();
				return false;
			}
		} else {
			IMethodBinding  method = invocation.resolveMethodBinding();
			objectPackage = method.getDeclaringClass().getBinaryName();
			objectName = method.getDeclaringClass().getName();
			return false;
		}
	}
	
	public String getObjectName(){
		return objectName;
	}

	public String getObjectPackageName() {
		return objectPackage;
	}
}
