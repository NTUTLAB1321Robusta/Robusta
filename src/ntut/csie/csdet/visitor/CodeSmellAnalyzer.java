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
 * ��M�פ���Ignore Exception
 * @author chewei
 */

public class CodeSmellAnalyzer extends RLBaseVisitor {
	
	// AST tree��root(�ɮצW��)
	private CompilationUnit root;
    
	// �x�s�ҧ�쪺ignore Exception 
	private List<CSMessage> codeSmellList;
	
	// �x�s�ҧ�쪺Dummy handler
	private List<CSMessage> dummyHandler;
	
	public CodeSmellAnalyzer(CompilationUnit root){
		super(true);
		this.root = root;
		codeSmellList = new ArrayList<CSMessage>();
		dummyHandler = new ArrayList<CSMessage>();
	}
	
	/**
	 * ���@���constructor,����ȷ|���ݭn��h����T
	 * �p�G�S���i�H�N����ѱ�
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
			//TODO ���ըS���D�N�i�H��TRY_STATEMENT�o�Ӧa��屼
			case ASTNode.TRY_STATEMENT:
//				System.out.println("�i====TRY_STATEMENT====�j");
//				System.out.println(node.toString());
				//this.processTryStatement(node);
				return true;
			case ASTNode.CATCH_CLAUSE:
				processCatchStatement(node);
				return true;
			default:
				//return true�h�~��X�ݨ�node���l�`�I,false�h���~��
				return true;
		}	
	}
		
	/**
	 * parse try block�����e
	 * @param node
	 */
	//TODO ���ըS���D�N�i�H��TRY_STATEMENT�o�Ӧa��屼
//	private void processTryStatement(ASTNode node){
//		
//		//�B�ztry block
//		TryStatement trystat = (TryStatement) node;		
//		CodeSmellAnalyzer visitor = new CodeSmellAnalyzer(this.root,true,0);
//		trystat.getBody().accept(visitor);	
//		
//		//�B�zcatch block
//		List catchList = trystat.catchClauses();
//		CatchClause cc = null;		
//		for (int i = 0, size = catchList.size(); i < size; i++) {
//			cc = (CatchClause) catchList.get(i);
//			SingleVariableDeclaration svd = (SingleVariableDeclaration) cc
//			.getStructuralProperty(CatchClause.EXCEPTION_PROPERTY);
//			//�P�_�O�_��ignore Exception,�O���ܴN�[�J��List��
//			judgeIgnoreEx(cc,svd);
//			
//			visitor = new CodeSmellAnalyzer(this.root,true,0);
//			cc.getBody().accept(visitor);
//		
//		}
//		
//		// �B�zFinally Block
//		Block finallyBlock = trystat.getFinally();
//		if (finallyBlock != null) {
//			visitor = new CodeSmellAnalyzer(this.root,true,0);
//			finallyBlock.accept(visitor);			
//			
//		}
//	}
	
	/**
	 * �h�M��catch���`�I,�åB�P�_�`�I����statement�O�_����
	 * @param node
	 */
	private void processCatchStatement(ASTNode node){
		CatchClause cc = (CatchClause) node;
		SingleVariableDeclaration svd = (SingleVariableDeclaration) cc
		.getStructuralProperty(CatchClause.EXCEPTION_PROPERTY);
		judgeIgnoreEx(cc,svd);
	}
	
	/**
	 * �P�_�o��catch block�O���Oignore EX
	 * @param cc : catch block����T
	 * @param svd : throw exception�|�Ψ�
	 */
	private void judgeIgnoreEx(CatchClause cc,SingleVariableDeclaration svd ){
		List statementTemp = cc.getBody().statements();
		if(statementTemp.size() == 0){			
			CSMessage csmsg = new CSMessage(RLMarkerAttribute.CS_INGNORE_EXCEPTION,svd.resolveBinding().getType(),											
					cc.toString(),cc.getStartPosition(),this.getLineNumber(cc.getStartPosition()),svd.getType().toString());
			this.codeSmellList.add(csmsg);
			System.out.println("�iIgnore Ex Position�j====>"+this.getLineNumber(cc.getStartPosition()));
		}else{
	        /*------------------------------------------------------------------------*
            -  ���pstatement���O�Ū�,��ܦ��i��s�bdummy handler,���t�~�g�@��class�Ӱ���,��]�O
                 ���Ʊ�nparse�C��method�ܦh��,code�����]�|�W�[,�ҥH�N�g�b�o��
            *-------------------------------------------------------------------------*/			
			for(int i=0;i<statementTemp.size();i++){
				if(statementTemp.get(i) instanceof ExpressionStatement){
					ExpressionStatement statement = (ExpressionStatement) statementTemp.get(i);

					if(statement.getExpression().toString().contains("printStackTrace")){
						System.out.println("�iPosition�j===>"+statement.getExpression().toString().indexOf("printStackTrace"));
						System.out.println("�iClass Name�j====>"+this.root.getJavaElement().getElementName());
						System.out.println("�iDH Line number�j====>"+this.getLineNumber(statement.getStartPosition()));
						System.out.println("�iDummy Handler�j====>"+statement.getExpression().toString());
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
