package ntut.csie.csdet.visitor;

import java.util.TreeMap;
import java.util.Iterator;
import ntut.csie.rleht.common.RLBaseVisitor;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.MethodInvocation;

/**
 * �Ψӧ�MStatement Expression����
 * org.apache.log4j.Logger��java.util.logging.Logger
 * @author chewei
 */
public class ASTBinding extends RLBaseVisitor{
	//�O�_����������Library
	private Boolean result;
	//�x�s����Library��Name�MMethod���W��
	private TreeMap<String, String> libMap;

	public ASTBinding(TreeMap<String, String> libMap){
		this.libMap = libMap;
		this.result = false;
	}

	protected boolean visitNode(ASTNode node){
		try {
			switch (node.getNodeType()) {
				//����Method
				case ASTNode.METHOD_INVOCATION:
					MethodInvocation mi = (MethodInvocation)node;

					//���oMethod��Library�W��
					String libName = mi.resolveMethodBinding().getDeclaringClass().getQualifiedName();
					//���oMethod���W��
					String methodName = mi.resolveMethodBinding().getName();

					//�p�G�Ӧ榳Array(�pjava.util.ArrayList<java.lang.Boolean>)�A��<>���e����
					if (libName.indexOf("<")!=-1)
						libName = libName.substring(0,libName.indexOf("<"));

					Iterator<String> libIt = libMap.keySet().iterator();
					//�P�_�O�_�n���� �B ���y�]�]�t������Library
					while(libIt.hasNext()){
						String temp = libIt.next();
						//�����w�]Library�Morg.apache.log4j.Logger��java.util.logging.Logger
						if (temp == "org.apache.log4j" ||temp == "java.util.logging"){
							//�u�n���]�t�N����
							if (libName.contains(temp)){
								result = true;
								return false;
							}
						//����Library.Method���Φ�
						}else if (libMap.get(temp) != null){
							if (libName.equals(temp) &&	methodName.equals(libMap.get(temp))){
								result = true;
								return false;
							}
						//�Y�u����Library��Method�A��Map��Value�O�Ū�
						}else{
							//�u����Library
							if (temp.indexOf(".")!=-1){
								if (libName.equals(temp)){
									result = true;
									return false;
								}
							//�u����Method
							}else{
								if (methodName.equals(temp)){
									result = true;
									return false;
								}
							}
						}
					}
					return true;
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

