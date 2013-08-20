package robusta.nodeCounter;

import ntut.csie.jdt.util.NodeUtils;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AssertStatement;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.BreakStatement;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ContinueStatement;
import org.eclipse.jdt.core.dom.DoStatement;
import org.eclipse.jdt.core.dom.EmptyStatement;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.LabeledStatement;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SwitchCase;
import org.eclipse.jdt.core.dom.SwitchStatement;
import org.eclipse.jdt.core.dom.SynchronizedStatement;
import org.eclipse.jdt.core.dom.ThrowStatement;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.core.dom.TypeDeclarationStatement;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.WhileStatement;

/**
 * 計算總共有多少 statement 位於偵測的 class 內的特定位置中
 * @author crimson
 */
/*
 * FIXME 計算上有Bug, 針對DummyAndIgnoreExample偵測時，得到136/39/4
 * 當把finally-catch內的「e.printStackTrace()」註解掉時，的到135/38/3，明顯有誤
 */
public class CountVisitor extends ASTVisitor {
	private int statementInCatchCounter = 0;
	private int statementInFinallyCounter = 0;
	private int statementInTryStatement = 0;
	private CompilationUnit unit;
	private TryStatement tryStatement;
	
	public CountVisitor() {
		
	}
	
	public CountVisitor(CompilationUnit unit) {
		this.unit = unit;
	}
	
	public boolean visit(TryStatement node) {
		if(NodeUtils.getSpecifiedParentNode(node, ASTNode.TRY_STATEMENT) == null) {
			tryStatement = node;
			statementInTryStatement++;
//			System.out.println("totalCount " + unit.getLineNumber(node.getStartPosition()));
		}
		judgement(node);
		return true;
	}
	
	public boolean visit(CatchClause node) {
		judgement(node);
		return true;
	}
	
	public boolean visit(Block node) {
		if(tryStatement != null && tryStatement.getFinally() != null) {
			if(tryStatement.getFinally().getStartPosition() == node.getStartPosition()) {
				statementInTryStatement++;
//				System.out.println("totalCount " + unit.getLineNumber(node.getStartPosition()));
				return true;
			}
		}
		if(NodeUtils.getSpecifiedParentNode(node, ASTNode.TRY_STATEMENT) != null) {
			ASTNode tryStatement = NodeUtils.getSpecifiedParentNode(node, ASTNode.TRY_STATEMENT);
			if(((TryStatement)tryStatement).getFinally() != null && ((TryStatement)tryStatement).getFinally().getStartPosition() == node.getStartPosition()) {
				statementInTryStatement++;
//				System.out.println("totalCount " + unit.getLineNumber(node.getStartPosition()));
				if(NodeUtils.getSpecifiedParentNode(node, ASTNode.CATCH_CLAUSE) != null) {
					statementInCatchCounter++;
//					System.out.println("catchCounter " + unit.getLineNumber(node.getStartPosition()));
				}
				if(isStatementInFinally(node)) {
					statementInFinallyCounter++;
//					System.out.println("finallyCounter " + unit.getLineNumber(node.getStartPosition()));
				}
			}
		}
		return true;
	}
	
	private void judgement(ASTNode node) {
		ASTNode tryBlock = NodeUtils.getSpecifiedParentNode(node, ASTNode.TRY_STATEMENT);
		ASTNode catchBlock = NodeUtils.getSpecifiedParentNode(node, ASTNode.CATCH_CLAUSE);
		
		if(tryBlock != null) {
			statementInTryStatement++;
//			System.out.println("totalCount " + unit.getLineNumber(node.getStartPosition()));
		}
		if(catchBlock != null) {
			statementInCatchCounter++;
//			System.out.println("catchCounter " + unit.getLineNumber(node.getStartPosition()));
		}
		if(isStatementInFinally(node)) {
			statementInFinallyCounter++;
//			System.out.println("finallyCounter " + unit.getLineNumber(node.getStartPosition()));
		}
	}
	
	private boolean isStatementInFinally(ASTNode node) {
		if(tryStatement != null && tryStatement.getFinally() != null) {
			ASTNode parent = NodeUtils.getSpecifiedParentNode(node, ASTNode.BLOCK);
			if(parent != null) {
				while(tryStatement.getFinally().getStartPosition() != parent.getStartPosition()) {
					parent = NodeUtils.getSpecifiedParentNode(parent, ASTNode.BLOCK);
					if(parent == null || parent.getNodeType() == ASTNode.METHOD_DECLARATION)
						break;
				}
				if(parent != null && parent.getNodeType() != ASTNode.METHOD_DECLARATION)
					return true;
			}
		}
		if(NodeUtils.getSpecifiedParentNode(node, ASTNode.TRY_STATEMENT) != null) {
			ASTNode tryStatement = NodeUtils.getSpecifiedParentNode(node, ASTNode.TRY_STATEMENT);
			if(((TryStatement)tryStatement).getFinally() != null) {
				ASTNode parent = NodeUtils.getSpecifiedParentNode(node, ASTNode.BLOCK);
				if(parent != null) {
					while(((TryStatement)tryStatement).getFinally().getStartPosition() != parent.getStartPosition()) {
						parent = NodeUtils.getSpecifiedParentNode(parent, ASTNode.BLOCK);
						if(parent == null || parent.getNodeType() == ASTNode.METHOD_DECLARATION)
							break;
					}
					if(parent != null && parent.getNodeType() != ASTNode.METHOD_DECLARATION)
						return true;
				}
			}
		}
		return false;
	}
	
	public boolean visit(SwitchCase node) {
		judgement(node);
		return true;
	}
	
	public boolean visit(ExpressionStatement node) {
		judgement(node);
		return true;
	}
	
	public boolean visit(AssertStatement node) {
		judgement(node);
		return true;
	}
	
	public boolean visit(BreakStatement node) {
		judgement(node);
		return true;
	}
	
	public boolean visit(ContinueStatement node) {
		judgement(node);
		return true;
	}
	
	public boolean visit(DoStatement node) {
		judgement(node);
		return true;
	}
	
	public boolean visit(EmptyStatement node) {
		judgement(node);
		return true;
	}
	
	public boolean visit(ForStatement node) {
		judgement(node);
		return true;
	}
	
	public boolean visit(IfStatement node) {
		judgement(node);
		return true;
	}
	
	public boolean visit(LabeledStatement node) {
		judgement(node);
		return true;
	}
	
	public boolean visit(ReturnStatement node) {
		judgement(node);
		return true;
	}
	
	public boolean visit(SwitchStatement node) {
		judgement(node);
		return true;
	}
	
	public boolean visit(SynchronizedStatement node) {
		judgement(node);
		return true;
	}
	
	public boolean visit(ThrowStatement node) {
		judgement(node);
		return true;
	}
	
	public boolean visit(TypeDeclarationStatement node) {
		judgement(node);
		return true;
	}
	
	public boolean visit(VariableDeclarationStatement node) {
		judgement(node);
		return true;
	}
	
	public boolean visit(EnhancedForStatement node) {
		judgement(node);
		return true;
	}
	
	public boolean visit(WhileStatement node) {
		judgement(node);
		return true;
	}
	
	public int getCatchCount() {
		return statementInCatchCounter;
	}
	
	public int getFinallyCount() {
		return statementInFinallyCounter;
	}
	
	public int getTotalCount() {
		return statementInTryStatement;
	}
	
	public void setCatchCount(int count) {
		statementInCatchCounter+=count;
	}
	
	public void setFinallyCount(int count) {
		statementInFinallyCounter+=count;
	}
	
	public void setTotalCount(int count) {
		statementInTryStatement+=count;
	}
}
