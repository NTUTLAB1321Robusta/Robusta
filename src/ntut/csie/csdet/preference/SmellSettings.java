package ntut.csie.csdet.preference;

import java.io.Closeable;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import ntut.csie.rleht.builder.RLMarkerAttribute;
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
 * &nbsp;&nbsp;&lt;SmellTypes name="NestedTryStatement" isDetecting="false"
 * /&gt;<br />
 * &nbsp;&nbsp;&lt;AnnotationTypes name="RobusnessLevel" enable="false" /&gt;<br />
 * &lt;/CodeSmells&gt; <br />
 */
public class SmellSettings {
	/*Sample
	 * <CodeSmells> <SmellTypes name="DummyHandler" isDetecting="true"> <pattern
	 * name="" isDetecting="" /> <extraRule name="EXTRARULE_ePrintStackTrace" />
	 * </SmellTypes> <SmellTypes name="NestedTryStatement" isDetecting="false"
	 * /> <Preferences name="ShowRLAnnotationWarning" enable="false" /> </CodeSmells>
	 */
	public final static String SETTING_FILENAME = "SmellSetting.xml";
	public final static String TAG_ROOT = "CodeSmells";
	public final static String TAG_SMELLTYPE4DETECTING = "SmellTypes";
	public final static String TAG_PATTERN = "pattern";
	public final static String TAG_EXTRARULE = "extraRule";
	public final static String ATTRIBUTE_NAME = "name";
	public final static String ATTRIBUTE_ISDETECTING = "isDetecting";
	public final static String ATTRIBUTE_ENABLE = "enable";

	public final static String TAG_PREFERENCE = "Preferences";
	public final static String PRE_SHOWRLANNOTATIONWARNING = "ShowRLAnnotationWarning";

