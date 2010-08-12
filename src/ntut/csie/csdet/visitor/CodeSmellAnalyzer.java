package ntut.csie.csdet.visitor;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import ntut.csie.csdet.data.CSMessage;
import ntut.csie.csdet.preference.JDomUtil;
import ntut.csie.rleht.builder.RLMarkerAttribute;
import ntut.csie.rleht.common.RLBaseVisitor;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.TryStatement;
import org.jdom.Attribute;
import org.jdom.Element;

/**
 * ��M�פ���Ignore Exception and dummy handler
 * 
 * @author chewei
 */
public class CodeSmellAnalyzer extends RLBaseVisitor {
	// AST tree��root(�ɮצW��)
	private CompilationUnit root;

	// �x�s�ҧ�쪺ignore Exception 
	private List<CSMessage> codeSmellList;

	// �x�s�ҧ�쪺dummy handler
	private List<CSMessage> dummyList;

	// �x�s����"Library��Name"�M"�O�_Library"
	// store�ϥΪ̭n������library�W�١A�M"�O�_�n������library"
	private TreeMap<String, Integer> libMap = new TreeMap<String, Integer>();
	
	// Code Information Counter //
	private int tryCounter = 0;
	private int catchCounter = 0;
	private int finallyCounter = 0;

	/**
	 * ���@���constructor,����ȷ|���ݭn��h����T �p�G�S���i�H�N����ѱ�
	 */
	public CodeSmellAnalyzer(CompilationUnit root) {
		super(true);
		this.root = root;
		codeSmellList = new ArrayList<CSMessage>();
		dummyList = new ArrayList<CSMessage>();
	}

