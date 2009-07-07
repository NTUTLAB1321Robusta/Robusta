package ntut.csie.csdet.visitor;

import java.util.ArrayList;
import java.util.List;

import ntut.csie.csdet.data.CSMessage;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.astview.NodeFinder;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.internal.corext.callhierarchy.CallHierarchy;
import org.eclipse.jdt.internal.corext.callhierarchy.MethodWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * OverLogging的Code Smell
 * @author Shiau
 *
 */
public class OverLoggingDetector {
	private static Logger logger = LoggerFactory.getLogger(OverLoggingDetector.class);

	//儲存所找到的ignore Exception 
	private List<CSMessage> overLoggingList = new ArrayList<CSMessage>();
	//AST Tree的root(檔案名稱)
	private CompilationUnit root;
	//最底層的Method
	MethodDeclaration startMethod;

	public OverLoggingDetector(CompilationUnit root, ASTNode method) {
		this.root = root;
		startMethod = (MethodDeclaration) method;
	}
	
	/**
	 * 尋查Method
	 */
	public void detect() {
		//將MethodDeclaration(AST)轉換成IMethod
		IMethod method = (IMethod) startMethod.resolveBinding().getJavaElement();

		//解析AST看有沒有發生Logging又throw Exception (空白字串表示最底層Method)
		LoggingAnalyzer visitor = new LoggingAnalyzer(root, "");
		startMethod.accept(visitor);
		//是否繼續偵測
		boolean isTrace = visitor.getIsKeepTrace();
		//儲存最底層Exception
		String baseException = visitor.getBaseException();

		if (isTrace) {
			//判斷Catch Throw的Exception是否與Method Throw的Exception相同
			if (isCTEqualMT(startMethod,baseException)) {
				//使用遞迴判斷是否發生OverLogging，若OverLogging則記錄其Message
				if (detectOverLogging(method,baseException))
					overLoggingList = visitor.getOverLoggingList();
			}
		}
	}
	
	/**
	 * 判斷是否OverLogging
	 * @param method		被Call的Method
	 * @param baseException	最底層的Exception
	 * @return				是否OverLogging
	 */
	private boolean detectOverLogging(IMethod method,String baseException)
	{
		boolean isOverLogging;
		
		//往下一層邁進
		MethodWrapper currentMW = new CallHierarchy().getCallerRoot(method);				
		MethodWrapper[] calls = currentMW.getCalls(new NullProgressMonitor());

		//若有Caller
		if (calls.length != 0) {
			for (MethodWrapper mw : calls) {
				if (mw.getMember() instanceof IMethod) {
					IMethod callerMethod = (IMethod) mw.getMember();

					//避免Recursive，若Caller Method仍然是自己就不偵測
					if (callerMethod.equals(method))
						continue;

					//IMethod轉成MethodDeclaration ASTNode
					MethodDeclaration methodNode = transMethodNode(callerMethod);

					//methodNode出錯時，就不偵測
					if (methodNode == null)
						continue;

					//解析AST看有沒有OverLogging
					LoggingAnalyzer visitor = new LoggingAnalyzer(null, baseException);
					methodNode.accept(visitor);

					//是否有Logging
					boolean isLogging = visitor.getIsLogging();
					//是否繼續Trace
					boolean isTrace = visitor.getIsKeepTrace();

					System.out.println(methodNode.getName() + " :");
					System.out.println("OverLogging :" + isLogging + "  | " + "Trace :" + isTrace);

					//若有Logging動作則回傳有Logging不再往下偵測
					if (isLogging)
						return true;

					//是否往上一層追蹤
					if (isTrace) {
						//判斷Method Throws的Exception與最底層的Throw Exception是否相同
						if (!isCTEqualMT(methodNode,baseException))
							continue;

						isOverLogging = detectOverLogging(callerMethod, baseException);
						
						//若上一層結果為OverLoggin就回傳true，否則繼續
						if (isOverLogging)
							return true;
						else
							continue;
					}
				}
			}
		}
		//沒有Caller，或所有Caller跑完仍沒有
		return false;
	}
	
	/**
	 * 判斷Catch Throw的Exception是否與Method Throw的Exception相同
	 * @param method
	 * @param catchThrowEx
	 * @return
	 */
	private boolean isCTEqualMT(MethodDeclaration method, String catchThrowEx) {
		//取得Method的Throw Exception
		ITypeBinding[] throwExList = method.resolveBinding().getExceptionTypes();
		//TODO 先不考慮複數個
		if (throwExList != null && throwExList.length == 1) {
			//若Throw Exception 與 Method Throw Exception一樣才偵測
			if (throwExList[0].getName().equals(catchThrowEx))
				return true;
		}
		return false;
	}

	/**
	 * 轉換成ASTNode MethodDeclaration
	 * @param method
	 * @return
	 */
	private MethodDeclaration transMethodNode(IMethod method) {
		MethodDeclaration md = null;
		
		try {
			//Parser Jar檔時，取不到ICompilationUnit
			if (method.getCompilationUnit() == null)
				return null;

			//產生AST
			ASTParser parserAST = ASTParser.newParser(AST.JLS3);
			parserAST.setKind(ASTParser.K_COMPILATION_UNIT);
			parserAST.setSource(method.getCompilationUnit());
			parserAST.setResolveBindings(true);
			ASTNode ast = parserAST.createAST(null);

			//取得AST的Method部份
			ASTNode methodNode = NodeFinder.perform(ast, method.getSourceRange().getOffset(), method.getSourceRange().getLength());

			//若此ASTNode屬於MethodDeclaration，則轉型
			if(methodNode instanceof MethodDeclaration) {
				md = (MethodDeclaration) methodNode;
			}
		} catch (JavaModelException e) {
			logger.error("[Java Model Exception] JavaModelException ", e);
		}

		return md;
	}

	/**
	 * 取得OverLogging資訊
	 * @return
	 */
	public List<CSMessage> getOverLoggingList() {
		return overLoggingList;
	}
}
