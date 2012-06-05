package ntut.csie.csdet.visitor;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;

import ntut.csie.csdet.data.MarkerInfo;
import ntut.csie.csdet.preference.JDomUtil;
import ntut.csie.rleht.builder.RLMarkerAttribute;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.TryStatement;
import org.jdom.Attribute;
import org.jdom.Element;

public class DummyHandlerVisitor extends ASTVisitor {
	final static public int LIBRARY = 1;
	final static public int METHOD = 2;
	final static public int LIBRARY_METHOD = 3;
	
	private List<MarkerInfo> dummyHandlerList;
	// 儲存偵測"Library的Name"和"是否Library"
	// store使用者要偵測的library名稱，和"是否要偵測此library"
	private TreeMap<String, Integer> libMap = new TreeMap<String, Integer>();
	private CompilationUnit root;
	// Code Information Counter //
	private int tryCounter = 0;
	private int catchCounter = 0;
	private int finallyCounter = 0;
	
	public DummyHandlerVisitor(CompilationUnit root) {
		super();
		dummyHandlerList = new ArrayList<MarkerInfo>();
		this.root = root;
		getDummySettings();
	}
	
	public boolean visit(TryStatement node) {
		tryCounter++;
		if(node.getFinally() != null)
			finallyCounter++;
		ASTNode parent = getSpecifiedParentNode(node, ASTNode.TRY_STATEMENT);
		if(parent == null) {
			/*
			 * 這個TryStatement不是在TryStatement裡面
			 */
			return true;
		} else {
			/*
			 * Try裡面本來就不應該有Try Catch的程式碼(Nested Try Block)。
			 * 所以如果遇到的TryStatement Node是Try Statement裡面，那就不繼續偵測。
			 * 
			 * 避免Close Stream時，不得不Dummy Handler的問題。
			 */
			return false;
		}
	}
	
	public boolean visit(MethodInvocation node) {
		ASTNode parentNode = node.getParent();
		if(parentNode.getNodeType() == ASTNode.EXPRESSION_STATEMENT) {
			detectDummyHandler((ExpressionStatement)parentNode);
		}
		return false;	
	}
	
