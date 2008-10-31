package ntut.csie.csdet.visitor;

import java.util.ArrayList;
import java.util.List;

import ntut.csie.csdet.data.CSMessage;
import ntut.csie.rleht.builder.RLMarkerAttribute;
import ntut.csie.rleht.common.RLBaseVisitor;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;

/**
 * 找專案中的Ignore Exception
 * @author chewei
 */

public class CodeSmellAnalyzer extends RLBaseVisitor {
	
	// AST tree的root(檔案名稱)
	private CompilationUnit root;
    
	// 儲存所找到的ignore Exception 
	private List<CSMessage> codeSmellList;
	
	// 儲存所找到的Dummy handler
	private List<CSMessage> dummyHandler;
	
	public CodeSmellAnalyzer(CompilationUnit root){
		super(true);
		this.root = root;
		codeSmellList = new ArrayList<CSMessage>();
		dummyHandler = new ArrayList<CSMessage>();
	}
	
	/**
	 * 先作兩個constructor,之後怕會有需要更多的資訊
	 * 如果沒有可以將其註解掉
	 */
	public CodeSmellAnalyzer(CompilationUnit root,boolean currentMethod,int pos){
		super(true);
		this.root = root;
		codeSmellList = new ArrayList<CSMessage>();
		dummyHandler = new ArrayList<CSMessage>();
	}
	
	
	/*
	 * (non-Javadoc) 
	 * @see ntut.csie.rleht.views.ASTBaseVisitor#visitNode(org.eclipse.jdt.core.dom.ASTNode)
	 */
	protected boolean visitNode(ASTNode node) {
		switch (node.getNodeType()) {
			//TODO 測試沒問題就可以把TRY_STATEMENT這個地方砍掉
			case ASTNode.TRY_STATEMENT:
//				System.out.println("【====TRY_STATEMENT====】");
//				System.out.println(node.toString());
				//this.processTryStatement(node);
				return true;
			case ASTNode.CATCH_CLAUSE:
				processCatchStatement(node);
				return true;
			default:
				//return true則繼續訪問其node的子節點,false則不繼續
				return true;
		}	
	}
		
	/**
	 * parse try block的內容
	 * @param node
	 */
	//TODO 測試沒問題就可以把TRY_STATEMENT這個地方砍掉
//	private void processTryStatement(ASTNode node){
//		
//		//處理try block
//		TryStatement trystat = (TryStatement) node;		
//		CodeSmellAnalyzer visitor = new CodeSmellAnalyzer(this.root,true,0);
//		trystat.getBody().accept(visitor);	
//		
//		//處理catch block
//		List catchList = trystat.catchClauses();
//		CatchClause cc = null;		
//		for (int i = 0, size = catchList.size(); i < size; i++) {
//			cc = (CatchClause) catchList.get(i);
//			SingleVariableDeclaration svd = (SingleVariableDeclaration) cc
//			.getStructuralProperty(CatchClause.EXCEPTION_PROPERTY);
//			//判斷是否為ignore Exception,是的話就加入到List中
//			judgeIgnoreEx(cc,svd);
//			
//			visitor = new CodeSmellAnalyzer(this.root,true,0);
//			cc.getBody().accept(visitor);
//		
//		}
//		
//		// 處理Finally Block
//		Block finallyBlock = trystat.getFinally();
//		if (finallyBlock != null) {
//			visitor = new CodeSmellAnalyzer(this.root,true,0);
//			finallyBlock.accept(visitor);			
//			
//		}
//	}
	
	/**
	 * 去尋找catch的節點,並且判斷節點內的statement是否為空
	 * @param node
	 */
	private void processCatchStatement(ASTNode node){
		CatchClause cc = (CatchClause) node;
		SingleVariableDeclaration svd = (SingleVariableDeclaration) cc
		.getStructuralProperty(CatchClause.EXCEPTION_PROPERTY);
		judgeIgnoreEx(cc,svd);
	}
	
	/**
	 * 判斷這個catch block是不是ignore EX
	 * @param cc : catch block的資訊
	 * @param svd : throw exception會用到
	 */
	private void judgeIgnoreEx(CatchClause cc,SingleVariableDeclaration svd ){
		List statementTemp = cc.getBody().statements();
		if(statementTemp.size() == 0){			
			CSMessage csmsg = new CSMessage(RLMarkerAttribute.CS_INGNORE_EXCEPTION,svd.resolveBinding().getType(),											
					cc.toString(),cc.getStartPosition(),this.getLineNumber(cc.getStartPosition()),svd.getType().toString());
			this.codeSmellList.add(csmsg);
			System.out.println("【Ignore Ex Position】====>"+this.getLineNumber(cc.getStartPosition()));
		}else{
	        /*------------------------------------------------------------------------*
            -  假如statement不是空的,表示有可能存在dummy handler,不另外寫一個class來偵測,原因是
                 不希望要parse每個method很多次,code部分也會增加,所以就寫在這邊
            *-------------------------------------------------------------------------*/			
			for(int i=0;i<statementTemp.size();i++){
				if(statementTemp.get(i) instanceof ExpressionStatement){
					ExpressionStatement statement = (ExpressionStatement) statementTemp.get(i);

					if(statement.getExpression().toString().contains("printStackTrace")){
						System.out.println("【Position】===>"+statement.getExpression().toString().indexOf("printStackTrace"));
						System.out.println("【Class Name】====>"+this.root.getJavaElement().getElementName());
						System.out.println("【DH Line number】====>"+this.getLineNumber(statement.getStartPosition()));
						System.out.println("【Dummy Handler】====>"+statement.getExpression().toString());
						CSMessage csmsg = new CSMessage(RLMarkerAttribute.CS_DUMMY_HANDLER,svd.resolveBinding().getType(),											
								cc.toString(),statement.getExpression().getStartPosition(),
								this.getLineNumber(statement.getStartPosition()),svd.getType().toString());
						this.codeSmellList.add(csmsg);
					}
				}
			}
		}
	}
	
//	private void judgeDummyHandler(){
//		
//	}
	
	/**
	 * 根據startPosition來取得行數
	 */
	private int getLineNumber(int pos) {
		return root.getLineNumber(pos);
	}
	
	/**
	 * 取得ignore Exception的List
	 */
	public List<CSMessage> getIgnoreExList(){
		return codeSmellList;
	}
	

}
