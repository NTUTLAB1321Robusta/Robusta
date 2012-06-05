package ntut.csie.csdet.visitor;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;

import ntut.csie.csdet.data.MarkerInfo;
import ntut.csie.csdet.preference.JDomUtil;
import ntut.csie.rleht.builder.RLMarkerAttribute;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.TryStatement;
import org.jdom.Attribute;
import org.jdom.Element;

public class DummyHandlerVisitor extends ASTVisitor {
	final static public int LIBRARY = 1;
	final static public int METHOD = 2;
	final static public int LIBRARY_METHOD = 3;
	
	private List<MarkerInfo> dummyHandlerList;
	// �x�s����"Library��Name"�M"�O�_Library"
	// store�ϥΪ̭n������library�W�١A�M"�O�_�n������library"
	private TreeMap<String, Integer> libMap = new TreeMap<String, Integer>();
	private CompilationUnit root;
	// Code Information Counter //
	private int tryCounter = 0;
	private int catchCounter = 0;
	private int finallyCounter = 0;
	
	public DummyHandlerVisitor(CompilationUnit root) {
		super();
		dummyHandlerList = new ArrayList<MarkerInfo>();
		this.root = root;
		getDummySettings();
	}
	
	public boolean visit(TryStatement node) {
		tryCounter++;
		if(node.getFinally() != null)
			finallyCounter++;
		ASTNode parent = getSpecifiedParentNode(node, ASTNode.TRY_STATEMENT);
		if(parent == null) {
			/*
			 * �o��TryStatement���O�bTryStatement�̭�
			 */
			return true;
		} else {
			/*
			 * Try�̭����ӴN�����Ӧ�Try Catch���{���X(Nested Try Block)�C
			 * �ҥH�p�G�J�쪺TryStatement Node�OTry Statement�̭��A���N���~�򰻴��C
			 * 
			 * �קKClose Stream�ɡA���o��Dummy Handler�����D�C
			 */
			return false;
		}
	}
	
	public boolean visit(MethodInvocation node) {
		ASTNode parentNode = node.getParent();
		if(parentNode.getNodeType() == ASTNode.EXPRESSION_STATEMENT) {
			detectDummyHandler((ExpressionStatement)parentNode);
		}
		return false;	
	}
	
	public void detectDummyHandler(ExpressionStatement node) {
		ASTNode parentCatchClauseNode = getSpecifiedParentNode(node, ASTNode.CATCH_CLAUSE);
		/*
		 * �p�G��쪺ExpressionStatement���O�bCatchClause�̭��A
		 * �h�����@DummyHandler
		 */
		if(parentCatchClauseNode == null) {
			return;
		}
		CatchClause cc = (CatchClause) parentCatchClauseNode;
		catchCounter++;
		/* 
		 * �p�G�b�o��catch clause�̭��A��throw statement�s�b�A
		 * �h����o��ExpressionStatement��@DummyHandler�C
		 */
		if(isThrowStatementInCatchClause(cc)) {
			return;
		}
		addDummyHandlerSmellInfo(node);
	}
	
	/**
	 * �ھڶǤJ��ExpressionStatement Node�A��X����ݪ�CatchClause
	 * @param node ExpressionStatement Node
	 */
	private void addDummyHandlerSmellInfo(ExpressionStatement node) {
		MethodInvocation mi = (MethodInvocation)node.getExpression();
		// ���oMethod��Library�W��
		String libName = mi.resolveMethodBinding().getDeclaringClass().getQualifiedName();
		// ���oMethod���W��
		String methodName = mi.resolveMethodBinding().getName();

		// �p�G�Ӧ榳Array(�pjava.util.ArrayList<java.lang.Boolean>)�A��<>���e����
		if (libName.indexOf("<") != -1)
			libName = libName.substring(0, libName.indexOf("<"));
		
		Iterator<String> libIt = libMap.keySet().iterator();
		// �P�_�O�_�n���� �B ���y�]�]�t������Library
		while(libIt.hasNext()){
			String temp = libIt.next();
			CatchClause cc = (CatchClause) getSpecifiedParentNode(node, ASTNode.CATCH_CLAUSE);
			SingleVariableDeclaration svd = cc.getException();
			MarkerInfo markerInfo = new MarkerInfo(	RLMarkerAttribute.CS_DUMMY_HANDLER, svd
													.resolveBinding().getType(), cc.toString(), cc
													.getStartPosition(), root.getLineNumber(node
													.getStartPosition()), svd.getType().toString());
			
			// �u����Library
			if (libMap.get(temp) == LIBRARY) {
				//�YLibrary���פj�󰻴����סA�_�h���ۦP�������L
				if (libName.length() >= temp.length())
				{
					//����e�b�q���ת��W�٬O�_�ۦP
					if (libName.substring(0, temp.length()).equals(temp))
						dummyHandlerList.add(markerInfo);
				}
			// �u����Method
			} else if (libMap.get(temp) == METHOD) {
				if (methodName.equals(temp))
					dummyHandlerList.add(markerInfo);
			// ����Library.Method���Φ�
			} else if (libMap.get(temp) == LIBRARY_METHOD) {
				int pos = temp.lastIndexOf(".");
				if (libName.equals(temp.substring(0, pos)) &&
					methodName.equals(temp.substring(pos + 1))) {
					dummyHandlerList.add(markerInfo);
				}
			}
		}
	}
	
