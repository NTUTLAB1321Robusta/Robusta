package ntut.csie.csdet.visitor;

import java.util.ArrayList;
import java.util.List;

import ntut.csie.csdet.data.CSMessage;
import ntut.csie.csdet.preference.JDomUtil;
import ntut.csie.rleht.builder.ASTMethodCollector;
import ntut.csie.rleht.builder.RLMarkerAttribute;
import ntut.csie.rleht.common.RLBaseVisitor;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.TryStatement;
import org.jdom.Document;
import org.jdom.Element;

public class CarelessCleanUpAnalyzer extends RLBaseVisitor{
	// AST tree��root(�ɮצW��)
	private CompilationUnit root;
	
	// �x�s��쪺Careless Cleanup
	private List<CSMessage> CarelessCleanUpList;
	
	ASTMethodCollector methodCollector;
	
	//�x�s��쪺Method List
	List<ASTNode> methodList;
	
	//�O�_���smell
	private boolean flag = false;
	
	//�O�_�n����"�ϥΪ�����귽���{���X�b�禡��"
	private boolean isDetUserMethod=false;
	
	//�غc�l
	public CarelessCleanUpAnalyzer(CompilationUnit root){
		super(true);
		this.root=root;
		
		CarelessCleanUpList=new ArrayList<CSMessage>();
		
		//����class����method
		methodCollector=new ASTMethodCollector();
		this.root.accept(methodCollector);
		methodList = methodCollector.getMethodList();
		
		//���ouser���Careless CleanUp���]�w
		getCarelessCleanUp();
	}
	
	/*
	 * (non-Javadoc) 
	 * @see ntut.csie.rleht.views.ASTBaseVisitor#visitNode(org.eclipse.jdt.core.dom.ASTNode)
	 */
	protected boolean visitNode(ASTNode node){
		switch(node.getNodeType()){
			case ASTNode.TRY_STATEMENT:
				//Find the smell in the try node
				processTryStatement(node);
				//Find the smell in the catch node
				processCatchStatement(node);
				return false;		
			default:
				return true;
		}
	}
	
	/**
	 * �B�ztry�`�I����statement
	 */
	private void processTryStatement(ASTNode node){
		TryStatement trystat=(TryStatement) node;
		List<?> statementTemp=trystat.getBody().statements();
		//�קK��try�`�I��Nested try block���c�᪺���G,�G�P�_try�`�I����statement�ƶq���j��1
		if(statementTemp.size()>1){
			//�P�_try�`�I����statement�O�_smell
			judgeCarelessCleanUp(statementTemp);
			
		}
	}
	
	/**
	 * �P�_try�`�I����statement�O�_��smell
	 */
	private void judgeCarelessCleanUp(List<?> statementTemp){
		Statement statement;
		for(int i=0;i<statementTemp.size();i++){
			if(statementTemp.get(i) instanceof Statement){
				statement = (Statement) statementTemp.get(i);	
				//��try�`�I����Method Invocation
				statement.accept(new ASTVisitor(){
					public boolean visit(MethodInvocation node){
						//���oMethod Invocation��Expression
						Expression expression=node.getExpression();
						//�Yexpression��null,�h��method invocation��is.close()������
						//�_�h��close(is)������
						if(expression!=null){
							/*
							 * �Y�P�ɺ����H�U��ӱ���,�h��smell
							 * 1.��class�ӷ��D�ϥΪ̦ۭq
							 * 2.��k�W�٬�"close"
							 */
							boolean isFromSource=node.resolveMethodBinding().getDeclaringClass().isFromSource();
							String methodName=node.resolveMethodBinding().getName();
							if((!isFromSource)&&(methodName.equals("close"))){
								addMarker(node);
							}
						}else{
							if(isDetUserMethod){
								//�B�z�禡
								processCallMethod(node);
								//�Yflag��true,�h�Ө禡��smell
								if(flag){				
									addMarker(node);
									flag = false;									
								}
							}
						}
					return true;
					}
				});
			}
		}
	}
	
