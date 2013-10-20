package robusta.specificNodeCounter;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;

/**
 * 根據設定，偵測檔案中特定節點的個數
 * 可以設定多個 SpecificNodeType
 * @author pig
 */
public class SpecificNodeVisitor extends ASTVisitor {

	/*
	 * 目前SpecificNodeCounter無法利用此class， 要使用時可以先利用 RLBuilder， 加入以下程式碼
	 * private int number = 0; 
	 * SpecificNodeVisitor snVisitor = new SpecificNodeVisitor();
	 * snVisitor.addNodeType(ASTNode.TRY_STATEMENT);
	 * root.accept(snVisitor);
	 * number += snVisitor.getCount();
	 */

	private List<Integer> specificNodeTypes;
	private int counter;

	public SpecificNodeVisitor() {
		specificNodeTypes = new ArrayList<Integer>();
		counter = 0;
	}

	public void preVisit(ASTNode node) {
		for (Integer type : specificNodeTypes) {
			if (node.getNodeType() == type.intValue()) {
				counter++;
				return;
			}
		}
	}

	/**
	 * 新增要計算的節點在 ASTNode 中的 type index
	 * @param index
	 */
	public void addNodeType(int index) {
		specificNodeTypes.add(index);
	}

	/**
	 * 將「Statement」新增到要偵測的 node types 中
	 */
	public void addStatementAsNodeType() {
		/*
		 * 此處認為這些types的交集即為statement，因為
		 * 	一、參考了 <code>Statement</code> 開頭註解中對JLS3的敘述（敘述中缺少SwitchCase）
		 * 	二、檢查過所有使用到 <code>Statement</code> 的同 package 的 class
		 * 
		 * 另外故意忽略 Block，因為人的直覺上 Block 並不是 Statement
		 */
		specificNodeTypes.add(ASTNode.IF_STATEMENT);
		specificNodeTypes.add(ASTNode.FOR_STATEMENT);
		specificNodeTypes.add(ASTNode.ENHANCED_FOR_STATEMENT);
		specificNodeTypes.add(ASTNode.WHILE_STATEMENT);
		specificNodeTypes.add(ASTNode.DO_STATEMENT);
		specificNodeTypes.add(ASTNode.TRY_STATEMENT);
		specificNodeTypes.add(ASTNode.SWITCH_STATEMENT);
		specificNodeTypes.add(ASTNode.SYNCHRONIZED_STATEMENT);
		specificNodeTypes.add(ASTNode.RETURN_STATEMENT);
		specificNodeTypes.add(ASTNode.THROW_STATEMENT);
		specificNodeTypes.add(ASTNode.BREAK_STATEMENT);
		specificNodeTypes.add(ASTNode.CONTINUE_STATEMENT);
		specificNodeTypes.add(ASTNode.EMPTY_STATEMENT);
		specificNodeTypes.add(ASTNode.EXPRESSION_STATEMENT);
		specificNodeTypes.add(ASTNode.LABELED_STATEMENT);
		specificNodeTypes.add(ASTNode.ASSERT_STATEMENT);
		specificNodeTypes.add(ASTNode.VARIABLE_DECLARATION_STATEMENT);
		specificNodeTypes.add(ASTNode.TYPE_DECLARATION_STATEMENT);
		specificNodeTypes.add(ASTNode.CONSTRUCTOR_INVOCATION);
		specificNodeTypes.add(ASTNode.SUPER_CONSTRUCTOR_INVOCATION);
		specificNodeTypes.add(ASTNode.SWITCH_CASE);
	}

	public int getCount() {
		return counter;
	}
}
