package ntut.csie.analyzer;

import java.io.Closeable;
import java.io.File;
import java.util.Iterator;
import java.util.TreeMap;

import ntut.csie.csdet.preference.RobustaSettings;
import ntut.csie.csdet.preference.SmellSettings;
import ntut.csie.robusta.agile.exception.RTag;
import ntut.csie.robusta.agile.exception.Robustness;
import ntut.csie.util.NodeUtils;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodInvocation;


public class UserDefinedMethodAnalyzer {
	public final static String SETTINGFILEPATH = ResourcesPlugin.getWorkspace().getRoot().getLocation().toOSString() + File.separator + SmellSettings.SETTING_FILENAME;
	protected TreeMap<String, SmellSettings.UserDefinedConstraintsType> methodTreeMap;
	protected boolean isEnable;
	
	@Robustness(value = { @RTag(level = 1, exception = java.lang.RuntimeException.class) })
	public UserDefinedMethodAnalyzer(String smellName) {
		SmellSettings smellSettings = new SmellSettings(SETTINGFILEPATH);
		methodTreeMap = smellSettings.getSmellSettings(SmellSettings.SMELL_CARELESSCLEANUP);
		isEnable = smellSettings.isDetectingSmell(SmellSettings.SMELL_CARELESSCLEANUP);
	}
	
	public static String getRobustaSettingXMLPath(IProject project) {
		return project.getLocation() + File.separator
				+ RobustaSettings.SETTING_FILENAME;
	}
	
	/**
	 * check whether library is under user define.
	 * @param node
	 * @return true，this node is under user define, the caller can add marker on it<br />
	 * 		   false，this node is not under user define.
	 */
	public boolean analyzeLibrary(MethodInvocation node) {
		if(methodTreeMap.isEmpty()) {
			return false;
		}
		
		Iterator<String> userDefinedMethodIterator = methodTreeMap.keySet().iterator();
		while(userDefinedMethodIterator.hasNext()) {
			String condition = userDefinedMethodIterator.next();
			//As we know, to avoid throw IOException from close(), the close() which override closable will cause resolveMethodBinding() return null
			if(node.resolveMethodBinding() == null)
				continue;
			
			if(node.resolveMethodBinding().getDeclaringClass() == null)
				continue;
			
			if(node.resolveMethodBinding().getDeclaringClass().getQualifiedName().equals(condition))
				return true;
		}
		return false;
	}
	
	/**
	 * tell whether method is under user define
	 * @param node
	 * @return true，this node is under user define, the caller can add marker on it<br />
	 * 		   false，this node is not under user define.
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
		
		if(methodTreeMap.get(matchedKey) == SmellSettings.UserDefinedConstraintsType.Method) {
			return true;
		} else {
			String declareClass = matchedKey.substring(0, dotIndex - 1);
			if(node.resolveMethodBinding().getDeclaringClass().getQualifiedName().equals(declareClass)) {
				return true;
			}		
		}
		return false;
	}
	
	/**
	 * Analyze extra rule of careless cleanup
	 */
	public boolean analyzeExtraRule(MethodInvocation node, CompilationUnit root) {
		// If use didn't select careless cleanup, do nothing
		if(methodTreeMap.isEmpty()) {
			return false;
		}
		
		// If use didn't select this extra rule, do nothing
		if(methodTreeMap.get(SmellSettings.EXTRARULE_CARELESSCLEANUP_ALSO_DETECT_OUT_OF_TRY_STATEMENT) == null) {
			return false;
		}

		// Check if parameters implemented closeable
		boolean isCloseable = NodeUtils.isParameterImplementedSpecifiedInterface(node, Closeable.class);
		boolean isAutoCloseable = NodeUtils.isParameterImplementedSpecifiedInterface(node, AutoCloseable.class);
		
		ASTNode mdNode = (node.resolveMethodBinding() != null) ? root.findDeclaringNode(node.resolveMethodBinding().getMethodDeclaration()): null;
		if(mdNode != null && (isCloseable || isAutoCloseable)) {
			DeclaredMethodAnalyzer analyzer = new DeclaredMethodAnalyzer();
			mdNode.accept(analyzer);
			return analyzer.BadSmellIsDetected();
		}
		
		return false;
	}
	
	/**
	 * get whether this feature is enable 
	 * @return
	 */
	public boolean getEnable() {
		return isEnable;
	}
}