	/**
	 * �B�zcatch�`�I����statement
	 */
	private void processCatchStatement(ASTNode node){
		TryStatement trystat=(TryStatement) node;
		List<?> catchList=trystat.catchClauses();
		CatchClause catchclause=null;
			for(int i=0;i<catchList.size();i++){
				catchclause= (CatchClause) catchList.get(i);
				//�קKcareless cleanup�����X�{�bcatch�϶��Ĥ@�h,�b�o��|������
				judgeCarelessCleanUp(catchclause.getBody().statements());
				//�Ycareless cleanup�X�{�bcatch����try,�h�|�~��traversal�U�h
				visitNode(catchclause);
			}
		
	}

	/**
	 * �B�z�禡
	 * @param node
	 */
	private void processCallMethod(MethodInvocation node){
		//���o��Method Invocation���W��
		String methodInvName=node.resolveMethodBinding().getName();
		String methodDecName;
		MethodDeclaration md;
		//���oMethod List�����C��Method Declaration
		for(int i=0;i<methodList.size();i++){
			//���oMethod Declaration���W��
			md=(MethodDeclaration) methodList.get(i);
			methodDecName=md.resolveBinding().getName();
			//�Y�W�٬ۦP,�h�B�z��Method Invocation
			if(methodDecName.equals(methodInvName)){
				judgeCallMethod(md);	
			}
				
		}
	}
	
	/**
	 * �P�_�禡�O�_��smell
	 * @param md
	 */
	private void judgeCallMethod(MethodDeclaration md){
		//���o��Method Declaration���Ҧ�statement
		List<?> mdStatement=md.getBody().statements();
		if(mdStatement.size()!=0){
			for(int j=0;j<mdStatement.size();j++){
				//��禡����try�`�I
				if(mdStatement.get(j) instanceof TryStatement){
					TryStatement trystat=(TryStatement) mdStatement.get(j);
					List<?> statementTemp=trystat.getBody().statements();
					//��try�`�I����statement�O�_��Method Invocation���`�I
						if(!statementTemp.isEmpty()){
							Statement statement;
							for(int k=0;k<statementTemp.size();k++){
								if(statementTemp.get(k) instanceof Statement){
									statement = (Statement) statementTemp.get(k);
										statement.accept(new ASTVisitor(){
											public boolean visit(MethodInvocation node){
												/*
												 * �Y��method��smell,�h������귽���{���X�������H�U�T�ӱ���
												 * 1.expression������
												 * 2.��class�ӷ��D�ϥΪ̦ۭq
												 * 3.��k�W�٬�"close"
												 */
												Expression expression=node.getExpression();
												boolean isFromSource=node.resolveMethodBinding().getDeclaringClass().isFromSource();
												String methodName=node.resolveMethodBinding().getName();
												if(expression!=null&&(!isFromSource)&&(methodName.equals("close"))){											
													flag = true;
													return false;
												}
												return true;
											}
										});
								}
							}
						}
				}
			}
		}
	}
	
	/**
	 * �N��쪺smell�[�JList��
	 */
	private void addMarker(ASTNode node){
		CSMessage csmsg=new CSMessage(RLMarkerAttribute.CS_CARELESS_CLEANUP,null,											
				node.toString(),node.getStartPosition(),
				getLineNumber(node.getStartPosition()),null);
		CarelessCleanUpList.add(csmsg);
	}
	
	/**
	 * �ھ�startPosition�Ө��o���
	 */
	private int getLineNumber(int pos) {
		return root.getLineNumber(pos);
	}
	
	/**
	 * ���oCareless CleanUp��list
	 * @return list
	 */
	public List<CSMessage> getCarelessCleanUpList(){
		return CarelessCleanUpList;
	}
	
	/**
	 * ���oUser��Careless CleanUp���]�w
	 */
	private void getCarelessCleanUp(){
		Document docJDom = JDomUtil.readXMLFile();
		String methodSet="";
		if(docJDom != null){
			//�qXML��Ū�X���e���]�w
			Element root = docJDom.getRootElement();
			if (root.getChild(JDomUtil.CarelessCleanUpTag) != null) {
				Element rule=root.getChild(JDomUtil.CarelessCleanUpTag).getChild("rule");
				methodSet = rule.getAttribute(JDomUtil.detUserMethod).getValue();
			}			
			isDetUserMethod=methodSet.equals("Y");
		}
	}
}
