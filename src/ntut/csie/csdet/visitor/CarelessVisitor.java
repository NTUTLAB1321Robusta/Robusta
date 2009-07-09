package ntut.csie.csdet.visitor;

import java.util.ArrayList;
import java.util.List;

import ntut.csie.csdet.data.CSMessage;
import ntut.csie.rleht.builder.RLMarkerAttribute;
import ntut.csie.rleht.common.RLBaseVisitor;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;

public class CarelessVisitor extends RLBaseVisitor{

	private List<CSMessage> CarelessCleanUpList;	
	
	// AST tree的root(檔案名稱)
	private CompilationUnit root;
	
	public CarelessVisitor(CompilationUnit root){
		this.root = root;
		CarelessCleanUpList = new ArrayList<CSMessage>();
	}
	
	protected boolean visitNode(ASTNode node){
		try {
			switch (node.getNodeType()) {
			case ASTNode.METHOD_INVOCATION:		
				//根據Method declaration來找出是否有main function
				MethodInvocation md = (MethodInvocation)node;
				processMethodNode(md);
				System.out.println("[node type]===>"+node.toString());
				return true;
			default:
				return true;
			}
		} catch (Exception e) {
			return false;
		} 
		
	}
	
	private void processMethodNode(MethodInvocation node){
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
