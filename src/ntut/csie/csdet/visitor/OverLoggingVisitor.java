package ntut.csie.csdet.visitor;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;

import ntut.csie.csdet.data.MarkerInfo;
import ntut.csie.csdet.preference.SmellSettings;
import ntut.csie.csdet.preference.SmellSettings.UserDefinedConstraintsType;
import ntut.csie.jdt.util.NodeUtils;
import ntut.csie.rleht.builder.RLMarkerAttribute;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.ThrowStatement;

public class OverLoggingVisitor extends ASTVisitor {
	// 是否要繼續偵測
	private boolean isKeepTrace = false;
	// 是否有Logging
	private boolean isLogging = false;
	// 轉型是否繼續追蹤
	private boolean isDetTransEx = false;
	// 是否找到callee
	private boolean isFoundCallee = false;
	// 是否偵測OverLoggingBadSmell
	private boolean isDetectingOverLoggingSmell;
	// Callee的Class和Method的資訊
	private String methodInfo;
	// 預先儲存可能是overlogging的ExpressionStatement
	private ASTNode suspectNode;
	// AST Tree的root(檔案名稱)
	private CompilationUnit root;
	// 儲存所找到的OverLogging Exception 
	private List<MarkerInfo> loggingList = new ArrayList<MarkerInfo>();
	// 儲存使用者定義的Log條件
	private TreeMap<String, UserDefinedConstraintsType> libMap = new TreeMap<String, UserDefinedConstraintsType>();
	// 設定檔
	private SmellSettings smellSettings;

	public OverLoggingVisitor(CompilationUnit root, String methodInfo) {
		this.root = root;
		this.methodInfo = methodInfo;
		smellSettings = new SmellSettings(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
		libMap = smellSettings.getSmellSettings(SmellSettings.SMELL_OVERLOGGING);
		isDetTransEx = (libMap.get(SmellSettings.EXTRARULE_OVERLOGGING_DETECTWRAPPINGEXCEPTION) != null) ? true : false;
		isDetectingOverLoggingSmell = smellSettings.isDetectingSmell(SmellSettings.SMELL_OVERLOGGING);
	}
	
	/**
	 * 根據設定檔的資訊，決定要不要拜訪整棵樹。
	 */
	public boolean visit(MethodDeclaration node) {
		return isDetectingOverLoggingSmell;
	}
	
	/**
	 * 判斷Callee的Method是否出現在這個Try之中
	 * 判斷是否有Logging
	 */
	public boolean visit(MethodInvocation node) {
//		if(NodeUtils.getSpecifiedParentNode(node, ASTNode.TRY_STATEMENT) != null)
//			return true;
//		
//		if(!node.getName().toString().equals(methodInfo))
//			return true;
//		
//		isFoundCallee = true;
		
		if(NodeUtils.getSpecifiedParentNode(node, ASTNode.CATCH_CLAUSE) == null)
			return true;
		
		if(!satisfyLoggingStatement(node))
			return true;
		// 有logging才加入之前疑似OverLogging的node，否則給予新發現的嫌疑犯
		if(isLogging)
			addOverLoggingMarkerInfo(suspectNode);
		else
			suspectNode = node;
		
		isLogging = true;
		
		return false;
	}

	/**
	 * 判斷有沒有Throw，決定要不要繼繼Trace
	 */
	public boolean visit(ThrowStatement node) {
		if(NodeUtils.getSpecifiedParentNode(node, ASTNode.CATCH_CLAUSE) == null)
			return true;
		
		// 若有要log才追蹤，沒有log的動作，那也就沒有over logging的問題
		if(isLogging)
			isKeepTrace = true;
		
		return true;
	}

	/**
	 * 判斷是否為Throw new Exception
	 */
	public boolean visit(ClassInstanceCreation node) {
		ASTNode parent = NodeUtils.getSpecifiedParentNode(node, ASTNode.CATCH_CLAUSE);
		if(parent == null)
			return true;
		
		// 若不偵測轉型 或 沒有將catch exception代入(eg:RuntimeException(e))，則不繼續偵測
		if (!isDetTransEx || node.arguments().size() == 0 || 
			!node.arguments().get(0).toString().equals(((CatchClause)parent).getException().getName().toString())) {
			isKeepTrace = false;
		}
		
		return false;
	}

	/**
	 * 儲存偵測到的over logging
	 * @param parent bad smell的parent
	 */
	private void addOverLoggingMarkerInfo(ASTNode node) {
		ASTNode compilationUnit = NodeUtils.getSpecifiedParentNode(node, ASTNode.COMPILATION_UNIT);
		// compilation unit如果是null，則不動作 
		if(compilationUnit == null)
			return;
		// 只儲存目前分析的檔案中的marker，如果追蹤到其他檔案，則不儲存
		if(compilationUnit.toString().equals(root.toString())) {
			ASTNode parent = NodeUtils.getSpecifiedParentNode(node, ASTNode.CATCH_CLAUSE);
			CatchClause cc = (CatchClause)parent;
			SingleVariableDeclaration svd = cc.getException();
			MarkerInfo marker = new MarkerInfo(	RLMarkerAttribute.CS_OVER_LOGGING, svd.resolveBinding().getType(), cc.toString(),										
												cc.getStartPosition(), root.getLineNumber(node.getStartPosition()), svd.getType().toString());
			loggingList.add(marker);
			suspectNode = null;
		}
	}

	private boolean satisfyLoggingStatement(MethodInvocation node) {
		if(libMap.isEmpty()) {
			return false;
		}
		
		String libName = node.resolveMethodBinding().getDeclaringClass().getQualifiedName();
		Iterator<String> iterator = libMap.keySet().iterator();
		while(iterator.hasNext()) {
			String condition = iterator.next();
			if(libName.equals(condition)) {
				return true;
			} else if(libName.length() >= condition.length()) {
				if(libName.substring(0, condition.length()).equals(condition))
					return true;
			}
		}
		return false;
	}

	/**
	 * 取得是否要繼續Trace
	 */
	public boolean getIsKeepTrace() {
		return isKeepTrace;
	}

	/**
	 * 回傳是否有Logging
	 * @return
	 */
	public boolean getIsLogging() {
		// 如果有要log，也要追蹤，而且有疑似OverLogging的node，就回傳false，為了讓detector繼續遞迴
		return !(isLogging && isKeepTrace && (suspectNode != null) ? true : false);
	}
	
	/**
	 * 取得OverLogging 行數的資訊
	 * @return
	 */
	public List<MarkerInfo> getOverLoggingList() {
		return loggingList;
	}
}
