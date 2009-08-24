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
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.TryStatement;
import org.jdom.Attribute;
import org.jdom.Element;

/**
 * 找專案中的Careless CleanUp
 * @author chenyimin
 */
public class CarelessCleanUpAnalyzer extends RLBaseVisitor{
	
	// AST tree的root(檔案名稱)
	private CompilationUnit root;
	
	// 儲存找到的Careless Cleanup
	private List<CSMessage> CarelessCleanUpList;
	
	ASTMethodCollector methodCollector;
	
	//儲存找到的Method List
	List<ASTNode> methodList;
	
	//是否找到Careless CleanUp
	private boolean flag = false;
	
	//是否要偵測"使用者釋放資源的程式碼在函式中"
	private boolean isDetUserMethod=false;
	
	//儲存"使用者要偵測的library名稱"和"是否要偵測此library"
	private TreeMap<String, Integer> libMap = new TreeMap<String, Integer>();
	
	//建構子
	public CarelessCleanUpAnalyzer(CompilationUnit root){
		super(true);
		this.root=root;
		
		CarelessCleanUpList=new ArrayList<CSMessage>();
		
		//收集class中的method
		methodCollector=new ASTMethodCollector();
		this.root.accept(methodCollector);
		methodList = methodCollector.getMethodList();
		
		//取得user對於Careless CleanUp的設定
		getCarelessCleanUp();
	}
	/*
	 * (non-Javadoc) 
	 * @see ntut.csie.rleht.views.ASTBaseVisitor#visitNode(org.eclipse.jdt.core.dom.ASTNode)
	 */
	protected boolean visitNode(ASTNode node){
		switch(node.getNodeType()){
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
	 * 處理try節點內的statement
	 */
	private void processTryStatement(ASTNode node){
		//取得try Block內的Statement
		TryStatement trystat=(TryStatement) node;
		List<?> statementTemp=trystat.getBody().statements();
		//避免該try節點為Nested try block重構後的結果,故判斷try節點內的statement數量須大於1
		if(statementTemp.size()>1){
			//判斷try節點內的statement是否有Careless CleanUp
			judgeCarelessCleanUp(statementTemp);
			
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
				if(findBindingLib(statement)){
					addMarker(statement);
				}else{
					statement.accept(new ASTVisitor(){
						public boolean visit(MethodInvocation node){
							//取得Method Invocation的Expression
							Expression expression=node.getExpression();
							//1.Expression為null,則該method invocation為object.close()的類型
							//2.Expression不為null,則為close(object)的類型
							if(expression!=null){
								/*
								 * 若同時滿足以下兩個條件,則為object.close(),視為smell
								 * 1.class非使用者自訂
								 * 2.方法名稱為"close"
								 */
								boolean isFromSource=node.resolveMethodBinding().getDeclaringClass().isFromSource();
								String methodName=node.resolveMethodBinding().getName();
								if((!isFromSource)&&(methodName.equals("close"))){
									addMarker(node);
								}
							}else{
								//使用者是否要另外偵測釋放資源的程式碼是否在函式中
								if(isDetUserMethod){
									//處理被呼叫的函式
									processCalledMethod(node);
									//若flag為true,則該函式為smell
									if(flag){				
										addMarker(node);
										flag = false;									
									}
								}
							}
							return true;
						}
					});
				}
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
	 * 處理被呼叫的函式
	 * @param node
	 */
	private void processCalledMethod(MethodInvocation node){
		//取得該Method Invocation的名稱
		String methodInvName=node.resolveMethodBinding().getName();
		String methodDecName;
		MethodDeclaration md;
		//是否有Method Declaration的Name與Method Invocation的Name相同
		for(int i=0;i<methodList.size();i++){
			//取得Method Declaration的名稱
			md=(MethodDeclaration) methodList.get(i);
			methodDecName=md.resolveBinding().getName();
			//若名稱相同,則處理該Method Invocation
			if(methodDecName.equals(methodInvName)){
				judgeCalledMethod(md);	
			}
				
		}
	}
	
	/**
	 * 判斷被呼叫的函式是否有Careless CleanUp
	 * @param MethodDeclaration
	 */
	private void judgeCalledMethod(MethodDeclaration md){
		//取得該Method Declaration的所有statement
		List<?> mdStatement=md.getBody().statements();
		//取得該Method Declaration的thrown exception name
		List<?> thrown=md.thrownExceptions();
		//TODO 整理Code
		if(mdStatement.size()!=0){
			for(int j=0;j<mdStatement.size();j++){
				//找函式內的try節點
				if(mdStatement.get(j) instanceof TryStatement){
					//取得try Block內的Statement
					TryStatement trystat=(TryStatement) mdStatement.get(j);
					List<?> statementTemp=trystat.getBody().statements();
					//找statement內是否有Method Invocation
						if(!statementTemp.isEmpty()){
							Statement statement;
							for(int k=0;k<statementTemp.size();k++){
								if(statementTemp.get(k) instanceof Statement){
									statement = (Statement) statementTemp.get(k);
										statement.accept(new ASTVisitor(){
											public boolean visit(MethodInvocation node){
												/*
												 * 若被呼叫的函式有Careless CleanUp,則其釋放資源的程式碼須滿足以下三個條件
												 * 1.expression不為空
												 * 2.class非使用者自訂
												 * 3.方法名稱為"close"
												 */
												Expression expression=node.getExpression();
												boolean isFromSource=node.resolveMethodBinding().getDeclaringClass().isFromSource();
												String methodName=node.resolveMethodBinding().getName();
												if(expression!=null&&(!isFromSource)&&(methodName.equals("close"))){											
													flag = true;
													//若已經找到有CarelessCleanUp,就不再往下一層找
													return false;
												}
												return true;
											}
										});
								}
							}
						}
				}
				//若不為空，代表有丟出例外
				if(thrown.size()!=0){
					//object.close 皆為Expression Statement
					if(mdStatement.get(j) instanceof ExpressionStatement){
						ExpressionStatement es=(ExpressionStatement) mdStatement.get(j);
						es.accept(new ASTVisitor(){
							public boolean visit(MethodInvocation node){
								/*
								 * 若該method有smell,則其釋放資源的程式碼須滿足以下三個條件
								 * 1.expression不為空
								 * 2.該class來源非使用者自訂
								 * 3.方法名稱為"close"
								 */
								Expression expression=node.getExpression();
								boolean isFromSource=node.resolveMethodBinding().getDeclaringClass().isFromSource();
								String methodName=node.resolveMethodBinding().getName();
								if(expression!=null&&(!isFromSource)&&(methodName.equals("close"))){											
									flag = true;
									return false;
								}
								return true;
							}
						});
					}
				}
			}
		}
	}
	/**
	 * 偵測使用者自訂的Rule
	 * @param statement
	 * @return boolean
	 */
	private boolean findBindingLib(Statement statement){
		ASTBinding visitor = new ASTBinding(libMap);
		statement.accept(visitor);
		if(visitor.getResult()){
			return true;
		}else{
			return false;
		}
	}
	
	/**
	 * 將找到的smell加入List中
	 */
	private void addMarker(ASTNode node){
		CSMessage csmsg=new CSMessage(RLMarkerAttribute.CS_CARELESS_CLEANUP,null,											
				node.toString(),node.getStartPosition(),
				getLineNumber(node.getStartPosition()),null);
		CarelessCleanUpList.add(csmsg);
	}
	
	private void addMarker(Statement statement){
		CSMessage csmsg=new CSMessage(RLMarkerAttribute.CS_CARELESS_CLEANUP,null,											
				statement.toString(),statement.getStartPosition(),
				getLineNumber(statement.getStartPosition()),null);
		CarelessCleanUpList.add(csmsg);
	}
	/**
	 * 根據startPosition來取得行數
	 */
	private int getLineNumber(int pos) {
		return root.getLineNumber(pos);
	}
	
	/**
	 * 取得Careless CleanUp的list
	 * @return list
	 */
	public List<CSMessage> getCarelessCleanUpList(){
		return CarelessCleanUpList;
	}
	
	/**
	 * 取得User對Careless CleanUp的設定
	 */
	private void getCarelessCleanUp(){
		Element root = JDomUtil.createXMLContent();
		
		// 如果是null表示xml檔是剛建好的,還沒有Careless CleanUp的tag,直接跳出去		
		if(root.getChild(JDomUtil.CarelessCleanUpTag) != null){
			// 這裡表示之前使用者已經有設定過preference了,去取得相關偵測設定值
			Element CarelessCleanUp = root.getChild(JDomUtil.CarelessCleanUpTag);
			Element rule = CarelessCleanUp.getChild("rule");
			String methodSet=rule.getAttribute(JDomUtil.detUserMethod).getValue();
			
			isDetUserMethod=methodSet.equals("Y");
			
			Element libRule = CarelessCleanUp.getChild("librule");
			// 把外部Library和Statement儲存在List內
			List<Attribute> libRuleList = libRule.getAttributes();
			
			//把外部的Library加入偵測名單內
			for (int i=0;i<libRuleList.size();i++)
			{
				if (libRuleList.get(i).getValue().equals("Y"))
				{
					String temp = libRuleList.get(i).getQualifiedName();					
					
					//若有.*為只偵測Library
					if (temp.indexOf(".EH_STAR")!=-1){
						int pos = temp.indexOf(".EH_STAR");
						libMap.put(temp.substring(0,pos), ASTBinding.LIBRARY);
					//若有*.為只偵測Method
					}else if (temp.indexOf("EH_STAR.") != -1){
						libMap.put(temp.substring(8), ASTBinding.METHOD);
					//都沒有為都偵測，偵測Library+Method
					}else if (temp.lastIndexOf(".") != -1){
						libMap.put(temp, ASTBinding.LIBRARY_METHOD);
					//若有其它形況則設成Method
					}else{
						libMap.put(temp, ASTBinding.METHOD);
					}
				}
			}
		}
	}
}
