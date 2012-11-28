package ntut.csie.csdet.preference;

import java.io.Closeable;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import ntut.csie.robusta.agile.exception.Robustness;
import ntut.csie.robusta.agile.exception.RTag;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.XMLOutputter;

/**
 * &lt;CodeSmells&gt;<br />
 * &nbsp;&nbsp;&lt;SmellTypes name="DummyHandler" isDetecting="true"&gt;<br />
 * &nbsp;&nbsp;&nbsp;&nbsp;&lt;pattern name="" isDetecting="" /&gt;<br />
 * &nbsp;&nbsp;&nbsp;&nbsp;&lt;extraRule name="EXTRARULE_ePrintStackTrace" /&gt;<br />
 * &nbsp;&nbsp;&lt;/SmellTypes&gt;<br />
 * &nbsp;&nbsp;&lt;SmellTypes name="NestedTryBlock" isDetecting="false" /&gt;<br />
 * &nbsp;&nbsp;&lt;AnnotationTypes name="RobusnessLevel" enable="false" /&gt;<br />
 * &lt;/CodeSmells&gt; <br />
 */
public class SmellSettings {
	/*
	 * <CodeSmells>
	 * 	<SmellTypes name="DummyHandler" isDetecting="true">
	 * 		<pattern name="" isDetecting="" />
	 * 		<extraRule name="EXTRARULE_ePrintStackTrace" />
	 * 	</SmellTypes>
	 * 	<SmellTypes name="NestedTryBlock" isDetecting="false" />
	 *  <AnnotationTypes name="RobusnessLevel" enable="false" />
	 * </CodeSmells> 
	 */
	public final static String SETTING_FILENAME = "SmellSetting.xml";
	public final static String TAG_ROOT = "CodeSmells";
	public final static String TAG_SMELLTYPE4DETECTING = "SmellTypes";
	public final static String TAG_ANNOTATIONTYPE = "AnnotationTypes";
	public final static String TAG_PATTERN = "pattern";
	public final static String TAG_EXTRARULE = "extraRule";
	public final static String ATTRIBUTE_NAME = "name";
	public final static String ATTRIBUTE_ISDETECTING = "isDetecting";	
	public final static String ATTRIBUTE_ENABLE = "enable";	
	
	public final static String SMELL_IGNORECHECKEDEXCEPTION = "IgnoreCheckedException";
	public final static String SMELL_DUMMYHANDLER = "DummyHandler";
	public final static String SMELL_NESTEDTRYBLOCK = "NestedTryBlock";
	public final static String SMELL_UNPROTECTEDMAINPROGRAM = "UnprotectedMainProgram";
	public final static String SMELL_OVERLOGGING = "OverLogging";
	public final static String SMELL_CARELESSCLEANUP = "CarelessCleanup";
	public final static String SMELL_OVERWRITTENLEADEXCEPTION = "OverwrittenLeadException";
	public final static String ANNOTATION_ROBUSTNESSLEVEL = "RobustnessLevel";

	/** 例外轉型後繼續偵測 */
	public final static String EXTRARULE_OVERLOGGING_DETECTWRAPPINGEXCEPTION = "DetectWrappingExcetion";
	/** 偵測釋放資源的程式碼是否在函式中 */
	public final static String EXTRARULE_CARELESSCLEANUP_DETECTISRELEASEIOCODEINDECLAREDMETHOD = "DetectIsReleaseIOCodeInDeclaredMethod";
	
	public final static String EXTRARULE_ePrintStackTrace = "printStackTrace";
	public final static String EXTRARULE_SystemOutPrint = "System.out.print";
	public final static String EXTRARULE_SystemOutPrintln = "System.out.println";
	public final static String EXTRARULE_SystemErrPrint = "System.err.print";
	public final static String EXTRARULE_SystemErrPrintln = "System.err.println";
	public final static String EXTRARULE_OrgApacheLog4j = "org.apache.log4j";
	public final static String EXTRARULE_JavaUtilLoggingLogger = "java.util.logging.Logger";
	private Document settingDoc;
	
	public SmellSettings() {
		settingDoc = new Document(new Element(TAG_ROOT));
	}
	
