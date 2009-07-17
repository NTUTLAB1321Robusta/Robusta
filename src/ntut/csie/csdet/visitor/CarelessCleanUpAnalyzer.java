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
import org.eclipse.jdt.core.dom.Expression;
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
	
	// �x�s��쪺Careless Cleanup
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
				return false;		
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

		if(statementTemp.size()!= 0){
			//�P�_�O�_��Careless CleanUp
			judgeCarelessCleanUp(statementTemp);
		}
		//�䧹try�`�I����,�h��catch�`�I,��������finally block
		List<?> catchList = trystat.catchClauses();
		CatchClause cc = null;
		for (int i = 0, size = catchList.size(); i < size; i++) {
			cc = (CatchClause) catchList.get(i);
			//�קKcareless cleanup�����X�{�bcatch�϶��Ĥ@�h,�b�o��|������
			judgeCarelessCleanUp(cc.getBody().statements());
			//�Ycareless cleanup�X�{�bcatch����try,�h�|�~��traversal�U�h
			CarelessCleanUpAnalyzer visitor = new CarelessCleanUpAnalyzer(root);
			cc.getBody().accept(visitor);	
			//�Ncatch�϶�����쪺��T��merge
			this.mergeCS(visitor.getCarelessCleanUpList());
			
		}
	}
	
	/**
	 * �P�_�O�_��Careless CleanUp
	 */
	private void judgeCarelessCleanUp(List<?> statementTemp){
		//��C��statementTemp��O�_���ŦX������xxx.close()
		for(int i=0;i<statementTemp.size();i++){
			if(statementTemp.get(i) instanceof Statement){
				Statement statement = (Statement) statementTemp.get(i);			
				//��MMethod Invocation��node
				statement.accept(new ASTVisitor(){
					public boolean visit(MethodInvocation node) {
						//�P�_class�ӷ��O�_��source code
						boolean isFromSource=node.resolveMethodBinding().getDeclaringClass().isFromSource();						
						//���oMethod���W��
						String methodName = node.resolveMethodBinding().getName();
						//���oExpression
						Expression exp=node.getExpression();
						
						/*
						 * �������󶷦P�ɺ������
						 * 1.��class�ӷ��D�ϥΪ̦ۭq
						 * 2.��k�W�٬�"close"
						 */
						if((exp!=null)&&(!isFromSource)&& methodName.equals("close")){
							//�إߤ@��Careless CleanUp type
							CSMessage csmsg=new CSMessage(RLMarkerAttribute.CS_CARELESS_CLEANUP,null,											
									node.toString(),node.getStartPosition(),
									getLineNumber(node.getStartPosition()),null);
							CarelessCleanUpList.add(csmsg);
						}//else if((exp==null)&&(isFromSource)){
//							if(visitMethodNode(node)){
//								CSMessage csmsg=new CSMessage(RLMarkerAttribute.CS_CARELESS_CLEANUP,null,											
//										node.toString(),node.getStartPosition(),
//										getLineNumber(node.getStartPosition()),null);
//								CarelessCleanUpList.add(csmsg);
//							}
//						}
						return true;
					}
				}
				);
			}
		}
	}
	
	public boolean visitMethodNode(MethodInvocation node){
		
		return true;
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
	
	/**
	 * �N��쪺smell��T�@merge
	 * @param childInfo
	 */
	private void mergeCS(List<CSMessage> childInfo){
		if (childInfo == null || childInfo.size() == 0) {
			return;
		}
		
		for(CSMessage msg : childInfo){
			this.CarelessCleanUpList.add(msg);
		}
	}
	
	
	public void clear(){
		if(CarelessCleanUpList != null)
			CarelessCleanUpList.clear();
	}
}
