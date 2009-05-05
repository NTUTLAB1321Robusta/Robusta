package ntut.csie.csdet.visitor;

import java.util.Iterator;
import java.util.TreeMap;

import ntut.csie.rleht.common.RLBaseVisitor;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.SimpleName;

/**
 * �Ψӧ�MStatement Expression����
 * org.apache.log4j.Logger��java.util.logging.Logger
 * @author chewei
 */
public class ASTBinding extends RLBaseVisitor{
	//�O�_����������Library
	private Boolean result;
	//�x�s����"Library��Name"�M"�O�_Library"
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
					//�����~��Library�Morg.apache.log4j.Logger��java.util.logging.Logger
					Iterator<String> libIt = libMap.keySet().iterator();
					while(libIt.hasNext()){
						String temp = libIt.next();
						//�P�_�O�_�n���� �B ���y�]�]�t������Library
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
	 * ���o�O�_����������Library
	 */
	public Boolean getResult(){
		return result;
	}
}

