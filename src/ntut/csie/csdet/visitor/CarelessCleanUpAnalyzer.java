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
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.CompilationUnit;
//import org.eclipse.jdt.core.dom.ForStatement;
//import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.TryStatement;
import org.jdom.Attribute;
import org.jdom.Element;

/**
 * ��M�פ���Careless CleanUp
 * @author chenyimin
 */
public class CarelessCleanUpAnalyzer extends RLBaseVisitor{
	
	/** AST tree��root(�ɮצW��) */
	private CompilationUnit _root;
	
	/** �x�s��쪺Careless Cleanup */
	private List<CSMessage> _lstCarelessCleanupInsideOfTryBlock;
	
	/** �x�s�btry block�~����쪺Careless Cleanup */
	private List<CSMessage> _lstCarelessCleanupOutsideOfTryBlock;
	
	/** ����class����method */
	private ASTMethodCollector _methodCollector;
	
	/** �x�s��쪺Method List */
	private List<ASTNode> _methodList;
	
	/** �O�_�n����"�ϥΪ�����귽���{���X�b�禡��" */
	private boolean isDetUserMethod = false;
	
	/** �x�s"�ϥΪ̭n������library�W��"�M"�O�_�n������library" */
	private TreeMap<String, Integer> libMap = new TreeMap<String, Integer>();
	
	/** �ˬd�{���̭�closeMethod��visitor */
	private CloseMethodAnalyzer _closeMethodAnalyzer;
	
//	/** �`���Ҧ�careless cleanup��ExpressionStatement */
//	private List<ExpressionStatement> _lstCarelessCleanupStatement;
	
	public CarelessCleanUpAnalyzer(CompilationUnit root){
		super(true);
		_root = root;
		_lstCarelessCleanupInsideOfTryBlock = new ArrayList<CSMessage>();
		_lstCarelessCleanupOutsideOfTryBlock = new ArrayList<CSMessage>();
		_methodCollector = new ASTMethodCollector();
		_root.accept(_methodCollector);
		_methodList = _methodCollector.getMethodList();
//		this._lstCarelessCleanupStatement = new ArrayList<ExpressionStatement>();
		getCarelessCleanUp();
	}
	
//	/** �NCareless Cleanup�@statement������Node�O���_�� */
//	private void addCarelessCleanupWarning(ASTNode node){
//		if (node.getParent() instanceof ExpressionStatement) {
//			ExpressionStatement statement = (ExpressionStatement) node.getParent();
//			this._lstCarelessCleanupStatement.add(statement);
//		}
//	}