	public void detectDummyHandler(ExpressionStatement node) {
		ASTNode parentCatchClauseNode = getSpecifiedParentNode(node, ASTNode.CATCH_CLAUSE);
		/*
		 * 如果找到的ExpressionStatement不是在CatchClause裡面，
		 * 則不能當作DummyHandler
		 */
		if(parentCatchClauseNode == null) {
			return;
		}
		CatchClause cc = (CatchClause) parentCatchClauseNode;
		catchCounter++;
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
	private void addDummyHandlerSmellInfo(ExpressionStatement node) {
		MethodInvocation mi = (MethodInvocation)node.getExpression();
		// 取得Method的Library名稱
		String libName = mi.resolveMethodBinding().getDeclaringClass().getQualifiedName();
		// 取得Method的名稱
		String methodName = mi.resolveMethodBinding().getName();

		// 如果該行有Array(如java.util.ArrayList<java.lang.Boolean>)，把<>內容拿掉
		if (libName.indexOf("<") != -1)
			libName = libName.substring(0, libName.indexOf("<"));
		
		Iterator<String> libIt = libMap.keySet().iterator();
		// 判斷是否要偵測 且 此句也包含欲偵測Library
		while(libIt.hasNext()){
			String temp = libIt.next();
			CatchClause cc = (CatchClause) getSpecifiedParentNode(node, ASTNode.CATCH_CLAUSE);
			SingleVariableDeclaration svd = cc.getException();
			MarkerInfo markerInfo = new MarkerInfo(	RLMarkerAttribute.CS_DUMMY_HANDLER, svd
													.resolveBinding().getType(), cc.toString(), cc
													.getStartPosition(), root.getLineNumber(node
													.getStartPosition()), svd.getType().toString());
			
			// 只偵測Library
			if (libMap.get(temp) == LIBRARY) {
				//若Library長度大於偵測長度，否則表不相同直接略過
				if (libName.length() >= temp.length())
				{
					//比較前半段長度的名稱是否相同
					if (libName.substring(0, temp.length()).equals(temp))
						dummyHandlerList.add(markerInfo);
				}
			// 只偵測Method
			} else if (libMap.get(temp) == METHOD) {
				if (methodName.equals(temp))
					dummyHandlerList.add(markerInfo);
			// 偵測Library.Method的形式
			} else if (libMap.get(temp) == LIBRARY_METHOD) {
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
	
	/**
	 * 從輸入的節點開始，尋找特定的父節點。
	 * @param startNode
	 * @param nodeType
	 * @return
	 */
	public ASTNode getSpecifiedParentNode(ASTNode startNode, int nodeType) {
		ASTNode resultNode = null;
		ASTNode parentNode = startNode.getParent();
		// 如果parentNode是null，表示傳進來的node已經是rootNode(CompilationUnit)
		if(parentNode != null) {
			while(parentNode.getNodeType() != nodeType) {
				parentNode = parentNode.getParent();
				// 無窮迴圈終止條件 - 已經沒有parentNode
				if (parentNode == null) {
					break;
				}
			}
			resultNode = parentNode; 
		}
		return resultNode;
	}
	
	/**
	 * 將user對於dummy handler的設定存下來
	 */
	private void getDummySettings() {
		Element root = JDomUtil.createXMLContent();
		// 如果是null表示xml檔是剛建好的,還沒有dummy handler的tag,直接跳出去

		if (root.getChild(JDomUtil.DummyHandlerTag) != null) {
			// 這裡表示之前使用者已經有設定過preference了,去取得相關偵測設定值
			Element dummyHandler = root.getChild(JDomUtil.DummyHandlerTag);
			Element rule = dummyHandler.getChild("rule");
			String eprintSet = rule.getAttribute(JDomUtil.e_printstacktrace)
					.getValue();
			String sysoSet = rule.getAttribute(JDomUtil.systemout_print)
					.getValue();
			String log4jSet = rule.getAttribute(JDomUtil.apache_log4j)
					.getValue();
			String javaLogger = rule.getAttribute(JDomUtil.java_Logger)
					.getValue();
			Element libRule = dummyHandler.getChild("librule");
			// 把外部Library和Statement儲存在List內
			List<Attribute> libRuleList = libRule.getAttributes();

			// 把內建偵測加入到名單內
			// 把e.print和system.out加入偵測內
			if (sysoSet.equals("Y")) {
				libMap.put("java.io.PrintStream.println",
						ExpressionStatementAnalyzer.LIBRARY_METHOD);
				libMap.put("java.io.PrintStream.print",
						ExpressionStatementAnalyzer.LIBRARY_METHOD);
			}
			if (eprintSet.equals("Y"))
				libMap.put("printStackTrace", ExpressionStatementAnalyzer.METHOD);
			// 把log4j和javaLog加入偵測內
			if (log4jSet.equals("Y"))
				libMap.put("org.apache.log4j", ExpressionStatementAnalyzer.LIBRARY);
			if (javaLogger.equals("Y"))
				libMap.put("java.util.logging", ExpressionStatementAnalyzer.LIBRARY);

			// 把外部的Library加入偵測名單內
			for (int i = 0; i < libRuleList.size(); i++) {
				if (libRuleList.get(i).getValue().equals("Y")) {
					String temp = libRuleList.get(i).getQualifiedName();

					// 若有.*為只偵測Library
					if (temp.indexOf(".EH_STAR") != -1) {
						int pos = temp.indexOf(".EH_STAR");
						libMap.put(temp.substring(0, pos), ExpressionStatementAnalyzer.LIBRARY);
						// 若有*.為只偵測Method
					} else if (temp.indexOf("EH_STAR.") != -1) {
						libMap.put(temp.substring(8), ExpressionStatementAnalyzer.METHOD);
						// 都沒有為都偵測，偵測Library+Method
					} else if (temp.lastIndexOf(".") != -1) {
						libMap.put(temp, ExpressionStatementAnalyzer.LIBRARY_METHOD);
						// 若有其它形況則設成Method
					} else {
						libMap.put(temp, ExpressionStatementAnalyzer.METHOD);
					}
				}
			}
		}
	}
	
	public int getTryCounter() {
		return tryCounter;
	}

	public int getCatchCounter() {
		return catchCounter;
	}

	public int getFinallyCounter() {
		return finallyCounter;
	}
}
