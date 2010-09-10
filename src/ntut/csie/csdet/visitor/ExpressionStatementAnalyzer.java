package ntut.csie.csdet.visitor;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;

import ntut.csie.rleht.common.RLBaseVisitor;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.MethodInvocation;

/**
 * 1. 用來找尋ExpressionStatement中的
 * org.apache.log4j.Logger及java.util.logging.Logger
 * 
 * 2. Detect if ThrowStatement is not exist, it can return a list of dummy ExpressionStatement  
 * @author chewei
 */
public class ExpressionStatementAnalyzer extends RLBaseVisitor{
	final static public int LIBRARY = 1;
	final static public int METHOD = 2;
	final static public int LIBRARY_METHOD = 3;
	
	//是否找到欲偵測的Library
	private Boolean isFound;
	
	//是否存在DummyHandler現象
	private Boolean isFoundThrow;
	
	//Collect all the ExpressionStatements might turn to be DummyHandler
	private List<ExpressionStatement> dummyList;
	
	//儲存偵測Library的Name和Method的名稱
	private TreeMap<String, Integer> libMap;

	public ExpressionStatementAnalyzer(TreeMap<String, Integer> libMap) {
		this.libMap = libMap;
		this.isFound = false;
		this.isFoundThrow = false;
		this.dummyList = new ArrayList<ExpressionStatement>();
	}

	/**
	 * Override the RLBAseVisitor
	 */
	protected boolean visitNode(ASTNode node){
		try {
			switch (node.getNodeType()) {
				//最外面的try block遇到Dummy已經會加mark一次，
				//所以try block裡面的try block就不要再次偵測
				case ASTNode.TRY_STATEMENT:
					return false;
			
				//偵測Method
				case ASTNode.METHOD_INVOCATION:
					// DummyHandler就不繼續往下trace
					return judgeMethodInvocation(node);

				case ASTNode.THROW_STATEMENT:	
					this.isFoundThrow = true;
					return false;

				default:
					return true;
			}
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * 目前可以偵測e.printStackTrace(), System.out.println(), Log4j, Java's logger<br />
	 * 逐一紀錄起來，可由getDummyHandlerList
	 * @param node
	 * @return
	 */
	private boolean judgeMethodInvocation(ASTNode node) {
		MethodInvocation mi = (MethodInvocation)node;
		//取得Method的Library名稱
		String libName = mi.resolveMethodBinding().getDeclaringClass().getQualifiedName();
		//取得Method的名稱
		String methodName = mi.resolveMethodBinding().getName();

		//如果該行有Array(如java.util.ArrayList<java.lang.Boolean>)，把<>內容拿掉
		if (libName.indexOf("<") != -1)
			libName = libName.substring(0, libName.indexOf("<"));

		Iterator<String> libIt = libMap.keySet().iterator();
		//判斷是否要偵測 且 此句也包含欲偵測Library
		while(libIt.hasNext()){
			String temp = libIt.next();

			//只偵測Library
			if (libMap.get(temp) == LIBRARY){
				//若Library長度大於偵測長度，否則表不相同直接略過
				if (libName.length() >= temp.length())
				{
					//比較前半段長度的名稱是否相同
					if (libName.substring(0, temp.length()).equals(temp)) {
						addDummyWarning(node);
						return false;
					}
				}
			//只偵測Method
			} else if (libMap.get(temp) == METHOD) {
				if (methodName.equals(temp)) {
					addDummyWarning(node);
					return false;
				}
			//偵測Library.Method的形式
			} else if (libMap.get(temp) == LIBRARY_METHOD) {
				int pos = temp.lastIndexOf(".");
				if (libName.equals(temp.substring(0, pos)) &&
					methodName.equals(temp.substring(pos + 1))) {
					addDummyWarning(node);
					return false;
				}
			}						
		}
		return true;
	}


	/**
	 * Add warning message when the found condition which is defined by user is conformed.
	 * @param node
	 */
	private void addDummyWarning(ASTNode node){
		if (node.getParent() instanceof ExpressionStatement) {
			ExpressionStatement statement = (ExpressionStatement) node.getParent();
			isFound = true;
			this.dummyList.add(statement);
		}
	}
	
	/**
	 * 取得是否找到欲偵測的Library
	 */
	public Boolean getResult(){
		return isFound;
	}
	
	/**
	 * Get the list of ExpresstionStatement might cause dummy handler situation.
	 * @return List&lt;ExpresstionStatemet&gt;, if there is no ThrowStatement <br /><br />
	 * 		   Null, if ThrowStatement is found.
	 */
	public List<ExpressionStatement> getDummyHandlerList(){
		if (this.isFoundThrow) {
			return null;
		} else{
			return dummyList;
		}
	}
}

