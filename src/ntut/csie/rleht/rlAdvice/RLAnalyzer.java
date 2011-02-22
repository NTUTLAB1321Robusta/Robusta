package ntut.csie.rleht.rlAdvice;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.TryStatement;

import ntut.csie.rleht.builder.ASTMethodCollector;
import ntut.csie.rleht.builder.RLMarkerAttribute;
import ntut.csie.rleht.common.RLBaseVisitor;

/**
 * ���R�|�ߥX�ҥ~�A�H�Φ��w�qRL���~��Method�A���ΤFvisitor pattern
 * @author Charles
 * @version 0.0.1
 *  2011/02/10
 */
public class RLAnalyzer extends RLBaseVisitor {

	/** AST tree��root(�ɮצW��) */
	private CompilationUnit root;
	
	/** ����class����method */
	private ASTMethodCollector methodCollector;
	
//	/** �x�s��쪺Method List */
//	private List<ASTNode> lstMethod;
	
	private List<RLAdviceMessage> lstRLAdviceMessage;
	
	public RLAnalyzer(CompilationUnit rootCompilationUnit){
		super(true);
		root = rootCompilationUnit;
		lstRLAdviceMessage = new ArrayList<RLAdviceMessage>();
		methodCollector = new ASTMethodCollector();
		root.accept(methodCollector);
	}

	/**
	 * �ˬd���ߥX�ҥ~���ŧi
	 */
	protected boolean visitNode(ASTNode node){
		switch(node.getNodeType()){
			case ASTNode.METHOD_INVOCATION:
				MethodInvocation mi = (MethodInvocation) node;
				addAdvice(mi);
				return false;
			default:
				return true;
		}
	}
	
	/**
	 * �ˬd��node���S���ߥX�ҥ~
	 * @param node
	 */
	private void addAdvice(ASTNode node) {
		MethodInvocation mi = (MethodInvocation) node;
		if(mi.resolveMethodBinding().getExceptionTypes().length > 0){
			addMarker(mi);
		}
	}
	
	public List<RLAdviceMessage> getExceptionRLAdviceList(){
		return lstRLAdviceMessage;
	}
	
	public void throwEx() throws IOException{
		throw new IOException();
	}

	/**
	 * �I�s�o��Class��function�A�b�ˬd�LMethodInvocation�A�B�o���|�ߥX�ҥ~��A �|addMarker�C
	 * �o�̪�addMarker�A�t�d��{���X����T�A������RLAdviceMessage�̭��C
	 * �۸���RLBuilder.class��addMarker�A�h�O�Q��RLAdviceMessage����T�H��A���s�K�@��marker
	 * @param node
	 */
	private void addMarker(ASTNode node){
		MethodInvocation mi = (MethodInvocation) node;
		ASTNode tryNode = findTryNode(node);
		TryStatement ts = null;
		if(tryNode != null){
		    ts = (TryStatement) findTryNode(node);
		    for(Object obj : ts.catchClauses()){
		    	CatchClause cc = (CatchClause)obj;
		    	ASTNode catchClause = (ASTNode)cc;
		        RLAdviceMessage rladvicemsg = new RLAdviceMessage(RLMarkerAttribute.CS_EXCEPTION_RLADVICE, 
		                null, mi.toString(), mi.getStartPosition(), catchClause.getStartPosition(),
		                root.getLineNumber(mi.getStartPosition()),
		                root.getLineNumber(catchClause.getStartPosition()), 
		                cc.getException().resolveBinding().getType().getBinaryName(), 
		                mi.resolveMethodBinding().getAnnotations());
		        lstRLAdviceMessage.add(rladvicemsg);
		    }
		}
	}
	
	/**
	 * ��X�o��node���ݪ�TryStatement
	 * @param node
	 * @return
	 */
	private ASTNode findTryNode(ASTNode node){
		ASTNode tryNodeCadidate = node.getParent(); 
		if(node.getParent().getNodeType() == ASTNode.TRY_STATEMENT){
			return tryNodeCadidate;
		}else if(node.getParent().getNodeType() == ASTNode.TYPE_DECLARATION){
			//����@�����W�䤣���A�ҥH�HTYPE_DECLARATION��@���W�䪺�̫�@�h
			return null;
		}else{
			return findTryNode(tryNodeCadidate);
		}
	}

}
