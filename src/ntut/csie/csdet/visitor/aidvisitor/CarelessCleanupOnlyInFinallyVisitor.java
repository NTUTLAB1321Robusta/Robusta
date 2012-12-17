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
 * 單純檢查Finally Block裡面的程式碼。
 * 如果再關閉串流的程式碼之前有程式碼會拋出例外，則關閉串流的程式碼即是careless cleanup。
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
	
	/**
	 * 考慮Finally Block裡面有TryStatement的情況。
	 * 透過TryStatementExceptionsVisitor來檢查，以了解finally裡面的 TryStatement會不會拋例外。
	 * (如果這個會拋例外的TryStatement在關閉串流的程式碼前面，則會造成careless cleanup。)
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
		 * (對RuntimeException沒轍。)
		 * 如果是關閉串流的程式碼，要做careless cleanup的檢查：
		 * 	如果在這行之前可能會發生例外，則這行為careless cleanup。
		 *  如果在這行之前不會發生例外，則這行就不算careless cleanup
		 */
		if(NodeUtils.isCloseResourceMethodInvocation(root, node)) {
			// 如果前面已經有程式碼會發生例外，則這個關閉串流的程式碼就是careless cleanup
			if(isExceptionRisable) {
				carelessCleanupNodes.add(node);
			} else {
				fineCleanupNodes.add(node);
			}
		}
		
		/* 
		 * 記錄這行程式碼會不會拋出例外：
		 * 	單純紀錄會不會拋出例外。
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
	 * 不被這個Visitor認定careless cleanup 的關閉資源動作。
	 * @return
	 */
	public List<MethodInvocation> getfineCleanupNodes() {
		return fineCleanupNodes;
	}
}
