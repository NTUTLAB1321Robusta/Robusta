package ntut.csie.csdet.visitor;

import java.util.List;
import java.util.TreeMap;

import ntut.csie.rleht.common.RLBaseVisitor;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.ThrowStatement;
import org.eclipse.jdt.core.dom.TryStatement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * �P�_�O�_��Logging�A�H�ΧP�_�O�_�n�~�򩹤W�hTrace
 * @author Shiau
 *
 */
public class LoggingAnalyzer extends RLBaseVisitor{
	private static Logger logger = LoggerFactory.getLogger(LoggingAnalyzer.class);
	//�O�_��Logging
	private boolean isLogging = false;
	//�O�_�n�~�򰻴�
	private boolean isKeepTrace = false;

	//Callee��Class�MMethod����T
	private String classInfo = "";
	private String methodInfo = "";
	//
	private boolean isDetTransEx = false;

	//�ϥΪ̩w�q��Log����(Key:library�W��,Value:Method�W��)
	TreeMap<String, Integer> libMap = new TreeMap<String, Integer>();

	//�D�̩��hMethod(�u�n��Logging)
	public LoggingAnalyzer(String classInfo, String methodInfo, TreeMap<String,Integer> libMap, boolean isDetTransEx) {
		this.classInfo = classInfo;
		this.methodInfo = methodInfo;
		this.libMap = libMap;
		this.isDetTransEx = isDetTransEx;
	}

	protected boolean visitNode(ASTNode node){
		try {
			switch (node.getNodeType()) {
				case ASTNode.TRY_STATEMENT:
					//�P�_Method�O�_�X�{�b�o��Try����
					processTryStatement(node);
					return true;
				default:
					return true;
			}
		} catch (Exception e) {
			logger.error("[visitNode] EXCEPTION ",e);
			return false;
		}
	}

	/**
	 * �P�_Callee��Method�O�_�X�{�b�o��Try����
	 * @param node
	 */
	private void processTryStatement(ASTNode node) {
		TryStatement trystat = (TryStatement) node;
		List trys = trystat.getBody().statements();
		for (int i = 0;i < trys.size(); i++) {
			
			CalleeMethodVisitor f = new CalleeMethodVisitor();
			trystat.accept(f);
			
			if (f.isFoundCallee) {
				//�T�wMethod�b�o��Try�`�I������A�h��catch�`�I�A��������finally block
				List catchList = trystat.catchClauses();
				CatchClause cc = null;
				for (int j = 0; j < catchList.size(); j++) {
					// TODO: ���ӧP�_��Catch clause�O�_�P�ߥX���ҥ~�����ۦP
					cc = (CatchClause) catchList.get(j);
					// �B�zCatchClause(�P�_Exception�O�_���૬�A�ð������S��Logging)
					processCatchStatement(cc);
				}
			}

			// �L�h����: ���O��Visitor�P�_Callee�O�_�bTry����
			//�Ytry��Statement��ExpressionStatement
//			if (trys.get(i) instanceof ExpressionStatement) {
//				ExpressionStatement expression = (ExpressionStatement) trys.get(i);
//				//�Y��ExpressionStatement��MethodInvocation
//				if (expression.getExpression() instanceof MethodInvocation) {
//					MethodInvocation mi = (MethodInvocation) expression.getExpression();
//
//					//���o��MethodInvocation��
//					String classInfo = mi.resolveMethodBinding().getDeclaringClass().getQualifiedName();
//					//�YTry����CalleeMethod(�X�{Class���A�PMethod�W�٬ۦP��Method)
//					if (mi.getName().toString().equals(methodInfo) && classInfo.equals(this.classInfo)) {
//						//�T�wMethod�b�o��Try�`�I������A�h��catch�`�I�A��������finally block
//						List catchList = trystat.catchClauses();
//						CatchClause cc = null;
//						for (int j = 0; j < catchList.size(); j++) {
//							cc = (CatchClause) catchList.get(j);
//							//�B�zCatchClause(�P�_Exception�O�_���૬�A�ð������S��Logging)
//							processCatchStatement(cc);
//						}
//					}
//				}
//			}
		}
	}
	
