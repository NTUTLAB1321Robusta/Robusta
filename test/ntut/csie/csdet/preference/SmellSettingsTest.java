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
	/** 產生出來的XML檔案 */
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
		assertFalse(smellSettings.isDetectingSmell(SmellSettings.SMELL_CARELESSCLEANUP));

		smellSettings.addDummyHandlerPattern("system.out.printLine", true);
		assertTrue(smellSettings.isDetectingSmell(SmellSettings.SMELL_DUMMYHANDLER));
		assertFalse(smellSettings.isDetectingSmell(SmellSettings.SMELL_CARELESSCLEANUP));
		
		smellSettings.setSmellTypeAttribute(SmellSettings.SMELL_IGNORECHECKEDEXCEPTION, SmellSettings.ATTRIBUTE_ISDETECTING, true);
		assertTrue(smellSettings.isDetectingSmell(SmellSettings.SMELL_DUMMYHANDLER));
		assertFalse(smellSettings.isDetectingSmell(SmellSettings.SMELL_CARELESSCLEANUP));
		assertTrue(smellSettings.isDetectingSmell(SmellSettings.SMELL_IGNORECHECKEDEXCEPTION));
	}
	
	@Test
	public void testGetAllDetectingPatterns() {
		/* 沒有Pattern存在 */
		List<String> patterns = smellSettings.getAllDetectingPatterns(SmellSettings.SMELL_CARELESSCLEANUP);
		assertEquals(0, patterns.size());
		
		/* 有CarelessCleaup Pattern與DummyHandelr Pattern存在，
		 * 蒐集所有CarelessCleanup Pattern
		 */
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
		
		// 加入的pattern是全新的
		addPattern.invoke(smellSettings, SmellSettings.SMELL_CARELESSCLEANUP, "a.b.c", true);
		smellSettings.writeXMLFile(smellSettingFile.getPath());
		String fileContent = readFileContents(smellSettingFile);
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
				+ "<CodeSmells><SmellTypes name=\"CarelessCleanup\" isDetecting=\"true\">"
				+ "<pattern name=\"a.b.c\" isDetecting=\"true\" />"
				+ "</SmellTypes></CodeSmells>", fileContent);
		
		// 加入的pattern是已經存在過的
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
		
		// 增加一個還沒存在過的規則
		addPattern.invoke(smellSettings, SmellSettings.SMELL_CARELESSCLEANUP, SmellSettings.EXTRARULE_CARELESSCLEANUP_DETECTISRELEASEIOCODEINDECLAREDMETHOD);
		smellSettings.writeXMLFile(smellSettingFile.getPath());
		String fileContent = readFileContents(smellSettingFile);
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
				"<CodeSmells><SmellTypes name=\"CarelessCleanup\" isDetecting=\"true\">" +
				"<extraRule name=\"" + 
				SmellSettings.EXTRARULE_CARELESSCLEANUP_DETECTISRELEASEIOCODEINDECLAREDMETHOD +
				"\" /></SmellTypes></CodeSmells>", fileContent);
		
		// 增加一個已經存在的規則
		addPattern.invoke(smellSettings, SmellSettings.SMELL_CARELESSCLEANUP, SmellSettings.EXTRARULE_CARELESSCLEANUP_DETECTISRELEASEIOCODEINDECLAREDMETHOD);
		smellSettings.writeXMLFile(smellSettingFile.getPath());
		fileContent = readFileContents(smellSettingFile);
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
				"<CodeSmells><SmellTypes name=\"CarelessCleanup\" isDetecting=\"true\">" +
				"<extraRule name=\"" + 
				SmellSettings.EXTRARULE_CARELESSCLEANUP_DETECTISRELEASEIOCODEINDECLAREDMETHOD +
				"\" /></SmellTypes></CodeSmells>", fileContent);
	}

	@Test
	public void testRemoveExtraRule() throws Exception {
		/* extraRule的節點不存在 */
		assertFalse(smellSettings.removeExtraRule(
			SmellSettings.SMELL_CARELESSCLEANUP,
			SmellSettings.EXTRARULE_CARELESSCLEANUP_DETECTISRELEASEIOCODEINDECLAREDMETHOD));
		smellSettings.writeXMLFile(smellSettingFile.getPath());
		String fileContent = readFileContents(smellSettingFile);
		assertEquals(	"<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
						"<CodeSmells>" +
						"<SmellTypes name=\"CarelessCleanup\" isDetecting=\"true\" />" +
						"</CodeSmells>", fileContent);
		
		/* extraRule的節點存在 */
		smellSettings.addExtraRule(SmellSettings.SMELL_CARELESSCLEANUP, SmellSettings.EXTRARULE_CARELESSCLEANUP_DETECTISRELEASEIOCODEINDECLAREDMETHOD);
		assertTrue(smellSettings.removeExtraRule(
				SmellSettings.SMELL_CARELESSCLEANUP,
				SmellSettings.EXTRARULE_CARELESSCLEANUP_DETECTISRELEASEIOCODEINDECLAREDMETHOD));
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
		// 檢查檔案是否生成
		assertTrue(smellSettingFile.exists());
		
		// 檢查檔案內容是否正確
		String fileContent = readFileContents(smellSettingFile);
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?><CodeSmells />", fileContent);
	}
	
	@Test
	public void testWriteXMLFile_OverwriteNonXMLFormatFile() throws Exception {
		// 生成一個文字檔案，裡面都是中文字
		String chineseString = "天地玄黃宇宙洪荒";
		FileWriter fw = new FileWriter(smellSettingFile);
		fw.write(chineseString);
		fw.close();
		// 確認檔案裡面的中文字
		String chineseContent = readFileContents(smellSettingFile);
		assertEquals(chineseString, chineseContent);
		
		// 生成XML檔案
		smellSettings.writeXMLFile(smellSettingFile.getPath());
		// 檢查檔案是否生成
		assertTrue(smellSettingFile.exists());
		
		// 檢查檔案內容是否正確
		String fileContent = readFileContents(smellSettingFile);
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?><CodeSmells />", fileContent);		
	}
	
	/**
	 * 如果SmellSettings的instance已經被產生出來，使用者同時再用其他文字編輯器寫入設定檔內容，
	 * 最後再用SmellSettigs存檔後，其他文字編輯器寫入的內容會遺失。
	 * @throws Exception
	 */
	@Test
	public void testWriteXMLFile_OtherTextWriterWriteSmellSettingXMLFormatFileAfterSmellSettingInstanceIsCreated() throws Exception {
		// 生成一個文字檔案，裡面是舊有的XML設定檔
		String xmlString = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><CodeSmells><SmellTypes name=\"DummyHandler\" isDetecting=\"true\"><pattern name=\"*.toString\" isDetecting=\"true\" /></SmellTypes></CodeSmells>";
		FileWriter fw = new FileWriter(smellSettingFile);
		fw.write(xmlString);
		fw.close();
		// 確認檔案裡面的XML設定檔內容
		String xmlReadContent = readFileContents(smellSettingFile);
		assertEquals(xmlString, xmlReadContent);
		
		// 生成XML檔案
		smellSettings.writeXMLFile(smellSettingFile.getPath());
		// 檢查檔案是否生成
		assertTrue(smellSettingFile.exists());
		
		// 檢查檔案內容是否正確
		String fileContent = readFileContents(smellSettingFile);
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?><CodeSmells />", fileContent);		
	}
	
	/**
	 * 相同路徑下已經有設定檔，則每次產生SmellSettings新的instance，都應該會自動讀取舊的設定檔。
	 * @throws Exception
	 */
	@Test
	public void testWriteXMLFile_UsingSameVariableTwiceWithDifferentNewInstance() throws Exception {
		// 先用一個新的instace產生xml檔
		smellSettings = new SmellSettings(smellSettingFile);
		smellSettings.addDummyHandlerPattern("*.toString", true);
		smellSettings.writeXMLFile(smellSettingFile.getPath());
		
		String expectedResult = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><CodeSmells><SmellTypes name=\"DummyHandler\" isDetecting=\"true\"><pattern name=\"*.toString\" isDetecting=\"true\" /></SmellTypes></CodeSmells>";
		
		// 確認內容
		String firstTimeContent = readFileContents(smellSettingFile);
		assertEquals(expectedResult, firstTimeContent);
		
		// 再用同一個變數產生新的instace，再產生xml檔
		smellSettings = new SmellSettings(smellSettingFile);
		smellSettings.writeXMLFile(smellSettingFile.getPath());
		// 確認內容
		String fileContent = readFileContents(smellSettingFile);
		assertEquals(expectedResult, fileContent);
	}
	
	/**
	 * 相同路徑下已經有設定檔，則每次產生SmellSettings新的instance，都應該會自動讀取舊的設定檔。
	 * @throws Exception
	 */
	@Test
	public void testWriteXMLFile_UsingTwoDifferentVariable() throws Exception {
		// 先用一個新的變數產生xml檔
		SmellSettings setting = new SmellSettings(smellSettingFile);
		setting.addDummyHandlerPattern("*.toString", true);
		setting.writeXMLFile(smellSettingFile.getPath());
		
		String expectedResult = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><CodeSmells><SmellTypes name=\"DummyHandler\" isDetecting=\"true\"><pattern name=\"*.toString\" isDetecting=\"true\" /></SmellTypes></CodeSmells>";
		
		// 確認內容
		String firstTimeContent = readFileContents(smellSettingFile);
		assertEquals(expectedResult, firstTimeContent);
		
		// 再用另一個變數產生xml檔
		smellSettings = new SmellSettings(smellSettingFile);
		smellSettings.writeXMLFile(smellSettingFile.getPath());
		// 確認內容
		String fileContent = readFileContents(smellSettingFile);
		assertEquals(expectedResult, fileContent);
	}
	
	@Test
	public void testRemovePatterns() throws Exception {
		// 準備測試需要內容
		smellSettingFile.createNewFile();
		smellSettings.addDummyHandlerPattern(SmellSettings.EXTRARULE_SystemErrPrint, true);
		smellSettings.addDummyHandlerPattern(SmellSettings.EXTRARULE_ePrintStackTrace, false);
		smellSettings.addDummyHandlerPattern(SmellSettings.EXTRARULE_OrgApacheLog4j, true);
		smellSettings.addDummyHandlerPattern(SmellSettings.EXTRARULE_SystemOutPrintln, false);
		smellSettings.addExtraRule(SmellSettings.SMELL_CARELESSCLEANUP, SmellSettings.EXTRARULE_CARELESSCLEANUP_DETECTISRELEASEIOCODEINDECLAREDMETHOD);
		smellSettings.writeXMLFile(smellSettingFile.getPath());
		String content = readFileContents(smellSettingFile);
		// 檢查準備資料是否正確
		assertEquals(	"<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
						"<CodeSmells><SmellTypes name=\"DummyHandler\" isDetecting=\"true\">" +
						"<pattern name=\"System.err.print\" isDetecting=\"true\" />" +
						"<pattern name=\"printStackTrace\" isDetecting=\"false\" />" +
						"<pattern name=\"org.apache.log4j\" isDetecting=\"true\" />" +
						"<pattern name=\"System.out.println\" isDetecting=\"false\" />" +
						"</SmellTypes><SmellTypes name=\"CarelessCleanup\" isDetecting=\"true\">" +
						"<extraRule name=\"DetectIsReleaseIOCodeInDeclaredMethod\" />" +
						"</SmellTypes></CodeSmells>", content);
		
		assertTrue(smellSettings.removePatterns(SmellSettings.SMELL_DUMMYHANDLER));
		smellSettings.writeXMLFile(smellSettingFile.getPath());
		// 驗證結果是否正確
		content = readFileContents(smellSettingFile);
		assertEquals(	"<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
						"<CodeSmells>" +
						"<SmellTypes name=\"DummyHandler\" isDetecting=\"true\" />" +
						"<SmellTypes name=\"CarelessCleanup\" isDetecting=\"true\">" +
						"<extraRule name=\"DetectIsReleaseIOCodeInDeclaredMethod\" />" +
						"</SmellTypes></CodeSmells>", content);
	}
	
	@Test
	public void testSetSmellTypeAttribute() throws Exception {
		// 準備測試需要內容
		smellSettingFile.createNewFile();
		smellSettings.addDummyHandlerPattern(SmellSettings.EXTRARULE_SystemErrPrint, true);
		smellSettings.addDummyHandlerPattern(SmellSettings.EXTRARULE_ePrintStackTrace, false);
		smellSettings.addDummyHandlerPattern(SmellSettings.EXTRARULE_OrgApacheLog4j, true);
		smellSettings.addDummyHandlerPattern(SmellSettings.EXTRARULE_SystemOutPrintln, false);
		smellSettings.addExtraRule(SmellSettings.SMELL_CARELESSCLEANUP, SmellSettings.EXTRARULE_CARELESSCLEANUP_DETECTISRELEASEIOCODEINDECLAREDMETHOD);
		smellSettings.writeXMLFile(smellSettingFile.getPath());
		String content = readFileContents(smellSettingFile);
		// 檢查準備資料是否正確
		assertEquals(	"<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
						"<CodeSmells>" +
						"<SmellTypes name=\"DummyHandler\" isDetecting=\"true\">" +
						"<pattern name=\"System.err.print\" isDetecting=\"true\" />" +
						"<pattern name=\"printStackTrace\" isDetecting=\"false\" />" +
						"<pattern name=\"org.apache.log4j\" isDetecting=\"true\" />" +
						"<pattern name=\"System.out.println\" isDetecting=\"false\" />" +
						"</SmellTypes><SmellTypes name=\"CarelessCleanup\" isDetecting=\"true\">" +
						"<extraRule name=\"DetectIsReleaseIOCodeInDeclaredMethod\" />" +
						"</SmellTypes></CodeSmells>", content);
		
		smellSettings.setSmellTypeAttribute(SmellSettings.SMELL_DUMMYHANDLER, SmellSettings.ATTRIBUTE_ISDETECTING, false);
		smellSettings.setSmellTypeAttribute(SmellSettings.SMELL_CARELESSCLEANUP, SmellSettings.ATTRIBUTE_ISDETECTING, false);
		smellSettings.writeXMLFile(smellSettingFile.getPath());
		// 驗證結果是否正確
		content = readFileContents(smellSettingFile);
		assertEquals(	"<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
						"<CodeSmells>" +
						"<SmellTypes name=\"DummyHandler\" isDetecting=\"false\">" +
						"<pattern name=\"System.err.print\" isDetecting=\"true\" />" +
						"<pattern name=\"printStackTrace\" isDetecting=\"false\" />" +
						"<pattern name=\"org.apache.log4j\" isDetecting=\"true\" />" +
						"<pattern name=\"System.out.println\" isDetecting=\"false\" />" +
						"</SmellTypes><SmellTypes name=\"CarelessCleanup\" isDetecting=\"false\">" +
						"<extraRule name=\"DetectIsReleaseIOCodeInDeclaredMethod\" />" +
						"</SmellTypes></CodeSmells>", content);
	}
	
	@Test
	public void testGetSemllPatterns() throws Exception {
		/** 當沒有這個Bad Smell設定值時 */
		smellSettingFile.createNewFile();
		String content = readFileContents(smellSettingFile);
		// 檢查準備資料是否正確
		assertEquals("", content);
		TreeMap<String, Boolean> libMap = smellSettings.getSemllPatterns(SmellSettings.SMELL_DUMMYHANDLER);
		// 驗證結果是否正確
		assertEquals(0, libMap.size());
		
		/** 當沒有任何設定值時 */
		// 準備測試需要內容
		smellSettings.getSmellType(SmellSettings.SMELL_DUMMYHANDLER);
		smellSettings.writeXMLFile(smellSettingFile.getPath());
		content = readFileContents(smellSettingFile);
		// 檢查準備資料是否正確
		assertEquals(	"<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
						"<CodeSmells><SmellTypes name=\"DummyHandler\" isDetecting=\"true\" />" +
						"</CodeSmells>", content);
		libMap = smellSettings.getSemllPatterns(SmellSettings.SMELL_DUMMYHANDLER);
		// 驗證結果是否正確
		assertEquals(0, libMap.size());
		
		/** 當有設定值時 */
		smellSettings.addDummyHandlerPattern(SmellSettings.EXTRARULE_SystemErrPrint, true);
		smellSettings.addDummyHandlerPattern(SmellSettings.EXTRARULE_ePrintStackTrace, false);
		smellSettings.addDummyHandlerPattern(SmellSettings.EXTRARULE_OrgApacheLog4j, true);
		smellSettings.writeXMLFile(smellSettingFile.getPath());
		content = readFileContents(smellSettingFile);
		// 檢查準備資料是否正確
		assertEquals(	"<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
						"<CodeSmells><SmellTypes name=\"DummyHandler\" isDetecting=\"true\">" +
						"<pattern name=\"System.err.print\" isDetecting=\"true\" />" +
						"<pattern name=\"printStackTrace\" isDetecting=\"false\" />" +
						"<pattern name=\"org.apache.log4j\" isDetecting=\"true\" />" +
						"</SmellTypes></CodeSmells>", content);
		
		libMap = smellSettings.getSemllPatterns(SmellSettings.SMELL_DUMMYHANDLER);
		// 驗證結果是否正確
		assertEquals(3, libMap.size());
		assertTrue(libMap.get(SmellSettings.EXTRARULE_SystemErrPrint));
		assertFalse(libMap.get(SmellSettings.EXTRARULE_ePrintStackTrace));
		assertTrue(libMap.get(SmellSettings.EXTRARULE_OrgApacheLog4j));
	}
	
	@Test
	public void testIsExtraRuleExist() throws Exception {
		smellSettingFile.createNewFile();
		
		/** 當bad smell的節點不存在時 */
		assertFalse(smellSettings.isExtraRuleExist(SmellSettings.SMELL_CARELESSCLEANUP, SmellSettings.EXTRARULE_CARELESSCLEANUP_DETECTISRELEASEIOCODEINDECLAREDMETHOD));
		
		/** 當bad smell的節點存在，卻沒有設定值時 */
		smellSettings.getSmellType(SmellSettings.SMELL_CARELESSCLEANUP);
		assertFalse(smellSettings.isExtraRuleExist(SmellSettings.SMELL_CARELESSCLEANUP, SmellSettings.EXTRARULE_CARELESSCLEANUP_DETECTISRELEASEIOCODEINDECLAREDMETHOD));
		
		/** 當bad smell的節點存在，也有設定值時 */
		smellSettings.addExtraRule(SmellSettings.SMELL_CARELESSCLEANUP, SmellSettings.EXTRARULE_CARELESSCLEANUP_DETECTISRELEASEIOCODEINDECLAREDMETHOD);
		smellSettings.writeXMLFile(smellSettingFile.getPath());
		assertTrue(smellSettings.isExtraRuleExist(SmellSettings.SMELL_CARELESSCLEANUP, SmellSettings.EXTRARULE_CARELESSCLEANUP_DETECTISRELEASEIOCODEINDECLAREDMETHOD));
	}
	
	@Test
	public void testGetSmellSettings() throws Exception {
		smellSettingFile.createNewFile();
		
		/** 當bad smell的節點不存在時 */
		assertEquals(0, smellSettings.getSmellSettings(SmellSettings.SMELL_DUMMYHANDLER).size());
		
		/** 當bad smell的節點存在，而只有extra rule時 */
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
		
		// 刪除檔案以便下個case測試
		assertTrue(smellSettingFile.delete());
		smellSettings = new SmellSettings(smellSettingFile.getPath());
		
		/** 當bad smell的節點存在，而只有pattern時 */
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
		
		/** 設定檔未選取時，則無任何讀取動作 */
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
		// 先用一個新的instace產生xml檔
		smellSettings = new SmellSettings(smellSettingFile);
		smellSettings.addExtraRule(SmellSettings.SMELL_DUMMYHANDLER, SmellSettings.EXTRARULE_ePrintStackTrace);
		smellSettings.writeXMLFile(smellSettingFile.getPath());
		
		String expectedResult = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><CodeSmells><SmellTypes name=\"DummyHandler\" isDetecting=\"true\"><extraRule name=\"printStackTrace\" /></SmellTypes></CodeSmells>";
		
		// 確認內容
		String firstTimeContent = readFileContents(smellSettingFile);
		assertEquals(expectedResult, firstTimeContent);
		
		// 呼叫activateAllConditions，使其勾選所有設定
		smellSettings.activateAllConditions(smellSettingFile.getPath());

		// 因為檔案存在，所以不會寫入任何新的資訊
		assertEquals(expectedResult, firstTimeContent);
		
		// 刪除已經存在的檔案，並且重新勾選所有設定
		assertTrue(smellSettingFile.delete());
		smellSettings.activateAllConditions(smellSettingFile.getPath());
		
		// 重新確認內容
		String fileContent = readFileContents(smellSettingFile);
		assertEquals(
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?><CodeSmells>" +
				"<SmellTypes name=\"DummyHandler\" isDetecting=\"true\">" +
				"<extraRule name=\"printStackTrace\" /><extraRule name=\"System.err.print\" />" +
				"<extraRule name=\"System.err.println\" /><extraRule name=\"System.out.print\" />" +
				"<extraRule name=\"System.out.println\" /><extraRule name=\"java.util.logging.Logger\" />" +
				"<extraRule name=\"org.apache.log4j\" />" +
				"</SmellTypes><SmellTypes name=\"IgnoreCheckedException\" isDetecting=\"true\" />" +
				"<SmellTypes name=\"NestedTryBlock\" isDetecting=\"true\" />" +
				"<SmellTypes name=\"UnprotectedMainProgram\" isDetecting=\"true\" />" +
				"<SmellTypes name=\"CarelessCleanup\" isDetecting=\"true\">" +
				"<extraRule name=\"DetectIsReleaseIOCodeInDeclaredMethod\" /></SmellTypes>" +
				"<SmellTypes name=\"OverLogging\" isDetecting=\"true\"><extraRule name=\"DetectWrappingExcetion\" />" +
				"<extraRule name=\"java.util.logging.Logger\" /><extraRule name=\"org.apache.log4j\" />" +
				"</SmellTypes></CodeSmells>",
				fileContent);
	}
	
	@Test
	public void testGetAnnotationType() throws Exception {
		assertFalse(smellSettingFile.exists());
		smellSettings.getAnnotationType(SmellSettings.ANNOTATION_ROBUSTNESSLEVEL);;
		smellSettings.writeXMLFile(smellSettingFile.getPath());
		assertTrue(smellSettingFile.exists());
		String fileContent = readFileContents(smellSettingFile);
		assertEquals(
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
				"<CodeSmells><AnnotationTypes name=\"RobustnessLevel\" enable=\"false\" />" +
				"</CodeSmells>", fileContent);	
	}
	
	@Test
	public void testGetAnnotationTypeAttribute() throws Exception {
		assertFalse(smellSettingFile.exists());
		smellSettings.getAnnotationType(SmellSettings.ANNOTATION_ROBUSTNESSLEVEL);;
		smellSettings.writeXMLFile(smellSettingFile.getPath());
		assertTrue(smellSettingFile.exists());
		assertFalse(smellSettings.isAddingRobustnessAnnotation());
	}
}
