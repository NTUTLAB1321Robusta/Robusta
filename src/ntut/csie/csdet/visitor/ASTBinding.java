package ntut.csie.csdet.visitor;

import java.util.Iterator;
import java.util.TreeMap;

import ntut.csie.rleht.common.RLBaseVisitor;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.SimpleName;

/**
 * 用來找尋Statement Expression中的
 * org.apache.log4j.Logger及java.util.logging.Logger
 * @author chewei
 */
public class ASTBinding extends RLBaseVisitor{
	//是否找到欲偵測的Library
	private Boolean result;
	//儲存偵測"Library的Name"和"是否Library"
	private TreeMap<String, Boolean> libMap;
	
	public ASTBinding(TreeMap<String, Boolean> libMap){
		this.libMap = libMap;
		this.result = false;
	}

	protected boolean visitNode(ASTNode node){
		try {
			switch (node.getNodeType()) {
			
				case ASTNode.SIMPLE_NAME:
					SimpleName name = (SimpleName) node;
					//偵測外部Library和org.apache.log4j.Logger及java.util.logging.Logger
					Iterator<String> libIt = libMap.keySet().iterator();
					while(libIt.hasNext()){
						String temp = libIt.next();
						//判斷是否要偵測 且 此句也包含欲偵測Library
						if(libMap.get(temp) && name.resolveTypeBinding().toString().contains(temp)){
							result = true;
							return false;
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

	/**
	 * 取得是否找到欲偵測的Library
	 */
	public Boolean getResult(){
		return result;
	}
}