	public final static String SMELL_EMPTYCATCHBLOCK = "EmptyCatchBlock";
	public final static String SMELL_DUMMYHANDLER = "DummyHandler";
	public final static String SMELL_NESTEDTRYSTATEMENT = "NestedTryStatement";
	public final static String SMELL_UNPROTECTEDMAINPROGRAM = "UnprotectedMainProgram";
	public final static String SMELL_CARELESSCLEANUP = "CarelessCleanup";
	public final static String SMELL_EXCEPTIONTHROWNFROMFINALLYBLOCK = "ExceptionThrownFromFinallyBlock";
	public final static String[] ALL_BAD_SMELLS = new String[] {
			SMELL_EMPTYCATCHBLOCK, SMELL_DUMMYHANDLER,
			SMELL_NESTEDTRYSTATEMENT, SMELL_UNPROTECTEDMAINPROGRAM, SMELL_CARELESSCLEANUP,
			SMELL_EXCEPTIONTHROWNFROMFINALLYBLOCK };
	
	
	/** detect out of try statement close method*/
	public final static String EXTRARULE_CARELESSCLEANUP_ALSO_DETECT_OUT_OF_TRY_STATEMENT = "DetectOutOfTryStatement";

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
		if (!xmlFile.exists()) {
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
	 * input bad smell name option and this function will return the selected bad smell to detect or not  
	 * @param badSmellName
	 *            bad smell name wants to be consulted
	 * @return
	 */
	public boolean isDetectingSmell(String badSmellName) {
		Element root = settingDoc.getRootElement();
		List<?> elements = root.getChildren(TAG_SMELLTYPE4DETECTING);

		for (Object s : elements) {
			Element smellTypeElement = (Element) s;
			if (smellTypeElement.getAttributeValue(ATTRIBUTE_NAME).equals(
					badSmellName)) {
				return Boolean.parseBoolean(smellTypeElement
						.getAttributeValue(ATTRIBUTE_ISDETECTING));
			}
		}
		// If the setting was not set in the configuration file, set it to TRUE
		// as default
		return true;
	}

	/**
	 * whether robustness annotation is enabled or not
	 * 
	 * 
	 * @return
	 */
	public boolean isAddingRobustnessAnnotation() {
		return getPreferenceAttribute(SmellSettings.PRE_SHOWRLANNOTATIONWARNING);
	}

	/**
	 * if input bad smell name doesn't exist, this function will create a new element named as this bad smell name and sets its isDetecting attribute as true.
	 * @param badSmellName
	 * @return
	 */
	public Element getSmellType(String badSmellName) {
		Element root = settingDoc.getRootElement();
		List<?> elements = root.getChildren(TAG_SMELLTYPE4DETECTING);
		Element tagSmellTypeElement = null;

		for (Object s : elements) {
			Element smellTypeElement = (Element) s;
			if (smellTypeElement.getAttribute(ATTRIBUTE_NAME).getValue()
					.equals(badSmellName)) {
				tagSmellTypeElement = smellTypeElement;
				return tagSmellTypeElement;
			}
		}

		if (tagSmellTypeElement == null) {
			tagSmellTypeElement = new Element(TAG_SMELLTYPE4DETECTING);
			tagSmellTypeElement.setAttribute(ATTRIBUTE_NAME, badSmellName);
			tagSmellTypeElement.setAttribute(ATTRIBUTE_ISDETECTING,
					String.valueOf(true));
			root.addContent(tagSmellTypeElement);
		}
		return tagSmellTypeElement;
	}

	public Element getPreference(String preferenceName) {
		Element root = settingDoc.getRootElement();
		List<?> elements = root.getChildren(TAG_PREFERENCE);
		Element tagPreferenceElement = null;
		for (Object s : elements) {
			Element preferenceElement = (Element) s;
			if (preferenceElement.getAttribute(ATTRIBUTE_NAME).getValue()
					.equals(preferenceName)) {
				tagPreferenceElement = preferenceElement;
				return tagPreferenceElement;
			}
		}

		if (tagPreferenceElement == null) {
			tagPreferenceElement = new Element(TAG_PREFERENCE);
			tagPreferenceElement.setAttribute(ATTRIBUTE_NAME, preferenceName);
			tagPreferenceElement.setAttribute(ATTRIBUTE_ENABLE,
					String.valueOf(true));
			root.addContent(tagPreferenceElement);
		}
		return tagPreferenceElement;
	}

	public void setSmellTypeAttribute(String badSmellName,
			String attributeName, Boolean attributeValue) {
		Element badSmellElement = getSmellType(badSmellName);
		badSmellElement.setAttribute(attributeName,
				String.valueOf(attributeValue));
	}

	public void setPreferenceAttribute(String preferenceName,
			String attributeName, Boolean attributeValue) {
		Element preElement = getPreference(preferenceName);
		preElement.setAttribute(attributeName, String.valueOf(attributeValue));
	}

	public boolean getPreferenceAttribute(String attributeName) {
		Element preElement = getPreference(attributeName);
		return Boolean.parseBoolean(preElement
				.getAttributeValue(ATTRIBUTE_ENABLE));
	}

	public void addDummyHandlerPattern(String patternName, boolean isDetecting) {
		addPattern(SMELL_DUMMYHANDLER, patternName, isDetecting);
	}

	public void addCarelessCleanupPattern(String patternName,
			boolean isDetecting) {
		addPattern(SMELL_CARELESSCLEANUP, patternName, isDetecting);
	}

	/**
	 * add specified and enabled bad smell patterns to list 
	 * 
	 * @param smellName
	 * @return
	 */
	public List<String> getAllDetectingPatterns(String smellName) {
		Element root = settingDoc.getRootElement();
		List<?> childrenElements = root.getChildren(TAG_SMELLTYPE4DETECTING);
		Element badSmellElement = null;
		for (Object object : childrenElements) {
			Element e = (Element) object;
			if (e.getAttributeValue(ATTRIBUTE_NAME).equals(smellName)) {
				badSmellElement = e;
				break;
			}
		}

		if (badSmellElement == null) {
			return new ArrayList<String>();
		}

		List<String> patternList = new ArrayList<String>();
		List<?> patternElements = badSmellElement.getChildren(TAG_PATTERN);
		for (Object object : patternElements) {
			Element e = (Element) object;
			if (e.getAttributeValue(ATTRIBUTE_ISDETECTING).equals(
					String.valueOf(true))) {
				patternList.add(e.getAttributeValue(ATTRIBUTE_NAME));
			}
		}
		return patternList;
	}

	/**
	 * this method is only used for test case.
	 * and it simulates user's configuration to detect different bad smell pattern.
	 * 
	 * 
	 * @param smellName
	 * @param type
	 * @return
	 */
	public List<String> getDetectingPatterns(String smellName,
			UserDefinedConstraintsType type) {
		List<String> adoptedPatterns = new ArrayList<String>();

		for (String pattern : getAllDetectingPatterns(smellName)) {
			if (pattern.indexOf(".*") != -1) {
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
				if (type == UserDefinedConstraintsType.Method) {
					adoptedPatterns.add(pattern);
				}
			}
		}
		return adoptedPatterns;
	}

	/**
	 * return specified bad smell's pattern configuration 
	 * 
	 * @param smellName
	 * @return
	 */
	public TreeMap<String, Boolean> getSmellPatterns(String smellName) {
		TreeMap<String, Boolean> result = new TreeMap<String, Boolean>();
		Element root = settingDoc.getRootElement();
		List<?> childrenElements = root.getChildren(TAG_SMELLTYPE4DETECTING);
		Element badSmellElement = null;
		for (Object object : childrenElements) {
			Element e = (Element) object;
			if (e.getAttributeValue(ATTRIBUTE_NAME).equals(smellName)) {
				badSmellElement = e;
				break;
			}
		}

		if (badSmellElement == null) {
			return result;
		}

		List<?> patternElements = badSmellElement.getChildren(TAG_PATTERN);
		for (Object object : patternElements) {
			Element e = (Element) object;
			result.put(e.getAttributeValue(ATTRIBUTE_NAME), Boolean
					.parseBoolean(e.getAttributeValue(ATTRIBUTE_ISDETECTING)));
		}

		return result;
	}

	/**
	 * patternContent is a piece of special code inputed by user manually, these patternContents
	 * will be stored and used as patterns to detect bad smells. all patternContents will be deleted 
	 * only when user deletes them manually.
	 *  
	 * @param badSmellName
	 * @param patternContent
	 * @param isDetecting
	 */
	private void addPattern(String badSmellName, String patternContent,
			boolean isDetecting) {
		Element badSmellElement = getSmellType(badSmellName);

		//check whether patternContent is duplicate, 
		List<?> patternElements = badSmellElement.getChildren(TAG_PATTERN);
		for (Object object : patternElements) {
			Element pattern = (Element) object;
			if (pattern.getAttribute(ATTRIBUTE_NAME).getValue()
					.equals(patternContent)) {
				pattern.setAttribute(ATTRIBUTE_ISDETECTING,
						String.valueOf(isDetecting));
				return;
			}
		}
		// if patternContent doesn't exist, add a new one.
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
	 * extraRule are options for user. only when user checks one of them,
	 * extraRule will be used to detect bad smell. 
	 * 
	 * @param badSmellName
	 * @param ruleName
	 */
	public void addExtraRule(String badSmellName, String ruleName) {
		Element badSmellElement = getSmellType(badSmellName);

		List<?> patternElements = badSmellElement.getChildren(TAG_EXTRARULE);
		for (Object object : patternElements) {
			Element pattern = (Element) object;
			if (pattern.getAttribute(ATTRIBUTE_NAME).getValue()
					.equals(ruleName)) {
				return;
			}
		}

		Element extraRule = new Element(TAG_EXTRARULE);
		extraRule.setAttribute(ATTRIBUTE_NAME, ruleName);
		badSmellElement.addContent(extraRule);
	}

	public boolean isExtraRuleExist(String badSmellName, String ruleName) {
		Element badSmellElement = getSmellType(badSmellName);

		List<?> extraRules = badSmellElement.getChildren();
		for (Object object : extraRules) {
			Element extraRule = (Element) object;
			if (extraRule.getAttribute(ATTRIBUTE_NAME).getValue()
					.equals(ruleName)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 
	 * @param badSmellName
	 * @param ruleName
	 * @return true means remove successfully。<br />
	 *         false means remove fail or extra rule does not exist。
	 */
	public boolean removeExtraRule(String badSmellName, String ruleName) {
		Element badSmellElement = getSmellType(badSmellName);
		List<?> extraRules = badSmellElement.getChildren(TAG_EXTRARULE);
		for (Object object : extraRules) {
			Element extraRule = (Element) object;
			if (extraRule.getAttribute(ATTRIBUTE_NAME).getValue()
					.equals(ruleName)) {
				return badSmellElement.removeContent(extraRule);
			}
		}
		return false;
	}

	public TreeMap<String, UserDefinedConstraintsType> getSmellSettings(
			String badSmellName) {
		TreeMap<String, UserDefinedConstraintsType> libMap = new TreeMap<String, UserDefinedConstraintsType>();
		Element badSmellElement = getSmellType(badSmellName);
		// doesn't detect anything without checking anything any bad smell type
		if (!Boolean.parseBoolean(badSmellElement
				.getAttributeValue(ATTRIBUTE_ISDETECTING))) {
			return libMap;
		}
		// add extra rules to libMap
		List<?> extraRules = badSmellElement.getChildren(TAG_EXTRARULE);
		for (Object object : extraRules) {
			Element extraRule = (Element) object;
			String rule = extraRule.getAttribute(ATTRIBUTE_NAME).getValue();
			if (rule.equals(EXTRARULE_SystemOutPrint)
					|| rule.equals(EXTRARULE_SystemOutPrintln)
					|| rule.equals(EXTRARULE_SystemErrPrint)
					|| rule.equals(EXTRARULE_SystemErrPrintln)) {
				libMap.put(
						"java.io.PrintStream" + rule.substring(rule.lastIndexOf(".")),
						UserDefinedConstraintsType.FullQulifiedMethod);
				continue;
			}
			if (rule.equals(EXTRARULE_OrgApacheLog4j)
					|| rule.equals(EXTRARULE_JavaUtilLoggingLogger)) {
				libMap.put(rule, UserDefinedConstraintsType.Library);
				continue;
			}
			if (rule.equals(EXTRARULE_ePrintStackTrace)) {
				libMap.put(rule, UserDefinedConstraintsType.Method);
				continue;
			}
			if (rule.equals(EXTRARULE_CARELESSCLEANUP_ALSO_DETECT_OUT_OF_TRY_STATEMENT)) {
				libMap.put(rule, UserDefinedConstraintsType.FullQulifiedMethod);
				continue;
			}
		}
		// add patterns to libMap
		List<String> patterns = getAllDetectingPatterns(badSmellName);
		for (String pattern : patterns) {
			if (pattern.indexOf(".*") != -1) {
				int pos = pattern.indexOf(".*");
				libMap.put(pattern.substring(0, pos),
						UserDefinedConstraintsType.Library);
			} else if (pattern.indexOf("*.") != -1) {
				libMap.put(pattern.substring(2),
						UserDefinedConstraintsType.Method);
			} else if (pattern.lastIndexOf(".") != -1) {
				libMap.put(pattern,
						UserDefinedConstraintsType.FullQulifiedMethod);
			} else {
				libMap.put(pattern, UserDefinedConstraintsType.Method);
			}
		}

		return libMap;
	}

	@Robustness(value = { @RTag(level = 1, exception = java.lang.RuntimeException.class) })
	public void writeXMLFile(String path) {
		XMLOutputter out = new XMLOutputter();
		FileWriter fw = null;
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
		if (ioInstance != null) {
			try {
				ioInstance.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public static enum UserDefinedConstraintsType {
		Library, Method, FullQulifiedMethod
	}

	/**
	 * check all conditions in configuration.
	 */
	public void activateAllConditionsIfNotConfugured(String path) {
		File settingFile = new File(path);
		if (settingFile.exists())
			return;

		setSmellTypeAttribute(SMELL_EMPTYCATCHBLOCK, ATTRIBUTE_ISDETECTING,
				true);

		setSmellTypeAttribute(SMELL_DUMMYHANDLER, ATTRIBUTE_ISDETECTING, true);
		addExtraRule(SMELL_DUMMYHANDLER, EXTRARULE_ePrintStackTrace);
		addExtraRule(SMELL_DUMMYHANDLER, EXTRARULE_SystemErrPrint);
		addExtraRule(SMELL_DUMMYHANDLER, EXTRARULE_SystemErrPrintln);
		addExtraRule(SMELL_DUMMYHANDLER, EXTRARULE_SystemOutPrint);
		addExtraRule(SMELL_DUMMYHANDLER, EXTRARULE_SystemOutPrintln);
		addExtraRule(SMELL_DUMMYHANDLER, EXTRARULE_JavaUtilLoggingLogger);
		addExtraRule(SMELL_DUMMYHANDLER, EXTRARULE_OrgApacheLog4j);

		setSmellTypeAttribute(SMELL_NESTEDTRYSTATEMENT, ATTRIBUTE_ISDETECTING,
				true);

		setSmellTypeAttribute(SMELL_UNPROTECTEDMAINPROGRAM,
				ATTRIBUTE_ISDETECTING, true);

		setSmellTypeAttribute(SMELL_CARELESSCLEANUP, ATTRIBUTE_ISDETECTING,
				true);
		addExtraRule(SMELL_CARELESSCLEANUP,
				EXTRARULE_CARELESSCLEANUP_ALSO_DETECT_OUT_OF_TRY_STATEMENT);

		setSmellTypeAttribute(SMELL_EXCEPTIONTHROWNFROMFINALLYBLOCK,
				ATTRIBUTE_ISDETECTING, true);

		writeXMLFile(path);
	}
	
	public static String toRLMarkerAttributeConstant(String smellType) {
		
		if(smellType.equals(SMELL_EMPTYCATCHBLOCK))
				return RLMarkerAttribute.CS_EMPTY_CATCH_BLOCK;
		else if(smellType.equals(SMELL_DUMMYHANDLER))
				return RLMarkerAttribute.CS_DUMMY_HANDLER;
		else if(smellType.equals(SMELL_NESTEDTRYSTATEMENT))
				return RLMarkerAttribute.CS_NESTED_TRY_STATEMENT;
		else if(smellType.equals(SMELL_UNPROTECTEDMAINPROGRAM))
				return RLMarkerAttribute.CS_UNPROTECTED_MAIN;
		else if(smellType.equals(SMELL_CARELESSCLEANUP))
				return RLMarkerAttribute.CS_CARELESS_CLEANUP;
		else if(smellType.equals(SMELL_EXCEPTIONTHROWNFROMFINALLYBLOCK))
				return RLMarkerAttribute.CS_EXCEPTION_THROWN_FROM_FINALLY_BLOCK;
		else
            throw new IllegalArgumentException("Invalid smell type: " + smellType);
	}
}