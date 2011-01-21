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

	/** visit��node�A���|�Q�নexpression�Ӱ��P�_ */
	private Expression _expression;
	
	/** �O�_���careless cleanup��statement */
	private boolean _isFoundCarelessCleanup;
	
	/** �ϥΪ̬O�_�n�D�����~���I�s���{���A���S��careless cleanup */
	private boolean _isDetectOutterMethodWithCarelessCleanup;
	
	private MethodInvocation _miNode;
	
	private List<ASTNode> _lstMethods;
	
	/**
	 * Constructor
	 * @param isDetectOutterMethodWithCarelessCleanup �ϥΪ̬O�_�n�����~���I�s���{���̡A���S��careless cleanup�����p
	 * @param lstMethods�@��ӵ{���X�̭��A�Ҧ�Method Name�����X
	 */
	public CloseMethodAnalyzer(boolean isDetectOutterMethodWithCarelessCleanup, List<ASTNode> lstMethods){
		_isDetectOutterMethodWithCarelessCleanup = isDetectOutterMethodWithCarelessCleanup;
		_lstMethods = lstMethods;
		_isFoundCarelessCleanup = false;	//��l�ƮɡA���w���򳣨S���
	}
	
	/**
	 * �O�_���careless cleanup��statement
	 * @return true: ���F false�G�S���
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
		//1. expression != null --> ��close(object)������
		//2. !isFromSource --> class���O�ϥΪ̦b�����}�o���{�����A�ҩw�q��class
		//3. methodName.equals("close") --> method���W�٬�"close"
		//4. !_isDetectOutterMethodWithCarelessCleanup --> �������~���{�����S��careless cleanup
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
//			//����if�O���O�b�̭�
//			for(int i = 0; i < _lstMethods.size(); i++){
//				System.out.println("M list: " + _lstMethods.get(i));
//			}
			return false;
		}
		return false;
	}
	
	/**
	 * �ѶǶi��visit��statement�A���^retrieve�L��parent node�A���MethodInvocation
	 * @return
	 */
	public MethodInvocation getMethodInvocation(){
		return _miNode;
	}
	
	/**
	 * �ˬd�A�{���̭��ҩI�s���~���{���A�O�_��careless cleanup���{�H
	 * @param methodList
	 * @return
	 */
	//TODO: �o��copy�L�Ӫ��{���X�]�Ӫ��F�A�ӭ��c�@�U(���ӮM��visitor pattern)
	private boolean isCallMethodWithCarelessCleanup(List<ASTNode> methodList){
		String methodInvName = _miNode.resolveMethodBinding().getName();
		String methodDecName;
		MethodDeclaration md;
		//�O�_��Method Declaration��Name�PMethod Invocation��Name�ۦP
		for (int i = 0; i < methodList.size(); i++) {
			//���oMethod Declaration���W��
			md = (MethodDeclaration) methodList.get(i);
			methodDecName = md.resolveBinding().getName();
			//�Y�W�٬ۦP,�h�B�z��Method Invocation
			if(methodDecName.equals(methodInvName)){
				//���o��Method Declaration���Ҧ�statement
				List<?> mdStatement = md.getBody().statements();
				//���o��Method Declaration��thrown exception name
				List<?> thrown = md.thrownExceptions();

				if (mdStatement.size() != 0) {
					for (int j = 0; j < mdStatement.size(); j++) {
						//��禡����try�`�I
						if(mdStatement.get(j) instanceof TryStatement){
							//���otry Block����Statement
							TryStatement trystat=(TryStatement) mdStatement.get(j);
							List<?> statementTemp=trystat.getBody().statements();
							//��statement���O�_��Method Invocation
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
						//��Method��ThrowException
						if(thrown.size()!=0){
							//object.close �Ҭ�Expression Statement
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
	 * �u��isCallMethodWithCarelessCleanup()�I�s�A�t�d�̫�@�B���P�_�A�ݥ~���{���O�_��careless cleanup
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
