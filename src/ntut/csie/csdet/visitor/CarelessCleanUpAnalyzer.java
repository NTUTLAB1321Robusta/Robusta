package ntut.csie.csdet.visitor;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import ntut.csie.csdet.data.CSMessage;
import ntut.csie.csdet.preference.JDomUtil;
import ntut.csie.rleht.builder.ASTMethodCollector;
import ntut.csie.rleht.builder.RLMarkerAttribute;
import ntut.csie.rleht.common.RLBaseVisitor;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.CompilationUnit;
//import org.eclipse.jdt.core.dom.ForStatement;
//import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.TryStatement;
import org.jdom.Attribute;
import org.jdom.Element;

/**
 * 找專案中的Careless CleanUp
 * @author chenyimin
 */
public class CarelessCleanUpAnalyzer extends RLBaseVisitor{
	
	/** AST tree的root(檔案名稱) */
	private CompilationUnit _root;
	
	/** 儲存找到的Careless Cleanup */
	private List<CSMessage> _lstCarelessCleanupInsideOfTryBlock;
	
	/** 儲存在try block外面找到的Careless Cleanup */
	private List<CSMessage> _lstCarelessCleanupOutsideOfTryBlock;
	
	/** 收集class中的method */
	private ASTMethodCollector _methodCollector;
	
	/** 儲存找到的Method List */
	private List<ASTNode> _methodList;
	
	/** 是否要偵測"使用者釋放資源的程式碼在函式中" */
	private boolean isDetUserMethod = false;
	
	/** 儲存"使用者要偵測的library名稱"和"是否要偵測此library" */
	private TreeMap<String, Integer> libMap = new TreeMap<String, Integer>();
	
	/** 檢查程式裡面closeMethod的visitor */
	private CloseMethodAnalyzer _closeMethodAnalyzer;
	
//	/** 蒐集所有careless cleanup的ExpressionStatement */
//	private List<ExpressionStatement> _lstCarelessCleanupStatement;
	
	public CarelessCleanUpAnalyzer(CompilationUnit root){
		super(true);
		_root = root;
		_lstCarelessCleanupInsideOfTryBlock = new ArrayList<CSMessage>();
		_lstCarelessCleanupOutsideOfTryBlock = new ArrayList<CSMessage>();
		_methodCollector = new ASTMethodCollector();
		_root.accept(_methodCollector);
		_methodList = _methodCollector.getMethodList();
//		this._lstCarelessCleanupStatement = new ArrayList<ExpressionStatement>();
		getCarelessCleanUp();
	}
	
//	/** 將Careless Cleanup　statement的所屬Node記錄起來 */
//	private void addCarelessCleanupWarning(ASTNode node){
//		if (node.getParent() instanceof ExpressionStatement) {
//			ExpressionStatement statement = (ExpressionStatement) node.getParent();
//			this._lstCarelessCleanupStatement.add(statement);
//		}
//	}

	/**
	 * (non-Javadoc) 
	 * @see ntut.csie.rleht.views.ASTBaseVisitor#visitNode(org.eclipse.jdt.core.dom.ASTNode)
	 */
	protected boolean visitNode(ASTNode node){
		switch(node.getNodeType()){
			//檢查有拋出例外的Method
			case ASTNode.METHOD_DECLARATION:
				MethodDeclaration md = (MethodDeclaration) node;
				//thrownExceptions的數量，大於等於1，表示這個方法有拋出例外
				if(md.thrownExceptions().size()>=1){
					processMethodDeclaration(md);
					return false;
				}else{
					return true;
				}
			case ASTNode.TRY_STATEMENT:
				//Find the smell in the try Block
				processTryStatement(node);
				//Find the smell in the catch Block
				processCatchStatement(node);
				return false;
			default:
				return true;
		}
	}
	
