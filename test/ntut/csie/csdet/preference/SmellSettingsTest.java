package ntut.csie.csdet.preference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.util.List;
import java.util.TreeMap;

import ntut.csie.csdet.preference.SmellSettings.UserDefinedConstraintsType;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class SmellSettingsTest {
	private File smellSettingFile;
	private SmellSettings smellSettings;
	
	@Before
	public void setUp() throws Exception {
		smellSettingFile = new File("./", SmellSettings.SETTING_FILENAME);
		if(smellSettingFile.exists()) {
			assertTrue(smellSettingFile.delete());
		}
		smellSettings = new SmellSettings(smellSettingFile);
	}

	@After
	public void tearDown() throws Exception {
		if(smellSettingFile.exists()) {
			assertTrue(smellSettingFile.delete());
		}
	}
	
	@Test
	public void testConstructor_File() throws Exception {
		smellSettings.writeXMLFile(smellSettingFile.getPath());
		assertTrue(smellSettingFile.exists());
		String fileContent = readFileContents(smellSettingFile);
		assertEquals(
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
				"<CodeSmells />", fileContent);		
	}
	
	@Test
	public void testConstructor_String() throws Exception {
		smellSettings = new SmellSettings(smellSettingFile);
		smellSettings.writeXMLFile(smellSettingFile.getPath());
		assertTrue(smellSettingFile.exists());
		String fileContent = readFileContents(smellSettingFile);
		assertEquals(
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
				"<CodeSmells />", fileContent);		
	}
	
	@Test
	public void testGetSmellType() throws Exception {
		// setting file dose not exist
		smellSettings.getSmellType(SmellSettings.SMELL_CARELESSCLEANUP);
		smellSettings.writeXMLFile(smellSettingFile.getPath());
		assertTrue(smellSettingFile.exists());
		String fileContent = readFileContents(smellSettingFile);
		assertEquals(
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
				"<CodeSmells><SmellTypes name=\"CarelessCleanup\" isDetecting=\"true\" />" +
				"</CodeSmells>", fileContent);
		
		// setting file exist
		smellSettings = new SmellSettings(smellSettingFile);
		fileContent = readFileContents(smellSettingFile);
		assertEquals(
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
				"<CodeSmells><SmellTypes name=\"CarelessCleanup\" isDetecting=\"true\" />" +
				"</CodeSmells>", fileContent);
		
		// setting file exist and add new element
		smellSettings = new SmellSettings(smellSettingFile);
		smellSettings.getSmellType(SmellSettings.SMELL_DUMMYHANDLER);
		smellSettings.writeXMLFile(smellSettingFile.getPath());
		fileContent = readFileContents(smellSettingFile);
		assertEquals(
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
				"<CodeSmells><SmellTypes name=\"CarelessCleanup\" isDetecting=\"true\" />" +
				"<SmellTypes name=\"DummyHandler\" isDetecting=\"true\" />" +
				"</CodeSmells>", fileContent);
		
		// setting file exist and add existed element
		smellSettings.getSmellType(SmellSettings.SMELL_DUMMYHANDLER);
		smellSettings.writeXMLFile(smellSettingFile.getPath());
		fileContent = readFileContents(smellSettingFile);
		assertEquals(
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
				"<CodeSmells><SmellTypes name=\"CarelessCleanup\" isDetecting=\"true\" />" +
				"<SmellTypes name=\"DummyHandler\" isDetecting=\"true\" />" +
				"</CodeSmells>", fileContent);	
	}
	
	@Test
	public void testAddDummyHandlerPattern() throws Exception {
		smellSettings.addDummyHandlerPattern("lib.*", true);
		smellSettings.addDummyHandlerPattern("e.PrintStackTrace", false);
		smellSettings.writeXMLFile(smellSettingFile.getPath());
		
		String fileContent = readFileContents(smellSettingFile);
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
				+ "<CodeSmells><SmellTypes name=\""
				+ SmellSettings.SMELL_DUMMYHANDLER
				+ "\" isDetecting=\"true\">"
				+ "<pattern name=\"lib.*\" isDetecting=\"true\" />"
				+ "<pattern name=\"e.PrintStackTrace\" isDetecting=\"false\" />"
				+ "</SmellTypes></CodeSmells>", fileContent);
	}
	
	@Test
	public void testOverLoggingPattern() throws Exception {
		smellSettings.addOverLoggingPattern("e.PrintStackTrace", false);
		smellSettings.writeXMLFile(smellSettingFile.getPath());
		
		String fileContent = readFileContents(smellSettingFile);
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
				+ "<CodeSmells><SmellTypes name=\""
				+ SmellSettings.SMELL_OVERLOGGING
				+ "\" isDetecting=\"true\">"
				+ "<pattern name=\"e.PrintStackTrace\" isDetecting=\"false\" />"
				+ "</SmellTypes></CodeSmells>", fileContent);
	}
	
	@Test
	public void testAddCarelessCleanupPattern() throws Exception {
		smellSettings.addCarelessCleanupPattern("*.lib", false);
		smellSettings.writeXMLFile(smellSettingFile.getPath());
		
		String fileContent = readFileContents(smellSettingFile);
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
				+ "<CodeSmells><SmellTypes name=\""
				+ SmellSettings.SMELL_CARELESSCLEANUP
				+ "\" isDetecting=\"true\">"
				+ "<pattern name=\"*.lib\" isDetecting=\"false\" />"
				+ "</SmellTypes></CodeSmells>", fileContent);
	}
	
	@Test
	public void testIsDetectingSmell() {
		smellSettings.addDummyHandlerPattern("e.printStackTrace", false);
		assertTrue(smellSettings.isDetectingSmell(SmellSettings.SMELL_DUMMYHANDLER));
		assertTrue(smellSettings.isDetectingSmell(SmellSettings.SMELL_CARELESSCLEANUP));

		smellSettings.addDummyHandlerPattern("system.out.printLine", true);
		assertTrue(smellSettings.isDetectingSmell(SmellSettings.SMELL_DUMMYHANDLER));
		assertTrue(smellSettings.isDetectingSmell(SmellSettings.SMELL_CARELESSCLEANUP));
		
		smellSettings.setSmellTypeAttribute(SmellSettings.SMELL_EMPTYCATCHBLOCK, SmellSettings.ATTRIBUTE_ISDETECTING, true);
		assertTrue(smellSettings.isDetectingSmell(SmellSettings.SMELL_DUMMYHANDLER));
		assertTrue(smellSettings.isDetectingSmell(SmellSettings.SMELL_CARELESSCLEANUP));
		assertTrue(smellSettings.isDetectingSmell(SmellSettings.SMELL_EMPTYCATCHBLOCK));
	}
	
	@Test
	public void testGetAllDetectingPatterns() {
		/* there is not any pattern */
		List<String> patterns = smellSettings.getAllDetectingPatterns(SmellSettings.SMELL_CARELESSCLEANUP);
		assertEquals(0, patterns.size());
		
		smellSettings.addDummyHandlerPattern("kkkkk.k", true);
		int testDataCount = 4;
		String[] patternContent = new String[testDataCount];
		patternContent[0] = "lib.*";
		patternContent[1] = "*.rain";
		patternContent[2] = "rain.dog";
		patternContent[3] = "raindog.cat";
		for (int i = 0; i < testDataCount - 1; i++) {
			smellSettings.addCarelessCleanupPattern(patternContent[i], true);
		}
		smellSettings.addCarelessCleanupPattern(patternContent[testDataCount - 1], false);
		patterns = smellSettings
				.getAllDetectingPatterns(SmellSettings.SMELL_CARELESSCLEANUP);
		assertEquals(testDataCount - 1, patterns.size());
		for (int i = 0; i < testDataCount - 1; i++) {
			assertEquals(patterns.get(i), patternContent[i]);
		}
	}
	
	@Test
	public void testGetDetectingPatterns() {
		smellSettings.addDummyHandlerPattern("kkkkk.k", true);
		smellSettings.addDummyHandlerPattern("abc.d.e", true);
		int testDataCount = 5;
		String[] patternContent = new String[testDataCount];
		patternContent[0] = "Lib.*";
		patternContent[1] = "*.rain";
		patternContent[2] = "Rain.dog.dig";
		patternContent[3] = "Raindog.cat";
		patternContent[4] = "cat";
		for (int i = 0; i < testDataCount; i++) {
			smellSettings.addCarelessCleanupPattern(patternContent[i], true);
		}
		
		List<String> librarys = smellSettings.getDetectingPatterns(SmellSettings.SMELL_CARELESSCLEANUP, UserDefinedConstraintsType.Library);
		assertEquals(1, librarys.size());
		assertEquals(patternContent[0], librarys.get(0));
		
		List<String> methods = smellSettings.getDetectingPatterns(SmellSettings.SMELL_CARELESSCLEANUP, UserDefinedConstraintsType.Method);
		assertEquals(2, methods.size());
		assertEquals(patternContent[1], methods.get(0));
		assertEquals(patternContent[4], methods.get(1));
		
		List<String> fullQualifieds = smellSettings.getDetectingPatterns(SmellSettings.SMELL_CARELESSCLEANUP, UserDefinedConstraintsType.FullQulifiedMethod);
		assertEquals(2, fullQualifieds.size());
		assertEquals(patternContent[2], fullQualifieds.get(0));
		assertEquals(patternContent[3], fullQualifieds.get(1));
	}
	
	@Test
	public void testAddPattern() throws Exception {
		Method addPattern = SmellSettings.class.getDeclaredMethod("addPattern", String.class, String.class, boolean.class);
		addPattern.setAccessible(true);
		
		// add a new pattern
		addPattern.invoke(smellSettings, SmellSettings.SMELL_CARELESSCLEANUP, "a.b.c", true);
		smellSettings.writeXMLFile(smellSettingFile.getPath());
		String fileContent = readFileContents(smellSettingFile);
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
				+ "<CodeSmells><SmellTypes name=\"CarelessCleanup\" isDetecting=\"true\">"
				+ "<pattern name=\"a.b.c\" isDetecting=\"true\" />"
				+ "</SmellTypes></CodeSmells>", fileContent);
		
		// add an existing pattern
		addPattern.invoke(smellSettings, SmellSettings.SMELL_CARELESSCLEANUP, "a.b.c", true);
		smellSettings.writeXMLFile(smellSettingFile.getPath());
		fileContent = readFileContents(smellSettingFile);
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
				+ "<CodeSmells><SmellTypes name=\"CarelessCleanup\" isDetecting=\"true\">"
				+ "<pattern name=\"a.b.c\" isDetecting=\"true\" />"
				+ "</SmellTypes></CodeSmells>", fileContent);
	}
	
	@Test
	public void testAddExtraRule() throws Exception {
		Method addPattern = SmellSettings.class.getDeclaredMethod("addExtraRule", String.class, String.class);
		addPattern.setAccessible(true);
		
		// add a new extra rule
		addPattern.invoke(smellSettings, SmellSettings.SMELL_CARELESSCLEANUP, SmellSettings.EXTRARULE_CARELESSCLEANUP_ALSO_DETECT_OUT_OF_TRY_STATEMENT);
		smellSettings.writeXMLFile(smellSettingFile.getPath());
		String fileContent = readFileContents(smellSettingFile);
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
				"<CodeSmells><SmellTypes name=\"CarelessCleanup\" isDetecting=\"true\">" +
				"<extraRule name=\"" + 
				SmellSettings.EXTRARULE_CARELESSCLEANUP_ALSO_DETECT_OUT_OF_TRY_STATEMENT +
				"\" /></SmellTypes></CodeSmells>", fileContent);
		
		// add an existing extra rule
		addPattern.invoke(smellSettings, SmellSettings.SMELL_CARELESSCLEANUP, SmellSettings.EXTRARULE_CARELESSCLEANUP_ALSO_DETECT_OUT_OF_TRY_STATEMENT);
		smellSettings.writeXMLFile(smellSettingFile.getPath());
		fileContent = readFileContents(smellSettingFile);
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
				"<CodeSmells><SmellTypes name=\"CarelessCleanup\" isDetecting=\"true\">" +
				"<extraRule name=\"" + 
				SmellSettings.EXTRARULE_CARELESSCLEANUP_ALSO_DETECT_OUT_OF_TRY_STATEMENT +
				"\" /></SmellTypes></CodeSmells>", fileContent);
	}

	@Test
	public void testRemoveExtraRule() throws Exception {
		/* without extraRule */
		assertFalse(smellSettings.removeExtraRule(
			SmellSettings.SMELL_CARELESSCLEANUP,
			SmellSettings.EXTRARULE_CARELESSCLEANUP_ALSO_DETECT_OUT_OF_TRY_STATEMENT));
		smellSettings.writeXMLFile(smellSettingFile.getPath());
		String fileContent = readFileContents(smellSettingFile);
		assertEquals(	"<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
						"<CodeSmells>" +
						"<SmellTypes name=\"CarelessCleanup\" isDetecting=\"true\" />" +
						"</CodeSmells>", fileContent);
		
		/* extraRule exist */
		smellSettings.addExtraRule(SmellSettings.SMELL_CARELESSCLEANUP, SmellSettings.EXTRARULE_CARELESSCLEANUP_ALSO_DETECT_OUT_OF_TRY_STATEMENT);
		assertTrue(smellSettings.removeExtraRule(
				SmellSettings.SMELL_CARELESSCLEANUP,
				SmellSettings.EXTRARULE_CARELESSCLEANUP_ALSO_DETECT_OUT_OF_TRY_STATEMENT));
		smellSettings.writeXMLFile(smellSettingFile.getPath());
		fileContent = readFileContents(smellSettingFile);
		assertEquals(	"<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
						"<CodeSmells>" +
						"<SmellTypes name=\"CarelessCleanup\" isDetecting=\"true\" />" +
						"</CodeSmells>", fileContent);
			
		/** dummy handler extra rule test */
		assertFalse(smellSettings.removeExtraRule(SmellSettings.SMELL_DUMMYHANDLER, SmellSettings.EXTRARULE_JavaUtilLoggingLogger));
		smellSettings.addExtraRule(SmellSettings.SMELL_DUMMYHANDLER, SmellSettings.EXTRARULE_JavaUtilLoggingLogger);
		smellSettings.writeXMLFile(smellSettingFile.getPath());
		fileContent = readFileContents(smellSettingFile);
		assertEquals(	"<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
						"<CodeSmells><SmellTypes name=\"CarelessCleanup\" isDetecting=\"true\" />" +
						"<SmellTypes name=\"DummyHandler\" isDetecting=\"true\">" +
						"<extraRule name=\"java.util.logging.Logger\" />" +
						"</SmellTypes></CodeSmells>", fileContent);
		
		assertFalse(smellSettings.removeExtraRule(SmellSettings.SMELL_DUMMYHANDLER, SmellSettings.EXTRARULE_ePrintStackTrace));
		smellSettings.addExtraRule(SmellSettings.SMELL_DUMMYHANDLER, SmellSettings.EXTRARULE_ePrintStackTrace);
		smellSettings.writeXMLFile(smellSettingFile.getPath());
		fileContent = readFileContents(smellSettingFile);
		assertEquals(	"<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
						"<CodeSmells><SmellTypes name=\"CarelessCleanup\" isDetecting=\"true\" />" +
						"<SmellTypes name=\"DummyHandler\" isDetecting=\"true\">" +
						"<extraRule name=\"java.util.logging.Logger\" />" +
						"<extraRule name=\"printStackTrace\" />" +
						"</SmellTypes></CodeSmells>", fileContent);
	}
	
	@Test
	public void testWriteXMLFile() throws Exception {
		smellSettings.writeXMLFile(smellSettingFile.getPath());
		assertTrue(smellSettingFile.exists());
		
		String fileContent = readFileContents(smellSettingFile);
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?><CodeSmells />", fileContent);
	}
	
	@Test
	public void testWriteXMLFile_OverwriteNonXMLFormatFile() throws Exception {
		// generate a file with Chinese character
		String chineseString = "天地玄黃宇宙洪荒";
		FileWriter fw = new FileWriter(smellSettingFile);
		fw.write(chineseString);
		fw.close();

		String chineseContent = readFileContents(smellSettingFile);
		assertEquals(chineseString, chineseContent);
		
		smellSettings.writeXMLFile(smellSettingFile.getPath());
		assertTrue(smellSettingFile.exists());
		
		String fileContent = readFileContents(smellSettingFile);
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?><CodeSmells />", fileContent);		
	}
	
	/**
	 * if the instance of SmellSettings has been generated and user modify the same SmellSettings file with other editor at the same time.
	 * when SmellSettings save, the modification of user made by other editor will vanish.
	 * @throws Exception
	 */
	@Test
	public void testWriteXMLFile_OtherTextWriterWriteSmellSettingXMLFormatFileAfterSmellSettingInstanceIsCreated() throws Exception {

		String xmlString = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><CodeSmells><SmellTypes name=\"DummyHandler\" isDetecting=\"true\"><pattern name=\"*.toString\" isDetecting=\"true\" /></SmellTypes></CodeSmells>";
		FileWriter fw = new FileWriter(smellSettingFile);
		fw.write(xmlString);
		fw.close();

		String xmlReadContent = readFileContents(smellSettingFile);
		assertEquals(xmlString, xmlReadContent);
		
		smellSettings.writeXMLFile(smellSettingFile.getPath());
		assertTrue(smellSettingFile.exists());
		
		String fileContent = readFileContents(smellSettingFile);
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?><CodeSmells />", fileContent);		
	}
	
	/**
	 * if there is an original setting file at the specified path, the generation of SmellSettings'instance will consult the original setting file.
	 * @throws Exception
	 */
	@Test
	public void testWriteXMLFile_UsingSameVariableTwiceWithDifferentNewInstance() throws Exception {
		smellSettings = new SmellSettings(smellSettingFile);
		smellSettings.addDummyHandlerPattern("*.toString", true);
		smellSettings.writeXMLFile(smellSettingFile.getPath());
		
		String expectedResult = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><CodeSmells><SmellTypes name=\"DummyHandler\" isDetecting=\"true\"><pattern name=\"*.toString\" isDetecting=\"true\" /></SmellTypes></CodeSmells>";
		
		String firstTimeContent = readFileContents(smellSettingFile);
		assertEquals(expectedResult, firstTimeContent);
		
		smellSettings = new SmellSettings(smellSettingFile);
		smellSettings.writeXMLFile(smellSettingFile.getPath());
		String fileContent = readFileContents(smellSettingFile);
		assertEquals(expectedResult, fileContent);
	}
	
	/**
	 * 
	 * if there is an original setting file at the specified path, the generation of SmellSettings'instance will consult the original setting file.
	 * @throws Exception
	 */
	@Test
	public void testWriteXMLFile_UsingTwoDifferentVariable() throws Exception {
		SmellSettings setting = new SmellSettings(smellSettingFile);
		setting.addDummyHandlerPattern("*.toString", true);
		setting.writeXMLFile(smellSettingFile.getPath());
		
		String expectedResult = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><CodeSmells><SmellTypes name=\"DummyHandler\" isDetecting=\"true\"><pattern name=\"*.toString\" isDetecting=\"true\" /></SmellTypes></CodeSmells>";
		
		String firstTimeContent = readFileContents(smellSettingFile);
		assertEquals(expectedResult, firstTimeContent);
		
		smellSettings = new SmellSettings(smellSettingFile);
		smellSettings.writeXMLFile(smellSettingFile.getPath());
		String fileContent = readFileContents(smellSettingFile);
		assertEquals(expectedResult, fileContent);
	}
	
	@Test
	public void testRemovePatterns() throws Exception {
		smellSettingFile.createNewFile();
		smellSettings.addDummyHandlerPattern(SmellSettings.EXTRARULE_SystemErrPrint, true);
		smellSettings.addDummyHandlerPattern(SmellSettings.EXTRARULE_ePrintStackTrace, false);
		smellSettings.addDummyHandlerPattern(SmellSettings.EXTRARULE_OrgApacheLog4j, true);
		smellSettings.addDummyHandlerPattern(SmellSettings.EXTRARULE_SystemOutPrintln, false);
		smellSettings.addExtraRule(SmellSettings.SMELL_CARELESSCLEANUP, SmellSettings.EXTRARULE_CARELESSCLEANUP_ALSO_DETECT_OUT_OF_TRY_STATEMENT);
		smellSettings.writeXMLFile(smellSettingFile.getPath());
		String content = readFileContents(smellSettingFile);
		assertEquals(	"<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
						"<CodeSmells><SmellTypes name=\"DummyHandler\" isDetecting=\"true\">" +
						"<pattern name=\"System.err.print\" isDetecting=\"true\" />" +
						"<pattern name=\"printStackTrace\" isDetecting=\"false\" />" +
						"<pattern name=\"org.apache.log4j\" isDetecting=\"true\" />" +
						"<pattern name=\"System.out.println\" isDetecting=\"false\" />" +
						"</SmellTypes><SmellTypes name=\"CarelessCleanup\" isDetecting=\"true\">" +
						"<extraRule name=\"DetectOutOfTryStatement\" />" +
						"</SmellTypes></CodeSmells>", content);
		
		assertTrue(smellSettings.removePatterns(SmellSettings.SMELL_DUMMYHANDLER));
		smellSettings.writeXMLFile(smellSettingFile.getPath());
		content = readFileContents(smellSettingFile);
		assertEquals(	"<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
						"<CodeSmells>" +
						"<SmellTypes name=\"DummyHandler\" isDetecting=\"true\" />" +
						"<SmellTypes name=\"CarelessCleanup\" isDetecting=\"true\">" +
						"<extraRule name=\"DetectOutOfTryStatement\" />" +
						"</SmellTypes></CodeSmells>", content);
	}
	
	@Test
	public void testSetSmellTypeAttribute() throws Exception {
		smellSettingFile.createNewFile();
		smellSettings.addDummyHandlerPattern(SmellSettings.EXTRARULE_SystemErrPrint, true);
		smellSettings.addDummyHandlerPattern(SmellSettings.EXTRARULE_ePrintStackTrace, false);
		smellSettings.addDummyHandlerPattern(SmellSettings.EXTRARULE_OrgApacheLog4j, true);
		smellSettings.addDummyHandlerPattern(SmellSettings.EXTRARULE_SystemOutPrintln, false);
		smellSettings.addExtraRule(SmellSettings.SMELL_CARELESSCLEANUP, SmellSettings.EXTRARULE_CARELESSCLEANUP_ALSO_DETECT_OUT_OF_TRY_STATEMENT);
		smellSettings.writeXMLFile(smellSettingFile.getPath());
		String content = readFileContents(smellSettingFile);
		assertEquals(	"<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
						"<CodeSmells>" +
						"<SmellTypes name=\"DummyHandler\" isDetecting=\"true\">" +
						"<pattern name=\"System.err.print\" isDetecting=\"true\" />" +
						"<pattern name=\"printStackTrace\" isDetecting=\"false\" />" +
						"<pattern name=\"org.apache.log4j\" isDetecting=\"true\" />" +
						"<pattern name=\"System.out.println\" isDetecting=\"false\" />" +
						"</SmellTypes><SmellTypes name=\"CarelessCleanup\" isDetecting=\"true\">" +
						"<extraRule name=\"DetectOutOfTryStatement\" />" +
						"</SmellTypes></CodeSmells>", content);
		
		smellSettings.setSmellTypeAttribute(SmellSettings.SMELL_DUMMYHANDLER, SmellSettings.ATTRIBUTE_ISDETECTING, false);
		smellSettings.setSmellTypeAttribute(SmellSettings.SMELL_CARELESSCLEANUP, SmellSettings.ATTRIBUTE_ISDETECTING, false);
		smellSettings.writeXMLFile(smellSettingFile.getPath());
		content = readFileContents(smellSettingFile);
		assertEquals(	"<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
						"<CodeSmells>" +
						"<SmellTypes name=\"DummyHandler\" isDetecting=\"false\">" +
						"<pattern name=\"System.err.print\" isDetecting=\"true\" />" +
						"<pattern name=\"printStackTrace\" isDetecting=\"false\" />" +
						"<pattern name=\"org.apache.log4j\" isDetecting=\"true\" />" +
						"<pattern name=\"System.out.println\" isDetecting=\"false\" />" +
						"</SmellTypes><SmellTypes name=\"CarelessCleanup\" isDetecting=\"false\">" +
						"<extraRule name=\"DetectOutOfTryStatement\" />" +
						"</SmellTypes></CodeSmells>", content);
	}
	
	@Test
	public void testGetSmellPatterns() throws Exception {
		/** when without specified bad smell setting */
		smellSettingFile.createNewFile();
		String content = readFileContents(smellSettingFile);
		assertEquals("", content);
		TreeMap<String, Boolean> libMap = smellSettings.getSmellPatterns(SmellSettings.SMELL_DUMMYHANDLER);
		assertEquals(0, libMap.size());
		
		/** when without any bad smell setting */
		smellSettings.getSmellType(SmellSettings.SMELL_DUMMYHANDLER);
		smellSettings.writeXMLFile(smellSettingFile.getPath());
		content = readFileContents(smellSettingFile);
		assertEquals(	"<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
						"<CodeSmells><SmellTypes name=\"DummyHandler\" isDetecting=\"true\" />" +
						"</CodeSmells>", content);
		libMap = smellSettings.getSmellPatterns(SmellSettings.SMELL_DUMMYHANDLER);
		assertEquals(0, libMap.size());
		
		/** when there are some bad smell setting */
		smellSettings.addDummyHandlerPattern(SmellSettings.EXTRARULE_SystemErrPrint, true);
		smellSettings.addDummyHandlerPattern(SmellSettings.EXTRARULE_ePrintStackTrace, false);
		smellSettings.addDummyHandlerPattern(SmellSettings.EXTRARULE_OrgApacheLog4j, true);
		smellSettings.writeXMLFile(smellSettingFile.getPath());
		content = readFileContents(smellSettingFile);
		assertEquals(	"<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
						"<CodeSmells><SmellTypes name=\"DummyHandler\" isDetecting=\"true\">" +
						"<pattern name=\"System.err.print\" isDetecting=\"true\" />" +
						"<pattern name=\"printStackTrace\" isDetecting=\"false\" />" +
						"<pattern name=\"org.apache.log4j\" isDetecting=\"true\" />" +
						"</SmellTypes></CodeSmells>", content);
		
		libMap = smellSettings.getSmellPatterns(SmellSettings.SMELL_DUMMYHANDLER);
		assertEquals(3, libMap.size());
		assertTrue(libMap.get(SmellSettings.EXTRARULE_SystemErrPrint));
		assertFalse(libMap.get(SmellSettings.EXTRARULE_ePrintStackTrace));
		assertTrue(libMap.get(SmellSettings.EXTRARULE_OrgApacheLog4j));
	}
	
	@Test
	public void testIsExtraRuleExist() throws Exception {
		smellSettingFile.createNewFile();
		
		/** when bad smell's node is missing*/
		assertFalse(smellSettings.isExtraRuleExist(SmellSettings.SMELL_CARELESSCLEANUP, SmellSettings.EXTRARULE_CARELESSCLEANUP_ALSO_DETECT_OUT_OF_TRY_STATEMENT));
		
		/** when bad smell's node is exist but without setting */
		smellSettings.getSmellType(SmellSettings.SMELL_CARELESSCLEANUP);
		assertFalse(smellSettings.isExtraRuleExist(SmellSettings.SMELL_CARELESSCLEANUP, SmellSettings.EXTRARULE_CARELESSCLEANUP_ALSO_DETECT_OUT_OF_TRY_STATEMENT));
		
		/** when bad smell's node is exist and setting has been specified*/
		smellSettings.addExtraRule(SmellSettings.SMELL_CARELESSCLEANUP, SmellSettings.EXTRARULE_CARELESSCLEANUP_ALSO_DETECT_OUT_OF_TRY_STATEMENT);
		smellSettings.writeXMLFile(smellSettingFile.getPath());
		assertTrue(smellSettings.isExtraRuleExist(SmellSettings.SMELL_CARELESSCLEANUP, SmellSettings.EXTRARULE_CARELESSCLEANUP_ALSO_DETECT_OUT_OF_TRY_STATEMENT));
	}
	
	@Test
	public void testGetSmellSettings() throws Exception {
		smellSettingFile.createNewFile();
		
		/** when bad smell's node is missing*/
		assertEquals(0, smellSettings.getSmellSettings(SmellSettings.SMELL_DUMMYHANDLER).size());
		
		/** when bad smell is exist and only with extra rule */
		smellSettings.addExtraRule(SmellSettings.SMELL_DUMMYHANDLER, SmellSettings.EXTRARULE_ePrintStackTrace);
		smellSettings.addExtraRule(SmellSettings.SMELL_DUMMYHANDLER, SmellSettings.EXTRARULE_JavaUtilLoggingLogger);
		smellSettings.addExtraRule(SmellSettings.SMELL_DUMMYHANDLER, SmellSettings.EXTRARULE_SystemOutPrintln);
		smellSettings.addExtraRule(SmellSettings.SMELL_DUMMYHANDLER, SmellSettings.EXTRARULE_SystemErrPrint);
		smellSettings.addExtraRule(SmellSettings.SMELL_DUMMYHANDLER, SmellSettings.EXTRARULE_SystemErrPrintln);
		smellSettings.writeXMLFile(smellSettingFile.getPath());
		
		TreeMap<String, UserDefinedConstraintsType> libMap = smellSettings.getSmellSettings(SmellSettings.SMELL_DUMMYHANDLER);
		assertEquals(4, libMap.size());
		assertNull(libMap.get(SmellSettings.EXTRARULE_SystemOutPrint));
		assertNull(libMap.get(SmellSettings.EXTRARULE_SystemOutPrintln));
		assertNull(libMap.get(SmellSettings.EXTRARULE_SystemErrPrint));
		assertNull(libMap.get(SmellSettings.EXTRARULE_SystemErrPrintln));
		assertEquals(UserDefinedConstraintsType.Library, libMap.get(SmellSettings.EXTRARULE_JavaUtilLoggingLogger));
		assertEquals(UserDefinedConstraintsType.Method, libMap.get(SmellSettings.EXTRARULE_ePrintStackTrace));
		
		// clear data for next test case
		assertTrue(smellSettingFile.delete());
		smellSettings = new SmellSettings(smellSettingFile.getPath());
		
		/** when bad smell is exist and only with bad smell pattern*/
		assertTrue(smellSettingFile.createNewFile());
		smellSettings.addDummyHandlerPattern("Java.io.File", true);
		smellSettings.addDummyHandlerPattern("Java.io.FileInputStream", false);
		smellSettings.addDummyHandlerPattern("Java.io.*", true);
		smellSettings.addDummyHandlerPattern("Java.*", true);
		smellSettings.addDummyHandlerPattern("*.io.File", true);
		smellSettings.addDummyHandlerPattern("*.io.FileInputStream", false);
		smellSettings.addDummyHandlerPattern("*.File", true);
		smellSettings.addDummyHandlerPattern("*.FileInputStream", false);
		smellSettings.writeXMLFile(smellSettingFile.getPath());
		
		libMap = smellSettings.getSmellSettings(SmellSettings.SMELL_DUMMYHANDLER);
		assertEquals(5, libMap.size());
		assertEquals(UserDefinedConstraintsType.FullQulifiedMethod, libMap.get("Java.io.File"));
		assertNull(libMap.get("Java.io.FileinputStream"));
		assertEquals(UserDefinedConstraintsType.Library, libMap.get("Java.io"));
		assertEquals(UserDefinedConstraintsType.Library, libMap.get("Java"));
		assertEquals(UserDefinedConstraintsType.Method, libMap.get("io.File"));
		assertNull(libMap.get("io.FileInputStream"));
		assertEquals(UserDefinedConstraintsType.Method, libMap.get("File"));
		assertNull(libMap.get("FileInputStream"));
		
		/**  extra rules and patterns both exist. */
		smellSettings.addExtraRule(SmellSettings.SMELL_DUMMYHANDLER, SmellSettings.EXTRARULE_ePrintStackTrace);
		smellSettings.addExtraRule(SmellSettings.SMELL_DUMMYHANDLER, SmellSettings.EXTRARULE_JavaUtilLoggingLogger);
		smellSettings.addExtraRule(SmellSettings.SMELL_DUMMYHANDLER, SmellSettings.EXTRARULE_SystemOutPrintln);
		smellSettings.addExtraRule(SmellSettings.SMELL_DUMMYHANDLER, SmellSettings.EXTRARULE_SystemErrPrint);
		smellSettings.addExtraRule(SmellSettings.SMELL_DUMMYHANDLER, SmellSettings.EXTRARULE_SystemErrPrintln);
		smellSettings.writeXMLFile(smellSettingFile.getPath());
		
		libMap = smellSettings.getSmellSettings(SmellSettings.SMELL_DUMMYHANDLER);
		assertEquals(9, libMap.size());
		assertNull(libMap.get(SmellSettings.EXTRARULE_SystemOutPrint));
		assertNull(libMap.get(SmellSettings.EXTRARULE_SystemOutPrintln));
		assertNull(libMap.get(SmellSettings.EXTRARULE_SystemErrPrint));
		assertNull(libMap.get(SmellSettings.EXTRARULE_SystemErrPrintln));
		assertEquals(UserDefinedConstraintsType.FullQulifiedMethod, libMap.get("java.io.PrintStream.println"));
		assertEquals(UserDefinedConstraintsType.FullQulifiedMethod, libMap.get("java.io.PrintStream.print"));
		assertEquals(UserDefinedConstraintsType.Library, libMap.get(SmellSettings.EXTRARULE_JavaUtilLoggingLogger));
		assertEquals(UserDefinedConstraintsType.Method, libMap.get(SmellSettings.EXTRARULE_ePrintStackTrace));
		assertEquals(UserDefinedConstraintsType.FullQulifiedMethod, libMap.get("Java.io.File"));
		assertNull(libMap.get("Java.io.FileinputStream"));
		assertEquals(UserDefinedConstraintsType.Library, libMap.get("Java.io"));
		assertEquals(UserDefinedConstraintsType.Library, libMap.get("Java"));
		assertEquals(UserDefinedConstraintsType.Method, libMap.get("io.File"));
		assertNull(libMap.get("io.FileInputStream"));
		assertEquals(UserDefinedConstraintsType.Method, libMap.get("File"));
		assertNull(libMap.get("FileInputStream"));
		
		/** without any action when without any selection of SmellSetting */
		smellSettings.setSmellTypeAttribute(SmellSettings.SMELL_DUMMYHANDLER, SmellSettings.ATTRIBUTE_ISDETECTING, false);
		libMap = smellSettings.getSmellSettings(SmellSettings.SMELL_DUMMYHANDLER);
		assertEquals(0, libMap.size());
	}
	
	private String readFileContents(File file) throws Exception {
		StringBuilder sb = new StringBuilder();
		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
		String lineContent;
		while((lineContent = br.readLine()) != null) {
			sb.append(lineContent);
		}
		br.close();
		return sb.toString();
	}

	@Test
	public void testActivateAllConditions() throws Exception {
		smellSettings = new SmellSettings(smellSettingFile);
		smellSettings.addExtraRule(SmellSettings.SMELL_DUMMYHANDLER, SmellSettings.EXTRARULE_ePrintStackTrace);
		smellSettings.writeXMLFile(smellSettingFile.getPath());
		
		String expectedResult = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><CodeSmells><SmellTypes name=\"DummyHandler\" isDetecting=\"true\"><extraRule name=\"printStackTrace\" /></SmellTypes></CodeSmells>";
		
		String firstTimeContent = readFileContents(smellSettingFile);
		assertEquals(expectedResult, firstTimeContent);
		
		smellSettings.activateAllConditionsIfNotConfugured(smellSettingFile.getPath());

		assertEquals(expectedResult, firstTimeContent);
		
		// clear data for next test case
		assertTrue(smellSettingFile.delete());
		smellSettings.activateAllConditionsIfNotConfugured(smellSettingFile.getPath());
		
		String fileContent = readFileContents(smellSettingFile);
		assertEquals(
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?><CodeSmells>" +
				"<SmellTypes name=\"DummyHandler\" isDetecting=\"true\">" +
				"<extraRule name=\"printStackTrace\" /><extraRule name=\"System.err.print\" />" +
				"<extraRule name=\"System.err.println\" /><extraRule name=\"System.out.print\" />" +
				"<extraRule name=\"System.out.println\" /><extraRule name=\"java.util.logging.Logger\" />" +
				"<extraRule name=\"org.apache.log4j\" />" +
				"</SmellTypes><SmellTypes name=\"EmptyCatchBlock\" isDetecting=\"true\" />" +
				"<SmellTypes name=\"NestedTryStatement\" isDetecting=\"true\" />" +
				"<SmellTypes name=\"UnprotectedMainProgram\" isDetecting=\"true\" />" +
				"<SmellTypes name=\"CarelessCleanup\" isDetecting=\"true\">" +
				"<extraRule name=\"DetectOutOfTryStatement\" /></SmellTypes>" +
				"<SmellTypes name=\"OverLogging\" isDetecting=\"true\"><extraRule name=\"DetectWrappingExcetion\" />" +
				"<extraRule name=\"java.util.logging.Logger\" /><extraRule name=\"org.apache.log4j\" />" +
				"</SmellTypes>"	+
				"<SmellTypes name=\"ExceptionThrownFromFinallyBlock\" isDetecting=\"true\" />" +
				"</CodeSmells>",
				fileContent);
	}
	
	@Test
	public void testGetPreference() throws Exception {
		assertFalse(smellSettingFile.exists());
		smellSettings.getPreference(SmellSettings.PRE_SHOWRLANNOTATIONWARNING);
		smellSettings.writeXMLFile(smellSettingFile.getPath());
		assertTrue(smellSettingFile.exists());
		String fileContent = readFileContents(smellSettingFile);
		assertEquals(
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
				"<CodeSmells><Preferences name=\"ShowRLAnnotationWarning\" enable=\"true\" />" +
				"</CodeSmells>", fileContent);
	}
	
	@Test
	public void testGetPreferenceAttribute() {
		assertFalse(smellSettingFile.exists());
		smellSettings.getPreference(SmellSettings.PRE_SHOWRLANNOTATIONWARNING);
		smellSettings.writeXMLFile(smellSettingFile.getPath());
		assertTrue(smellSettingFile.exists());
		assertTrue(smellSettings.getPreferenceAttribute(SmellSettings.PRE_SHOWRLANNOTATIONWARNING));
	}
	
	@Test
	public void testSetPreferenceAttribute() {
		assertFalse(smellSettingFile.exists());
		smellSettings.getPreference(SmellSettings.PRE_SHOWRLANNOTATIONWARNING);
		smellSettings.writeXMLFile(smellSettingFile.getPath());
		assertTrue(smellSettings.getPreferenceAttribute(SmellSettings.PRE_SHOWRLANNOTATIONWARNING));
		smellSettings.setPreferenceAttribute(SmellSettings.PRE_SHOWRLANNOTATIONWARNING, SmellSettings.ATTRIBUTE_ENABLE, false);
		smellSettings.writeXMLFile(smellSettingFile.getPath());
		assertFalse(smellSettings.getPreferenceAttribute(SmellSettings.PRE_SHOWRLANNOTATIONWARNING));
		smellSettings.setPreferenceAttribute(SmellSettings.PRE_SHOWRLANNOTATIONWARNING, SmellSettings.ATTRIBUTE_ENABLE, true);
		smellSettings.writeXMLFile(smellSettingFile.getPath());
		assertTrue(smellSettings.getPreferenceAttribute(SmellSettings.PRE_SHOWRLANNOTATIONWARNING));
	}
}
