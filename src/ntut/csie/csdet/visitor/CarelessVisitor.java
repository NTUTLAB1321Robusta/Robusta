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
	
	// AST tree��root(�ɮצW��)
	private CompilationUnit root;
	
	public CarelessVisitor(CompilationUnit root){
		this.root = root;
		CarelessCleanUpList = new ArrayList<CSMessage>();
	}
	
	protected boolean visitNode(ASTNode node){
		try {
			switch (node.getNodeType()) {
			case ASTNode.METHOD_INVOCATION:		
				//�ھ�Method declaration�ӧ�X�O�_��main function
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
		
		//���oMethod���W��
		String methodName = node.resolveMethodBinding().getName();

		/*
		 * �������󶷦P�ɺ������
		 * 1.��class�ӷ��D�ϥΪ̦ۭq
		 * 2.��k�W�٬�"close"
		 */
		if((!isFromSource)&& methodName.equals("close")){
			//�إߤ@��Careless CleanUp type
			CSMessage csmsg=new CSMessage(RLMarkerAttribute.CS_CARELESS_CLEANUP,null,											
					node.toString(),node.getStartPosition(),
					getLineNumber(node.getStartPosition()),null);
			CarelessCleanUpList.add(csmsg);
		}
	}
	
	/**
	 * ���oCareless CleanUp��list
	 * @return list
	 */
	public List<CSMessage> getCarelessCleanUpList(){
		return CarelessCleanUpList;
	}
	
	/**
	 * �ھ�startPosition�Ө��o���
	 */
	private int getLineNumber(int pos) {
		return root.getLineNumber(pos);
	}
}
