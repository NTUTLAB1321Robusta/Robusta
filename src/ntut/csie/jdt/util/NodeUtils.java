package ntut.csie.jdt.util;

import java.io.Closeable;
import java.util.List;

import ntut.csie.csdet.preference.SmellSettings;
import ntut.csie.csdet.visitor.UserDefinedMethodAnalyzer;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SimpleName;
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
	
	public static boolean isMethodDeclarationThrowException(ASTNode node) {
		if(node.getNodeType() == ASTNode.COMPILATION_UNIT) {
			throw new RuntimeException("Abatract Syntax Tree traversing error. by Charles.");
		}
		
		if(node.getNodeType() == ASTNode.METHOD_DECLARATION) {
			if(((MethodDeclaration)node).thrownExceptions().size() == 0)
				return false;
			else
				return true;  
		}
		
		return(isMethodDeclarationThrowException(node.getParent()));
	}
	
	public static ITypeBinding[] getMethodInvocationThrownCheckedExceptions(MethodInvocation node) {
		// 如果使用者進行了快速修復，則會蒐集到ListRewrite的資訊，node.resolveMethodBinding()會變成null
		if(node.resolveMethodBinding() == null) {
			return null;
		}
		
		// visit原始程式碼的時候，可以蒐集到node.resolveMethodBinding()
		if(node.resolveMethodBinding().getExceptionTypes().length <= 0) {
			return null;
		}
		
		return node.resolveMethodBinding().getExceptionTypes();
	}
	
	/**
	 * 檢查method invocation的程式碼是不是關閉資源的動作
	 * @param root method invocation所在的java檔案
	 * @param node 要被檢查的程式碼
	 * @return
	 */
	public static boolean isCloseResourceMethodInvocation(CompilationUnit root, MethodInvocation node) {
		boolean userDefinedLibResult = false;
		boolean userDefinedResult = false;
		boolean userDefinedExtraRule = false;
		boolean defaultResult = false;
		
		UserDefinedMethodAnalyzer userDefinedMethodAnalyzer = new UserDefinedMethodAnalyzer(SmellSettings.SMELL_CARELESSCLEANUP);
		if(userDefinedMethodAnalyzer.analyzeLibrary(node)) {
			userDefinedLibResult = true;
		}
		
		if(userDefinedMethodAnalyzer.analyzeMethods(node)) {
			userDefinedResult = true;
		}
		
		if(userDefinedMethodAnalyzer.analyzeExtraRule(node, root)) {
			userDefinedExtraRule = true;
		}
		
		if(userDefinedMethodAnalyzer.getEnable()) {
			defaultResult = isNodeACloseCodeAndImplementatedCloseable(node);
		}
				
		return (userDefinedLibResult || userDefinedResult || userDefinedExtraRule || defaultResult);
	}
	
	/**
	 * 檢查是否實作Closeable#close的程式碼
	 * @param node 
	 * @return 如果這個node實作Closeable而且是close的動作，才會回傳True，其餘一律回傳False。
	 */
	public static boolean isNodeACloseCodeAndImplementatedCloseable(MethodInvocation node) {
		// 尋找method name為close
		if(!node.getName().toString().equals("close")) {
			return false;
		}

		/*
		 *	尋找這個close是不是實作Closeable 
		 */
		if (NodeUtils.isITypeBindingImplemented(node.resolveMethodBinding()
				.getDeclaringClass(), Closeable.class)) {
			return true;
		}

		return false;
	}
	
	/**
	 * 如果是xx.close()的形式，則可以從xx的SimpleName取得Binding的變數名稱
	 * @param expression
	 * @return
	 */
	public static SimpleName getMethodInvocationBindingVariableSimpleName(Expression expression) {
		// 如果是close(xxx)的形式，則傳進來的expression為null
		if(expression == null) {
			return null;
		}
		
		if(expression.getNodeType() == ASTNode.METHOD_INVOCATION) {
			MethodInvocation expressionChild = (MethodInvocation) expression;
			return getMethodInvocationBindingVariableSimpleName(expressionChild.getExpression());
		} else if (expression.getNodeType() == ASTNode.SIMPLE_NAME) {
			return (SimpleName) expression;
		}
		
		return null;
	}
}