	/**
	 * 偵測有拋出例外的Method中，是否有close的動作(dna2me)
	 * @param md
	 */
	private void processMethodDeclaration(MethodDeclaration md) {
		List<?> mdStatements = md.getBody().statements();
		
		//Method裡面只有一行close的情況，就不mark
		if (mdStatements.size() <= 1)
			//確定這個Statement是MethodInvocation(避免這一個Statement是IfStatement/WhileStatement/....)
			if(mdStatements.size() == 1)
				if(mdStatements.get(0) instanceof MethodInvocation)
					return;

		for (int i = 0; i < mdStatements.size(); i++) {
			//node是Try Statement(new出自己，再用一次visitNode這個Method)
			if(mdStatements.get(i) instanceof TryStatement){
				TryStatement ts = (TryStatement)mdStatements.get(i);
				CarelessCleanUpAnalyzer ccuaVisitor = new CarelessCleanUpAnalyzer(this._root);
				ts.accept(ccuaVisitor);
				
				//合併本來的instance與new出來instance的CSMessage
					//in try block
				List<CSMessage> ccuInTryBlockList = ccuaVisitor.getCarelessCleanUpList(true);
				this.mergeCSMessage(ccuInTryBlockList, true);
					//not in try block
				ccuInTryBlockList = ccuaVisitor.getCarelessCleanUpList(false);
				this.mergeCSMessage(ccuInTryBlockList, false);
			}
			//node不是Try Statement
			else if(mdStatements.get(i) instanceof Statement){
//				this._isCarelessCleanupInTryBlock = false;
				markCarelessCleanUpStatement((Statement)mdStatements.get(i), false);
			}
		}
	}
	
//	/** flag, 區分這個careless cleanup是不是在try block裡面 */
//	private boolean _isCarelessCleanupInTryBlock;
//	
	/**
	 * 結合此instance和另外new出來instance的CSMessage
	 * @param childInfo
	 */
	private void mergeCSMessage(List<CSMessage> childInfo, boolean isInTryBlock){
		if (childInfo == null || childInfo.size() == 0) {
			return;
		}
		if(isInTryBlock){
			for(CSMessage msg : childInfo){
				this._lstCarelessCleanupInsideOfTryBlock.add(msg);
			}
		}else{
			for(CSMessage msg: childInfo){
				this._lstCarelessCleanupOutsideOfTryBlock.add(msg);
			}
		}
	}
	
	/**
	 * 處理try節點內的statement
	 * 1. 如果try裡面只做close的動作，不能算是bad smell
	 * 2. 如果try裡面只有if，if又只做close的動作，也不能算是bad smell
	 */
	private void processTryStatement(ASTNode node){
		//取得try Block內的Statement
		TryStatement trystat=(TryStatement) node;
		List<?> statementTemp=trystat.getBody().statements();
		//避免該try節點為Nested try block重構後的結果,故判斷try節點內的statement數量須大於1
		//statementTemp沒東西，當然不判斷
		if(statementTemp.size() <= 0){
			return;
		}
		//如果size == 1，而且是MethodInvocation，就statementTemp只有一行程式碼，所以不判斷
		else if(statementTemp.size() == 1){
			if(statementTemp.get(0) instanceof ExpressionStatement){
				return;
			}
			//p.s.如果以後Catch也要用，記得要Extract出來
			//如果是IfStatement，沒有Else，在Then裡面又只有一行，那也是不判斷
			else if (statementTemp.get(0) instanceof IfStatement){
				IfStatement ifst = (IfStatement)statementTemp.get(0);
				//沒有ElseStatement
				if (ifst.getElseStatement() == null) {
					if(ifst.getThenStatement() instanceof ExpressionStatement){
//						System.out.println("if沒括號");
						return;
					}else{
//						System.out.println("if有括號");
						Block bk = (Block)ifst.getThenStatement();
						if(bk.statements().size() == 1){
//							System.out.println("if有括號，而且只有一行");
							return;
						}
					}
				}
			}
		}
		judgeCarelessCleanUp(statementTemp);
	}

