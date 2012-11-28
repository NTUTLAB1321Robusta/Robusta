package ntut.csie.csdet.visitor.aidvisitor;

import java.util.ArrayList;
import java.util.List;

import ntut.csie.jdt.util.NodeUtils;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.ThrowStatement;

/**
 * �ˬd�S�w��Block�̥��b����Node�|�ߥX�ҥ~�C
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
	
	public boolean visit(ThrowStatement node) {
		isExceptionRisable = true;
		return true;
	}
	
	public boolean visit(MethodInvocation node) {
		if(NodeUtils.isCloseResourceMethodInvocation(root, node)) {
			// �p�G�e���w�g���{���X�|�o�ͨҥ~�A�h�o��������y���{���X�N�Ocareless cleanup
			if(isExceptionRisable) {
				carelessCleanupNodes.add(node);
			} else {
				fineCleanupNodes.add(node);
			}
		}
		
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
