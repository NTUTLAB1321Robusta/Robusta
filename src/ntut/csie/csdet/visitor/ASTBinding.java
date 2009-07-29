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
	final static public int LIBRARY = 1;
	final static public int METHOD = 2;
	final static public int LIBRARY_METHOD = 3;
	
	//�O�_����������Library
	private Boolean result;
	//�x�s����Library��Name�MMethod���W��
	private TreeMap<String, Integer> libMap;

	public ASTBinding(TreeMap<String, Integer> libMap){
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
						//�u����Library
						if (libMap.get(temp) == LIBRARY){
							//�YLibrary���פj�󰻴����סA�_�h���ۦP�������L
							if (libName.length() >= temp.length())
							{
								//����e�b�q���ת��W�٬O�_�ۦP
								if (libName.substring(0,temp.length()).equals(temp)){
									result = true;
									return false;
								}
							}
						//�u����Method
						}else if (libMap.get(temp) == METHOD){
							if (methodName.equals(temp)){
								result = true;
								return false;
							}
						//����Library.Method���Φ�
						}else if (libMap.get(temp) == LIBRARY_METHOD){
							int pos = temp.lastIndexOf(".");
							System.out.println(temp.substring(0, pos) + "  " + temp.substring(pos+1));
							if (libName.equals(temp.substring(0, pos))
								&& methodName.equals(temp.substring(pos+1))){
								result = true;
								return false;
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

