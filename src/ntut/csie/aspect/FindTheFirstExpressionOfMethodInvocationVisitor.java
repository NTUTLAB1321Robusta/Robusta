package ntut.csie.aspect;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SimpleName;

public class FindTheFirstExpressionOfMethodInvocationVisitor extends ASTVisitor {
	
	SimpleName theFirstExpression = null;
	@Override
	public boolean visit(MethodInvocation invocation) {
		Expression expression = invocation.getExpression();
		if(expression.getNodeType() == ASTNode.METHOD_INVOCATION){
			return true;
		}else{
			theFirstExpression = (SimpleName)expression;
			return false;
		}
	}
	
	public String getTheFirstExpression(){
		return theFirstExpression.resolveTypeBinding().getName().toString();
	}
}
