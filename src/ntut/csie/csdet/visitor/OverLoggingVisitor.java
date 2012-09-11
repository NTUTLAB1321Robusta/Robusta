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
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.ThrowStatement;

public class OverLoggingVisitor extends ASTVisitor {
	// �O�_�n�~�򰻴�
	boolean isKeepTrace = false;
	// �O�_��Logging
	boolean isLogging = false;
	// �૬�O�_�~��l��
	boolean isDetTransEx = false;
	// �O�_���callee
	boolean isFoundCallee = false;
	// Callee��Class�MMethod����T
	String methodInfo;
	// �w���x�s�i��Ooverlogging��ExpressionStatement
	ASTNode suspectNode;
	// AST Tree��root(�ɮצW��)
	CompilationUnit root;
	// �x�s�ҧ�쪺OverLogging Exception 
	List<MarkerInfo> loggingList = new ArrayList<MarkerInfo>();
	// �x�s�ϥΪ̩w�q��Log����
	TreeMap<String, UserDefinedConstraintsType> libMap = new TreeMap<String, UserDefinedConstraintsType>();
	// �]�w��
	SmellSettings smellSettings;

	public OverLoggingVisitor(CompilationUnit root, String methodInfo) {
		this.root = root;
		this.methodInfo = methodInfo;
		smellSettings = new SmellSettings(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
		libMap = smellSettings.getSmellSettings(SmellSettings.SMELL_OVERLOGGING);
		isDetTransEx = (libMap.get(SmellSettings.EXTRARULE_OVERLOGGING_DETECTWRAPPINGEXCEPTION) != null) ? true : false;
	}
	
	/**
	 * �P�_Callee��Method�O�_�X�{�b�o��Try����
	 * �P�_�O�_��Logging
	 */
	public boolean visit(MethodInvocation node) {
//		if(NodeUtils.getSpecifiedParentNode(node, ASTNode.TRY_STATEMENT) != null)
//			return true;
//		
//		if(!node.getName().toString().equals(methodInfo))
//			return true;
//		
//		isFoundCallee = true;
		
		if(NodeUtils.getSpecifiedParentNode(node, ASTNode.CATCH_CLAUSE) == null)
			return true;
		
		if(!satisfyLoggingStatement(node))
			return true;
		// ��logging�~�[�J���e�æ�OverLogging��node�A�_�h�����s�o�{�����å�
		if(isLogging)
			addOverLoggingMarkerInfo(suspectNode);
		else
			suspectNode = node;
		
		isLogging = true;
		
		return false;
	}

	/**
	 * �P�_���S��Throw�A�M�w�n���n�~�~Trace
	 */
	public boolean visit(ThrowStatement node) {
		if(NodeUtils.getSpecifiedParentNode(node, ASTNode.CATCH_CLAUSE) == null)
			return true;
		
		// �Y���nlog�~�l�ܡA�S��log���ʧ@�A���]�N�S��over logging�����D
		if(isLogging)
			isKeepTrace = true;
		
		return true;
	}

	/**
	 * �P�_�O�_��Throw new Exception
	 */
	public boolean visit(ClassInstanceCreation node) {
		ASTNode parent = NodeUtils.getSpecifiedParentNode(node, ASTNode.CATCH_CLAUSE);
		if(parent == null)
			return true;
		
		// �Y�������૬ �� �S���Ncatch exception�N�J(eg:RuntimeException(e))�A�h���~�򰻴�
		if (!isDetTransEx || node.arguments().size() == 0 || 
			!node.arguments().get(0).toString().equals(((CatchClause)parent).getException().getName().toString())) {
			isKeepTrace = false;
		}
		
		return false;
	}

	/**
	 * �x�s�����쪺over logging
	 * @param parent bad smell��parent
	 */
	private void addOverLoggingMarkerInfo(ASTNode node) {
		ASTNode compilationUnit = NodeUtils.getSpecifiedParentNode(node, ASTNode.COMPILATION_UNIT);
		// compilation unit�p�G�Onull�A�h���ʧ@ 
		if(compilationUnit == null)
			return;
		// �u�x�s�ثe���R���ɮפ���marker�A�p�G�l�ܨ��L�ɮסA�h���x�s
		if(compilationUnit.toString().equals(root.toString())) {
			ASTNode parent = NodeUtils.getSpecifiedParentNode(node, ASTNode.CATCH_CLAUSE);
			CatchClause cc = (CatchClause)parent;
			SingleVariableDeclaration svd = cc.getException();
			MarkerInfo marker = new MarkerInfo(	RLMarkerAttribute.CS_OVER_LOGGING, svd.resolveBinding().getType(), cc.toString(),										
												cc.getStartPosition(), root.getLineNumber(node.getStartPosition()), svd.getType().toString());
			loggingList.add(marker);
			suspectNode = null;
		}
	}

	private boolean satisfyLoggingStatement(MethodInvocation node) {
		if(libMap.isEmpty()) {
			return false;
		}
		
		String libName = node.resolveMethodBinding().getDeclaringClass().getQualifiedName();
		Iterator<String> iterator = libMap.keySet().iterator();
		while(iterator.hasNext()) {
			String condition = iterator.next();
			if(libName.equals(condition)) {
				return true;
			} else if(libName.length() >= condition.length()) {
				if(libName.substring(0, condition.length()).equals(condition))
					return true;
			}
		}
		return false;
	}

	/**
	 * ���o�O�_�n�~��Trace
	 */
	public boolean getIsKeepTrace() {
		return isKeepTrace;
	}

	/**
	 * �^�ǬO�_��Logging
	 * @return
	 */
	public boolean getIsLogging() {
		// �p�G���nlog�A�]�n�l�ܡA�ӥB���æ�OverLogging��node�A�N�^��false�A���F��detector�~�򻼰j
		return !(isLogging && isKeepTrace && (suspectNode != null) ? true : false);
	}
	
	/**
	 * ���oOverLogging ��ƪ���T
	 * @return
	 */
	public List<MarkerInfo> getOverLoggingList() {
		return loggingList;
	}
}
