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
	// 儲存找到的Unguaranteed Cleanup
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
				return true;
			//Find the smell in the catch node
			case ASTNode.CATCH_CLAUSE:
				processCatchStatement(node);
				return true;
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

		if(statementTemp.size()!=0){
			//判斷是否有Careless CleanUp
			judgeCarelessCleanUp(statementTemp);
		}
	}
	
	/**
	 * 判斷是否有Careless CleanUp
	 */
	private void judgeCarelessCleanUp(List<?> statementTemp){
		//對每個statementTemp找是否有符合的條件xxx.close()
		for(int i=0;i<statementTemp.size();i++){
			if(statementTemp.get(i) instanceof Statement){
				Statement expStatement = (Statement) statementTemp.get(i);
				//找尋Method Invocation的node
//				CarelessVisitor vistor = new CarelessVisitor(root);
//				expStatement.accept(vistor);
//				CarelessCleanUpList = vistor.getCarelessCleanUpList();
				expStatement.accept(new ASTVisitor(true){
					public boolean visit(MethodInvocation node) {
					
						//判斷class來源是否為source code
						boolean isFromSource=node.resolveMethodBinding().getDeclaringClass().isFromSource();
						
						//取得Method的名稱
						String methodName = node.resolveMethodBinding().getName();
	
						/*
						 * 偵測條件須同時滿足兩個
						 * 1.該class來源非使用者自訂
						 * 2.方法名稱為"close"
						 */
						if((!isFromSource)&& methodName.equals("close")){
							//建立一個Careless CleanUp type
							CSMessage csmsg=new CSMessage(RLMarkerAttribute.CS_CARELESS_CLEANUP,null,											
									node.toString(),node.getStartPosition(),
									getLineNumber(node.getStartPosition()),null);
							CarelessCleanUpList.add(csmsg);
						}
						return true;
					}
				}
				);
			}
		}
	}
	
	/**
	 * 判斷catch節點內的statement是否有XXX.close()
	 */
	private void processCatchStatement(ASTNode node){
		//轉換成catch node
		CatchClause cc=(CatchClause) node;
		//取得catch node的statement
		List<?> statementTemp=cc.getBody().statements();
		if(statementTemp.size()!=0){
			//判斷是否有Careless CleanUp type
			judgeCarelessCleanUp(statementTemp);
		}
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
}
