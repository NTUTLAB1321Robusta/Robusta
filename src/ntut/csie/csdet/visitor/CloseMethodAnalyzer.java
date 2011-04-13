package ntut.csie.csdet.visitor;

import java.util.List;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.TryStatement;

public class CloseMethodAnalyzer extends ASTVisitor {

	/** visit的node，都會被轉成expression來做判斷 */
	private Expression _expression;
	
	/** 是否找到careless cleanup的statement */
	private boolean _isFoundCarelessCleanup;
	
	/** 使用者是否要求偵測外部呼叫的程式，有沒有careless cleanup */
	private boolean _isDetectOutterMethodWithCarelessCleanup;
	
	private MethodInvocation _miNode;
	
	private List<ASTNode> _lstMethods;
	
	/**
	 * Constructor
	 * @param isDetectOutterMethodWithCarelessCleanup 使用者是否要偵測外部呼叫的程式裡，有沒有careless cleanup的情況
	 * @param lstMethods　原來程式碼裡面，所有Method Name的集合
	 */
	public CloseMethodAnalyzer(boolean isDetectOutterMethodWithCarelessCleanup, List<ASTNode> lstMethods){
		_isDetectOutterMethodWithCarelessCleanup = isDetectOutterMethodWithCarelessCleanup;
		_lstMethods = lstMethods;
		_isFoundCarelessCleanup = false;	//初始化時，假定什麼都沒找到
	}
	
	/**
	 * 是否找到careless cleanup的statement
	 * @return true: 找到； false：沒找到
	 */
	public boolean isFoundCarelessCleanup(){
		return this._isFoundCarelessCleanup;
	}
	
	/**
	 * Check the method throws exception or not.
	 * @param mi
	 * @return true if mi throws exception. false if don't throw.
	 */
	public boolean isMethodInvocatoinWithException(MethodInvocation mi){
		if(mi.resolveMethodBinding().getExceptionTypes().length != 0){
			return true;
		}
		return false;
	}
	
	public boolean visit(MethodInvocation miNode){
		this._expression = miNode.getExpression();
		IMethodBinding methodBinding = miNode.resolveMethodBinding();
		if (methodBinding == null)	return false;

		boolean isFromSource = methodBinding.getDeclaringClass().isFromSource();
		String methodName = miNode.resolveMethodBinding().getName();
		//1. expression != null --> 為close(object)的類型
		//2. !isFromSource --> class不是使用者在此次開發的程式中，所定義的class
		//3. methodName.equals("close") --> method的名稱為"close"
		//4. !_isDetectOutterMethodWithCarelessCleanup --> 不偵測外部程式有沒有careless cleanup
		if((this._expression != null) && (!isFromSource) && (methodName.equals("close")) && (!_isDetectOutterMethodWithCarelessCleanup)){
			this._isFoundCarelessCleanup = true;
			_miNode = miNode;
			return true;
		}else if(_isDetectOutterMethodWithCarelessCleanup){
			_miNode = miNode;

			if(isCallMethodWithCarelessCleanup(_lstMethods)){
				this._isFoundCarelessCleanup = true;
				return true;
			}
//			//測試if是不是在裡面
//			for(int i = 0; i < _lstMethods.size(); i++){
//				System.out.println("M list: " + _lstMethods.get(i));
//			}
			return false;
		}
		return false;
	}
	
	/**
	 * 由傳進來visit的statement，往回retrieve他的parent node，找到MethodInvocation
	 * @return
	 */
	public MethodInvocation getMethodInvocation(){
		return _miNode;
	}
	
	/**
	 * 檢查，程式裡面所呼叫的外部程式，是否有careless cleanup的現象
	 * @param methodList
	 * @return
	 */
	//TODO: 這個copy過來的程式碼也太長了，該重構一下(應該套用visitor pattern)
	private boolean isCallMethodWithCarelessCleanup(List<ASTNode> methodList){
		String methodInvName = _miNode.resolveMethodBinding().getName();
		String methodDecName;
		MethodDeclaration md;
		//是否有Method Declaration的Name與Method Invocation的Name相同
		for (int i = 0; i < methodList.size(); i++) {
			//取得Method Declaration的名稱
			md = (MethodDeclaration) methodList.get(i);
			methodDecName = md.resolveBinding().getName();
			//若名稱相同,則處理該Method Invocation
			if (methodDecName.equals(methodInvName)) {
				// 防止Interface之類的Method Declaration出錯
				if (md.getBody() == null)
					continue;
				//取得該Method Declaration的所有statement
				List<?> mdStatement = md.getBody().statements();
				//取得該Method Declaration的thrown exception name
				List<?> thrown = md.thrownExceptions();

				if (mdStatement.size() != 0) {
					for (int j = 0; j < mdStatement.size(); j++) {
						//找函式內的try節點
						if(mdStatement.get(j) instanceof TryStatement){
							//取得try Block內的Statement
							TryStatement trystat=(TryStatement) mdStatement.get(j);
							List<?> statementTemp=trystat.getBody().statements();
							//找statement內是否有Method Invocation
								if(!statementTemp.isEmpty()){
									Statement statement;
									for (int k = 0; k < statementTemp.size(); k++) {
										if(statementTemp.get(k) instanceof Statement){
											statement = (Statement) statementTemp.get(k);
											return isOutterMethodWithCarelessCleanup(statement);
										}
									}
								}
						}
						//找Method有ThrowException
						if(thrown.size()!=0){
							//object.close 皆為Expression Statement
							if(mdStatement.get(j) instanceof ExpressionStatement){
								ExpressionStatement es=(ExpressionStatement) mdStatement.get(j);
								return isOutterMethodWithCarelessCleanup(es);
							}
						}
					}
				}
			}
		}
		return false;
	}

	/**
	 * 只由isCallMethodWithCarelessCleanup()呼叫，負責最後一步的判斷，看外部程式是否有careless cleanup
	 * @param st
	 * @return
	 */
	private boolean isOutterMethodWithCarelessCleanup(Statement st){
		CloseMethodAnalyzer closeMethodAnalyzer = new CloseMethodAnalyzer(false, this._lstMethods);
		st.accept(closeMethodAnalyzer);
		if(closeMethodAnalyzer.isFoundCarelessCleanup()){
			return true;
		}
		return false;
	}
}
