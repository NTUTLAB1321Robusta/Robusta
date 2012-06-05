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
			 * �o��TryStatement���O�bTryStatement�̭�
			 */
			return true;
		} else {
			/*
			 * Try�̭����ӴN�����Ӧ�Try Catch���{���X(Nested Try Block)�C
			 * �ҥH�p�G�J�쪺TryStatement Node�OTry Statement�̭��A���N���~�򰻴��C
			 * 
			 * �קKClose Stream�ɡA���o��Dummy Handler�����D�C
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
	 * �q��J���`�I�}�l�A�M��S�w�����`�I�C
	 * @param startNode
	 * @param nodeType
	 * @return
	 */
	public ASTNode getSpecifiedParentNode(ASTNode startNode, int nodeType) {
		ASTNode resultNode = null;
		ASTNode parentNode = startNode.getParent();
		// �p�GparentNode�Onull�A��ܶǶi�Ӫ�node�w�g�OrootNode(CompilationUnit)
		if(parentNode != null) {
			while(parentNode.getNodeType() != nodeType) {
				parentNode = parentNode.getParent();
				// �L�a�j��פ���� - �w�g�S��parentNode
				if (parentNode == null) {
					break;
				}
			}
			resultNode = parentNode; 
		}
		return resultNode;
	}
}
