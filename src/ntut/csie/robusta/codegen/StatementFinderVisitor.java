package ntut.csie.robusta.codegen;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.MethodDeclaration;

public class StatementFinderVisitor extends ASTVisitor {

	/**	將找到的結果存在這裡 */
	private ExpressionStatement foundExpressionStatement;
	
	/** 想比對的startPosition */
	private int comparisingStartPosition;
	
	/** 是否繼續Visit整個Tree */
	private boolean isKeepVisiting;
	
	public StatementFinderVisitor(int startPosition) {
		foundExpressionStatement = null;
		isKeepVisiting = true;
		comparisingStartPosition = startPosition;
	}
	
	public boolean visit(MethodDeclaration methodDeclaration) {
		/*
		 * 如果不繼續visit整個Tree，就在MethodDeclaration的節點擋掉，
		 * 不繼續往子節點拜訪，加快結束的速度。
		 * 這個是針對使用此Class的Caller要求拜訪CompilationUnit的時候才有作用。
		 * 換句話說，如果只是拜訪IfStatement, TryStatement..., and so on，這段程式碼就沒差。
		 */
		return isKeepVisiting;
	}
	
	public boolean visit(ExpressionStatement node) {
		if(node.getStartPosition() == comparisingStartPosition) {
			foundExpressionStatement = node;
			isKeepVisiting = false;
		}
		return false;
	}
	
	public ExpressionStatement getFoundExpressionStatement() {
		return foundExpressionStatement;
	}
}
