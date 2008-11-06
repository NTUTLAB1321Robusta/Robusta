package ntut.csie.csdet.visitor;

import java.util.ArrayList;
import java.util.List;

import ntut.csie.csdet.data.CSMessage;
import ntut.csie.csdet.preference.JDomUtil;
import ntut.csie.rleht.builder.RLMarkerAttribute;
import ntut.csie.rleht.common.RLBaseVisitor;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.ThrowStatement;
import org.jdom.Attribute;
import org.jdom.Element;

/**
 * ��M�פ���Ignore Exception
 * @author chewei
 */

public class CodeSmellAnalyzer extends RLBaseVisitor {
	
	// AST tree��root(�ɮצW��)
	private CompilationUnit root;
    
	// �x�s�ҧ�쪺ignore Exception 
	private List<CSMessage> codeSmellList;
	
	// �x�s�ҧ�쪺dummy handler
	private List<CSMessage> dummyList;
	
	public CodeSmellAnalyzer(CompilationUnit root){
		super(true);
		this.root = root;
		codeSmellList = new ArrayList<CSMessage>();
		dummyList = new ArrayList<CSMessage>();
	}
	
	/**
	 * ���@���constructor,����ȷ|���ݭn��h����T
	 * �p�G�S���i�H�N����ѱ�
	 */
	public CodeSmellAnalyzer(CompilationUnit root,boolean currentMethod,int pos){
		super(true);
		this.root = root;
		codeSmellList = new ArrayList<CSMessage>();
		dummyList = new ArrayList<CSMessage>();
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
		//�ഫ��catch node
		CatchClause cc = (CatchClause) node;
		//����catch(Exception e)�䤤��e
		SingleVariableDeclaration svd = (SingleVariableDeclaration) cc
		.getStructuralProperty(CatchClause.EXCEPTION_PROPERTY);
		//�P�_�o��catch�O�_��ignore exception or dummy handler
		judgeIgnoreEx(cc,svd);
	}
	
	/**
	 * �P�_�o��catch block�O���Oignore EX
	 * @param cc : catch block����T
	 * @param svd : throw exception�|�Ψ�
	 */
	private void judgeIgnoreEx(CatchClause cc,SingleVariableDeclaration svd ){
		List statementTemp = cc.getBody().statements();
		// �p�Gcatch statement�̭��O�Ū���,��ܬOignore exception
		if(statementTemp.size() == 0){	
			//�إߤ@��ignore exception type
			CSMessage csmsg = new CSMessage(RLMarkerAttribute.CS_INGNORE_EXCEPTION,svd.resolveBinding().getType(),											
									cc.toString(),cc.getStartPosition(),
									this.getLineNumber(cc.getStartPosition()),svd.getType().toString());
			this.codeSmellList.add(csmsg);
		}else{
	        /*------------------------------------------------------------------------*
            -  ���pstatement���O�Ū�,��ܦ��i��s�bdummy handler,���t�~�g�@��class�Ӱ���,
                 ��]�O���Ʊ�bRLBilder�nparse�C��method�ܦh��,code�����]�|�W�[,�ҥH�N�g�b�o��
            *-------------------------------------------------------------------------*/	
			judgeDummyHandler(statementTemp,cc,svd);
		}
	}
	
	/**
	 * �P�_�o��catch���O�_��dummy handler
	 * @param statementTemp
	 * @param cc
	 * @param svd
	 */
	private void judgeDummyHandler(List statementTemp,CatchClause cc,SingleVariableDeclaration svd){
        /*------------------------------------------------------------------------*
        -  ���]�o��catch�̭���throw�F��,�N�P�w���Odummy handler
             �p�G�u�n���@��e.printStackTrace�Ϊ̲ŦXuser�ҳ]�w������,�N�P�w��dummy handler  
        *-------------------------------------------------------------------------*/	

		// �Q�Φ�flag�ӰO���쩳�[�J�F�h�֪�dummy handler
		int flag = 0;

		for(int i=0;i<statementTemp.size();i++){
			//���oExpression statement,�]��e.printstackTrace�o�����O��o�ث��A			
			if(statementTemp.get(i) instanceof ExpressionStatement){
				ExpressionStatement statement = (ExpressionStatement) statementTemp.get(i);
				//�����oxml�ɪ��]�w,false��ܹw�]�u��e.printStackTrace()
				if(getDummySettings()){
					// if true,�N���e.printStackTrace and system.out.print() and println 
					if(statement.getExpression().toString().contains("System.out.print")||
							statement.getExpression().toString().contains("printStackTrace")){					
						//�إ�Dummy handler��type
//						if(dummyList.size() == 0){
						CSMessage csmsg = new CSMessage(RLMarkerAttribute.CS_DUMMY_HANDLER,
								svd.resolveBinding().getType(),cc.toString(),cc.getStartPosition(),
								this.getLineNumber(statement.getStartPosition()),svd.getType().toString());
							this.dummyList.add(csmsg);
							// �s�W�@��dummy handler
							flag++;
//						}
					}
				}
				else{
					if(statement.getExpression().toString().contains("printStackTrace")){					
						//�إ�Dummy handler��type
						CSMessage csmsg = new CSMessage(RLMarkerAttribute.CS_DUMMY_HANDLER,
								svd.resolveBinding().getType(),cc.toString(),cc.getStartPosition(),
								this.getLineNumber(statement.getStartPosition()),svd.getType().toString());
						this.dummyList.add(csmsg);
						// �s�W�@��dummy handler
						flag++;
					}
				}

			}else if(statementTemp.get(i) instanceof ThrowStatement){
				// �I�즳throw �F��X��,�N�P�w���Odummy handler
				// �i��|�I�즳e.printStackTrace(),���U�@��Sthrow�F��X��
				// �ҥH�����o���e�[�F�X��dummy handler,���۱qlist�̧��ݶ}�l����
				int size = this.dummyList.size()-1;
				for(int x=0;x<flag;x++){
					this.dummyList.remove(size-x);
				}
				
			}
		}
	}

	/**
	 * �Nuser���dummy handler���]�w�s�U��
	 * @return
	 */
	private boolean getDummySettings(){
		Element root = JDomUtil.createXMLContent();
		// �p�G�Onull���xml�ɬO��ئn��,�٨S��dummy handler��tag,�������X�h
		if(root.getChild(JDomUtil.DummyHandlerTag) == null){
			return false;
		}else{
			// �o�̪�ܤ��e�ϥΪ̤w�g���]�w�Lpreference�F
			Element dummyHandler = root.getChild(JDomUtil.DummyHandlerTag);
			Element rule = dummyHandler.getChild("rule");
			Attribute systemout = rule.getAttribute(JDomUtil.systemoutprint);
			String settings = systemout.getValue();
			if(settings.equals("Y")){
				return true;	
			}else{
				return false;
			}
		}
	}
	
	/**
	 * �ھ�startPosition�Ө��o���
	 */
	private int getLineNumber(int pos) {
		return root.getLineNumber(pos);
	}
	
	/**
	 * ���o,dummy handler��List
	 */
	public List<CSMessage> getIgnoreExList(){
		return codeSmellList;
	}
	
	/**
	 * ���odummy handler��List
	 */
	public List<CSMessage> getDummyList(){
		return dummyList;
	}
}
