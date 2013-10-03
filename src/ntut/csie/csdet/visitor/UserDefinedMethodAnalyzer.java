package ntut.csie.csdet.visitor;

import java.io.Closeable;
import java.io.File;
import java.util.Iterator;
import java.util.TreeMap;

import ntut.csie.csdet.preference.RobustaSettings;
import ntut.csie.csdet.preference.SmellSettings;
import ntut.csie.jdt.util.NodeUtils;
import ntut.csie.robusta.agile.exception.RTag;
import ntut.csie.robusta.agile.exception.Robustness;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodInvocation;


public class UserDefinedMethodAnalyzer {
	public final static String SETTINGFILEPATH = ResourcesPlugin.getWorkspace().getRoot().getLocation().toOSString() + File.separator + SmellSettings.SETTING_FILENAME;
	private TreeMap<String, SmellSettings.UserDefinedConstraintsType> methodTreeMap;
	boolean isEnable;
	
	@Robustness(value = { @RTag(level = 1, exception = java.lang.RuntimeException.class) })
	public UserDefinedMethodAnalyzer(String smellName) {
		SmellSettings smellSettings = new SmellSettings(SETTINGFILEPATH);
		methodTreeMap = smellSettings.getSmellSettings(SmellSettings.SMELL_CARELESSCLEANUP);
		isEnable = smellSettings.isDetectingSmell(SmellSettings.SMELL_CARELESSCLEANUP);
	}
	
	public static String getRobustaSettingXMLPath(String projectName) {
		return ResourcesPlugin.getWorkspace().getRoot().getLocation()
				.toOSString()
				+ File.separator
				+ projectName
				+ File.separator
				+ RobustaSettings.SETTING_FILENAME;
	}
	
	/**
	 * 判斷這個Library是不是使用者定義的Library。
	 * @param node
	 * @return true，這個node符合使用者定義的Library，外部程式可以考慮加上marker<br />
	 * 		   false，這個node不符合使用者定義的Library。
	 */
	public boolean analyzeLibrary(MethodInvocation node) {
		if(methodTreeMap.isEmpty()) {
			return false;
		}
		
		Iterator<String> userDefinedMethodIterator = methodTreeMap.keySet().iterator();
		while(userDefinedMethodIterator.hasNext()) {
			String condition = userDefinedMethodIterator.next();
			// 目前知道的情況：Override Closeable的close，使其不會拋出IOException，會造成resolveMethodBinding為null
			if(node.resolveMethodBinding() == null) {
				continue;
			}
			if(node.resolveMethodBinding().getDeclaringClass().getQualifiedName().equals(condition)) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * 判斷這個方法(method / Lib+method)是不是使用者定義的method
	 * @param node
	 * @return true，這個node符合使用者定義的method，外部程式可以考慮加上marker<br />
	 * 		   false，這個node不符合使用者定義的method。
	 */
	public boolean analyzeMethods(MethodInvocation node) {
		if(methodTreeMap.isEmpty()) {
			return false;
		}
		
		String matchedKey = "";
		int dotIndex = 0;
		Iterator<String> methodTreeMapIterator = methodTreeMap.keySet().iterator();
		while(methodTreeMapIterator.hasNext()) {
			String condition = methodTreeMapIterator.next();
			if(methodTreeMap.get(condition) != SmellSettings.UserDefinedConstraintsType.Library) {
				dotIndex = condition.lastIndexOf(".") + 1;
			}
			
			if(dotIndex == -1) {
				dotIndex = 0;
			}
			
			if(node.getName().toString().equals(condition.substring(dotIndex))) {
				matchedKey = condition;
				break;
			}
		}
		
		if(matchedKey.isEmpty()) {
			return false;
		}
		
		// method name符合，且是SmellSettings.UserDefinedConstraintsType.Method
		if(methodTreeMap.get(matchedKey) == SmellSettings.UserDefinedConstraintsType.Method) {
			return true;
		} else {
		// method name符合，且是SmellSettings.UserDefinedConstraintsType.FullQulifiedMethod
			String declareClass = matchedKey.substring(0, dotIndex - 1);
			if(node.resolveMethodBinding().getDeclaringClass().getQualifiedName().equals(declareClass)) {
				return true;
			}		
		}
		return false;
	}
	
	/**
	 * 判斷careless cleanup的extra rule
	 * @param node
	 * @param root
	 * @return
	 */
	public boolean analyzeExtraRule(MethodInvocation node, CompilationUnit root) {
		// 沒有勾選careless cleanup，則不處理
		if(methodTreeMap.isEmpty()) {
			return false;
		}
		
		// 沒把extra rule勾選，則不處理
		if(methodTreeMap.get(SmellSettings.EXTRARULE_CARELESSCLEANUP_DETECTISRELEASEIOCODEINDECLAREDMETHOD) == null) {
			return false;
		}
		
//		// 檢查MethodInvocation是否在finally裡面
//		if(NodeUtils.isMethodInvocationInFinally(node))
//			return false;
		
		//  檢查傳入的參數是否有實作closeable的
		boolean isCloseable = NodeUtils.isParameterImplemented(node, Closeable.class);
		
		ASTNode mdNode = (node.resolveMethodBinding() != null) ? root.findDeclaringNode(node.resolveMethodBinding().getMethodDeclaration()): null;
		if(mdNode != null && isCloseable) {
			DeclaredMethodAnalyzer analyzer = new DeclaredMethodAnalyzer();
			mdNode.accept(analyzer);
			return analyzer.BadSmellIsDetected();
		}
		
		return false;
	}
	
	/**
	 * 取得是否偵測的設定
	 * @return
	 */
	public boolean getEnable() {
		return isEnable;
	}
}
