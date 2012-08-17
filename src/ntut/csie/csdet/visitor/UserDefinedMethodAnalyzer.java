package ntut.csie.csdet.visitor;

import java.io.Closeable;
import java.io.File;
import java.util.Iterator;
import java.util.TreeMap;

import ntut.csie.csdet.preference.SmellSettings;
import ntut.csie.jdt.util.NodeUtils;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodInvocation;

import agile.exception.RL;
import agile.exception.Robustness;

public class UserDefinedMethodAnalyzer {
	public final static String SETTINGFILEPATH = ResourcesPlugin.getWorkspace().getRoot().getLocation().toOSString() + File.separator + SmellSettings.SETTING_FILENAME;
	private TreeMap<String, SmellSettings.UserDefinedConstraintsType> methodTreeMap;
	boolean isEnable;
	
	@Robustness(value = { @RL(level = 1, exception = java.lang.RuntimeException.class) })
	public UserDefinedMethodAnalyzer(String smellName) {
		SmellSettings smellSettings = new SmellSettings(SETTINGFILEPATH);
		methodTreeMap = smellSettings.getSmellSettings(SmellSettings.SMELL_CARELESSCLEANUP);
		isEnable = smellSettings.isDetectingSmell(SmellSettings.SMELL_CARELESSCLEANUP);
	}
	
	/**
	 * �P�_�o��Library�O���O�ϥΪ̩w�q��Library�C
	 * @param node
	 * @return true�A�o��node�ŦX�ϥΪ̩w�q��Library�A�~���{���i�H�Ҽ{�[�Wmarker<br />
	 * 		   false�A�o��node���ŦX�ϥΪ̩w�q��Library�C
	 */
	public boolean analyzeLibrary(MethodInvocation node) {
		if(methodTreeMap.isEmpty()) {
			return false;
		}
		
		Iterator<String> userDefinedMethodIterator = methodTreeMap.keySet().iterator();
		while(userDefinedMethodIterator.hasNext()) {
			String condition = userDefinedMethodIterator.next();
			if(node.resolveMethodBinding().getDeclaringClass().getQualifiedName().equals(condition)) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * �P�_�o�Ӥ�k(method / Lib+method)�O���O�ϥΪ̩w�q��method
	 * @param node
	 * @return true�A�o��node�ŦX�ϥΪ̩w�q��method�A�~���{���i�H�Ҽ{�[�Wmarker<br />
	 * 		   false�A�o��node���ŦX�ϥΪ̩w�q��method�C
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
		
		// method name�ŦX�A�B�OSmellSettings.UserDefinedConstraintsType.Method
		if(methodTreeMap.get(matchedKey) == SmellSettings.UserDefinedConstraintsType.Method) {
			return true;
		} else {
		// method name�ŦX�A�B�OSmellSettings.UserDefinedConstraintsType.FullQulifiedMethod
			String declareClass = matchedKey.substring(0, dotIndex - 1);
			if(node.resolveMethodBinding().getDeclaringClass().getQualifiedName().equals(declareClass)) {
				return true;
			}		
		}
		return false;
	}
	
	/**
	 * �P�_careless cleanup��extra rule
	 * @param node
	 * @param root
	 * @return
	 */
	public boolean analyzeExtraRule(MethodInvocation node, CompilationUnit root) {
		// �S���Ŀ�careless cleanup�A�h���B�z
		if(methodTreeMap.isEmpty()) {
			return false;
		}
		
		// �S��extra rule�Ŀ�A�h���B�z
		if(methodTreeMap.get(SmellSettings.EXTRARULE_CARELESSCLEANUP_DETECTISRELEASEIOCODEINDECLAREDMETHOD) == null) {
			return false;
		}
		
		// �ˬdMethodInvocation�O�_�bfinally�̭�
		if(NodeUtils.isMethodInvocationInFinally(node))
			return false;
		
		//  �ˬd�ǤJ���ѼƬO�_����@closeable��
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
	 * ���o�O�_�������]�w
	 * @return
	 */
	public boolean getEnable() {
		return isEnable;
	}
}
