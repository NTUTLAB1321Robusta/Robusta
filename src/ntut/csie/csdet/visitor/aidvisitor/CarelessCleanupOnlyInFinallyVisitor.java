package ntut.csie.csdet.visitor.aidvisitor;

import java.util.ArrayList;
import java.util.List;

import ntut.csie.jdt.util.NodeUtils;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.ThrowStatement;

/**
 * 檢查特定的Block最先在哪個Node會拋出例外。
 * 
 * 注意：不是只有Finally的節點叫做Block。
 * @author charles
 *
 */
public class CarelessCleanupOnlyInFinallyVisitor extends ASTVisitor {
	private CompilationUnit root;
	private boolean isExceptionRisable;
	private List<MethodInvocation> carelessCleanupNodes;
	
	/** 蒐集不是careless cleanup，但是是關閉串流的程式碼 */
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
			// 如果前面已經有程式碼會發生例外，則這個關閉串流的程式碼就是careless cleanup
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
	 * 不被這個Visitor認定careless cleanup 的關閉資源動作。
	 * @return
	 */
	public List<MethodInvocation> getfineCleanupNodes() {
		return fineCleanupNodes;
	}
}
