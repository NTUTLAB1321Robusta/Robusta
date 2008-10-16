package ntut.csie.csdet.visitor;

import java.util.ArrayList;
import java.util.List;

import ntut.csie.csdet.data.CSMessage;
import ntut.csie.rleht.builder.RLMarkerAttribute;
import ntut.csie.rleht.common.RLBaseVisitor;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.TryStatement;

public class CodeSmellAnalyzer extends RLBaseVisitor {
	
	// AST tree的root(檔案名稱)
	private CompilationUnit root;
    
	// 儲存所找到的ignore Exception 
	private List<CSMessage> codeSmellList;
	
	public CodeSmellAnalyzer(CompilationUnit root){
		super(true);
		this.root = root;
		codeSmellList = new ArrayList<CSMessage>();
	}
	
	/**
	 * 先作兩個constructor,之後怕會有需要更多的資訊
	 * 如果沒有可以將其註解掉
	 */
	public CodeSmellAnalyzer(CompilationUnit root,boolean currentMethod,int pos){
		super(true);
		this.root = root;
		codeSmellList = new ArrayList<CSMessage>();
	}
	
	
	/*
	 * (non-Javadoc) 
	 * @see ntut.csie.rleht.views.ASTBaseVisitor#visitNode(org.eclipse.jdt.core.dom.ASTNode)
	 */
	protected boolean visitNode(ASTNode node) {
		switch (node.getNodeType()) {
	
			case ASTNode.TRY_STATEMENT:
//				System.out.println("【====TRY_STATEMENT====】");
//				System.out.println(node.toString());
				this.processTryStatement(node);
				return true;
//			case ASTNode.CATCH_CLAUSE:
//				System.out.println("【====CATCH_CLAUSE====】");
//				System.out.println(node.toString());
//				return true;
			default:
				//return true則繼續訪問其node的子節點,false則不繼續
				return true;
		}	
	}
		
	/**
	 * parse try block的內容
	 * @param node
	 */
	private void processTryStatement(ASTNode node){
		
		//處理try block
		TryStatement trystat = (TryStatement) node;		
		CodeSmellAnalyzer visitor = new CodeSmellAnalyzer(this.root,true,0);
		trystat.getBody().accept(visitor);
		//System.out.println("【this.root】=====>"+this.root.getJavaElement().getElementName());
		//System.out.println("【try Block】");
		//System.out.println(trystat.getBody().toString());		
		
		//處理catch block
		List catchList = trystat.catchClauses();
		CatchClause cc = null;		
		for (int i = 0, size = catchList.size(); i < size; i++) {
			cc = (CatchClause) catchList.get(i);
			//System.out.println("【Catch Clause】");
			//System.out.println(cc.getBody().toString());
			SingleVariableDeclaration svd = (SingleVariableDeclaration) cc
			.getStructuralProperty(CatchClause.EXCEPTION_PROPERTY);
			//判斷是否為ignore Exception,是的話就加入到List中
			judgeIgnoreEx(cc,svd);
			visitor = new CodeSmellAnalyzer(this.root,true,0);
			cc.getBody().accept(visitor);
			
		}
		
		// 處理Finally Block
		Block finallyBlock = trystat.getFinally();
		if (finallyBlock != null) {
			//System.out.println("【Finally Block】");
			visitor = new CodeSmellAnalyzer(this.root,true,0);
			finallyBlock.accept(visitor);			
			
		}
	}
	
	/**
	 * 判斷這個catch block是不是ignore EX
	 * @param cc : catch block的資訊
	 * @param svd : 未來可能會用到binding,所以先存起來
	 */
	private void judgeIgnoreEx(CatchClause cc,SingleVariableDeclaration svd ){
		List statementTemp = cc.getBody().statements();
		if(statementTemp.size() == 0){			
			CSMessage csmsg = new CSMessage(RLMarkerAttribute.CS_INGNORE_EXCEPTION,svd.resolveBinding().getType(),											
					cc.toString(),cc.getStartPosition(),this.getLineNumber(cc.getStartPosition()),svd.getType().toString());
			this.codeSmellList.add(csmsg);
//			System.out.println("【Find Ignore Exception】");
//			System.out.println("【Ignore Ex line】====>"+this.getLineNumber(cc.getStartPosition()));
		}
	}
	
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
