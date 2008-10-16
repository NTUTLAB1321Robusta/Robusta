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
	
	// AST tree��root(�ɮצW��)
	private CompilationUnit root;
    
	// �x�s�ҧ�쪺ignore Exception 
	private List<CSMessage> codeSmellList;
	
	public CodeSmellAnalyzer(CompilationUnit root){
		super(true);
		this.root = root;
		codeSmellList = new ArrayList<CSMessage>();
	}
	
	/**
	 * ���@���constructor,����ȷ|���ݭn��h����T
	 * �p�G�S���i�H�N����ѱ�
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
//				System.out.println("�i====TRY_STATEMENT====�j");
//				System.out.println(node.toString());
				this.processTryStatement(node);
				return true;
//			case ASTNode.CATCH_CLAUSE:
//				System.out.println("�i====CATCH_CLAUSE====�j");
//				System.out.println(node.toString());
//				return true;
			default:
				//return true�h�~��X�ݨ�node���l�`�I,false�h���~��
				return true;
		}	
	}
		
	/**
	 * parse try block�����e
	 * @param node
	 */
	private void processTryStatement(ASTNode node){
		
		//�B�ztry block
		TryStatement trystat = (TryStatement) node;		
		CodeSmellAnalyzer visitor = new CodeSmellAnalyzer(this.root,true,0);
		trystat.getBody().accept(visitor);
		//System.out.println("�ithis.root�j=====>"+this.root.getJavaElement().getElementName());
		//System.out.println("�itry Block�j");
		//System.out.println(trystat.getBody().toString());		
		
		//�B�zcatch block
		List catchList = trystat.catchClauses();
		CatchClause cc = null;		
		for (int i = 0, size = catchList.size(); i < size; i++) {
			cc = (CatchClause) catchList.get(i);
			//System.out.println("�iCatch Clause�j");
			//System.out.println(cc.getBody().toString());
			SingleVariableDeclaration svd = (SingleVariableDeclaration) cc
			.getStructuralProperty(CatchClause.EXCEPTION_PROPERTY);
			//�P�_�O�_��ignore Exception,�O���ܴN�[�J��List��
			judgeIgnoreEx(cc,svd);
			visitor = new CodeSmellAnalyzer(this.root,true,0);
			cc.getBody().accept(visitor);
			
		}
		
		// �B�zFinally Block
		Block finallyBlock = trystat.getFinally();
		if (finallyBlock != null) {
			//System.out.println("�iFinally Block�j");
			visitor = new CodeSmellAnalyzer(this.root,true,0);
			finallyBlock.accept(visitor);			
			
		}
	}
	
	/**
	 * �P�_�o��catch block�O���Oignore EX
	 * @param cc : catch block����T
	 * @param svd : ���ӥi��|�Ψ�binding,�ҥH���s�_��
	 */
	private void judgeIgnoreEx(CatchClause cc,SingleVariableDeclaration svd ){
		List statementTemp = cc.getBody().statements();
		if(statementTemp.size() == 0){			
			CSMessage csmsg = new CSMessage(RLMarkerAttribute.CS_INGNORE_EXCEPTION,svd.resolveBinding().getType(),											
					cc.toString(),cc.getStartPosition(),this.getLineNumber(cc.getStartPosition()),svd.getType().toString());
			this.codeSmellList.add(csmsg);
//			System.out.println("�iFind Ignore Exception�j");
//			System.out.println("�iIgnore Ex line�j====>"+this.getLineNumber(cc.getStartPosition()));
		}
	}
	
	/**
	 * �ھ�startPosition�Ө��o���
	 */
	private int getLineNumber(int pos) {
		return root.getLineNumber(pos);
	}
	
	/**
	 * ���oignore Exception��List
	 */
	public List<CSMessage> getIgnoreExList(){
		return codeSmellList;
	}
}
