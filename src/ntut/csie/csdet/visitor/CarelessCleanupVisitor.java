package ntut.csie.csdet.visitor;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.List;

import ntut.csie.csdet.data.MarkerInfo;
import ntut.csie.csdet.preference.SmellSettings;
import ntut.csie.jdt.util.NodeUtils;
import ntut.csie.rleht.builder.RLMarkerAttribute;
import ntut.csie.robusta.agile.exception.RTag;
import ntut.csie.robusta.agile.exception.Robustness;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.TryStatement;

public class CarelessCleanupVisitor extends ASTVisitor {
	/** AST的root */
	private CompilationUnit root;
	/** 儲存找到的例外處理壞味道程式碼所在的行數以及程式碼片段...等 */
	private List<MarkerInfo> carelessCleanupList;
	private boolean isDetectingCarelessCleanupSmell;
	
	public CarelessCleanupVisitor(CompilationUnit compilationUnit){
		super();
		this.root = compilationUnit;
		carelessCleanupList = new ArrayList<MarkerInfo>();
		SmellSettings smellSettings = new SmellSettings(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
		isDetectingCarelessCleanupSmell = smellSettings.isDetectingSmell(SmellSettings.SMELL_CARELESSCLEANUP);
	}
	
	public List<MarkerInfo> getCarelessCleanupList() {
		return carelessCleanupList;
	}
	
	/**
	 * 根據設定檔的資訊，決定要不要拜訪整棵樹。
	 */
	public boolean visit(MethodDeclaration node) {
		return isDetectingCarelessCleanupSmell;
	}
	
	/**********************************************************
	 * 檢查IfStatement與TryStatement：
	 * 如果所屬的MethodDeclaration沒有拋出例外；
	 * If沒有else的敘述，且If只有一個Statement；
	 * Try沒有finally的敘述且catch沒有拋出例外，Try裡面也只有一個Statement；
	 * 則不會去檢查包含在裡面的MethodInvocation，
	 * 亦即不管裡面的Statement是甚麼，都不會被檢查。
	 **********************************************************/

	// FIXME - for, while, do-while, parenthesis
	
	/**
	 * 增加不檢查close的條件:If
	 */
	@Robustness(value = { @RTag(level = 1, exception = java.lang.RuntimeException.class) })
	public boolean visit(IfStatement node) {
		if(isMethodDeclarationThrowException(node)) {
			return true;
		}
		
		// else有程式碼，那整個if就要被檢驗
		if(node.getElseStatement() != null) {
			return true;
		}
		
		/*
		 *  if 裡面只有一個try statement 的語法有兩種寫法。
		 *  一個是有花括號的，你會在then statement抓到Block。
		 *  一個是沒有花括號的，你會直接抓到try statement。
		 */
		Statement thenStatement = node.getThenStatement();
		if(thenStatement.getNodeType() == ASTNode.EXPRESSION_STATEMENT) {
			return false;
		}
		
		if(thenStatement.getNodeType() != ASTNode.BLOCK) {
			return true;
		}
		
		Block thenBlock = (Block) thenStatement;
		if(thenBlock.statements().size() != 1) {
			return true;
		} else if(thenBlock.statements().size() == 1) {
			// 如果這個IfStatement被TryStatement包著，則往上找兩層會找到TryStatement
			ASTNode parentTryStatement = node.getParent().getParent();
			if (parentTryStatement.getNodeType() != ASTNode.TRY_STATEMENT) {
				return true;
			} else {
				// 如果包著這個if的try有拋出例外，那就要檢查if裡面有沒有careless cleanup
				if(isTryStatementThrowException((TryStatement)parentTryStatement)) {
					return true;
				}
			}
		}
		return false;
	}
	
	/**
	 * 增加不檢查close的條件:Try
	 */
	@Robustness(value = { @RTag(level = 1, exception = java.lang.RuntimeException.class) })
	public boolean visit(TryStatement node) {
		if(isMethodDeclarationThrowException(node)) {
			return true;
		}
		
		// Try裡面超過一個Statement(就有可能不只close的動作)
		if(node.getBody().statements().size() != 1) {
			return true;
		} else if (node.getFinally() != null) {
			return true;
		} else {
			// 要注意這一個Statement是不是if
			if(((Statement)node.getBody().statements().get(0)).getNodeType() == ASTNode.IF_STATEMENT) {
				return true;
			}
		}
		
		// 如果Catch裡面有拋出例外，就不是可以存放在Finally中，關閉串流的動作
		if(isTryStatementThrowException(node)) {
			return true;
		}
		return false;
	}
	
	/**
	 * 檢查這個TryStatement是不是有拋出例外
	 * @param node
	 * @return true有拋出例外，false沒拋出例外
	 */
	private boolean isTryStatementThrowException(TryStatement node) {
		// 如果Catch裡面有拋出例外，就不是可以存放在Finally中，關閉串流的動作
		for (int i = 0; i < node.catchClauses().size(); i++) {
			CatchClause cc = (CatchClause)node.catchClauses().get(i);
			for(int j = 0; j< cc.getBody().statements().size(); j++){
				Statement statement = (Statement)cc.getBody().statements().get(j);
				if(statement.getNodeType() == ASTNode.THROW_STATEMENT) {
					return true;
				}
			}
		}		
		return false;
	}
	
	/**
	 * 檢查這個MethodDeclaration是否有拋出例外
	 * (RuntimeException沒辦法利用這個方法檢查)
	 * @param node
	 * @return
	 */
	@Robustness(value = { @RTag(level = 1, exception = java.lang.RuntimeException.class) })
	private boolean isMethodDeclarationThrowException(ASTNode node) {
		if(node.getNodeType() == ASTNode.COMPILATION_UNIT) {
			throw new RuntimeException("Abatract Syntax Tree traversing error. by Charles.");
		}
		
		if(node.getNodeType() == ASTNode.METHOD_DECLARATION) {
			if(((MethodDeclaration)node).thrownExceptions().size() == 0)
				return false;
			else
				return true;  
		}
		
		return(isMethodDeclarationThrowException(node.getParent()));
	}
	
	private boolean visitDefault(MethodInvocation node) {
		// 尋找method name為close
		if(!node.getName().toString().equals("close")) {
			return false;
		}

		/*
		 *	尋找這個close是不是實作Closeable 
		 */
		if(node.resolveMethodBinding() == null) {
			System.out.println("aa");
		}
		if (!NodeUtils.isITypeBindingImplemented(node.resolveMethodBinding()
				.getDeclaringClass(), Closeable.class)) {
			return true;
		}
		
		// 經過上述檢查後，抓到的close就要被加上Marker
		collectSmell(node);
		return false;
	}
	
	@Robustness(value = { @RTag(level = 1, exception = java.lang.RuntimeException.class) })
	public boolean visit(MethodInvocation node) {
		boolean userDefinedLibResult = true;
		boolean userDefinedResult = true;
		boolean userDefinedExtraRule = true;
		boolean defaultResult = true;
		
		UserDefinedMethodAnalyzer userDefinedMethodAnalyzer = new UserDefinedMethodAnalyzer(SmellSettings.SMELL_CARELESSCLEANUP);
		if(userDefinedMethodAnalyzer.analyzeLibrary(node)) {
			collectSmell(node);
			userDefinedLibResult = false;
		}
		
		if(userDefinedMethodAnalyzer.analyzeMethods(node)) {
			collectSmell(node);
			userDefinedResult = false;
		}
		
		if(userDefinedMethodAnalyzer.analyzeExtraRule(node, root)) {
			collectSmell(node);
			userDefinedExtraRule = false;
		}
		
		if(userDefinedMethodAnalyzer.getEnable()) {
			defaultResult = visitDefault(node);
		}
		
		if(userDefinedLibResult || userDefinedResult || userDefinedExtraRule || defaultResult) {
			return true;
		}
		
		return false;
	}
	
	private void collectSmell(MethodInvocation node) {
		StringBuilder exceptions = new StringBuilder();
		ITypeBinding[] exceptionTypes = collectMethodInvocationThrownException(node);
		if (exceptionTypes != null) {
			for (ITypeBinding itb : exceptionTypes) {
				exceptions.append(itb.toString());
				exceptions.append(",");
			}
		}
		ASTNode parentNode = NodeUtils.getSpecifiedParentNode(node, ASTNode.TRY_STATEMENT);
		MarkerInfo markerInfo = new MarkerInfo(RLMarkerAttribute.CS_CARELESS_CLEANUP,
			(node.getExpression() != null)? node.getExpression().resolveTypeBinding() : null,
			node.toString(), node.getStartPosition(),
			root.getLineNumber(node.getStartPosition()),
			(exceptionTypes != null)? exceptions.toString() : null);
		markerInfo.setIsInTry((parentNode != null)? true:false);
		carelessCleanupList.add(markerInfo);
	}
	
	/**
	 * 蒐集MethodInvocation上所有拋出的例外。
	 * 通常只能蒐集到Checked Exception，因為只有它們會被強制要求寫在MethodDeclaration上。
	 * @param node 你想蒐集例外的MethodInvocation
	 * @return
	 */
	private ITypeBinding[] collectMethodInvocationThrownException(MethodInvocation node) {
		// 如果使用者進行了快速修復，則會蒐集到ListRewrite的資訊，node.resolveMethodBinding()會變成null
		if(node.resolveMethodBinding() == null) {
			return null;
		}
		
		// visit原始程式碼的時候，可以蒐集到node.resolveMethodBinding()
		if(node.resolveMethodBinding().getExceptionTypes().length <= 0) {
			return null;
		}
		
		return node.resolveMethodBinding().getExceptionTypes();
	}
	
	/**
	 * 如果是finally block就不檢查
	 */
	public boolean visit(Block node) {
		ASTNode tryStatement = NodeUtils.getSpecifiedParentNode(node, ASTNode.TRY_STATEMENT);
		if(tryStatement == null) {
			return true;
		}
		
		if(((TryStatement)tryStatement).getFinally() == null) {
			return true;
		}
		
		if(((TryStatement)tryStatement).getFinally().equals(node)) {
			return false;
		}
		
		return true;
	}
}
