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
 * 1. �Ψӧ�MExpressionStatement����
 * org.apache.log4j.Logger��java.util.logging.Logger
 * 
 * 2. Detect if ThrowStatement is not exist, it can return a list of dummy ExpressionStatement  
 * @author chewei
 */
public class ExpressionStatementAnalyzer extends RLBaseVisitor{
	final static public int LIBRARY = 1;
	final static public int METHOD = 2;
	final static public int LIBRARY_METHOD = 3;
	
	//�O�_����������Library
	private Boolean isFound;
	
	//�O�_�s�bDummyHandler�{�H
	private Boolean isFoundThrow;
	
	//Collect all the ExpressionStatements might turn to be DummyHandler
	private List<ExpressionStatement> dummyList;
	
	//�x�s����Library��Name�MMethod���W��
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
				//�̥~����try block�J��Dummy�w�g�|�[mark�@���A
				//�ҥHtry block�̭���try block�N���n�A������
				case ASTNode.TRY_STATEMENT:
					return false;
			
				//����Method
				case ASTNode.METHOD_INVOCATION:
					// DummyHandler�N���~�򩹤Utrace
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
	 * �ثe�i�H����e.printStackTrace(), System.out.println(), Log4j, Java's logger<br />
	 * �v�@�����_�ӡA�i��getDummyHandlerList
	 * @param node
	 * @return
	 */
	private boolean judgeMethodInvocation(ASTNode node) {
		MethodInvocation mi = (MethodInvocation)node;
		//���oMethod��Library�W��
		String libName = mi.resolveMethodBinding().getDeclaringClass().getQualifiedName();
		//���oMethod���W��
		String methodName = mi.resolveMethodBinding().getName();

		//�p�G�Ӧ榳Array(�pjava.util.ArrayList<java.lang.Boolean>)�A��<>���e����
		if (libName.indexOf("<") != -1)
			libName = libName.substring(0, libName.indexOf("<"));

		Iterator<String> libIt = libMap.keySet().iterator();
		//�P�_�O�_�n���� �B ���y�]�]�t������Library
		while(libIt.hasNext()){
			String temp = libIt.next();

			//�u����Library
			if (libMap.get(temp) == LIBRARY){
				//�YLibrary���פj�󰻴����סA�_�h���ۦP�������L
				if (libName.length() >= temp.length())
				{
					//����e�b�q���ת��W�٬O�_�ۦP
					if (libName.substring(0, temp.length()).equals(temp)) {
						addDummyWarning(node);
						return false;
					}
				}
			//�u����Method
			} else if (libMap.get(temp) == METHOD) {
				if (methodName.equals(temp)) {
					addDummyWarning(node);
					return false;
				}
			//����Library.Method���Φ�
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
	 * ���o�O�_����������Library
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

