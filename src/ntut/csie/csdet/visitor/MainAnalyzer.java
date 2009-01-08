package ntut.csie.csdet.visitor;

import java.util.ArrayList;
import java.util.List;

import ntut.csie.csdet.data.CSMessage;
import ntut.csie.rleht.builder.RLMarkerAttribute;
import ntut.csie.rleht.common.RLBaseVisitor;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
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
	
	
	private void processMethodDeclaration(MethodDeclaration node){
		
		if(node.resolveBinding().toString().contains("void main(java.lang.String[])")){
			CSMessage csmsg = new CSMessage(RLMarkerAttribute.CS_Unprotected_Main,null,											
			node.toString(),node.getStartPosition(),
			this.getLineNumber(node.getStartPosition()),null);
			this.unprotectedMainList.add(csmsg);
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