	/**
	 * 判斷單一statement是不是Careless Clean Up
	 * @param st
	 * @param isInTryBlock
	 */
	private void markCarelessCleanUpStatement(Statement st, boolean isInTryBlock){
		//標記符合使用者自訂的Careless Cleanup規則的Statement
		if(visitBindingLib(st)){
			//findExceptionInfo((ASTNode) statement);
			addMarker(st, isInTryBlock);
		}
		//標記Careless Cleanup的Statement
		else{
			_closeMethodAnalyzer = new CloseMethodAnalyzer(false, _methodList);
			st.accept(_closeMethodAnalyzer);
			if(_closeMethodAnalyzer.isFoundCarelessCleanup()){
				addMarker(_closeMethodAnalyzer.getMethodInvocation(), isInTryBlock);
			}else{
				if(isDetUserMethod){
					_closeMethodAnalyzer = new CloseMethodAnalyzer(true, _methodList);
					st.accept(_closeMethodAnalyzer);
					if(_closeMethodAnalyzer.isFoundCarelessCleanup()){
						addMarker(_closeMethodAnalyzer.getMethodInvocation(), isInTryBlock);
					}
				}
			}
		}
	}
	
	/**
	 * 判斷try節點內的statement是否有Careless CleanUp
	 */
	private void judgeCarelessCleanUp(List<?> statementTemp){
		Statement statement;
		for(int i=0;i<statementTemp.size();i++){
			if(statementTemp.get(i) instanceof Statement){
				statement = (Statement) statementTemp.get(i);
				//若該statement包含使用者自訂的Rule,則為smell
				//否則找statement內是否有Method Invocation
				markCarelessCleanUpStatement(statement, true);
			}
		}
	}
	
	/**
	 * 處理catch節點內的statement
	 */
	private void processCatchStatement(ASTNode node){
		TryStatement trystat=(TryStatement) node;
		List<?> catchList=trystat.catchClauses();
		CatchClause catchclause=null;
			for(int i=0;i<catchList.size();i++){
				catchclause= (CatchClause) catchList.get(i);
				//避免careless cleanup直接出現在catch區塊第一層,在這邊會先偵測
				judgeCarelessCleanUp(catchclause.getBody().statements());
				//若careless cleanup出現在catch中的try,則會繼續traversal下去
				visitNode(catchclause);
			}
	}
	
	/**
	 * 偵測使用者自訂的Rule
	 * @param statement
	 * @return boolean
	 */
	private boolean visitBindingLib(Statement statement) {
		ExpressionStatementAnalyzer visitor = new ExpressionStatementAnalyzer(libMap);
		statement.accept(visitor);
		if (visitor.getResult()) {
			return true;
		} else {
			return false;
		}
	}
	
	/**
	 * 將找到的smell加入List中(本工具預設的smell)
	 * @param isInTryBlock
	 */
	private void addMarker(ASTNode node, boolean isInTryBlock){
		MethodInvocation mi = (MethodInvocation)node;
		String exceptionType = null; 
		if(mi.resolveMethodBinding().getExceptionTypes().length == 1){
			exceptionType = mi.resolveMethodBinding().getExceptionTypes().toString();
		}else if (mi.resolveMethodBinding().getExceptionTypes().length > 1){
			exceptionType = "Exceptions";
		}
		CSMessage csmsg = new CSMessage(RLMarkerAttribute.CS_CARELESS_CLEANUP,
				null, node.toString(), node.getStartPosition(),
				getLineNumber(node.getStartPosition()), exceptionType);

		//區分careless cleanup是否在try block中
		if (isInTryBlock){
			_lstCarelessCleanupInsideOfTryBlock.add(csmsg);
		}else{
			_lstCarelessCleanupOutsideOfTryBlock.add(csmsg);
		}	
	}
	
