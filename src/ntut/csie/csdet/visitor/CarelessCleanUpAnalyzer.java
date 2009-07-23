package ntut.csie.csdet.visitor;

import java.util.ArrayList;
import java.util.List;

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
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.TryStatement;
import org.jdom.Document;
import org.jdom.Element;

public class CarelessCleanUpAnalyzer extends RLBaseVisitor{
	// AST tree的root(檔案名稱)
	private CompilationUnit root;
	
	// 儲存找到的Careless Cleanup
	private List<CSMessage> CarelessCleanUpList;
	
	ASTMethodCollector methodCollector;
	
	//儲存找到的Method List
	List<ASTNode> methodList;
	
	//是否找到smell
	private boolean flag = false;
	
	//是否要偵測"使用者釋放資源的程式碼在函式中"
	private boolean isDetUserMethod=false;
	
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
				//Find the smell in the try node
				processTryStatement(node);
				//Find the smell in the catch node
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
		TryStatement trystat=(TryStatement) node;
		List<?> statementTemp=trystat.getBody().statements();
		//避免該try節點為Nested try block重構後的結果,故判斷try節點內的statement數量須大於1
		if(statementTemp.size()>1){
			//判斷try節點內的statement是否smell
			judgeCarelessCleanUp(statementTemp);
			
		}
	}
	
	/**
	 * 判斷try節點內的statement是否有smell
	 */
	private void judgeCarelessCleanUp(List<?> statementTemp){
		Statement statement;
		for(int i=0;i<statementTemp.size();i++){
			if(statementTemp.get(i) instanceof Statement){
				statement = (Statement) statementTemp.get(i);	
				//找try節點內的Method Invocation
				statement.accept(new ASTVisitor(){
					public boolean visit(MethodInvocation node){
						//取得Method Invocation的Expression
						Expression expression=node.getExpression();
						//若expression為null,則該method invocation為is.close()的類型
						//否則為close(is)的類型
						if(expression!=null){
							/*
							 * 若同時滿足以下兩個條件,則為smell
							 * 1.該class來源非使用者自訂
							 * 2.方法名稱為"close"
							 */
							boolean isFromSource=node.resolveMethodBinding().getDeclaringClass().isFromSource();
							String methodName=node.resolveMethodBinding().getName();
							if((!isFromSource)&&(methodName.equals("close"))){
								addMarker(node);
							}
						}else{
							if(isDetUserMethod){
								//處理函式
								processCallMethod(node);
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
	 * 處理函式
	 * @param node
	 */
	private void processCallMethod(MethodInvocation node){
		//取得該Method Invocation的名稱
		String methodInvName=node.resolveMethodBinding().getName();
		String methodDecName;
		MethodDeclaration md;
		//取得Method List中的每個Method Declaration
		for(int i=0;i<methodList.size();i++){
			//取得Method Declaration的名稱
			md=(MethodDeclaration) methodList.get(i);
			methodDecName=md.resolveBinding().getName();
			//若名稱相同,則處理該Method Invocation
			if(methodDecName.equals(methodInvName)){
				judgeCallMethod(md);	
			}
				
		}
	}
	
	/**
	 * 判斷函式是否有smell
	 * @param md
	 */
	private void judgeCallMethod(MethodDeclaration md){
		//取得該Method Declaration的所有statement
		List<?> mdStatement=md.getBody().statements();
		if(mdStatement.size()!=0){
			for(int j=0;j<mdStatement.size();j++){
				//找函式內的try節點
				if(mdStatement.get(j) instanceof TryStatement){
					TryStatement trystat=(TryStatement) mdStatement.get(j);
					List<?> statementTemp=trystat.getBody().statements();
					//找try節點內的statement是否有Method Invocation的節點
						if(!statementTemp.isEmpty()){
							Statement statement;
							for(int k=0;k<statementTemp.size();k++){
								if(statementTemp.get(k) instanceof Statement){
									statement = (Statement) statementTemp.get(k);
										statement.accept(new ASTVisitor(){
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
		Document docJDom = JDomUtil.readXMLFile();
		String methodSet="";
		if(docJDom != null){
			//從XML裡讀出之前的設定
			Element root = docJDom.getRootElement();
			if (root.getChild(JDomUtil.CarelessCleanUpTag) != null) {
				Element rule=root.getChild(JDomUtil.CarelessCleanUpTag).getChild("rule");
				methodSet = rule.getAttribute(JDomUtil.detUserMethod).getValue();
			}			
			isDetUserMethod=methodSet.equals("Y");
		}
	}
}
