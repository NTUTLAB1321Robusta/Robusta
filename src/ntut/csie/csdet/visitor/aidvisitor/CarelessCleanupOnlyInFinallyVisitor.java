package ntut.csie.csdet.visitor.aidvisitor;

import java.util.ArrayList;
import java.util.List;

import ntut.csie.jdt.util.NodeUtils;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.ThrowStatement;
import org.eclipse.jdt.core.dom.TryStatement;

/**
 * ����ˬdFinally Block�̭����{���X�C
 * �p�G�A������y���{���X���e���{���X�|�ߥX�ҥ~�A�h������y���{���X�Y�Ocareless cleanup�C
 * 
 * �`�N�G���O�u��Finally���`�I�s��Block�C
 * @author charles
 *
 */
public class CarelessCleanupOnlyInFinallyVisitor extends ASTVisitor {
	private CompilationUnit root;
	private boolean isExceptionRisable;
	private List<MethodInvocation> carelessCleanupNodes;
	
	/** �`�����Ocareless cleanup�A���O�O������y���{���X */
	private List<MethodInvocation> fineCleanupNodes;
	
	public CarelessCleanupOnlyInFinallyVisitor(CompilationUnit compilationUnit) {
		root = compilationUnit;
		isExceptionRisable = false;
		carelessCleanupNodes = new ArrayList<MethodInvocation>();
		fineCleanupNodes = new ArrayList<MethodInvocation>();
	}
	
	/**
	 * �Ҽ{Finally Block�̭���TryStatement�����p�C
	 * �z�LTryStatementExceptionsVisitor���ˬd�A�H�F��finally�̭��� TryStatement�|���|�ߨҥ~�C
	 * (�p�G�o�ӷ|�ߨҥ~��TryStatement�b������y���{���X�e���A�h�|�y��careless cleanup�C)
	 */
	public boolean visit(TryStatement node) {
		TryStatementExceptionsVisitor tryStatementVisitor = new TryStatementExceptionsVisitor(node);
		node.accept(tryStatementVisitor);
		if(tryStatementVisitor.getTotalExceptionStrings().length > 0) {
			isExceptionRisable = true;
		}
		return false;
	}
	
	public boolean visit(ThrowStatement node) {
		isExceptionRisable = true;
		return true;
	}
	
	public boolean visit(MethodInvocation node) {
		/* 
		 * (��RuntimeException�S��C)
		 * �p�G�O������y���{���X�A�n��careless cleanup���ˬd�G
		 * 	�p�G�b�o�椧�e�i��|�o�ͨҥ~�A�h�o�欰careless cleanup�C
		 *  �p�G�b�o�椧�e���|�o�ͨҥ~�A�h�o��N����careless cleanup
		 */
		if(NodeUtils.isCloseResourceMethodInvocation(root, node)) {
			// �p�G�e���w�g���{���X�|�o�ͨҥ~�A�h�o��������y���{���X�N�Ocareless cleanup
			if(isExceptionRisable) {
				carelessCleanupNodes.add(node);
			} else {
				fineCleanupNodes.add(node);
			}
		}
		
		/* 
		 * �O���o��{���X�|���|�ߥX�ҥ~�G
		 * 	��¬����|���|�ߥX�ҥ~�C
		 */
		if (node.resolveMethodBinding().getExceptionTypes().length != 0) {
			isExceptionRisable = true;
		}
		
		return true;
	}
	
	public List<MethodInvocation> getCarelessCleanupNodes() {
		return carelessCleanupNodes;
	}
	
	/**
	 * ���Q�o��Visitor�{�wcareless cleanup �������귽�ʧ@�C
	 * @return
	 */
	public List<MethodInvocation> getfineCleanupNodes() {
		return fineCleanupNodes;
	}
}
