package ntut.csie.csdet.visitor;

import java.util.ArrayList;
import java.util.List;

import ntut.csie.csdet.data.CSMessage;
import ntut.csie.rleht.builder.RLMarkerAttribute;
import ntut.csie.rleht.common.RLBaseVisitor;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.TryStatement;
/**
 * ��M�פ���Careless CleanUp
 * @author yimin
 */
public class CarelessCleanUpAnalyzer extends RLBaseVisitor{
	// AST tree��root(�ɮצW��)
	private CompilationUnit root;
	// �x�s��쪺Unguaranteed Cleanup
	private List<CSMessage> CarelessCleanUpList;
	
	//Constructor
	public CarelessCleanUpAnalyzer(CompilationUnit root){
		super(true);
		this.root = root;
		CarelessCleanUpList=new ArrayList<CSMessage>();
	}
	
	/*
	 * (non-Javadoc) 
	 * @see ntut.csie.rleht.views.ASTBaseVisitor#visitNode(org.eclipse.jdt.core.dom.ASTNode)
	 */
	protected boolean visitNode(ASTNode node){
		switch(node.getNodeType()){
			//Find the smell in the try node
			case ASTNode.TRY_STATEMENT:
				processTryStatement(node);
				return true;
			//Find the smell in the catch node
			case ASTNode.CATCH_CLAUSE:
				processCatchStatement(node);
				return true;
			default:
				return true;
		}
	}
	
	/**
	 * �P�_try�`�I����statement�O�_��XXX.close()
	 */
	private void processTryStatement(ASTNode node){
		TryStatement trystat=(TryStatement) node;
		List<?> statementTemp=trystat.getBody().statements();

		if(statementTemp.size()!=0){
			//�P�_�O�_��Careless CleanUp
			judgeCarelessCleanUp(statementTemp);
		}
	}
	
	/**
	 * �P�_�O�_��Careless CleanUp
	 */
	private void judgeCarelessCleanUp(List<?> statementTemp){
		//��C��statementTemp��O�_���ŦX������xxx.close()
		for(int i=0;i<statementTemp.size();i++){
			if(statementTemp.get(i) instanceof Statement){
				Statement expStatement = (Statement) statementTemp.get(i);
				//��MMethod Invocation��node
//				CarelessVisitor vistor = new CarelessVisitor(root);
//				expStatement.accept(vistor);
//				CarelessCleanUpList = vistor.getCarelessCleanUpList();
				expStatement.accept(new ASTVisitor(true){
					public boolean visit(MethodInvocation node) {
					
						//�P�_class�ӷ��O�_��source code
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
						return true;
					}
				}
				);
			}
		}
	}
	
	/**
	 * �P�_catch�`�I����statement�O�_��XXX.close()
	 */
	private void processCatchStatement(ASTNode node){
		//�ഫ��catch node
		CatchClause cc=(CatchClause) node;
		//���ocatch node��statement
		List<?> statementTemp=cc.getBody().statements();
		if(statementTemp.size()!=0){
			//�P�_�O�_��Careless CleanUp type
			judgeCarelessCleanUp(statementTemp);
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
