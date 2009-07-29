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
 * 判斷是否有Logging又有Throw，並記錄Logging的Message
 * @author Shiau
 *
 */
public class LoggingThrowAnalyzer extends RLBaseVisitor{
	private static Logger logger = LoggerFactory.getLogger(LoggingThrowAnalyzer.class);

	//AST Tree的root(檔案名稱)
	private CompilationUnit root;

	//是否要繼續偵測
	boolean isKeepTrace = false;
	
	//最底層Throw的Exception的Type
	private String baseException = "";
	
	//儲存所找到的ignore Exception 
	private List<CSMessage> loggingList = new ArrayList<CSMessage>();
	
	//Store使用者要偵測的library名稱和Method名稱
	TreeMap<String, Integer> libMap = new TreeMap<String, Integer>();
	
	//傳進來的是最底層的Method(即要找Logging和Throw)
	public LoggingThrowAnalyzer(CompilationUnit root, TreeMap<String,Integer> libMap) {
		this.root = root;
		this.libMap = libMap;
	}

	protected boolean visitNode(ASTNode node){
		try {
			switch (node.getNodeType()) {
				case ASTNode.CATCH_CLAUSE :
					//轉換成catch node
					CatchClause cc = (CatchClause) node;
					//取的catch(Exception e)其中的e
					SingleVariableDeclaration svd = (SingleVariableDeclaration) cc
					.getStructuralProperty(CatchClause.EXCEPTION_PROPERTY);

					//若為第一層的Method
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
	 * 尋找catch的節點,並且判斷節點內的Statement是否有OverLogging的情況
	 * @param statementTemp
	 * @param catchExcepiton 
	 */
	private void detectOverLogging(CatchClause cc, SingleVariableDeclaration svd) {
		List statementTemp = cc.getBody().statements();
		//有沒有Logging動作
		boolean isLogging = false;
		//暫時的OverLogging List，看有沒有Throw決定是不是OverLogging，再記錄
		List<CSMessage> tempList = new ArrayList<CSMessage>();

		//Trace Catch中每個Statement
		for (int i = 0; i < statementTemp.size(); i++) {
			//取得Expression statement
			if(statementTemp.get(i) instanceof ExpressionStatement) {
				ExpressionStatement statement = (ExpressionStatement) statementTemp.get(i);

				//判斷是否有Logging
				isLogging = judgeLogging(cc, svd, statement, tempList);
			}
			//判斷是不是Throw Statement
			if(statementTemp.get(i) instanceof ThrowStatement) {
				ThrowStatement throwState = (ThrowStatement) statementTemp.get(i);
				//若有Logging又有Throw Exception
				//Call它的Method也可能會Logging，即發生OverLogging，所以繼續往下Trace
				if (isLogging) {
					if (tempList != null)
						this.loggingList.addAll(tempList);

					//繼續Trace
					isKeepTrace = true;
					//紀錄最底層Method的Throw Exception Type
					if (throwState.getExpression() instanceof ClassInstanceCreation) {
						ClassInstanceCreation cic = (ClassInstanceCreation) throwState.getExpression();
						//若是throw new Exception，則去取它的Type
						baseException = cic.getType().toString();
					} else
						//若是throw e，則去取catch的Exception
						baseException = svd.getType().toString();
				}
			}
		}
	}

	/**
	 * 判斷是否有Logging
	 * @param statement
	 * @param loggingList 
	 */
	private boolean judgeLogging(CatchClause cc, SingleVariableDeclaration svd,
							ExpressionStatement statement, List<CSMessage> loggingList) {
		//偵測使用者所設定Logging的Library
		if (findBindingLib(statement)) {			
			addLoggingMessage(cc, svd, statement, loggingList);
			return true;
		}
		return false;
	}

	/**
	 * 用來找尋這個Catch Clause中，是否有使用者所設定Logging動作
	 */
	private Boolean findBindingLib(ExpressionStatement statement){
		ASTBinding visitor = new ASTBinding(libMap);
		statement.getExpression().accept(visitor);
		if(visitor.getResult()){
		//假如找到log4j or java.logger,就把之前找到的smell去掉
			return true;
		}else{
			return false;
		}
	}

	/**
	 * 增加Logging的訊息
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
	 * 根據StartPosition來取得行數
	 */
	private int getLineNumber(int pos) {
		if (root != null)
			return root.getLineNumber(pos);
		else
			return 0;
	}

	/**
	 * 取得是否要繼續Trace
	 */
	public boolean getIsKeepTrace() {
		return this.isKeepTrace;
	}

	/**
	 * 取得OverLogging 行數的資訊
	 * @return
	 */
	public List<CSMessage> getOverLoggingList() {
		return loggingList;
	}

	/**
	 * 取得最底層的Exception型態
	 * @return
	 */
	public String getBaseException() {
		return baseException;
	}
}
