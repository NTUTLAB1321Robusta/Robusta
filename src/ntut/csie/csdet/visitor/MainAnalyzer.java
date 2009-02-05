package ntut.csie.csdet.visitor;

import java.util.ArrayList;
import java.util.List;

import ntut.csie.csdet.data.CSMessage;
import ntut.csie.rleht.builder.RLMarkerAttribute;
import ntut.csie.rleht.common.RLBaseVisitor;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.TryStatement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MainAnalyzer extends RLBaseVisitor{
	private static Logger logger = LoggerFactory.getLogger(MainAnalyzer.class);
	// 儲存所找到的ignore Exception 
	private List<CSMessage> unprotectedMainList;	
	
	// AST tree的root(檔案名稱)
	private CompilationUnit root;
	
	public MainAnalyzer(CompilationUnit root){
		this.root = root;
		unprotectedMainList = new ArrayList<CSMessage>();
	}
	
	protected boolean visitNode(ASTNode node){
		try {
			switch (node.getNodeType()) {

				case ASTNode.METHOD_DECLARATION:		
					//根據Method declaration來找出是否有main function
					MethodDeclaration md = (MethodDeclaration)node;
					processMethodDeclaration(md);
					return true;
				default:
					return true;
			}
		} catch (Exception e) {
			logger.error("[visitNode] EXCEPTION ",e);
			return false;
		}
	}
	
	/**
	 * 尋找main function
	 */
	private void processMethodDeclaration(MethodDeclaration node){
		// parse AST tree看看是否有void main(java.lang.String[])
		if(node.resolveBinding().toString().contains("void main(java.lang.String[])")){
			
			List statement = node.getBody().statements();
			if(processMainFunction(statement)){
				//如果有找到code smell就將其加入
				CSMessage csmsg = new CSMessage(RLMarkerAttribute.CS_Unprotected_Main,null,											
						node.toString(),node.getStartPosition(),
						this.getLineNumber(node.getStartPosition()),null);
						this.unprotectedMainList.add(csmsg);				
			}
			
			
		}
	}
	
	/**
	 * 檢查main function中是否有code smell
	 * @param statement
	 * @return
	 */
	private boolean processMainFunction(List statement){
		if(statement.size() == 0){
			// main function裡面什麼就不是算是code smell
			return false;
		}else if(statement.size() == 1){
			if(statement.get(0) instanceof TryStatement)
				return false;
			return true;
		}else{
			/*如果Main Block有兩種以上的statement,就表示有東西沒被
			 * Try block包住,或者根本沒有try block
			 */
			return true;
		}
	}
	
	
	/**
	 * 根據startPosition來取得行數
	 */
	private int getLineNumber(int pos) {
		return root.getLineNumber(pos);
	}
	
	/**
	 * 取得unprotected Main的清單
	 */
	public List<CSMessage> getUnprotedMainList(){
		return unprotectedMainList;
	}
}
