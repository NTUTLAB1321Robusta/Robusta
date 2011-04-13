package ntut.csie.csdet.visitor;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

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
 * �P�_�O�_��Logging�S��Throw�A�ðO��Logging��Message
 * @author Shiau
 *
 */
public class LoggingThrowAnalyzer extends RLBaseVisitor{
	private static Logger logger = LoggerFactory.getLogger(LoggingThrowAnalyzer.class);

	//AST Tree��root(�ɮצW��)
	private CompilationUnit root;
	//Store�ϥΪ̭n������library�W�٩MMethod�W��
	TreeMap<String, Integer> libMap = new TreeMap<String, Integer>();
	//
	private boolean isDetTransEx = false;

	//�O�_�n�~�򰻴�
	private boolean isKeepTrace = false;
	//�x�s�ҧ�쪺ignore Exception 
	private List<CSMessage> loggingList = new ArrayList<CSMessage>();

	//�Ƕi�Ӫ��O�̩��h��Method(�Y�n��Logging�MThrow)
	public LoggingThrowAnalyzer(CompilationUnit root, TreeMap<String, Integer> libMap, boolean isDetTransEx) {
		this.root = root;
		this.libMap = libMap;
		this.isDetTransEx = isDetTransEx;
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
					detectOverLogging(cc, svd);
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
		//���S��Logging�ʧ@
		boolean isLogging = false;
		//�Ȯɪ�OverLogging List�A�ݦ��S��Throw�M�w�O���OOverLogging�A�A�O��
		List<CSMessage> tempList = new ArrayList<CSMessage>();

		//Trace Catch���C��Statement
		for (int i = 0; i < statementTemp.size(); i++) {
			//���oExpression statement
			if(statementTemp.get(i) instanceof ExpressionStatement) {
				ExpressionStatement statement = (ExpressionStatement) statementTemp.get(i);

				//�P�_�O�_��Logging
				if (judgeLogging(cc, svd, statement, tempList))
					isLogging = true;
			}
			//�P�_�O���OThrow Statement
			if(statementTemp.get(i) instanceof ThrowStatement) {
				ThrowStatement throwState = (ThrowStatement) statementTemp.get(i);
				//�Y��Logging�S��Throw Exception
				//Call����Method�]�i��|Logging�A�Y�o��OverLogging�A�ҥH�~�򩹤UTrace
				if (isLogging) {
					if (tempList != null)
						this.loggingList.addAll(tempList);
					//�~��Trace
					isKeepTrace = true;

					// �YMethod�૬(Throw New Exception)
					if (throwState.getExpression() instanceof ClassInstanceCreation) {
						ClassInstanceCreation cic = (ClassInstanceCreation) throwState.getExpression();

						List argumentList = cic.arguments();

						// �Y�������૬ �� �S���Ncatch exception�N�J(eg:RuntimeException(e))
						// �h���~�򰻴�
						if (!isDetTransEx ||
							argumentList.size() != 1 ||
							!argumentList.get(0).toString().equals(cc.getException().getName().toString()))
							isKeepTrace = false;
					}
				}
			}
		}
	}

	/**
	 * �P�_�O�_��Logging
	 * @param statement
	 * @param loggingList 
	 */
	private boolean judgeLogging(CatchClause cc, SingleVariableDeclaration svd,
							ExpressionStatement statement, List<CSMessage> loggingList) {
		//�����ϥΪ̩ҳ]�wLogging��Library
		if (findBindingLib(statement)) {			
			addLoggingMessage(cc, svd, statement, loggingList);
			return true;
		}
		return false;
	}

	/**
	 * �Ψӧ�M�o��Catch Clause���A�O�_���ϥΪ̩ҳ]�wLogging�ʧ@
	 */
	private Boolean findBindingLib(ExpressionStatement statement){
		ExpressionStatementAnalyzer visitor = new ExpressionStatementAnalyzer(libMap);
		statement.getExpression().accept(visitor);
		if(visitor.getResult()){
			return true;
		}else{
			return false;
		}
	}

	/**
	 * �W�[Logging���T��
	 * @param cc
	 * @param svd
	 * @param statement
	 * @param loggingList
	 */
	private void addLoggingMessage(CatchClause cc,SingleVariableDeclaration svd,
						ExpressionStatement statement, List<CSMessage> loggingList) {
		CSMessage csmsg = new CSMessage(RLMarkerAttribute.CS_OVER_LOGGING, svd.resolveBinding().getType(),											
										cc.toString(), cc.getStartPosition(),
										this.getLineNumber(statement.getStartPosition()), svd.getType().toString());
		loggingList.add(csmsg);
	}
	
	/**
	 * �ھ�StartPosition�Ө��o���
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
	 * ���oOverLogging ��ƪ���T
	 * @return
	 */
	public List<CSMessage> getOverLoggingList() {
		return loggingList;
	}
}
