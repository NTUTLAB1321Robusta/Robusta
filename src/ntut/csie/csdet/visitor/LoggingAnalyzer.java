package ntut.csie.csdet.visitor;

import java.util.ArrayList;
import java.util.List;

import ntut.csie.csdet.data.CSMessage;
import ntut.csie.rleht.builder.RLMarkerAttribute;
import ntut.csie.rleht.common.RLBaseVisitor;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.ThrowStatement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ��AST�P�_�O�_�o��OverLogging�A�ðO��Logging��Message
 * @author Shiau
 *
 */
public class LoggingAnalyzer extends RLBaseVisitor{
	private static Logger logger = LoggerFactory.getLogger(LoggingAnalyzer.class);
	
	// �x�s�ҧ�쪺ignore Exception 
	private List<CSMessage> loggingList;
	
	//AST Tree��root(�ɮצW��)
	private CompilationUnit root;
	
	//�̩��hThrow��Exception��Type
	String baseException;
	
	//�P�_�O���O�����̩��h��Method
	boolean isBaseMethod = false;
	//�O�_��Logging
	boolean isLogging = false;
	//�O�_�n�~�򰻴�
	boolean isKeepTrace = false;

	public LoggingAnalyzer(CompilationUnit root, String baseException) {
		this.root = root;

		this.baseException = baseException;

		this.loggingList = new ArrayList<CSMessage>();
		
		//�Y�S���̩��h��Exception�A��ܭn�����̩��hMethod
		if (baseException == "")
			isBaseMethod = true;
	}

	protected boolean visitNode(ASTNode node){
		try {
			switch (node.getNodeType()) {
				case ASTNode.CATCH_CLAUSE :
					//�ഫ��catch node
					CatchClause cc = (CatchClause) node;
					//����catch(Exception e)�䤤��e
					SingleVariableDeclaration svd = (SingleVariableDeclaration) cc
					.getStructuralProperty(CatchClause.EXCEPTION_PROPERTY);

					//�Y���Ĥ@�h��Method
					if (isBaseMethod) {
						detectOverLogging(cc, svd);
					} else {
						String catchExcepiton = svd.getType().toString();
						//�YCatch�쪺Exception�P�W�@�h�ǨӪ�Exception���P�A�h������
						if (catchExcepiton.equals(baseException))
							detectOverLogging(cc, svd);
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

	/**
	 * �M��catch���`�I,�åB�P�_�`�I����Statement�O�_��OverLogging�����p
	 * @param statementTemp
	 * @param catchExcepiton 
	 */
	private void detectOverLogging(CatchClause cc, SingleVariableDeclaration svd) {
		List statementTemp = cc.getBody().statements();
		
		for (int i = 0; i < statementTemp.size(); i++) {
			//���oExpression statement,�]��e.printstackTrace
			if(statementTemp.get(i) instanceof ExpressionStatement) {
				ExpressionStatement statement = (ExpressionStatement) statementTemp.get(i);

				//�P�_�O�_��Logging
				judgeLogging(cc, svd, statement);
			}
			if(statementTemp.get(i) instanceof ThrowStatement) {
				ThrowStatement throwState = (ThrowStatement) statementTemp.get(i);
				//�Y���Ĥ@�Ӱ�����Method
				if (isBaseMethod) {
					//�Y��Logging�S��Throw Exception
					//Call����Method�]�i��|Logging�A�Y�o��OverLogging�A�ҥH�~�򩹤UTrace
					if (isLogging) {
						//�~��Trace
						isKeepTrace = true;
						//�����̩��hMethod��Throw Exception Type
						if (throwState.getExpression() instanceof ClassInstanceCreation) {
							ClassInstanceCreation cic = (ClassInstanceCreation) throwState.getExpression();
							//�Y�Othrow new Exception�A�h�h������Type
							baseException = cic.getType().toString();
						} else							
							//�Y�Othrow e�A�h�h��catch��Exception
							baseException = svd.getType().toString();
					}
				} else {
					//�P�_�O�_Throw new Exception�A���N���l��
					if (throwState.getExpression() instanceof ClassInstanceCreation)
						isKeepTrace = false;
					else
						isKeepTrace = true;
				}
			}
		}
	}

	/**
	 * �P�_�O�_��Logging
	 * @param statement
	 */
	private void judgeLogging(CatchClause cc, SingleVariableDeclaration svd, ExpressionStatement statement) {
		//����Statement�MprintStackTrace�O�_��Logging
		String st = statement.getExpression().toString();
		if(st.contains("printStackTrace")) {
			addLoggingMessage(cc, svd, statement);
			isLogging = true;
		}
	}
	
	/**
	 * �W�[Logging���T��
	 * @param cc
	 * @param svd
	 * @param statement
	 */
	private void addLoggingMessage(CatchClause cc,SingleVariableDeclaration svd, ExpressionStatement statement) {
		CSMessage csmsg = new CSMessage(RLMarkerAttribute.CS_OVER_LOGGING, svd.resolveBinding().getType(),											
										cc.toString(), cc.getStartPosition(),
										this.getLineNumber(statement.getStartPosition()), svd.getType().toString());
		this.loggingList.add(csmsg);
	}

	/**
	 * �ھ�startPosition�Ө��o���
	 */
	private int getLineNumber(int pos) {
		if (root != null)
			return root.getLineNumber(pos);
		else
			return 0;
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
	/**
	 * ���oOverLogging ��ƪ���T
	 * @return
	 */
	public List<CSMessage> getOverLoggingList() {
		return loggingList;
	}
	/**
	 * ���o�̩��h��Exception���A
	 * @return
	 */
	public String getBaseException() {
		return baseException;
	}
}