	public List<MarkerInfo> getDummyList() {
		return dummyHandlerList;
	}

	/**
	 * ���w��CatchClause�̭��A�O���O��ThrowStatement�C
	 * @param catchClause
	 * @return
	 */
	public boolean isThrowStatementInCatchClause(CatchClause catchClause) {
		List<?> ccStatements = catchClause.getBody().statements();
		for (Object ccNode : ccStatements) {
			if (((ASTNode) ccNode).getNodeType() == ASTNode.THROW_STATEMENT) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * �q��J���`�I�}�l�A�M��S�w�����`�I�C
	 * @param startNode
	 * @param nodeType
	 * @return
	 */
	public ASTNode getSpecifiedParentNode(ASTNode startNode, int nodeType) {
		ASTNode resultNode = null;
		ASTNode parentNode = startNode.getParent();
		// �p�GparentNode�Onull�A��ܶǶi�Ӫ�node�w�g�OrootNode(CompilationUnit)
		if(parentNode != null) {
			while(parentNode.getNodeType() != nodeType) {
				parentNode = parentNode.getParent();
				// �L�a�j��פ���� - �w�g�S��parentNode
				if (parentNode == null) {
					break;
				}
			}
			resultNode = parentNode; 
		}
		return resultNode;
	}
	
	/**
	 * �Nuser���dummy handler���]�w�s�U��
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
						ExpressionStatementAnalyzer.LIBRARY_METHOD);
				libMap.put("java.io.PrintStream.print",
						ExpressionStatementAnalyzer.LIBRARY_METHOD);
			}
			if (eprintSet.equals("Y"))
				libMap.put("printStackTrace", ExpressionStatementAnalyzer.METHOD);
			// ��log4j�MjavaLog�[�J������
			if (log4jSet.equals("Y"))
				libMap.put("org.apache.log4j", ExpressionStatementAnalyzer.LIBRARY);
			if (javaLogger.equals("Y"))
				libMap.put("java.util.logging", ExpressionStatementAnalyzer.LIBRARY);

			// ��~����Library�[�J�����W�椺
			for (int i = 0; i < libRuleList.size(); i++) {
				if (libRuleList.get(i).getValue().equals("Y")) {
					String temp = libRuleList.get(i).getQualifiedName();

					// �Y��.*���u����Library
					if (temp.indexOf(".EH_STAR") != -1) {
						int pos = temp.indexOf(".EH_STAR");
						libMap.put(temp.substring(0, pos), ExpressionStatementAnalyzer.LIBRARY);
						// �Y��*.���u����Method
					} else if (temp.indexOf("EH_STAR.") != -1) {
						libMap.put(temp.substring(8), ExpressionStatementAnalyzer.METHOD);
						// ���S�����������A����Library+Method
					} else if (temp.lastIndexOf(".") != -1) {
						libMap.put(temp, ExpressionStatementAnalyzer.LIBRARY_METHOD);
						// �Y���䥦�Ϊp�h�]��Method
					} else {
						libMap.put(temp, ExpressionStatementAnalyzer.METHOD);
					}
				}
			}
		}
	}
	
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
