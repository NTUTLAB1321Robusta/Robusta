package ntut.csie.csdet.visitor;

import java.io.Closeable;
import java.io.File;
import java.util.Iterator;
import java.util.TreeMap;

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
	
	/**
	 * 耞硂Library琌ぃ琌ㄏノ﹚竡Library
	 * @param node
	 * @return true硂node才ㄏノ﹚竡Library场祘Ασ納marker<br />
	 * 		   false硂nodeぃ才ㄏノ﹚竡Library
	 */
	public boolean analyzeLibrary(MethodInvocation node) {
		if(methodTreeMap.isEmpty()) {
			return false;
		}
		
		Iterator<String> userDefinedMethodIterator = methodTreeMap.keySet().iterator();
		while(userDefinedMethodIterator.hasNext()) {
			String condition = userDefinedMethodIterator.next();
			// ヘ玡笵薄猵Override Closeablecloseㄏㄤぃ穦┻IOException穦硑ΘresolveMethodBindingnull
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
	 * 耞硂よ猭(method / Lib+method)琌ぃ琌ㄏノ﹚竡method
	 * @param node
	 * @return true硂node才ㄏノ﹚竡method场祘Ασ納marker<br />
	 * 		   false硂nodeぃ才ㄏノ﹚竡method
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
		
		// method name才琌SmellSettings.UserDefinedConstraintsType.Method
		if(methodTreeMap.get(matchedKey) == SmellSettings.UserDefinedConstraintsType.Method) {
			return true;
		} else {
		// method name才琌SmellSettings.UserDefinedConstraintsType.FullQulifiedMethod
			String declareClass = matchedKey.substring(0, dotIndex - 1);
			if(node.resolveMethodBinding().getDeclaringClass().getQualifiedName().equals(declareClass)) {
				return true;
			}		
		}
		return false;
	}
	
	/**
	 * 耞careless cleanupextra rule
	 * @param node
	 * @param root
	 * @return
	 */
	public boolean analyzeExtraRule(MethodInvocation node, CompilationUnit root) {
		// ⊿Τつ匡careless cleanup玥ぃ矪瞶
		if(methodTreeMap.isEmpty()) {
			return false;
		}
		
		// ⊿рextra ruleつ匡玥ぃ矪瞶
		if(methodTreeMap.get(SmellSettings.EXTRARULE_CARELESSCLEANUP_DETECTISRELEASEIOCODEINDECLAREDMETHOD) == null) {
			return false;
		}
		
//		// 浪琩MethodInvocation琌finally柑
//		if(NodeUtils.isMethodInvocationInFinally(node))
//			return false;
		
		//  浪琩肚把计琌Τ龟closeable
		boolean isCloseable = NodeUtils.isParameterImplemented(node, Closeable.class);
		
		ASTNode mdNode = root.findDeclaringNode(node.resolveMethodBinding().getMethodDeclaration());
		if(mdNode != null && isCloseable) {
			DeclaredMethodAnalyzer analyzer = new DeclaredMethodAnalyzer();
			mdNode.accept(analyzer);
			return analyzer.BadSmellIsDetected();
		}
		
		return false;
	}
	
	/**
	 * 眔琌盎代砞﹚
	 * @return
	 */
	public boolean getEnable() {
		return isEnable;
	}
}
