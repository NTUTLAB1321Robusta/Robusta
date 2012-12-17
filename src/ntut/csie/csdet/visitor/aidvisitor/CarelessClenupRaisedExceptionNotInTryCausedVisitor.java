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
 * 檢查Finally block裡面關閉資源之Method Invocation的instane，
 * 是不是在Try外面，並且會拋出例外。
 * 
 * 用MethodDeclaration去accept，並且傳入已經知道的close清單進來。
 * MethodInvocation再去比對，看是不是有match的instance
 * 
 * @author charles
 *
 */
public class CarelessClenupRaisedExceptionNotInTryCausedVisitor extends	ASTVisitor {

	/** 負責做資源關閉的MethodInvocation(嫌疑犯) */
	private List<MethodInvocation> closeResources;
	
	/** 確定是careless cleanup的MethodInvocation */
	private List<MethodInvocation> carelessCleanupMethod;
	
	/** MethodDeclaration所有關閉資源的method invocation所Binding的instance。*/
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
	 * 忽略TryStatement的檢查。
	 */
	public boolean visit(TryStatement node) {
		TryStatementExceptionsVisitor tryStatementVisitor = new TryStatementExceptionsVisitor(node);
		node.accept(tryStatementVisitor);
		if(!(tryStatementVisitor.getTotalExceptionStrings().length > 0)) {
			return false;
		}
		
		for(int i = 0; i<closeResources.size(); i++) {
			TryStatement closeResourcesTryStatement = (TryStatement) NodeUtils.getSpecifiedParentNode(closeResources.get(i), ASTNode.TRY_STATEMENT);
			// 如果比到的 TryStatement剛好是關閉資源的程式碼所在的TryStatement，則略過檢查
			if(node.equals(closeResourcesTryStatement)) {
				continue;
			}

			if(isNodeBetweenCreationAndClose(node, closeResources.get(i))) {
				// 將close的動作加入careless cleanup 清單
				carelessCleanupMethod.add(closeResources.get(i));
				
				/* 將加過的close動作移除掉 */
				closeResourcesInstanceBinding.remove(i);
				closeResources.remove(i);
				break;
			}
		}
		
		return false;
	}
	
	public boolean visit(MethodInvocation node) {
		SimpleName nodeVariable = NodeUtils.getMethodInvocationBindingVariableSimpleName(node.getExpression());
		// System.out.println(fis.toString()); 這種Node可能就會NULL
		if(nodeVariable == null) {
			return true;
		}
		
		// 如果這個Node本身不會拋出例外，則不會是造成careless cleanup的原因
		int checkedExceptionLength = node.resolveMethodBinding().getExceptionTypes().length;
		if(checkedExceptionLength == 0) {
			return true;
		}
		
		for(int i = 0; i<closeResources.size(); i++) {
			if(isNodeBetweenCreationAndClose(node, closeResources.get(i))) {
				// 將close的動作加入careless cleanup 清單
				carelessCleanupMethod.add(closeResources.get(i));
				
				/* 將加過的close動作移除掉 */
				closeResourcesInstanceBinding.remove(i);
				closeResources.remove(i);
				return false;
			}
		}
		return true;
	}
	
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
		int nodeExceptionLength = node.resolveConstructorBinding().getExceptionTypes().length;
		
		// 在try外面會拋例外的ClassInstanceCreation有可能造成finally裡面的close是careless cleanup
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
	 * 任意傳進來的node是否在closeResource以及此closeResource宣告的位置之間
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
		 * 找出closeResource的instance在哪個節點被宣告
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
 