package ntut.csie.csdet.visitor;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

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
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.TryStatement;
import org.jdom.Attribute;
import org.jdom.Element;

/**
 * ��M�פ���Careless CleanUp
 * @author chenyimin
 */
public class CarelessCleanUpAnalyzer extends RLBaseVisitor{
	
	// AST tree��root(�ɮצW��)
	private CompilationUnit root;
	
	// �x�s��쪺Careless Cleanup
	private List<CSMessage> CarelessCleanUpList;
	
	ASTMethodCollector methodCollector;
	
	//�x�s��쪺Method List
	List<ASTNode> methodList;
	
	//�O�_���Careless CleanUp
	private boolean flag = false;
	
	//�O�_�n����"�ϥΪ�����귽���{���X�b�禡��"
	private boolean isDetUserMethod=false;
	
	//�x�s"�ϥΪ̭n������library�W��"�M"�O�_�n������library"
	private TreeMap<String, Integer> libMap = new TreeMap<String, Integer>();
	
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
				//Find the smell in the try Block
				processTryStatement(node);
				//Find the smell in the catch Block
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
		//���otry Block����Statement
		TryStatement trystat=(TryStatement) node;
		List<?> statementTemp=trystat.getBody().statements();
		//�קK��try�`�I��Nested try block���c�᪺���G,�G�P�_try�`�I����statement�ƶq���j��1
		if(statementTemp.size()>1){
			//�P�_try�`�I����statement�O�_��Careless CleanUp
			judgeCarelessCleanUp(statementTemp);
		}
	}
	
	/**
	 * �P�_try�`�I����statement�O�_��Careless CleanUp
	 */
	private void judgeCarelessCleanUp(List<?> statementTemp){
		Statement statement;
		for(int i=0;i<statementTemp.size();i++){
			if(statementTemp.get(i) instanceof Statement){
				statement = (Statement) statementTemp.get(i);
				//�Y��statement�]�t�ϥΪ̦ۭq��Rule,�h��smell
				//�_�h��statement���O�_��Method Invocation
				if(findBindingLib(statement)){
					//findExceptionInfo((ASTNode) statement);
					addMarker(statement);
				}else{
					statement.accept(new ASTVisitor(){
						public boolean visit(MethodInvocation node){
							//���oMethod Invocation��Expression
							Expression expression=node.getExpression();
							//1.Expression��null,�h��method invocation��object.close()������
							//2.Expression����null,�h��close(object)������
							if(expression!=null){
								/*
								 * �Y�P�ɺ����H�U��ӱ���,�h��object.close(),����smell
								 * 1.class�D�ϥΪ̦ۭq
								 * 2.��k�W�٬�"close"
								 */
								boolean isFromSource=node.resolveMethodBinding().getDeclaringClass().isFromSource();
								String methodName=node.resolveMethodBinding().getName();
								
								if((!isFromSource)&&(methodName.equals("close"))){
									addMarker(node);
								}
							} else {
								//�ϥΪ̬O�_�n�t�~��������귽���{���X�O�_�b�禡��
								if(isDetUserMethod){
									//�B�z�Q�I�s���禡
									processCalledMethod(node);
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
	 * �B�z�Q�I�s���禡
	 * @param node
	 */
	private void processCalledMethod(MethodInvocation node){
		//���o��Method Invocation���W��
		String methodInvName=node.resolveMethodBinding().getName();
		String methodDecName;
		MethodDeclaration md;
		//�O�_��Method Declaration��Name�PMethod Invocation��Name�ۦP
		for(int i=0;i<methodList.size();i++){
			//���oMethod Declaration���W��
			md=(MethodDeclaration) methodList.get(i);
			methodDecName=md.resolveBinding().getName();
			//�Y�W�٬ۦP,�h�B�z��Method Invocation
			if(methodDecName.equals(methodInvName)){
				judgeCalledMethod(md);	
			}
				
		}
	}
	
	/**
	 * �P�_�Q�I�s���禡�O�_��Careless CleanUp
	 * @param MethodDeclaration
	 */
	private void judgeCalledMethod(MethodDeclaration md) {
		//���o��Method Declaration���Ҧ�statement
		List<?> mdStatement = md.getBody().statements();
		//���o��Method Declaration��thrown exception name
		List<?> thrown=md.thrownExceptions();
		//TODO ��zCode
		if (mdStatement.size() != 0) {
			for (int j = 0; j < mdStatement.size(); j++) {
				//��禡����try�`�I
				if(mdStatement.get(j) instanceof TryStatement){
					//���otry Block����Statement
					TryStatement trystat=(TryStatement) mdStatement.get(j);
					List<?> statementTemp=trystat.getBody().statements();
					//��statement���O�_��Method Invocation
						if(!statementTemp.isEmpty()){
							Statement statement;
							for(int k=0;k<statementTemp.size();k++){
								if(statementTemp.get(k) instanceof Statement){
									statement = (Statement) statementTemp.get(k);
										statement.accept(new ASTVisitor(){
											public boolean visit(MethodInvocation node){
												/*
												 * �Y�Q�I�s���禡��Careless CleanUp,�h������귽���{���X�������H�U�T�ӱ���
												 * 1.expression������
												 * 2.class�D�ϥΪ̦ۭq
												 * 3.��k�W�٬�"close"
												 */
												Expression expression=node.getExpression();
												boolean isFromSource=node.resolveMethodBinding().getDeclaringClass().isFromSource();
												String methodName=node.resolveMethodBinding().getName();
												if(expression!=null&&(!isFromSource)&&(methodName.equals("close"))){											
													flag = true;
													//�Y�w�g��즳CarelessCleanUp,�N���A���U�@�h��
													return false;
												}
												return true;
											}
										});
								}
							}
						}
				}

				/* �Y�����šA�N����X�ҥ~
				 * private void closeFile(FileOutputStream fos) throws IOException {
				 * 	fos.close();
				 * }
				 */
				if(thrown.size()!=0){
					//object.close �Ҭ�Expression Statement
					if(mdStatement.get(j) instanceof ExpressionStatement){
						ExpressionStatement es=(ExpressionStatement) mdStatement.get(j);
						es.accept(new ASTVisitor(){
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
	/**
	 * �����ϥΪ̦ۭq��Rule
	 * @param statement
	 * @return boolean
	 */
	private boolean findBindingLib(Statement statement){
		ASTBinding visitor = new ASTBinding(libMap);
		statement.accept(visitor);
		if(visitor.getResult()){
			return true;
		}else{
			return false;
		}
	}
	
	/**
	 * �N��쪺smell�[�JList��
	 */
	private void addMarker(ASTNode node){
		CSMessage csmsg = new CSMessage(RLMarkerAttribute.CS_CARELESS_CLEANUP,
				null, node.toString(), node.getStartPosition(),
				getLineNumber(node.getStartPosition()), null);
		CarelessCleanUpList.add(csmsg);
	}
	
	private void addMarker(Statement statement){
		CSMessage csmsg=new CSMessage(RLMarkerAttribute.CS_CARELESS_CLEANUP,null,											
				statement.toString(),statement.getStartPosition(),
				getLineNumber(statement.getStartPosition()),null);
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
		Element root = JDomUtil.createXMLContent();
		
		// �p�G�Onull���xml�ɬO��ئn��,�٨S��Careless CleanUp��tag,�������X�h		
		if(root.getChild(JDomUtil.CarelessCleanUpTag) != null){
			// �o�̪�ܤ��e�ϥΪ̤w�g���]�w�Lpreference�F,�h���o���������]�w��
			Element CarelessCleanUp = root.getChild(JDomUtil.CarelessCleanUpTag);
			Element rule = CarelessCleanUp.getChild("rule");
			String methodSet=rule.getAttribute(JDomUtil.det_user_method).getValue();
			
			isDetUserMethod=methodSet.equals("Y");
			
			Element libRule = CarelessCleanUp.getChild("librule");
			// ��~��Library�MStatement�x�s�bList��
			List<Attribute> libRuleList = libRule.getAttributes();
			
			//��~����Library�[�J�����W�椺
			for (int i=0;i<libRuleList.size();i++) {
				if (libRuleList.get(i).getValue().equals("Y")) {
					String temp = libRuleList.get(i).getQualifiedName();					
					
					//�Y��.*���u����Library
					if (temp.indexOf(".EH_STAR") != -1) {
						int pos = temp.indexOf(".EH_STAR");
						libMap.put(temp.substring(0,pos), ASTBinding.LIBRARY);
					//�Y��*.���u����Method
					} else if (temp.indexOf("EH_STAR.") != -1) {
						libMap.put(temp.substring(8), ASTBinding.METHOD);
					//���S�����������A����Library+Method
					} else if (temp.lastIndexOf(".") != -1) {
						libMap.put(temp, ASTBinding.LIBRARY_METHOD);
					//�Y���䥦�Ϊp�h�]��Method
					} else {
						libMap.put(temp, ASTBinding.METHOD);
					}
				}
			}
		}
	}
	public void findExceptionInfo(ASTNode node){
		//this.iType=node.resolveMethodBinding().getExceptionTypes();
	}
}
