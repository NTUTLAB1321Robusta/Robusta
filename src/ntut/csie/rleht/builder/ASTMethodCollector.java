package ntut.csie.rleht.builder;

import java.util.ArrayList;
import java.util.List;

import ntut.csie.rleht.common.RLBaseVisitor;

import org.eclipse.jdt.core.dom.ASTNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 收集所有class中的method
 * @author allen
 */

public class ASTMethodCollector extends RLBaseVisitor {
	private static Logger logger = LoggerFactory.getLogger(ASTMethodCollector.class);
	private List<ASTNode> methodList;


	public ASTMethodCollector() {
		super(true);
		methodList = new ArrayList<ASTNode>();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ntut.csie.rleht.views.ASTBaseVisitor#visitNode(org.eclipse.jdt.core.dom.ASTNode)
	 */
	protected boolean visitNode(ASTNode node) {

		try {

			switch (node.getNodeType()) {
				case ASTNode.METHOD_DECLARATION:
					this.methodList.add(node);
					return true;
				default:
					return true;

			}
		}
		catch (Exception ex) {
			logger.error("[visitNode] EXCEPTION ",ex);
			return false;
		}
	}

	public List<ASTNode> getMethodList() {
		return methodList;
	}
	
}
