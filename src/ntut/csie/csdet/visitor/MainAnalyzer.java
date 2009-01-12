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
				CSMessage csmsg = new CSMessage(RLMarkerAttribute.CS_Unprotected_Main,null,											
						node.toString(),node.getStartPosition(),
						this.getLineNumber(node.getStartPosition()),null);
						this.unprotectedMainList.add(csmsg);				
			}
			
			
		}
	}
	
	/**
	 * �ˬdmain function���O�_��try block
	 * @param statement
	 * @return
	 */
	private boolean processMainFunction(List statement){
		if(statement.size() == 0){
			// main function�̭����򳣨S���N�Ocode smell
			return true;
		}else{
			for(int i=0;i<statement.size();i++){
				//���pmain function����try catch��,�N�����Ocode smell
				if(statement.get(i) instanceof TryStatement){
					return false;
				}
			}
			// for loop�]�����S���N����code smell
			return true;
		}
	}
	
	
	/**
	 * �ھ�startPosition�Ө��o���
	 */
	private int getLineNumber(int pos) {
		return root.getLineNumber(pos);
	}
	
	/**
	 * ���ounprotected Main���M��
	 */
	public List<CSMessage> getUnprotedMainList(){
		return unprotectedMainList;
	}
}
