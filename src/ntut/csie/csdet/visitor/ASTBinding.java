package ntut.csie.csdet.visitor;

import ntut.csie.rleht.common.RLBaseVisitor;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.SimpleName;

/**
 * 用來找尋Statement Expression中的
 *    org.apache.log4j.Logger及java.util.logging.Logger
 * @author chewei
 */
public class ASTBinding extends RLBaseVisitor{
	private Boolean result;
	private int strategy;
	
	public ASTBinding(int choice){
		this.result = false;
		this.strategy = choice;
	}

	protected boolean visitNode(ASTNode node){
		try {
			switch (node.getNodeType()) {
				case ASTNode.METHOD_INVOCATION:

					return true;
			
				case ASTNode.SIMPLE_NAME:
					SimpleName name = (SimpleName) node;
					
					if(strategy == 0 || strategy == 4 ){
						// log4j,java Logger
						if(name.resolveTypeBinding().toString().contains("org.apache.log4j.Logger")||
								name.resolveTypeBinding().toString().contains("java.util.logging.Logger")){
							result = true;
						}
					}else if(strategy == 1 || strategy == 5){
						// java Logger
						if(name.resolveTypeBinding().toString().contains("java.util.logging.Logger")){
							result = true;
						}
					}else if(strategy == 2 || strategy == 6){
						// log4j
						if(name.resolveTypeBinding().toString().contains("org.apache.log4j.Logger")){
							result = true;
						}
					}

					return false;

				default:
					return true;

			}
		} catch (Exception e) {
			return false;
		}
	}
	
	public Boolean getResult(){
		return result;
	}
}