	/**
	 * �P�_Exception�O�_���૬�A�ð������S��Logging
	 * @param node
	 */
	private void processCatchStatement(ASTNode node) {
		//�ഫ��catch node
		CatchClause cc = (CatchClause) node;
		//����catch(Exception e)�䤤��e
		SingleVariableDeclaration svd = (SingleVariableDeclaration) cc.getStructuralProperty(CatchClause.EXCEPTION_PROPERTY);

		//�Y���Ĥ@�h��Method
		String catchExcepiton = svd.getType().toString();

		//����Catch���e
		detectOverLogging(cc);
	}

	/**
	 * �M��catch���`�I�åB�P�_�`�I����Statement�A�O�_��Logging�H�έn���n�~�򩹤W�hTrace
	 * @param cc
	 */
	private void detectOverLogging(CatchClause cc) {
		List statementTemp = cc.getBody().statements();

		for (int i = 0; i < statementTemp.size(); i++) {
			//���oExpression statement
			if(statementTemp.get(i) instanceof ExpressionStatement) {
				ExpressionStatement statement = (ExpressionStatement) statementTemp.get(i);

				//�P�_�O�_��Logging
				judgeLogging(statement);
			}

			//�P�_���S��Throw�A�M�w�n���n�~�~Trace
			if(statementTemp.get(i) instanceof ThrowStatement) {
				isKeepTrace = true;

				ThrowStatement throwState = (ThrowStatement) statementTemp.get(i);
				//�P�_�O�_��Throw new Exception
				if (throwState.getExpression() instanceof ClassInstanceCreation) {
					ClassInstanceCreation cic = (ClassInstanceCreation) throwState.getExpression();
					List argumentList = cic.arguments();

					//�Y�������૬ �� �S���Ncatch exception�N�J(eg:RuntimeException(e))
					//�h���~�򰻴�
					if (!isDetTransEx ||
						argumentList.size() < 1 ||
						//argumentList.size() != 1 ||
						!argumentList.get(0).toString().equals(cc.getException().getName().toString()))
						isKeepTrace = false;
				}
			}
		}
	}

	/**
	 * �Ψӧ�M�o��Catch Clause���O�_��logging
	 * @param statement
	 */
	private void judgeLogging(ExpressionStatement statement) {
		ExpressionStatementAnalyzer visitor = new ExpressionStatementAnalyzer(libMap);
		statement.accept(visitor);

		if (visitor.getResult()) {
			isLogging = true;
		}
	}

	/**
	 * ���o�O�_�n�~��Trace
	 */
	public boolean getIsKeepTrace() {
		return this.isKeepTrace;
	}

	/**
	 * �^�ǬO�_��Logging
	 * @return
	 */
	public boolean getIsLogging() {
		return isLogging;
	}

	/** �M��Callee��Method�O�_�bTry���� **/
	class CalleeMethodVisitor extends RLBaseVisitor {
		// �O�_�����w��Method
		private boolean isFoundCallee = false;

		protected boolean visitNode(ASTNode node){
			try {
				switch (node.getNodeType()) {
					case ASTNode.METHOD_INVOCATION:
						MethodInvocation mi = (MethodInvocation) node;

						// TODO:�@�ثe�u�����Method�O�_�@�ˡAClass��T�������
						// ���o��MethodInvocation��
						String className = mi.resolveMethodBinding().getDeclaringClass().getQualifiedName();
						if (mi.getName().toString().equals(methodInfo)) {
							isFoundCallee = true;
						}
						return true;

					default:
						return true;
				}
			} catch (Exception e) {
				logger.error("[visitNode] EXCEPTION ",e);
				return false;
			}
		}

		/** @return	�O�_���Callee **/
		public boolean isFoundCallee() {
			return isFoundCallee;
		}
	}
}
