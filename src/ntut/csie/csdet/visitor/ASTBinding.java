package ntut.csie.csdet.visitor;

import java.util.TreeMap;
import java.util.Iterator;
import ntut.csie.rleht.common.RLBaseVisitor;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.MethodInvocation;

/**
 * 用來找尋Statement Expression中的
 * org.apache.log4j.Logger及java.util.logging.Logger
 * @author chewei
 */
public class ASTBinding extends RLBaseVisitor{
	final static public int LIBRARY = 1;
	final static public int METHOD = 2;
	final static public int LIBRARY_METHOD = 3;
	
	//是否找到欲偵測的Library
	private Boolean result;
	//儲存偵測Library的Name和Method的名稱
	private TreeMap<String, Integer> libMap;

	public ASTBinding(TreeMap<String, Integer> libMap){
		this.libMap = libMap;
		this.result = false;
	}

	protected boolean visitNode(ASTNode node){
		try {
			switch (node.getNodeType()) {
				//偵測Method
				case ASTNode.METHOD_INVOCATION:
					MethodInvocation mi = (MethodInvocation)node;

					//取得Method的Library名稱
					String libName = mi.resolveMethodBinding().getDeclaringClass().getQualifiedName();
					//取得Method的名稱
					String methodName = mi.resolveMethodBinding().getName();

					//如果該行有Array(如java.util.ArrayList<java.lang.Boolean>)，把<>內容拿掉
					if (libName.indexOf("<")!=-1)
						libName = libName.substring(0,libName.indexOf("<"));

					Iterator<String> libIt = libMap.keySet().iterator();
					//判斷是否要偵測 且 此句也包含欲偵測Library
					while(libIt.hasNext()){
						String temp = libIt.next();
						//只偵測Library
						if (libMap.get(temp) == LIBRARY){
							//若Library長度大於偵測長度，否則表不相同直接略過
							if (libName.length() >= temp.length())
							{
								//比較前半段長度的名稱是否相同
								if (libName.substring(0,temp.length()).equals(temp)){
									result = true;
									return false;
								}
							}
						//只偵測Method
						}else if (libMap.get(temp) == METHOD){
							if (methodName.equals(temp)){
								result = true;
								return false;
							}
						//偵測Library.Method的形式
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
	 * 取得是否找到欲偵測的Library
	 */
	public Boolean getResult(){
		return result;
	}
}

