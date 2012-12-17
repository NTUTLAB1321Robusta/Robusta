package ntut.csie.csdet.visitor.aidvisitor;

import java.util.Enumeration;
import java.util.Hashtable;

import ntut.csie.jdt.util.NodeUtils;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.ThrowStatement;
import org.eclipse.jdt.core.dom.TryStatement;

public class TryStatementExceptionsVisitor extends ASTVisitor {

	private TryStatement rootTryStatement;
	private Hashtable<String, Integer> tryThrowsExceptions;
	private Hashtable<String, Integer> catchedExeptions;
	private Hashtable<String, Integer> catchThrowsExeptions;
	private Hashtable<String, Integer> finallyThrowsExceptions;
	
	private int[] tryBlockRange;
	private int[] finallyBlockRange;

	public TryStatementExceptionsVisitor(TryStatement tryStatement) {
		rootTryStatement = tryStatement;
		tryThrowsExceptions = new Hashtable<String, Integer>();
		catchedExeptions = new Hashtable<String, Integer>();
		catchThrowsExeptions = new Hashtable<String, Integer>();
		finallyThrowsExceptions = new Hashtable<String, Integer>();
		tryBlockRange = new int[2];
		finallyBlockRange = new int[2];
		resolveRootTryStatementBlockRange();
	}
	
	/**
	 * 解析各Block的範圍，以利每個字節點做例外的歸類
	 */
	private void resolveRootTryStatementBlockRange() {
		ASTNode tryBlock = rootTryStatement.getBody();
		tryBlockRange[0] = tryBlock.getStartPosition();
		tryBlockRange[1] = tryBlockRange[0] + tryBlock.getLength();
		
		ASTNode finallyBlock = rootTryStatement.getFinally();
		if(finallyBlock != null) {
			finallyBlockRange[0] = finallyBlock.getStartPosition();
			finallyBlockRange[1] = finallyBlockRange[0] + finallyBlock.getLength();
		} else {
			finallyBlockRange[0] = 0;
			finallyBlockRange[1] = 0;			
		}
	}
	
	public boolean visit(TryStatement node) {
		if(node.equals(rootTryStatement)) {
			return true;
		}
		TryStatementExceptionsVisitor tVisitor = new TryStatementExceptionsVisitor(node);
		node.accept(tVisitor);
		String[] nestedTryTotalKey = tVisitor.getTotalExceptionStrings();
		Hashtable<String, Integer> thisNodeBelongs = getNodeExceptionContainer(node);
		for (int i = 0; i < nestedTryTotalKey.length; i++) {
			addException(nestedTryTotalKey[i], thisNodeBelongs);
		}
		return false;
	}
	
	public boolean visit(CatchClause node) {
		String catchedExceptionName = node.getException().getType().resolveBinding().getQualifiedName();
		addException(catchedExceptionName, catchedExeptions);
		return true;
	}
	
	public boolean visit(MethodInvocation node) {
		ITypeBinding[] checkedExceptions = node.resolveMethodBinding().getExceptionTypes();
		for(ITypeBinding exception : checkedExceptions) {
			String exceptionQualifiedName = exception.getQualifiedName();
			addException(exceptionQualifiedName, getNodeExceptionContainer(node));
		}
		return true;
	}
	
	/* ==========================================================================
	 * 在AST裡面，"throw e;" 與 "throw new Exception(e);"
	 * 都算是一個ThrowStatement的節點。
	 * 但是 "new Exception(e)" 是一個ClassInstanceCreation的節點。
	 * 
	 *  下面兩個visit功能，
	 *  visit(ClassInstanceCreation node) 希望蒐集 "throw new Exception(e);" 拋出的例外類型
	 *  visit(ThrowStatement node) 希望蒐集 "throw e;" 拋出的例外類型
	 ==========================================================================*/
	
	public boolean visit(ClassInstanceCreation node) {
		// 如果是丟出新的例外，也要蒐集起來
		ASTNode throwStatement = NodeUtils.getSpecifiedParentNode(node, ASTNode.THROW_STATEMENT);
		// 如果父節點是throw statement，這就是一個拋出例外的class instance creation
		if(throwStatement != null) {
			String typeQualifiedName = node.resolveTypeBinding().getQualifiedName();
			addException(typeQualifiedName, getNodeExceptionContainer(node));
		}
		return false;
	}
	
	public boolean visit(ThrowStatement node) {
		// 如果是throw e，此node的expression應該為simple name節點
		if(node.getExpression().getNodeType() != ASTNode.SIMPLE_NAME) {
			return true;
		}
		String bindingExceptoinQualifiedName = node.getExpression().resolveTypeBinding().getQualifiedName();
		addException(bindingExceptoinQualifiedName, getNodeExceptionContainer(node));
		return true;
	}
	
	private Hashtable<String, Integer> getNodeExceptionContainer(ASTNode node) {
		int nodeStartPosition = node.getStartPosition();
		int nodeEndPosition = nodeStartPosition + node.getLength();
		if ((nodeStartPosition > tryBlockRange[0]) && (nodeEndPosition < tryBlockRange[1])) {
			return tryThrowsExceptions;
		} else if((nodeStartPosition > finallyBlockRange[0]) && (nodeEndPosition < finallyBlockRange[1])) {
			return finallyThrowsExceptions;
		} else {
			return catchThrowsExeptions;
		}
	}
	
	private void addException(String exceptionQualifiedName, Hashtable<String, Integer>thrownExceptions) {
		if(thrownExceptions.containsKey(exceptionQualifiedName)) {
			Integer count = (Integer)thrownExceptions.get(exceptionQualifiedName);
			count++;
			thrownExceptions.put(exceptionQualifiedName, count);
		} else {
			thrownExceptions.put(exceptionQualifiedName, 1);
		}
	}
	
	public Hashtable<String, Integer> getTotalExceptions() {
		Enumeration<String> catchedExceptionKey = catchedExeptions.keys();
		while(catchedExceptionKey.hasMoreElements()) {
			String key = catchedExceptionKey.nextElement();
			if(tryThrowsExceptions.containsKey(key)) {
				tryThrowsExceptions.remove(key);
			}
		}
		
		Enumeration<String> catchThrowsExceptionKey = catchThrowsExeptions.keys();
		while(catchThrowsExceptionKey.hasMoreElements()) {
			String key = catchThrowsExceptionKey.nextElement();
			tryThrowsExceptions.put(key, 1);
		}
		
		Enumeration<String> finallyThrowsExceptionKey = finallyThrowsExceptions.keys();
		while(finallyThrowsExceptionKey.hasMoreElements()) {
			String key = finallyThrowsExceptionKey.nextElement();
			tryThrowsExceptions.put(key, 1);
		}
		
		return tryThrowsExceptions;
	}
	
	public String[] getTotalExceptionStrings() {
		String[] result = null;
		Hashtable<String, Integer> totalExceptions = getTotalExceptions();
		result = totalExceptions.keySet().toArray(new String[totalExceptions.size()]);
		return result;
	}
}
