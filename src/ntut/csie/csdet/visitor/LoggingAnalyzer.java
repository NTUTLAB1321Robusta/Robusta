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
 * 判斷是否有Logging，以及判斷是否要繼續往上層Trace
 * @author Shiau
 *
 */
public class LoggingAnalyzer extends RLBaseVisitor{
	private static Logger logger = LoggerFactory.getLogger(LoggingAnalyzer.class);

	//最底層Throw的Exception的Type
	String baseException;

	//是否有Logging
	boolean isLogging = false;
	//是否要繼續偵測
	boolean isKeepTrace = false;

	//Callee的Class和Method的資訊
	private String classInfo = "";
	private String methodInfo = "";

	//使用者定義的Log條件(Key:library名稱,Value:Method名稱)
	TreeMap<String, Integer> libMap = new TreeMap<String, Integer>();

	//非最底層Method(只要找Logging)
	public LoggingAnalyzer(String baseException, String classInfo, String methodInfo, TreeMap<String,Integer> libMap) {
		this.baseException = baseException;
		this.classInfo = classInfo;
		this.methodInfo = methodInfo;
		this.libMap = libMap;
	}

	protected boolean visitNode(ASTNode node){
		try {
			switch (node.getNodeType()) {
				case ASTNode.TRY_STATEMENT:
					//判斷Method是否出現在這個Try之中
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
	 * 判斷Callee的Method是否出現在這個Try之中
	 * @param node
	 */
	private void processTryStatement(ASTNode node) {
		TryStatement trystat = (TryStatement) node;
		List trys = trystat.getBody().statements();
			
		for (int i = 0;i < trys.size(); i++) {
			//若try中Statement為ExpressionStatement
			if (trys.get(i) instanceof ExpressionStatement) {
				ExpressionStatement expression = (ExpressionStatement) trys.get(i);
				//若此ExpressionStatement為MethodInvocation
				if (expression.getExpression() instanceof MethodInvocation) {
					MethodInvocation mi = (MethodInvocation) expression.getExpression();

					//取得此MethodInvocation的
					String classInfo = mi.resolveMethodBinding().getDeclaringClass().getQualifiedName();
					//若Try中有CalleeMethod(出現Class型態與Method名稱相同的Method)
					if (mi.getName().toString().equals(methodInfo) && classInfo.equals(this.classInfo)) {
						//確定Method在這個Try節點之中後，去找catch節點，直接忽略finally block
						List catchList = trystat.catchClauses();
						CatchClause cc = null;
						for (int j = 0; j < catchList.size(); j++) {
							cc = (CatchClause) catchList.get(i);
							//處理CatchClause(判斷Exception是否有轉型，並偵測有沒有Logging)
							processCatchStatement(cc);
						}

					}
				}
			}
		}
	}
	
	/**
	 * 判斷Exception是否有轉型，並偵測有沒有Logging
	 * @param node
	 */
	private void processCatchStatement(ASTNode node) {
		//轉換成catch node
		CatchClause cc = (CatchClause) node;
		//取的catch(Exception e)其中的e
		SingleVariableDeclaration svd = (SingleVariableDeclaration) cc
		.getStructuralProperty(CatchClause.EXCEPTION_PROPERTY);

		//若為第一層的Method
		String catchExcepiton = svd.getType().toString();
		
		//若baseException為空白(表示使用者設定Exception轉態後仍偵測)
		//或Catch到的Exception與上一層傳來的Exception不同，則不偵測
		if (baseException.equals("") || catchExcepiton.equals(baseException))
			detectOverLogging(cc);
	}

	/**
	 * 尋找catch的節點並且判斷節點內的Statement，是否有Logging以及要不要繼續往上層Trace
	 * @param cc
	 */
	private void detectOverLogging(CatchClause cc) {
		List statementTemp = cc.getBody().statements();
		
		for (int i = 0; i < statementTemp.size(); i++) {
			//取得Expression statement
			if(statementTemp.get(i) instanceof ExpressionStatement) {
				ExpressionStatement statement = (ExpressionStatement) statementTemp.get(i);

				//判斷是否有Logging
				judgeLogging(statement);
			}
			//判斷有沒有Throw，決定要不要繼繼Trace
			if(statementTemp.get(i) instanceof ThrowStatement) {
				ThrowStatement throwState = (ThrowStatement) statementTemp.get(i);
				//判斷是否Throw new Exception，有就不追蹤
				if (throwState.getExpression() instanceof ClassInstanceCreation)
					isKeepTrace = false;
				else
					isKeepTrace = true;
			}
		}
	}

	/**
	 * 用來找尋這個Catch Clause中是否有logging
	 * @param statement
	 */
	private void judgeLogging(ExpressionStatement statement) {
		ASTBinding visitor = new ASTBinding(libMap);
		statement.getExpression().accept(visitor);

		if (visitor.getResult())
			isLogging = true;
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
}
