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
 * 用AST判斷是否發生OverLogging，並記錄Logging的Message
 * @author Shiau
 *
 */
public class LoggingAnalyzer extends RLBaseVisitor{
	private static Logger logger = LoggerFactory.getLogger(LoggingAnalyzer.class);
	
	// 儲存所找到的ignore Exception 
	private List<CSMessage> loggingList;
	
	//AST Tree的root(檔案名稱)
	private CompilationUnit root;
	
	//最底層Throw的Exception的Type
	String baseException;
	
	//判斷是不是偵測最底層的Method
	boolean isBaseMethod = false;
	//是否有Logging
	boolean isLogging = false;
	//是否要繼續偵測
	boolean isKeepTrace = false;

	public LoggingAnalyzer(CompilationUnit root, String baseException) {
		this.root = root;

		this.baseException = baseException;

		this.loggingList = new ArrayList<CSMessage>();
		
		//若沒有最底層的Exception，表示要偵測最底層Method
		if (baseException == "")
			isBaseMethod = true;
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
					if (isBaseMethod) {
						detectOverLogging(cc, svd);
					} else {
						String catchExcepiton = svd.getType().toString();
						//若Catch到的Exception與上一層傳來的Exception不同，則不偵測
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
	 * 尋找catch的節點,並且判斷節點內的Statement是否有OverLogging的情況
	 * @param statementTemp
	 * @param catchExcepiton 
	 */
	private void detectOverLogging(CatchClause cc, SingleVariableDeclaration svd) {
		List statementTemp = cc.getBody().statements();
		
		for (int i = 0; i < statementTemp.size(); i++) {
			//取得Expression statement,因為e.printstackTrace
			if(statementTemp.get(i) instanceof ExpressionStatement) {
				ExpressionStatement statement = (ExpressionStatement) statementTemp.get(i);

				//判斷是否有Logging
				judgeLogging(cc, svd, statement);
			}
			if(statementTemp.get(i) instanceof ThrowStatement) {
				ThrowStatement throwState = (ThrowStatement) statementTemp.get(i);
				//若為第一個偵測的Method
				if (isBaseMethod) {
					//若有Logging又有Throw Exception
					//Call它的Method也可能會Logging，即發生OverLogging，所以繼續往下Trace
					if (isLogging) {
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
				} else {
					//判斷是否Throw new Exception，有就不追蹤
					if (throwState.getExpression() instanceof ClassInstanceCreation)
						isKeepTrace = false;
					else
						isKeepTrace = true;
				}
			}
		}
	}

	/**
	 * 判斷是否有Logging
	 * @param statement
	 */
	private void judgeLogging(CatchClause cc, SingleVariableDeclaration svd, ExpressionStatement statement) {
		//偵測Statement和printStackTrace是否有Logging
		String st = statement.getExpression().toString();
		if(st.contains("printStackTrace")) {
			addLoggingMessage(cc, svd, statement);
			isLogging = true;
		}
	}
	
	/**
	 * 增加Logging的訊息
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
	 * 根據startPosition來取得行數
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
	 * 回傳是否有Logging
	 * @return
	 */
	public boolean getIsLogging() {
		return isLogging;
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