	/**
	 * 將找到的smell加入List中(使用者定義的smell)
	 * @param isInTryBlock TODO
	 */
	private void addMarker(Statement statement, boolean isInTryBlock){
		CSMessage csmsg=new CSMessage(RLMarkerAttribute.CS_CARELESS_CLEANUP ,null,											
				statement.toString(),statement.getStartPosition(),
				getLineNumber(statement.getStartPosition()),null);

		//區分careless cleanup是否在try block中
		if (isInTryBlock){
			_lstCarelessCleanupInsideOfTryBlock.add(csmsg);
		}else{
			_lstCarelessCleanupOutsideOfTryBlock.add(csmsg);
		}
	}
	
	/**
	 * 根據startPosition來取得行數
	 */
	private int getLineNumber(int pos) {
		return _root.getLineNumber(pos);
	}
	
	/**
	 * 選擇取得try block內/外的Careless CleanUp的list
	 * @param isGettingFromTryBlock (true: 取得TryBlock裡面的； false: 取得TryBlock外面的)
	 * @return list
	 */
	public List<CSMessage> getCarelessCleanUpList(boolean isGettingFromTryBlock){
		if(isGettingFromTryBlock){
			return _lstCarelessCleanupInsideOfTryBlock;
		}else{
			return _lstCarelessCleanupOutsideOfTryBlock;
		}
	}
	
	/**
	 * 取得所有的Careless Cleanup list
	 * @return
	 */
	public List<CSMessage> getCarelessCleanUpList(){
		List<CSMessage> csmsg = _lstCarelessCleanupInsideOfTryBlock;
		for(CSMessage msg : _lstCarelessCleanupOutsideOfTryBlock){
			csmsg.add(msg);
		}
		return csmsg;
	}
	
//	/**
//	 * 取得try block外，Careless Cleanup的CSMessage List
//	 * @return
//	 */
//	public List<CSMessage> getCarelessCleanUpList(){
//		return this._lstCarelessCleanupInsideOfTryBlock;
//	}
	
	/**
	 * 取得User對Careless CleanUp的設定(From xml)
	 */
	private void getCarelessCleanUp(){
		Element root = JDomUtil.createXMLContent();
		
		// 如果是null表示xml檔是剛建好的,還沒有Careless CleanUp的tag,直接跳出去		
		if(root.getChild(JDomUtil.CarelessCleanUpTag) != null){
			// 這裡表示之前使用者已經有設定過preference了,去取得相關偵測設定值
			Element CarelessCleanUp = root.getChild(JDomUtil.CarelessCleanUpTag);
			Element rule = CarelessCleanUp.getChild("rule");
			String methodSet = rule.getAttribute(JDomUtil.det_user_method).getValue();
			
			isDetUserMethod = methodSet.equals("Y");
			
			Element libRule = CarelessCleanUp.getChild("librule");
			// 把外部Library和Statement儲存在List內
			List<Attribute> libRuleList = libRule.getAttributes();
			
			//把外部的Library加入偵測名單內
			for (int i=0;i<libRuleList.size();i++) {
				if (libRuleList.get(i).getValue().equals("Y")) {
					String temp = libRuleList.get(i).getQualifiedName();					
					
					//若有.*為只偵測Library
					if (temp.indexOf(".EH_STAR") != -1) {
						int pos = temp.indexOf(".EH_STAR");
						libMap.put(temp.substring(0,pos), ExpressionStatementAnalyzer.LIBRARY);
					//若有*.為只偵測Method
					} else if (temp.indexOf("EH_STAR.") != -1) {
						libMap.put(temp.substring(8), ExpressionStatementAnalyzer.METHOD);
					//都沒有為都偵測，偵測Library+Method
					} else if (temp.lastIndexOf(".") != -1) {
						libMap.put(temp, ExpressionStatementAnalyzer.LIBRARY_METHOD);
					//若有其它形況則設成Method
					} else {
						libMap.put(temp, ExpressionStatementAnalyzer.METHOD);
					}
				}
			}
		}
	}
}