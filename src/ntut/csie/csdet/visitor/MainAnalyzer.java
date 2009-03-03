package ntut.csie.csdet.visitor;

import java.util.ArrayList;
import java.util.List;

import ntut.csie.csdet.data.CSMessage;
import ntut.csie.rleht.builder.RLMarkerAttribute;
import ntut.csie.rleht.common.RLBaseVisitor;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IExtendedModifier;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.TryStatement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MainAnalyzer extends RLBaseVisitor{
	private static Logger logger = LoggerFactory.getLogger(MainAnalyzer.class);
	// �x�s�ҧ�쪺ignore Exception 
	private List<CSMessage> unprotectedMainList;	
	
	// AST tree��root(�ɮצW��)
	private CompilationUnit root;
	
	public MainAnalyzer(CompilationUnit root){
		this.root = root;
		unprotectedMainList = new ArrayList<CSMessage>();
	}
	
	protected boolean visitNode(ASTNode node){
		try {
			switch (node.getNodeType()) {
				case ASTNode.METHOD_DECLARATION:		
					//�ھ�Method declaration�ӧ�X�O�_��main function
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
	 * �M��main function
	 */
	private void processMethodDeclaration(MethodDeclaration node){
		// parse AST tree�ݬݬO�_��void main(java.lang.String[])
		if(node.resolveBinding().toString().contains("void main(java.lang.String[])")){
			List statement = node.getBody().statements();
			if(processMainFunction(statement)){
				//�p�G�����code smell�N�N��[�J
				CSMessage csmsg = new CSMessage(RLMarkerAttribute.CS_UNPROTECTED_MAIN,null,											
						node.toString(),node.getStartPosition(),
						this.getLineNumber(node.getStartPosition(),node),null);
						this.unprotectedMainList.add(csmsg);				
			}
		}
	}
	
	/**
	 * �ˬdmain function���O�_��code smell
	 * @param statement
	 * @return
	 */
	private boolean processMainFunction(List statement){
		if(statement.size() == 0){
			// main function�̭����򳣨S���N���O��Ocode smell
			return false;
		}else if(statement.size() == 1){
			if(statement.get(0) instanceof TryStatement){
				TryStatement ts = (TryStatement)statement.get(0);
				List catchList = ts.catchClauses();
				for(int i=0;i<catchList.size();i++){
					CatchClause cc = (CatchClause)catchList.get(i);
					SingleVariableDeclaration svd = cc.getException();
					//�p�G��try�٭n�P�_catch�O�_��catch(Exception ..)
					if(svd.getType().toString().equals("Exception")){
						//�p�G��catch(Exception ..)�N����code smell
						return false;
					}					
				}
			}				
			return true;
		}else{
			/* �p�GMain Block����إH�W��statement,�N��ܦ��F��S�Q
			 * Try block�]��,�Ϊ̮ڥ��S��try block
			 */
			return true;
		}
	}
	
	
	/**
	 * �ھ�startPosition�Ө��o���
	 */
	private int getLineNumber(int pos,MethodDeclaration method) {
		List<IExtendedModifier> modifiers = method.modifiers();
		for (int i = 0, size = modifiers.size(); i < size; i++) {
			//�p�G�쥻main function�W��annotation����,marker�|�ܦ��Цbannotation����
			//�ҥH�z�L�M��public���檺��m,�Ө��omarker�n�Хܪ����
			if ((!modifiers.get(i).isAnnotation()) && (modifiers.get(i).toString().contains("public"))) {
				ASTNode temp = (ASTNode)modifiers.get(i);
				return root.getLineNumber(temp.getStartPosition());
			}
		}
		//�p�G�S��annotation,�N�i�H�������omain function����
		return root.getLineNumber(pos);
	}
	
	/**
	 * ���ounprotected Main���M��
	 */
	public List<CSMessage> getUnprotedMainList(){
		return unprotectedMainList;
	}
}
