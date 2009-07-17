package ntut.csie.csdet.visitor;

import java.util.ArrayList;
import java.util.List;

import ntut.csie.csdet.data.CSMessage;
import ntut.csie.rleht.builder.RLMarkerAttribute;
import ntut.csie.rleht.common.RLBaseVisitor;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.TryStatement;

/**
 * 找專案中的Careless CleanUp
 * @author yimin
 */
public class CarelessCleanUpAnalyzer extends RLBaseVisitor{
	
	// AST tree的root(檔案名稱)
	private CompilationUnit root;
	
	// 儲存找到的Careless Cleanup
	private List<CSMessage> CarelessCleanUpList;
	
	//Constructor
	public CarelessCleanUpAnalyzer(CompilationUnit root){
		super(true);
		this.root = root;
		CarelessCleanUpList=new ArrayList<CSMessage>();
	}
	
	/*
	 * (non-Javadoc) 
	 * @see ntut.csie.rleht.views.ASTBaseVisitor#visitNode(org.eclipse.jdt.core.dom.ASTNode)
	 */
	protected boolean visitNode(ASTNode node){
		switch(node.getNodeType()){
			//Find the smell in the try node
			case ASTNode.TRY_STATEMENT:
				processTryStatement(node);
				return false;		
			default:
				return true;
		}
	}
	
	/**
	 * 判斷try節點內的statement是否有XXX.close()
	 */
	private void processTryStatement(ASTNode node){
		TryStatement trystat=(TryStatement) node;
		List<?> statementTemp=trystat.getBody().statements();

		if(statementTemp.size()!= 0){
			//判斷是否有Careless CleanUp
			judgeCarelessCleanUp(statementTemp);
		}
		//找完try節點之後,去找catch節點,直接忽略finally block
		List<?> catchList = trystat.catchClauses();
		CatchClause cc = null;
		for (int i = 0, size = catchList.size(); i < size; i++) {
			cc = (CatchClause) catchList.get(i);
			//避免careless cleanup直接出現在catch區塊第一層,在這邊會先偵測
			judgeCarelessCleanUp(cc.getBody().statements());
			//若careless cleanup出現在catch中的try,則會繼續traversal下去
			CarelessCleanUpAnalyzer visitor = new CarelessCleanUpAnalyzer(root);
			cc.getBody().accept(visitor);	
			//將catch區塊中找到的資訊做merge
			this.mergeCS(visitor.getCarelessCleanUpList());
			
		}
	}
	
	/**
	 * 判斷是否有Careless CleanUp
	 */
	private void judgeCarelessCleanUp(List<?> statementTemp){
		//對每個statementTemp找是否有符合的條件xxx.close()
		for(int i=0;i<statementTemp.size();i++){
			if(statementTemp.get(i) instanceof Statement){
				Statement statement = (Statement) statementTemp.get(i);			
				//找尋Method Invocation的node
				statement.accept(new ASTVisitor(){
					public boolean visit(MethodInvocation node) {
						//判斷class來源是否為source code
						boolean isFromSource=node.resolveMethodBinding().getDeclaringClass().isFromSource();						
						//取得Method的名稱
						String methodName = node.resolveMethodBinding().getName();
						//取得Expression
						Expression exp=node.getExpression();
						
						/*
						 * 偵測條件須同時滿足兩個
						 * 1.該class來源非使用者自訂
						 * 2.方法名稱為"close"
						 */
						if((exp!=null)&&(!isFromSource)&& methodName.equals("close")){
							//建立一個Careless CleanUp type
							CSMessage csmsg=new CSMessage(RLMarkerAttribute.CS_CARELESS_CLEANUP,null,											
									node.toString(),node.getStartPosition(),
									getLineNumber(node.getStartPosition()),null);
							CarelessCleanUpList.add(csmsg);
						}//else if((exp==null)&&(isFromSource)){
//							if(visitMethodNode(node)){
//								CSMessage csmsg=new CSMessage(RLMarkerAttribute.CS_CARELESS_CLEANUP,null,											
//										node.toString(),node.getStartPosition(),
//										getLineNumber(node.getStartPosition()),null);
//								CarelessCleanUpList.add(csmsg);
//							}
//						}
						return true;
					}
				}
				);
			}
		}
	}
	
	public boolean visitMethodNode(MethodInvocation node){
		
		return true;
	}
	/**
	 * 取得Careless CleanUp的list
	 * @return list
	 */
	public List<CSMessage> getCarelessCleanUpList(){
		return CarelessCleanUpList;
	}
	
	/**
	 * 根據startPosition來取得行數
	 */
	private int getLineNumber(int pos) {
		return root.getLineNumber(pos);
	}
	
	/**
	 * 將找到的smell資訊作merge
	 * @param childInfo
	 */
	private void mergeCS(List<CSMessage> childInfo){
		if (childInfo == null || childInfo.size() == 0) {
			return;
		}
		
		for(CSMessage msg : childInfo){
			this.CarelessCleanUpList.add(msg);
		}
	}
	
	
	public void clear(){
		if(CarelessCleanUpList != null)
			CarelessCleanUpList.clear();
	}
}
