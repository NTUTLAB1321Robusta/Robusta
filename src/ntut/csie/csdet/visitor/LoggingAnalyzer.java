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
	//是否有Logging
	private boolean isLogging = false;
	//是否要繼續偵測
	private boolean isKeepTrace = false;

	//Callee的Class和Method的資訊
	private String classInfo = "";
	private String methodInfo = "";
	//
	private boolean isDetTransEx = false;

	//使用者定義的Log條件(Key:library名稱,Value:Method名稱)
	TreeMap<String, Integer> libMap = new TreeMap<String, Integer>();

	//非最底層Method(只要找Logging)
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
			
			CalleeMethodVisitor f = new CalleeMethodVisitor();
			trystat.accept(f);
			
			if (f.isFoundCallee) {
				//確定Method在這個Try節點之中後，去找catch節點，直接忽略finally block
				List catchList = trystat.catchClauses();
				CatchClause cc = null;
				for (int j = 0; j < catchList.size(); j++) {
					// TODO: 應該判斷該Catch clause是否與拋出的例外類型相同
					cc = (CatchClause) catchList.get(j);
					// 處理CatchClause(判斷Exception是否有轉型，並偵測有沒有Logging)
					processCatchStatement(cc);
				}
			}

			// 過去版本: 不是用Visitor判斷Callee是否在Try之中
			//若try中Statement為ExpressionStatement
//			if (trys.get(i) instanceof ExpressionStatement) {
//				ExpressionStatement expression = (ExpressionStatement) trys.get(i);
//				//若此ExpressionStatement為MethodInvocation
//				if (expression.getExpression() instanceof MethodInvocation) {
//					MethodInvocation mi = (MethodInvocation) expression.getExpression();
//
//					//取得此MethodInvocation的
//					String classInfo = mi.resolveMethodBinding().getDeclaringClass().getQualifiedName();
//					//若Try中有CalleeMethod(出現Class型態與Method名稱相同的Method)
//					if (mi.getName().toString().equals(methodInfo) && classInfo.equals(this.classInfo)) {
//						//確定Method在這個Try節點之中後，去找catch節點，直接忽略finally block
//						List catchList = trystat.catchClauses();
//						CatchClause cc = null;
//						for (int j = 0; j < catchList.size(); j++) {
//							cc = (CatchClause) catchList.get(j);
//							//處理CatchClause(判斷Exception是否有轉型，並偵測有沒有Logging)
//							processCatchStatement(cc);
//						}
//					}
//				}
//			}
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
		SingleVariableDeclaration svd = (SingleVariableDeclaration) cc.getStructuralProperty(CatchClause.EXCEPTION_PROPERTY);

		//若為第一層的Method
		String catchExcepiton = svd.getType().toString();

		//偵測Catch內容
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
				isKeepTrace = true;

				ThrowStatement throwState = (ThrowStatement) statementTemp.get(i);
				//判斷是否為Throw new Exception
				if (throwState.getExpression() instanceof ClassInstanceCreation) {
					ClassInstanceCreation cic = (ClassInstanceCreation) throwState.getExpression();
					List argumentList = cic.arguments();

					//若不偵測轉型 或 沒有將catch exception代入(eg:RuntimeException(e))
					//則不繼續偵測
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
	 * 用來找尋這個Catch Clause中是否有logging
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

	/** 尋找Callee的Method是否在Try之中 **/
	class CalleeMethodVisitor extends RLBaseVisitor {
		// 是否找到指定的Method
		private boolean isFoundCallee = false;

		protected boolean visitNode(ASTNode node){
			try {
				switch (node.getNodeType()) {
					case ASTNode.METHOD_INVOCATION:
						MethodInvocation mi = (MethodInvocation) node;

						// TODO:　目前只先比較Method是否一樣，Class資訊先不比較
						// 取得此MethodInvocation的
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

		/** @return	是否找到Callee **/
		public boolean isFoundCallee() {
			return isFoundCallee;
		}
	}
}
