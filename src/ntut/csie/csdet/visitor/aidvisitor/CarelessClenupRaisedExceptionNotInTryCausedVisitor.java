package ntut.csie.csdet.visitor.aidvisitor;

import java.util.ArrayList;
import java.util.List;

import ntut.csie.jdt.util.NodeUtils;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.ThrowStatement;
import org.eclipse.jdt.core.dom.TryStatement;

/**
 * �ˬdFinally block�̭������귽��Method Invocation��instane�A
 * �O���O�bTry�~���A�åB�|�ߥX�ҥ~�C
 * 
 * ��MethodDeclaration�haccept�A�åB�ǤJ�w�g���D��close�M��i�ӡC
 * MethodInvocation�A�h���A�ݬO���O��match��instance
 * @author charles
 *
 */
public class CarelessClenupRaisedExceptionNotInTryCausedVisitor extends	ASTVisitor {

	/** �t�d���귽������MethodInvocation(���å�) */
	private List<MethodInvocation> closeResources;
	
	/** �T�w�Ocareless cleanup��MethodInvocation */
	private List<MethodInvocation> carelessCleanupMethod;
	
	/** MethodDeclaration�Ҧ������귽��method invocation��Binding��instance�C*/
	private List<IBinding> closeResourcesInstanceBinding;

	public CarelessClenupRaisedExceptionNotInTryCausedVisitor(List<MethodInvocation> closeResources) {
		this.closeResources = closeResources;
		carelessCleanupMethod = new ArrayList<MethodInvocation>();
		closeResourcesInstanceBinding = new ArrayList<IBinding>();
		for(MethodInvocation closeResource : closeResources) {
			SimpleName closeResourceDeclaredInstance = NodeUtils.getMethodInvocationBindingVariableSimpleName(closeResource.getExpression());
			if(closeResourceDeclaredInstance != null) {
				closeResourcesInstanceBinding.add(closeResourceDeclaredInstance.resolveBinding());
			}
		}
	}
	
	/**
	 * ����TryStatement���ˬd�C
	 */
	public boolean visit(TryStatement node) {
		return false;
	}
	
	public boolean visit(MethodInvocation node) {
		SimpleName nodeVariable = NodeUtils.getMethodInvocationBindingVariableSimpleName(node.getExpression());
		// System.out.println(fis.toString()); �o��Node�i��N�|NULL
		if(nodeVariable == null) {
			return true;
		}
		
		// �p�G�o��Node�������|�ߥX�ҥ~�A�h���|�O�y��careless cleanup����]
		int checkedExceptionLength = node.resolveMethodBinding().getExceptionTypes().length;
		if(checkedExceptionLength == 0) {
			return true;
		}
		
		for(int i = 0; i<closeResources.size(); i++) {
			if(isNodeBetweenCreationAndClose(node, closeResources.get(i))) {
				// �Nclose���ʧ@�[�Jcareless cleanup �M��
				carelessCleanupMethod.add(closeResources.get(i));
				
				/* �N�[�L��close�ʧ@������ */
				closeResourcesInstanceBinding.remove(i);
				closeResources.remove(i);
				return false;
			}
		}
		return true;
	}
	
//	public boolean visit(VariableDeclarationFragment node) {
//		for(int i = 0; i<closeResourcesInstanceBinding.size(); i++) {
//			// �p�G�ŧi����m��closeResource�O�P�@��variable�A�N���~�򩹤U����
//			if(node.getName().resolveBinding().equals(closeResourcesInstanceBinding.get(i))){
//				// �o��return false�N���|�i�hclass instance creation
//				return false;
//			}
//		}
//		return true;
//	}
	
	public boolean visit(ThrowStatement node) {
		for (int i = 0; i < closeResources.size(); i++) {
			if (isNodeBetweenCreationAndClose(node, closeResources.get(i))) {
				carelessCleanupMethod.add(closeResources.get(i));
				closeResources.remove(i);
				closeResourcesInstanceBinding.remove(i);
				break;
			}
		}
		return false;
	}
	
	public boolean visit(ClassInstanceCreation node) {
		// �btry�~�����|�ߨҥ~��ClassInstanceCreation�A���|�y��close��careless cleanup
//		if(node.resolveConstructorBinding().getExceptionTypes().length == 0) {
//			return false;
//		}
		int nodeExceptionLength = node.resolveConstructorBinding().getExceptionTypes().length;
		
		// �btry�~���|�ߨҥ~��ClassInstanceCreation���i��y��finally�̭���close�Ocareless cleanup
		for (int i = 0; i < closeResources.size(); i++) {
			if ((isNodeBetweenCreationAndClose(node, closeResources.get(i))) &&
				(nodeExceptionLength != 0)){
				carelessCleanupMethod.add(closeResources.get(i));
				closeResources.remove(i);
				closeResourcesInstanceBinding.remove(i);
				return false;
			}
		}
		
		return true;
	}
	
	public List<MethodInvocation> getCarelessCleanupNodes() {
		return carelessCleanupMethod;
	}
	
	/**
	 * ���N�Ƕi�Ӫ�node�O�_�bcloseResource�H�Φ�closeResource�ŧi����m����
	 * @param node
	 * @param closeResource
	 * @return
	 */
	private boolean isNodeBetweenCreationAndClose(ASTNode node, MethodInvocation closeResource) {
		boolean isBetween = false;
		int creationNodeStartPosition = 0;
		int closeNodeStartPosition = closeResource.getStartPosition();
		int astNodeStartPosition = node.getStartPosition();
		
		/*
		 * ��XcloseResource��instance�b���Ӹ`�I�Q�ŧi
		 */
		ClassInstanceCreationVisitor cicVisitor = new ClassInstanceCreationVisitor(closeResource);
		ASTNode methodDeclaration = NodeUtils.getSpecifiedParentNode(closeResource, ASTNode.METHOD_DECLARATION);
		methodDeclaration.accept(cicVisitor);
		ClassInstanceCreation creationNode = cicVisitor.getClassInstanceCreation();
		if(creationNode != null) {
			creationNodeStartPosition = creationNode.getStartPosition();
		}
		
		if ((astNodeStartPosition > creationNodeStartPosition) &&
			(astNodeStartPosition < closeNodeStartPosition)) {
			isBetween = true;
		}
		return isBetween;
	}
}
 