	/**
	 * (non-Javadoc) 
	 * @see ntut.csie.rleht.views.ASTBaseVisitor#visitNode(org.eclipse.jdt.core.dom.ASTNode)
	 */
	protected boolean visitNode(ASTNode node){
		switch(node.getNodeType()){
			//�ˬd���ߥX�ҥ~��Method
			case ASTNode.METHOD_DECLARATION:
				MethodDeclaration md = (MethodDeclaration) node;
				//thrownExceptions���ƶq�A�j�󵥩�1�A��ܳo�Ӥ�k���ߥX�ҥ~
				if(md.thrownExceptions().size()>=1){
					processMethodDeclaration(md);
					return false;
				}else{
					return true;
				}
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
	 * �������ߥX�ҥ~��Method���A�O�_��close���ʧ@(dna2me)
	 * @param md
	 */
	private void processMethodDeclaration(MethodDeclaration md) {
		List<?> mdStatements = md.getBody().statements();
		
		//Method�̭��u���@��close�����p�A�N��mark
		if (mdStatements.size() <= 1)
			//�T�w�o��Statement�OMethodInvocation(�קK�o�@��Statement�OIfStatement/WhileStatement/....)
			if(mdStatements.size() == 1)
				if(mdStatements.get(0) instanceof MethodInvocation)
					return;

		for (int i = 0; i < mdStatements.size(); i++) {
			//node�OTry Statement(new�X�ۤv�A�A�Τ@��visitNode�o��Method)
			if(mdStatements.get(i) instanceof TryStatement){
				TryStatement ts = (TryStatement)mdStatements.get(i);
				CarelessCleanUpAnalyzer ccuaVisitor = new CarelessCleanUpAnalyzer(this._root);
				ts.accept(ccuaVisitor);
				
				//�X�֥��Ӫ�instance�Pnew�X��instance��CSMessage
					//in try block
				List<CSMessage> ccuInTryBlockList = ccuaVisitor.getCarelessCleanUpList(true);
				this.mergeCSMessage(ccuInTryBlockList, true);
					//not in try block
				ccuInTryBlockList = ccuaVisitor.getCarelessCleanUpList(false);
				this.mergeCSMessage(ccuInTryBlockList, false);
			}
			//node���OTry Statement
			else if(mdStatements.get(i) instanceof Statement){
//				this._isCarelessCleanupInTryBlock = false;
				markCarelessCleanUpStatement((Statement)mdStatements.get(i), false);
			}
		}
	}
	
//	/** flag, �Ϥ��o��careless cleanup�O���O�btry block�̭� */
//	private boolean _isCarelessCleanupInTryBlock;
//	
	/**
	 * ���X��instance�M�t�~new�X��instance��CSMessage
	 * @param childInfo
	 */
	private void mergeCSMessage(List<CSMessage> childInfo, boolean isInTryBlock){
		if (childInfo == null || childInfo.size() == 0) {
			return;
		}
		if(isInTryBlock){
			for(CSMessage msg : childInfo){
				this._lstCarelessCleanupInsideOfTryBlock.add(msg);
			}
		}else{
			for(CSMessage msg: childInfo){
				this._lstCarelessCleanupOutsideOfTryBlock.add(msg);
			}
		}
	}
	
	/**
	 * �B�ztry�`�I����statement
	 * 1. �p�Gtry�̭��u��close���ʧ@�A�����Obad smell
	 * 2. �p�Gtry�̭��u��if�Aif�S�u��close���ʧ@�A�]�����Obad smell
	 */
	private void processTryStatement(ASTNode node){
		//���otry Block����Statement
		TryStatement trystat=(TryStatement) node;
		List<?> statementTemp=trystat.getBody().statements();
		//�קK��try�`�I��Nested try block���c�᪺���G,�G�P�_try�`�I����statement�ƶq���j��1
		//statementTemp�S�F��A��M���P�_
		if(statementTemp.size() <= 0){
			return;
		}
		//�p�Gsize == 1�A�ӥB�OMethodInvocation�A�NstatementTemp�u���@��{���X�A�ҥH���P�_
		else if(statementTemp.size() == 1){
			if(statementTemp.get(0) instanceof ExpressionStatement){
				return;
			}
			//p.s.�p�G�H��Catch�]�n�ΡA�O�o�nExtract�X��
			//�p�G�OIfStatement�A�S��Else�A�bThen�̭��S�u���@��A���]�O���P�_
			else if (statementTemp.get(0) instanceof IfStatement){
				IfStatement ifst = (IfStatement)statementTemp.get(0);
				//�S��ElseStatement
				if (ifst.getElseStatement() == null) {
					if(ifst.getThenStatement() instanceof ExpressionStatement){
//						System.out.println("if�S�A��");
						return;
					}else{
//						System.out.println("if���A��");
						Block bk = (Block)ifst.getThenStatement();
						if(bk.statements().size() == 1){
//							System.out.println("if���A���A�ӥB�u���@��");
							return;
						}
					}
				}
			}
		}
		judgeCarelessCleanUp(statementTemp);
	}

	/**
	 * �P�_��@statement�O���OCareless Clean Up
	 * @param st
	 * @param isInTryBlock
	 */
	private void markCarelessCleanUpStatement(Statement st, boolean isInTryBlock){
		//�аO�ŦX�ϥΪ̦ۭq��Careless Cleanup�W�h��Statement
		if(visitBindingLib(st)){
			//findExceptionInfo((ASTNode) statement);
			addMarker(st, isInTryBlock);
		}
		//�аOCareless Cleanup��Statement
		else{
			_closeMethodAnalyzer = new CloseMethodAnalyzer(false, _methodList);
			st.accept(_closeMethodAnalyzer);
			if(_closeMethodAnalyzer.isFoundCarelessCleanup()){
				addMarker(_closeMethodAnalyzer.getMethodInvocation(), isInTryBlock);
			}else{
				if(isDetUserMethod){
					_closeMethodAnalyzer = new CloseMethodAnalyzer(true, _methodList);
					st.accept(_closeMethodAnalyzer);
					if(_closeMethodAnalyzer.isFoundCarelessCleanup()){
						addMarker(_closeMethodAnalyzer.getMethodInvocation(), isInTryBlock);
					}
				}
			}
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
				markCarelessCleanUpStatement(statement, true);
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
	 * �����ϥΪ̦ۭq��Rule
	 * @param statement
	 * @return boolean
	 */
	private boolean visitBindingLib(Statement statement) {
		ExpressionStatementAnalyzer visitor = new ExpressionStatementAnalyzer(libMap);
		statement.accept(visitor);
		if (visitor.getResult()) {
			return true;
		} else {
			return false;
		}
	}
	
	/**
	 * �N��쪺smell�[�JList��(���u��w�]��smell)
	 * @param isInTryBlock
	 */
	private void addMarker(ASTNode node, boolean isInTryBlock){
		MethodInvocation mi = (MethodInvocation)node;
		String exceptionType = null; 
		if(mi.resolveMethodBinding().getExceptionTypes().length == 1){
			exceptionType = mi.resolveMethodBinding().getExceptionTypes().toString();
		}else if (mi.resolveMethodBinding().getExceptionTypes().length > 1){
			exceptionType = "Exceptions";
		}
		CSMessage csmsg = new CSMessage(RLMarkerAttribute.CS_CARELESS_CLEANUP,
				null, node.toString(), node.getStartPosition(),
				getLineNumber(node.getStartPosition()), exceptionType);

		//�Ϥ�careless cleanup�O�_�btry block��
		if (isInTryBlock){
			_lstCarelessCleanupInsideOfTryBlock.add(csmsg);
		}else{
			_lstCarelessCleanupOutsideOfTryBlock.add(csmsg);
		}	
	}
	
	/**
	 * �N��쪺smell�[�JList��(�ϥΪ̩w�q��smell)
	 * @param isInTryBlock TODO
	 */
	private void addMarker(Statement statement, boolean isInTryBlock){
		CSMessage csmsg=new CSMessage(RLMarkerAttribute.CS_CARELESS_CLEANUP ,null,											
				statement.toString(),statement.getStartPosition(),
				getLineNumber(statement.getStartPosition()),null);

		//�Ϥ�careless cleanup�O�_�btry block��
		if (isInTryBlock){
			_lstCarelessCleanupInsideOfTryBlock.add(csmsg);
		}else{
			_lstCarelessCleanupOutsideOfTryBlock.add(csmsg);
		}
	}
	
	/**
	 * �ھ�startPosition�Ө��o���
	 */
	private int getLineNumber(int pos) {
		return _root.getLineNumber(pos);
	}
	
	/**
	 * ��ܨ��otry block��/�~��Careless CleanUp��list
	 * @param isGettingFromTryBlock (true: ���oTryBlock�̭����F false: ���oTryBlock�~����)
	 * @return list
	 */
	public List<CSMessage> getCarelessCleanUpList(boolean isGettingFromTryBlock){
		if(isGettingFromTryBlock){
			return _lstCarelessCleanupInsideOfTryBlock;
		}else{
			return _lstCarelessCleanupOutsideOfTryBlock;
		}
	}
	
	/**
	 * ���o�Ҧ���Careless Cleanup list
	 * @return
	 */
	public List<CSMessage> getCarelessCleanUpList(){
		List<CSMessage> csmsg = _lstCarelessCleanupInsideOfTryBlock;
		for(CSMessage msg : _lstCarelessCleanupOutsideOfTryBlock){
			csmsg.add(msg);
		}
		return csmsg;
	}
	
//	/**
//	 * ���otry block�~�ACareless Cleanup��CSMessage List
//	 * @return
//	 */
//	public List<CSMessage> getCarelessCleanUpList(){
//		return this._lstCarelessCleanupInsideOfTryBlock;
//	}
	
	/**
	 * ���oUser��Careless CleanUp���]�w(From xml)
	 */
	private void getCarelessCleanUp(){
		Element root = JDomUtil.createXMLContent();
		
		// �p�G�Onull���xml�ɬO��ئn��,�٨S��Careless CleanUp��tag,�������X�h		
		if(root.getChild(JDomUtil.CarelessCleanUpTag) != null){
			// �o�̪�ܤ��e�ϥΪ̤w�g���]�w�Lpreference�F,�h���o���������]�w��
			Element CarelessCleanUp = root.getChild(JDomUtil.CarelessCleanUpTag);
			Element rule = CarelessCleanUp.getChild("rule");
			String methodSet = rule.getAttribute(JDomUtil.det_user_method).getValue();
			
			isDetUserMethod = methodSet.equals("Y");
			
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
						libMap.put(temp.substring(0,pos), ExpressionStatementAnalyzer.LIBRARY);
					//�Y��*.���u����Method
					} else if (temp.indexOf("EH_STAR.") != -1) {
						libMap.put(temp.substring(8), ExpressionStatementAnalyzer.METHOD);
					//���S�����������A����Library+Method
					} else if (temp.lastIndexOf(".") != -1) {
						libMap.put(temp, ExpressionStatementAnalyzer.LIBRARY_METHOD);
					//�Y���䥦�Ϊp�h�]��Method
					} else {
						libMap.put(temp, ExpressionStatementAnalyzer.METHOD);
					}
				}
			}
		}
	}
}