	public CodeSmellAnalyzer(CompilationUnit root,boolean currentMethod,int pos){
		super(true);
		this.root = root;
		codeSmellList = new ArrayList<CSMessage>();
		dummyList = new ArrayList<CSMessage>();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ntut.csie.rleht.views.ASTBaseVisitor#visitNode(org.eclipse.jdt.core.dom.ASTNode)
	 */
	protected boolean visitNode(ASTNode node) {
		switch (node.getNodeType()) {
		case ASTNode.TRY_STATEMENT:
			tryCounter++;

			// /�p��Finally�Ӽ�///
			TryStatement trystat = (TryStatement) node;
			Block finallyBlock = trystat.getFinally();
			if (finallyBlock != null)
				finallyCounter++;

			return true;
		case ASTNode.CATCH_CLAUSE:
			processCatchStatement(node);
			catchCounter++;
			return true;
		default:
			// return true�h�~��X�ݨ�node���l�`�I,false�h���~��
			return true;
		}
	}

	/**
	 * �h�M��catch���`�I,�åB�P�_�`�I����statement�O�_����
	 * 
	 * @param node
	 */
	private void processCatchStatement(ASTNode node) {
		// �ഫ��catch node
		CatchClause cc = (CatchClause) node;
		// ���ocatch(Exception e)�䤤��e
		SingleVariableDeclaration svd = (SingleVariableDeclaration) cc
				.getStructuralProperty(CatchClause.EXCEPTION_PROPERTY);
		// �P�_�o��catch�O�_��ignore exception or dummy handler
		judgeIgnoreEx(cc, svd);
	}

	/**
	 * �P�_�o��catch block�O���Oignore EX
	 * 
	 * @param cc :
	 *            catch block����T
	 * @param svd :
	 *            throw exception�|�Ψ�
	 */
	private void judgeIgnoreEx(CatchClause cc, SingleVariableDeclaration svd ){
		List statementList = cc.getBody().statements();
		// �p�Gcatch statement�̭��O�Ū���,��ܬOignore exception
		if (statementList.size() == 0) {
			// �إߤ@��ignore exception type
			CSMessage csmsg = new CSMessage(
					RLMarkerAttribute.CS_INGNORE_EXCEPTION, svd
							.resolveBinding().getType(), cc.toString(), cc
							.getStartPosition(), this.getLineNumber(cc
							.getStartPosition()), svd.getType().toString());
			this.codeSmellList.add(csmsg);
		} else {
			/*------------------------------------------------------------------------*
			-  ���pstatement���O�Ū�,��ܦ��i��s�bdummy handler,���t�~�g�@��class�Ӱ���,
			     ��]�O���Ʊ�bRLBilder�nparse�C��method�ܦh��,code�����]�|�W�[,�ҥH�N�g�b�o��
			 *-------------------------------------------------------------------------*/
			judgeDummyHandler(statementList, cc, svd);
		}
	}

	/**
	 * �P�_�o��catch���O�_��dummy handler
	 * @param catchStatementList
	 * @param cc
	 * @param svd
	 */
	private void judgeDummyHandler(List catchStatementList, CatchClause cc, SingleVariableDeclaration svd){
		/*------------------------------------------------------------------------*
		-  ���]�o��catch�̭���throw�F�� or ���ϥ�Log��API,�N�P�w���Odummy handler
		     �p�G�u�n���@��e.printStackTrace�Ϊ̲ŦXuser�ҳ]�w������,�N�P�w��dummy handler  
		 *-------------------------------------------------------------------------*/	
		getDummySettings();	
		//Use LogAnalyzer to detect if there is any statements may cause dummy handler. 
		LogAnalyzer logAnalyzer = new LogAnalyzer(libMap);
		cc.accept(logAnalyzer);

		if(logAnalyzer.getDummyHandlerList() != null){
			for (int i = 0; i < logAnalyzer.getDummyHandlerList().size(); i++) {
				addDummyMessage(cc, svd, logAnalyzer.getDummyHandlerList().get(i));
			}
		}
	}

	/**
	 * �o��Dummy Handler���T��
	 */
	private void addDummyMessage(CatchClause cc, SingleVariableDeclaration svd,
			ExpressionStatement statement) {
		CSMessage csmsg = new CSMessage(RLMarkerAttribute.CS_DUMMY_HANDLER, svd
				.resolveBinding().getType(), cc.toString(), cc
				.getStartPosition(), this.getLineNumber(statement
				.getStartPosition()), svd.getType().toString());
		this.dummyList.add(csmsg);
	}

	/**
	 * �Nuser���dummy handler���]�w�s�U��
	 * 
	 * @return
	 */
	private void getDummySettings() {
		Element root = JDomUtil.createXMLContent();
		// �p�G�Onull���xml�ɬO��ئn��,�٨S��dummy handler��tag,�������X�h

		if (root.getChild(JDomUtil.DummyHandlerTag) != null) {
			// �o�̪�ܤ��e�ϥΪ̤w�g���]�w�Lpreference�F,�h���o���������]�w��
			Element dummyHandler = root.getChild(JDomUtil.DummyHandlerTag);
			Element rule = dummyHandler.getChild("rule");
			String eprintSet = rule.getAttribute(JDomUtil.e_printstacktrace)
					.getValue();
			String sysoSet = rule.getAttribute(JDomUtil.systemout_print)
					.getValue();
			String log4jSet = rule.getAttribute(JDomUtil.apache_log4j)
					.getValue();
			String javaLogger = rule.getAttribute(JDomUtil.java_Logger)
					.getValue();
			Element libRule = dummyHandler.getChild("librule");
			// ��~��Library�MStatement�x�s�bList��
			List<Attribute> libRuleList = libRule.getAttributes();

			// �⤺�ذ����[�J��W�椺
			// ��e.print�Msystem.out�[�J������
			if (sysoSet.equals("Y")) {
				libMap.put("java.io.PrintStream.println",
						LogAnalyzer.LIBRARY_METHOD);
				libMap.put("java.io.PrintStream.print",
						LogAnalyzer.LIBRARY_METHOD);
			}
			if (eprintSet.equals("Y"))
				libMap.put("printStackTrace", LogAnalyzer.METHOD);
			// ��log4j�MjavaLog�[�J������
			if (log4jSet.equals("Y"))
				libMap.put("org.apache.log4j", LogAnalyzer.LIBRARY);
			if (javaLogger.equals("Y"))
				libMap.put("java.util.logging", LogAnalyzer.LIBRARY);

			// ��~����Library�[�J�����W�椺
			for (int i = 0; i < libRuleList.size(); i++) {
				if (libRuleList.get(i).getValue().equals("Y")) {
					String temp = libRuleList.get(i).getQualifiedName();

					// �Y��.*���u����Library
					if (temp.indexOf(".EH_STAR") != -1) {
						int pos = temp.indexOf(".EH_STAR");
						libMap.put(temp.substring(0, pos), LogAnalyzer.LIBRARY);
						// �Y��*.���u����Method
					} else if (temp.indexOf("EH_STAR.") != -1) {
						libMap.put(temp.substring(8), LogAnalyzer.METHOD);
						// ���S�����������A����Library+Method
					} else if (temp.lastIndexOf(".") != -1) {
						libMap.put(temp, LogAnalyzer.LIBRARY_METHOD);
						// �Y���䥦�Ϊp�h�]��Method
					} else {
						libMap.put(temp, LogAnalyzer.METHOD);
					}
				}
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
	 * ���odummy handler��List
	 */
	public List<CSMessage> getIgnoreExList() {
		return codeSmellList;
	}

	/**
	 * ���odummy handler��List
	 */
	public List<CSMessage> getDummyList() {
		return dummyList;
	}

	/**
	 * ���oCode Information
	 * 
	 * @return
	 */
	public int getTryCounter() {
		return tryCounter;
	}

	public int getCatchCounter() {
		return catchCounter;
	}

	public int getFinallyCounter() {
		return finallyCounter;
	}
}
