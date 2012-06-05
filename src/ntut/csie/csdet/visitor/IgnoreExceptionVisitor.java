package ntut.csie.csdet.visitor;

import java.util.ArrayList;
import java.util.List;

import ntut.csie.csdet.data.MarkerInfo;
import ntut.csie.rleht.builder.RLMarkerAttribute;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.TryStatement;

public class IgnoreExceptionVisitor extends ASTVisitor {
	CompilationUnit root;
	private List<MarkerInfo> ignoreExceptionList;
	
	public IgnoreExceptionVisitor(CompilationUnit root) {
		super();
		this.root = root;
		ignoreExceptionList = new ArrayList<MarkerInfo>();
	}
	
	public boolean visit(TryStatement node) {
		ASTNode parent = getSpecifiedParentNode(node, ASTNode.TRY_STATEMENT);
		if(parent == null) {
			/*
			 * 這個TryStatement不是在TryStatement裡面
			 */
			return true;
		} else {
			/*
			 * Try裡面本來就不應該有Try Catch的程式碼(Nested Try Block)。
			 * 所以如果遇到的TryStatement Node是Try Statement裡面，那就不繼續偵測。
			 * 
			 * 避免Close Stream時，不得不Dummy Handler的問題。
			 */
			return false;
		}
	}
	
	public boolean visit(CatchClause node) {
		List<?> statements  = node.getBody().statements();
		if(statements.size() ==  0) {
			SingleVariableDeclaration svd = node.getException();
			MarkerInfo markerInfo = new MarkerInfo(	RLMarkerAttribute.CS_INGNORE_EXCEPTION, 
													svd.resolveBinding().getType(),
													node.toString(), node.getStartPosition(),
													root.getLineNumber(node.getStartPosition()),
													svd.getType().toString());
			ignoreExceptionList.add(markerInfo);
		}
		return false;	
	}
	
	public List<MarkerInfo> getIgnoreList() {
		return ignoreExceptionList;
	}
	
	/**
	 * 從輸入的節點開始，尋找特定的父節點。
	 * @param startNode
	 * @param nodeType
	 * @return
	 */
	public ASTNode getSpecifiedParentNode(ASTNode startNode, int nodeType) {
		ASTNode resultNode = null;
		ASTNode parentNode = startNode.getParent();
		// 如果parentNode是null，表示傳進來的node已經是rootNode(CompilationUnit)
		if(parentNode != null) {
			while(parentNode.getNodeType() != nodeType) {
				parentNode = parentNode.getParent();
				// 無窮迴圈終止條件 - 已經沒有parentNode
				if (parentNode == null) {
					break;
				}
			}
			resultNode = parentNode; 
		}
		return resultNode;
	}
}