	@Robustness(value = { @RTag(level = 1, exception = java.lang.RuntimeException.class) })
	public SmellSettings(File xmlFile) {
		this();
		if(!xmlFile.exists()) {
			return;
		}
		SAXBuilder builder = new SAXBuilder();
		try {
			settingDoc = builder.build(xmlFile);
		} catch (JDOMException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	@Robustness(value = { @RTag(level = 1, exception = java.lang.RuntimeException.class) })
	public SmellSettings(String xmlFilepath) {
		this(new File(xmlFilepath));
	}
	
	/**
	 * 輸入要要查詢的bad smell名稱，回傳設定檔中是否有勾選要偵測
	 * @param badSmellName 要查詢的bad smell名稱
	 * @return 
	 */
	public boolean isDetectingSmell(String badSmellName) {
		Element root = settingDoc.getRootElement();
		List<?> elements = root.getChildren(TAG_SMELLTYPE4DETECTING);
	
		for (Object s : elements) {
			Element smellTypeElement = (Element)s;
			if(smellTypeElement.getAttributeValue(ATTRIBUTE_NAME).equals(badSmellName)) {
				return Boolean.parseBoolean(smellTypeElement.getAttributeValue(ATTRIBUTE_ISDETECTING));
			}
		}
		return false;
	}
	
	/**
	 * 是否要使用強健度等級註記
	 * @return
	 */
	public boolean isAddingRobustnessAnnotation() {
		Element annotationType = getAnnotationType(TAG_ANNOTATIONTYPE);
		return Boolean.parseBoolean(annotationType.getAttributeValue(ATTRIBUTE_ENABLE));
	}

	/**
	 * 如果想取得的bad smell name不存在，
	 * 就會自動產生以這個bad smell name為屬性name的節點，
	 * 並且設定isDetecting屬性為true。
	 * @param badSmellName
	 * @return
	 */
	public Element getSmellType(String badSmellName) {
		Element root = settingDoc.getRootElement();
		List<?> elements = root.getChildren(TAG_SMELLTYPE4DETECTING);
		Element tagSmellTypeElement = null;
		
		for (Object s : elements) {
			Element smellTypeElement = (Element)s;
			if(smellTypeElement.getAttribute(ATTRIBUTE_NAME).getValue().equals(badSmellName)) {
				tagSmellTypeElement = smellTypeElement;
				return tagSmellTypeElement;
			}
		}
					
		if(tagSmellTypeElement==null) {
			tagSmellTypeElement = new Element(TAG_SMELLTYPE4DETECTING);
			tagSmellTypeElement.setAttribute(ATTRIBUTE_NAME, badSmellName);
			tagSmellTypeElement.setAttribute(ATTRIBUTE_ISDETECTING, String.valueOf(true));
			root.addContent(tagSmellTypeElement);
		}
		return tagSmellTypeElement;
	}
	
	/**
	 * 取得強健度等級的節點。
	 * 如果節點不存在，就會自動產生一個enable為false的強健度等級節點。
	 * @param annotationName
	 * @return
	 */
	public Element getAnnotationType(String annotationName) {
		Element root = settingDoc.getRootElement();
		List<?> elements = root.getChildren(TAG_ANNOTATIONTYPE);
		Element tagAnnotationTypeElement = null;
		for (Object s : elements) {
			Element annotationTypeElement = (Element)s;
			if(annotationTypeElement.getName().equals(annotationName)) {
				tagAnnotationTypeElement = annotationTypeElement;
				return tagAnnotationTypeElement;
			}
		}
		
		if(tagAnnotationTypeElement == null) {
			tagAnnotationTypeElement = new Element(TAG_ANNOTATIONTYPE);
			tagAnnotationTypeElement.setAttribute(ATTRIBUTE_NAME, annotationName);
			tagAnnotationTypeElement.setAttribute(ATTRIBUTE_ENABLE, String.valueOf(false));
			root.addContent(tagAnnotationTypeElement);
		}
		return tagAnnotationTypeElement;
	}
		
	public void setSmellTypeAttribute(String badSmellName, String attributeName, Boolean attributeValue) {
		Element badSmellElement = getSmellType(badSmellName);
		badSmellElement.setAttribute(attributeName, String.valueOf(attributeValue));
	}
	
	public void addDummyHandlerPattern(String patternName, boolean isDetecting) {
		addPattern(SMELL_DUMMYHANDLER, patternName, isDetecting);
	}
	
	public void addOverLoggingPattern(String patternName, boolean isDetecting) {
		addPattern(SMELL_OVERLOGGING, patternName, isDetecting);
	}
	
	public void addCarelessCleanupPattern(String patternName, boolean isDetecting) {
		addPattern(SMELL_CARELESSCLEANUP, patternName, isDetecting);
	}
	
	/**
	 * 找出指定的bad smell中所有啟用的patterns
	 * 沒有啟用的pattern就不會被加入list中
	 * @param smellName
	 * @return
	 */
	public List<String> getAllDetectingPatterns(String smellName) {
		Element root = settingDoc.getRootElement();
		List<?> childrenElements = root.getChildren(TAG_SMELLTYPE4DETECTING);
		Element badSmellElement = null;
		for (Object object : childrenElements) {
			Element e = (Element) object;
			if(e.getAttributeValue(ATTRIBUTE_NAME).equals(smellName)) {
				badSmellElement = e;
				break;
			}
		}

		if(badSmellElement == null) {
			return new ArrayList<String>();
		}
		
		List<String> patternList = new ArrayList<String>();
		List<?> patternElements = badSmellElement.getChildren(TAG_PATTERN);
		for (Object object : patternElements) {
			Element e = (Element) object;
			if(e.getAttributeValue(ATTRIBUTE_ISDETECTING).equals(String.valueOf(true))) {
				patternList.add(e.getAttributeValue(ATTRIBUTE_NAME));
			}
		}
		return patternList;
	}
	
	/**
	 * 讓使用者選擇要尋找的Pattern是Class、Method、或是Class+Method
	 * @param smellName
	 * @param type
	 * @return
	 */
	public List<String> getDetectingPatterns(String smellName, UserDefinedConstraintsType type) {
		List<String> adoptedPatterns = new ArrayList<String>();
		
		for(String pattern : getAllDetectingPatterns(smellName)) {
			if(pattern.indexOf(".*") != -1) {
				if (type == UserDefinedConstraintsType.Library) {
					adoptedPatterns.add(pattern);
				}
			} else if (pattern.indexOf("*.") != -1) {
				if (type == UserDefinedConstraintsType.Method) {
					adoptedPatterns.add(pattern);
				}
			} else if (pattern.indexOf(".") != -1) {
				if (type == UserDefinedConstraintsType.FullQulifiedMethod) {
					adoptedPatterns.add(pattern);
				}
			} else {
				if(type == UserDefinedConstraintsType.Method) {
					adoptedPatterns.add(pattern);
				}
			}
		}
		return adoptedPatterns;
	}
	
	/**
	 * 取回特定smell所有Pattern的設定值
	 * @param smellName
	 * @return
	 */
	public TreeMap<String, Boolean> getSemllPatterns(String smellName) {
		TreeMap<String, Boolean> result = new TreeMap<String, Boolean>();
		Element root = settingDoc.getRootElement();
		List<?> childrenElements = root.getChildren(TAG_SMELLTYPE4DETECTING);
		Element badSmellElement = null;
		for (Object object : childrenElements) {
			Element e = (Element) object;
			if(e.getAttributeValue(ATTRIBUTE_NAME).equals(smellName)) {
				badSmellElement = e;
				break;
			}
		}
		
		if(badSmellElement == null) {
			return result;
		}
		
		List<?> patternElements = badSmellElement.getChildren(TAG_PATTERN);
		for (Object object : patternElements) {
			Element e = (Element) object;
			result.put(e.getAttributeValue(ATTRIBUTE_NAME), Boolean.parseBoolean(e.getAttributeValue(ATTRIBUTE_ISDETECTING)));
		}
		
		return result;
	}
	
	/**
	 * pattern是使用者自行輸入的程式碼，這些程式碼會被記錄起來，
	 * 然後用一個isDetecting來決定檢查bad smell的時候要不要一起做檢查。
	 * 只有在使用者刪除自行輸入的程式碼時，pattern node才會被刪除。
	 * @param badSmellName
	 * @param patternContent
	 * @param isDetecting
	 */
	private void addPattern(String badSmellName, String patternContent, boolean isDetecting) {	
		Element badSmellElement = getSmellType(badSmellName);
		
		// 除了這裡做檢查動作，防止使用者加入重複的pattern，前端也要記得檢查
		List<?> patternElements = badSmellElement.getChildren(TAG_PATTERN);
		for (Object object : patternElements) {
			Element pattern = (Element) object;
			if(pattern.getAttribute(ATTRIBUTE_NAME).getValue().equals(patternContent)) {
				pattern.setAttribute(ATTRIBUTE_ISDETECTING, String.valueOf(isDetecting));		
				return;
			}
		}
		// 確定pattern不存在，就加入新的node
		Element pattern = new Element(TAG_PATTERN);
		pattern.setAttribute(ATTRIBUTE_NAME, patternContent);
		pattern.setAttribute(ATTRIBUTE_ISDETECTING, String.valueOf(isDetecting));

		badSmellElement.addContent(pattern);
	}
	
	public boolean removePatterns(String smellName) {
		Element badSmellElement = getSmellType(smellName);
		return badSmellElement.removeChildren(TAG_PATTERN);
	}
	
	/**
	 * extraRule是我們提供給使用者勾選的選項，所以當使用者有勾選時，
	 * 這個extraRule的node才會出現，使用者如果取消勾選，就會刪除這個node。
	 * @param badSmellName
	 * @param ruleName
	 */
	public void addExtraRule(String badSmellName, String ruleName) {	
		Element badSmellElement = getSmellType(badSmellName);
		
		List<?> patternElements = badSmellElement.getChildren(TAG_EXTRARULE);
		for (Object object : patternElements) {
			Element pattern = (Element) object;
			if(pattern.getAttribute(ATTRIBUTE_NAME).getValue().equals(ruleName)) {
				return;
			}
		}

		Element extraRule = new Element(TAG_EXTRARULE);
		extraRule.setAttribute(ATTRIBUTE_NAME, ruleName);	
		badSmellElement.addContent(extraRule);
	}
	
	public boolean isExtraRuleExist(String badSmellName, String ruleName) {
		Element badSmellElement = getSmellType(badSmellName);
		
		// 如果節點在，則找出是否有這個extra rule
		List<?> extraRules = badSmellElement.getChildren();
		for(Object object : extraRules) {
			Element extraRule = (Element) object;
			if(extraRule.getAttribute(ATTRIBUTE_NAME).getValue().equals(ruleName)) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * 
	 * @param badSmellName
	 * @param ruleName
	 * @return true代表Rule存在，並且移除成功。<br />
	 * 		   false可能是移除失敗，也可能是Rule從來不存在。
	 */
	public boolean removeExtraRule(String badSmellName, String ruleName) {
		Element badSmellElement = getSmellType(badSmellName);
		List<?> extraRules = badSmellElement.getChildren(TAG_EXTRARULE);
		for(Object object : extraRules) {
			Element extraRule = (Element) object;
			if(extraRule.getAttribute(ATTRIBUTE_NAME).getValue().equals(ruleName)) {
				return badSmellElement.removeContent(extraRule);
			}
		}
		return false;
	}
	
	public TreeMap<String, UserDefinedConstraintsType> getSmellSettings(String badSmellName) {
		TreeMap<String, UserDefinedConstraintsType> libMap = new TreeMap<String, UserDefinedConstraintsType>();
		Element badSmellElement = getSmellType(badSmellName);
		// 為選取偵測此bad smell，則不做任何讀取動作
		if(!Boolean.parseBoolean(badSmellElement.getAttributeValue(ATTRIBUTE_ISDETECTING))) {
			return libMap;
		}
		// add extra rules to libMap
		List<?> extraRules = badSmellElement.getChildren(TAG_EXTRARULE);
		for(Object object : extraRules) {
			Element extraRule = (Element) object;
			String rule = extraRule.getAttribute(ATTRIBUTE_NAME).getValue();
			if(	rule.equals(EXTRARULE_SystemOutPrint) || rule.equals(EXTRARULE_SystemOutPrintln) ||
				rule.equals(EXTRARULE_SystemErrPrint) || rule.equals(EXTRARULE_SystemErrPrintln)) {
				libMap.put("java.io.PrintStream" + rule.substring(rule.lastIndexOf(".")), UserDefinedConstraintsType.FullQulifiedMethod);
				continue;
			}
			if(rule.equals(EXTRARULE_OrgApacheLog4j) || rule.equals(EXTRARULE_JavaUtilLoggingLogger)) {
				libMap.put(rule, UserDefinedConstraintsType.Library);
				continue;
			}
			if(rule.equals(EXTRARULE_ePrintStackTrace)) {
				libMap.put(rule, UserDefinedConstraintsType.Method);
				continue;
			}
			if(rule.equals(EXTRARULE_CARELESSCLEANUP_DETECTISRELEASEIOCODEINDECLAREDMETHOD)) {
				libMap.put(rule, UserDefinedConstraintsType.FullQulifiedMethod);
				continue;
			}
			if(rule.equals(EXTRARULE_OVERLOGGING_DETECTWRAPPINGEXCEPTION)) {
				libMap.put(rule, UserDefinedConstraintsType.FullQulifiedMethod);
			}
		}
		// add patterns to libMap
		List<String> patterns = getAllDetectingPatterns(badSmellName);
		for(String pattern : patterns) {
			if(pattern.indexOf(".*") != -1) {
				int pos = pattern.indexOf(".*");
				libMap.put(pattern.substring(0, pos), UserDefinedConstraintsType.Library);
			} else if(pattern.indexOf("*.") != -1) {
				libMap.put(pattern.substring(2), UserDefinedConstraintsType.Method);
			} else if(pattern.lastIndexOf(".") != -1) {
				libMap.put(pattern, UserDefinedConstraintsType.FullQulifiedMethod);
			} else {
				libMap.put(pattern, UserDefinedConstraintsType.Method);
			}
		}
		
		return libMap;
	}

	@Robustness(value = { @RTag(level = 1, exception = java.lang.RuntimeException.class) })
	public void writeXMLFile(String path) {
		FileWriter fw = null;
		XMLOutputter out = new XMLOutputter();
		try {
			fw = new FileWriter(path);
			out.output(settingDoc, fw);
		} catch (IOException e) {
			throw new RuntimeException(e);
		} finally {
			close(fw);
		}
	}

	private void close(Closeable ioInstance) {
		if(ioInstance != null) {
			try {
				ioInstance.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public static enum UserDefinedConstraintsType {
		Library,
		Method,
		FullQulifiedMethod
	}
	
	/**
	 * 將所有的條件都勾選，並寫到設定檔中
	 */
	public void activateAllConditions(String path) {
		File settingFile = new File(path);
		if(settingFile.exists())
			return;
		
		setSmellTypeAttribute(SMELL_IGNORECHECKEDEXCEPTION, ATTRIBUTE_ISDETECTING, true);

		setSmellTypeAttribute(SMELL_DUMMYHANDLER, ATTRIBUTE_ISDETECTING, true);
		addExtraRule(SMELL_DUMMYHANDLER, EXTRARULE_ePrintStackTrace);
		addExtraRule(SMELL_DUMMYHANDLER, EXTRARULE_SystemErrPrint);
		addExtraRule(SMELL_DUMMYHANDLER, EXTRARULE_SystemErrPrintln);
		addExtraRule(SMELL_DUMMYHANDLER, EXTRARULE_SystemOutPrint);
		addExtraRule(SMELL_DUMMYHANDLER, EXTRARULE_SystemOutPrintln);
		addExtraRule(SMELL_DUMMYHANDLER, EXTRARULE_JavaUtilLoggingLogger);
		addExtraRule(SMELL_DUMMYHANDLER, EXTRARULE_OrgApacheLog4j);
		
		setSmellTypeAttribute(SMELL_NESTEDTRYBLOCK, ATTRIBUTE_ISDETECTING, true);
		
		setSmellTypeAttribute(SMELL_UNPROTECTEDMAINPROGRAM, ATTRIBUTE_ISDETECTING, true);

		setSmellTypeAttribute(SMELL_CARELESSCLEANUP, ATTRIBUTE_ISDETECTING, true);
		addExtraRule(SMELL_CARELESSCLEANUP, EXTRARULE_CARELESSCLEANUP_DETECTISRELEASEIOCODEINDECLAREDMETHOD);

		setSmellTypeAttribute(SMELL_OVERLOGGING, ATTRIBUTE_ISDETECTING, true);
		addExtraRule(SMELL_OVERLOGGING, EXTRARULE_OVERLOGGING_DETECTWRAPPINGEXCEPTION);
		addExtraRule(SMELL_OVERLOGGING, EXTRARULE_JavaUtilLoggingLogger);
		addExtraRule(SMELL_OVERLOGGING, EXTRARULE_OrgApacheLog4j);
		
		writeXMLFile(path);
	}
}