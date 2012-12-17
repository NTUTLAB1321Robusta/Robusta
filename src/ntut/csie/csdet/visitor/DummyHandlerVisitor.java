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
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;

public class DummyHandlerVisitor extends ASTVisitor {
	private List<MarkerInfo> dummyHandlerList;
	// 儲存偵測"Library的Name"和"是否Library"
	// store使用者要偵測的library名稱，和"是否要偵測此library"
	private TreeMap<String, UserDefinedConstraintsType> libMap;// = new TreeMap<String, UserDefinedConstraintsType>();
	private boolean isDetectingDummyHandlerSmell;
	private CompilationUnit root;
	
	public DummyHandlerVisitor(CompilationUnit root) {
		super();
		dummyHandlerList = new ArrayList<MarkerInfo>();
		this.root = root;
		SmellSettings smellSettings = new SmellSettings(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
		libMap = smellSettings.getSmellSettings(SmellSettings.SMELL_DUMMYHANDLER);
		isDetectingDummyHandlerSmell = smellSettings.isDetectingSmell(SmellSettings.SMELL_DUMMYHANDLER);
	}
	
	/**
	 * 根據設定檔的資訊，決定要不要拜訪整棵樹。
	 */
	public boolean visit(MethodDeclaration node) {
		// 如果是Main Program，就不拜訪
		if(node.getName().toString().equals("main")) {
			return false;
		}
		return isDetectingDummyHandlerSmell;
	}
	
	public boolean visit(MethodInvocation node) {
		detectDummyHandler(node);
		return false;
	}
	
	public void detectDummyHandler(MethodInvocation node) {
		ASTNode parentCatchClauseNode = NodeUtils.getSpecifiedParentNode(node, ASTNode.CATCH_CLAUSE);
		/*
		 * 如果找到的ExpressionStatement不是在CatchClause裡面，
		 * 則不能當作DummyHandler
		 */
		if(parentCatchClauseNode == null) {
			return;
		}
		CatchClause cc = (CatchClause) parentCatchClauseNode;
		/* 
		 * 如果在這個catch clause裡面，有throw statement存在，
		 * 則不把這個ExpressionStatement當作DummyHandler。
		 */
		if(isThrowStatementInCatchClause(cc)) {
			return;
		}
		addDummyHandlerSmellInfo(node);
	}
	
	/**
	 * 根據傳入的ExpressionStatement Node，找出其所屬的CatchClause
	 * @param node ExpressionStatement Node
	 */
	private void addDummyHandlerSmellInfo(MethodInvocation node) {
			// 取得Method的Library名稱
			String libName = node.resolveMethodBinding().getDeclaringClass().getQualifiedName();
			// 取得Method的名稱
			String methodName = node.resolveMethodBinding().getName();

			// 如果該行有Array(如java.util.ArrayList<java.lang.Boolean>)，把<>與其內容都拿掉
			if (libName.indexOf("<") != -1)
				libName = libName.substring(0, libName.indexOf("<"));
			
			Iterator<String> libIt = libMap.keySet().iterator();
			// 判斷是否要偵測 且 此句也包含欲偵測Library
			while(libIt.hasNext()){
				String temp = libIt.next();
				CatchClause cc = (CatchClause) NodeUtils.getSpecifiedParentNode(node, ASTNode.CATCH_CLAUSE);
				SingleVariableDeclaration svd = cc.getException();
				MarkerInfo markerInfo = new MarkerInfo(	RLMarkerAttribute.CS_DUMMY_HANDLER, svd
														.resolveBinding().getType(), cc.toString(), cc
														.getStartPosition(), root.getLineNumber(node
														.getStartPosition()), svd.getType().toString());
				
				// 只偵測Library
				if (libMap.get(temp) == UserDefinedConstraintsType.Library) {
					//若Library長度大於偵測長度，否則表不相同直接略過
					if (libName.length() >= temp.length()) {
						//比較前半段長度的名稱是否相同
						if (libName.substring(0, temp.length()).equals(temp))
							dummyHandlerList.add(markerInfo);
					}
				// 只偵測Method
				} else if (libMap.get(temp) == UserDefinedConstraintsType.Method) {
					if (methodName.equals(temp))
						dummyHandlerList.add(markerInfo);
				// 偵測Library.Method的形式
				} else if (libMap.get(temp) == UserDefinedConstraintsType.FullQulifiedMethod) {
					int pos = temp.lastIndexOf(".");
					if (libName.equals(temp.substring(0, pos)) &&
						methodName.equals(temp.substring(pos + 1))) {
						dummyHandlerList.add(markerInfo);
					}
				}
			}
		
	}
	
	public List<MarkerInfo> getDummyList() {
		return dummyHandlerList;
	}

	/**
	 * 指定的CatchClause裡面，是不是有ThrowStatement。
	 * @param catchClause
	 * @return
	 */
	public boolean isThrowStatementInCatchClause(CatchClause catchClause) {
		List<?> ccStatements = catchClause.getBody().statements();
		for (Object ccNode : ccStatements) {
			if (((ASTNode) ccNode).getNodeType() == ASTNode.THROW_STATEMENT) {
				return true;
			}
		}
		return false;
	}
}
