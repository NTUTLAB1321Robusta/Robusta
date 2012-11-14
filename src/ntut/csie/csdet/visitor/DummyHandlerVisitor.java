package ntut.csie.csdet.visitor;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;

import ntut.csie.csdet.data.MarkerInfo;
import ntut.csie.csdet.preference.SmellSettings;
import ntut.csie.csdet.preference.SmellSettings.UserDefinedConstraintsType;
import ntut.csie.jdt.util.NodeUtils;
import ntut.csie.rleht.builder.RLMarkerAttribute;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.TryStatement;

public class DummyHandlerVisitor extends ASTVisitor {
	private List<MarkerInfo> dummyHandlerList;
	// �x�s����"Library��Name"�M"�O�_Library"
	// store�ϥΪ̭n������library�W�١A�M"�O�_�n������library"
	private TreeMap<String, UserDefinedConstraintsType> libMap;// = new TreeMap<String, UserDefinedConstraintsType>();
	private boolean isDetectingDummyHandlerSmell;
	private CompilationUnit root;
	
	public DummyHandlerVisitor(CompilationUnit root) {
		super();
		dummyHandlerList = new ArrayList<MarkerInfo>();
		this.root = root;
		SmellSettings smellSettings = new SmellSettings(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
		libMap = smellSettings.getSmellSettings(SmellSettings.SMELL_DUMMYHANDLER);
		isDetectingDummyHandlerSmell = smellSettings.isDetectingSmell(SmellSettings.SMELL_DUMMYHANDLER);
	}
	
	public boolean visit(TryStatement node) {
		ASTNode parent = NodeUtils.getSpecifiedParentNode(node, ASTNode.TRY_STATEMENT);
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
	
	/**
	 * �ھڳ]�w�ɪ���T�A�M�w�n���n���X��ʾ�C
	 */
	public boolean visit(MethodDeclaration node) {
		// �p�G�OMain Program�A�N�����X
		if(node.getName().toString().equals("main")) {
			return false;
		}
		return isDetectingDummyHandlerSmell;
	}
	
	public boolean visit(MethodInvocation node) {
		detectDummyHandler(node);
		return false;
	}
	
	public void detectDummyHandler(MethodInvocation node) {
		ASTNode parentCatchClauseNode = NodeUtils.getSpecifiedParentNode(node, ASTNode.CATCH_CLAUSE);
		/*
		 * �p�G��쪺ExpressionStatement���O�bCatchClause�̭��A
		 * �h�����@DummyHandler
		 */
		if(parentCatchClauseNode == null) {
			return;
		}
		CatchClause cc = (CatchClause) parentCatchClauseNode;
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
	private void addDummyHandlerSmellInfo(MethodInvocation node) {
			// ���oMethod��Library�W��
			String libName = node.resolveMethodBinding().getDeclaringClass().getQualifiedName();
			// ���oMethod���W��
			String methodName = node.resolveMethodBinding().getName();

			// �p�G�Ӧ榳Array(�pjava.util.ArrayList<java.lang.Boolean>)�A��<>�P�䤺�e������
			if (libName.indexOf("<") != -1)
				libName = libName.substring(0, libName.indexOf("<"));
			
			Iterator<String> libIt = libMap.keySet().iterator();
			// �P�_�O�_�n���� �B ���y�]�]�t������Library
			while(libIt.hasNext()){
				String temp = libIt.next();
				CatchClause cc = (CatchClause) NodeUtils.getSpecifiedParentNode(node, ASTNode.CATCH_CLAUSE);
				SingleVariableDeclaration svd = cc.getException();
				MarkerInfo markerInfo = new MarkerInfo(	RLMarkerAttribute.CS_DUMMY_HANDLER, svd
														.resolveBinding().getType(), cc.toString(), cc
														.getStartPosition(), root.getLineNumber(node
														.getStartPosition()), svd.getType().toString());
				
				// �u����Library
				if (libMap.get(temp) == UserDefinedConstraintsType.Library) {
					//�YLibrary���פj�󰻴����סA�_�h���ۦP�������L
					if (libName.length() >= temp.length()) {
						//����e�b�q���ת��W�٬O�_�ۦP
						if (libName.substring(0, temp.length()).equals(temp))
							dummyHandlerList.add(markerInfo);
					}
				// �u����Method
				} else if (libMap.get(temp) == UserDefinedConstraintsType.Method) {
					if (methodName.equals(temp))
						dummyHandlerList.add(markerInfo);
				// ����Library.Method���Φ�
				} else if (libMap.get(temp) == UserDefinedConstraintsType.FullQulifiedMethod) {
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
}
