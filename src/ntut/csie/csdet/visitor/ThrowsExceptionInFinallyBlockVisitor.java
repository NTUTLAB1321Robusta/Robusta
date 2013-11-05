package ntut.csie.csdet.visitor;

import java.util.ArrayList;
import java.util.List;

import ntut.csie.csdet.data.MarkerInfo;
import ntut.csie.csdet.preference.SmellSettings;
import ntut.csie.jdt.util.NodeUtils;
import ntut.csie.rleht.builder.RLMarkerAttribute;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.dom.ThrowStatement;
import org.eclipse.jdt.core.dom.TryStatement;

public class ThrowsExceptionInFinallyBlockVisitor extends ASTVisitor {
	private CompilationUnit root;
	private List<MarkerInfo> throwsInFinallyList;
	private boolean isTarget; // true:就會被檢查為TEFB；false:不檢查
	private boolean inFinally;// true:在目前節點中最上層的finally block裡面；false:在其他任何地方
	private boolean isDetectingThrowsExceptionInFinallyBlockSmell;
	private Block outterFinally;
	
	public ThrowsExceptionInFinallyBlockVisitor(CompilationUnit compilationUnit) {
		throwsInFinallyList = new ArrayList<MarkerInfo>();
		root = compilationUnit;
		isTarget = false;
		inFinally = false;
		outterFinally = null;
		SmellSettings smellSettings = new SmellSettings(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
		isDetectingThrowsExceptionInFinallyBlockSmell = smellSettings.isDetectingSmell(SmellSettings.SMELL_THROWSEXCEPTIONINFINALLYBLOCK);
	}
	
	/**
	 * 根據設定檔的資訊，決定要不要拜訪整棵樹。
	 */
	public boolean visit(MethodDeclaration node) {
		isTarget = false;
		inFinally = false;
		outterFinally = null;
		return isDetectingThrowsExceptionInFinallyBlockSmell;
	}
	
	public boolean visit(TryStatement node) {
		// try statement 沒有 catch clause，就要偵測try block，這條件是用來判斷裡面的try statement用的
		if(node.catchClauses().size() != 0)
			isTarget = false;
		
		// try 外面沒有 try 表示它是最外層，則初始化條件 
		if(NodeUtils.getSpecifiedParentNode(node, ASTNode.TRY_STATEMENT) == null) {
			isTarget = false;
			inFinally = false;
		}
		return true;
	}
	
	public boolean visit(CatchClause node) {
		if(inFinally)
			isTarget = true;
		return true;
	}
	
	public boolean visit(ThrowStatement node) {
		if(isTarget)
			addMarkerInfo(node);
		else if(inFinally)
			addMarkerInfo(node);
		return false;
	}
	
	public boolean visit(Block node) {
		ASTNode parentNode = NodeUtils.getSpecifiedParentNode(node, ASTNode.TRY_STATEMENT);
		// block的父親是try statement
		if(parentNode != null && parentNode.getNodeType() == ASTNode.TRY_STATEMENT) {
			// 父親有finally block，且父親的finally block等於這個block，表示這是我們要找的finally block
			if(((TryStatement) parentNode).getFinally() != null && ((TryStatement) parentNode).getFinally().getStartPosition() == node.getStartPosition()) {
				inFinally = true;
				isTarget = true;
				// 紀錄第一層finally block
				if(outterFinally == null)
					outterFinally = node;
			}
		}
		
		return true;
	}
	
	public boolean visit(MethodInvocation node) {
		if(isTarget) {
			ITypeBinding[] exTypes =  node.resolveMethodBinding().getExceptionTypes();
			// method invocation會拋checked exception的話
			if(exTypes.length > 0)
				addMarkerInfo(node);
		}
		return true; 
	}
	
	public boolean visit(SuperMethodInvocation node) {
		if(isTarget) {
			ITypeBinding[] exTypes =  node.resolveMethodBinding().getExceptionTypes();
			// method invocation會拋checked exception的話
			if(exTypes.length > 0)
				addMarkerInfo(node);
		}
		return true; 
	}
	
	public void endVisit(Block node) {
		TryStatement parentNode = (node.getParent().getNodeType() == ASTNode.TRY_STATEMENT) ? (TryStatement)node.getParent() : null;
		// 判斷是否離開finally block
		if(parentNode != null && parentNode.getFinally() != null && parentNode.getFinally().getStartPosition() == node.getStartPosition()) {
			isTarget = false;
		}
		// 判斷是否離開第一層finally block
		if(outterFinally != null && outterFinally.getStartPosition() == node.getStartPosition()) {
			inFinally = false;
			outterFinally = null;
		}
	}
	
	private void addMarkerInfo(MethodInvocation node) {
		MarkerInfo markerInfo = new MarkerInfo(RLMarkerAttribute.CS_THROWS_EXCEPTION_IN_FINALLY_BLOCK,
				(node.getExpression() != null)? node.getExpression().resolveTypeBinding() : null,
						node.toString(), node.getStartPosition(),
				root.getLineNumber(node.getStartPosition()), null);
		markerInfo.setMethodThrownExceptions(node.resolveMethodBinding().getExceptionTypes());
		throwsInFinallyList.add(markerInfo);
	}
	
	private void addMarkerInfo(SuperMethodInvocation node) {
		MarkerInfo markerInfo = new MarkerInfo(RLMarkerAttribute.CS_THROWS_EXCEPTION_IN_FINALLY_BLOCK,
				null, node.toString(), node.getStartPosition(),
				root.getLineNumber(node.getStartPosition()), null);
		markerInfo.setMethodThrownExceptions(node.resolveMethodBinding().getExceptionTypes());
		throwsInFinallyList.add(markerInfo);
	}
	
	private void addMarkerInfo(ThrowStatement node) {
		MarkerInfo markerInfo = new MarkerInfo(RLMarkerAttribute.CS_THROWS_EXCEPTION_IN_FINALLY_BLOCK,
				(node.getExpression() != null)? node.getExpression().resolveTypeBinding() : null,
						node.toString(), node.getStartPosition(),
				root.getLineNumber(node.getStartPosition()), null);
		throwsInFinallyList.add(markerInfo);
	}
	
	public List<MarkerInfo> getThrowsInFinallyList() {
		return throwsInFinallyList;
	}
}
