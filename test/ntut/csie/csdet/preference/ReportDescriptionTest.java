package ntut.csie.csdet.preference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStreamReader;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ReportDescriptionTest {
	private File reportDescriptionFile;
	private ReportDescription reportDescription;
	
	@Before
	public void setUp() throws Exception {
		reportDescriptionFile = new File("./", ReportDescription.SETTING_FILENAME);
		if (reportDescriptionFile.exists()) {
			assertTrue(reportDescriptionFile.delete());
		}
		reportDescription = new ReportDescription(reportDescriptionFile);
	}

	@After
	public void tearDown() throws Exception {
		if (reportDescriptionFile.exists()) {
			assertTrue(reportDescriptionFile.delete());
		}
	}

	@Test
	public void testConstructor_File() throws Exception {
		reportDescription.writeXMLFile(reportDescriptionFile.getPath());
		assertTrue(reportDescriptionFile.exists());
		String fileContent = readFileContents(reportDescriptionFile);
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
				+ "<Reports />", fileContent);
	}

	@Test
	public void testConstructor_String() throws Exception {
		reportDescription = new ReportDescription(reportDescriptionFile);
		reportDescription.writeXMLFile(reportDescriptionFile.getPath());
		assertTrue(reportDescriptionFile.exists());
		String fileContent = readFileContents(reportDescriptionFile);
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
				+ "<Reports />", fileContent);
	}

	private String readFileContents(File file) throws Exception {
		StringBuilder sb = new StringBuilder();
		BufferedReader br = new BufferedReader(new InputStreamReader(
				new FileInputStream(file)));
		String lineContent;
		while ((lineContent = br.readLine()) != null) {
			sb.append(lineContent);
		}
		br.close();
		return sb.toString();
	}

	@Test
	public void getDescription() throws Exception {
		// setting file dose not exist
		reportDescription.getDescription("MonSep16090034CST2013");
		reportDescription.writeXMLFile(reportDescriptionFile.getPath());
		assertTrue(reportDescriptionFile.exists());
		String fileContent = readFileContents(reportDescriptionFile);
		assertEquals(
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
						+ "<Reports><Report name=\"MonSep16090034CST2013\" description=\"\" />"
						+ "</Reports>", fileContent);

		// setting file exist
		reportDescription = new ReportDescription(reportDescriptionFile);
		fileContent = readFileContents(reportDescriptionFile);
		assertEquals(
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
						+ "<Reports><Report name=\"MonSep16090034CST2013\" description=\"\" />"
						+ "</Reports>", fileContent);

		// setting file exist and add new element
		reportDescription = new ReportDescription(reportDescriptionFile);
		reportDescription.getDescription("ThuOct10105702CST2013");
		reportDescription.writeXMLFile(reportDescriptionFile.getPath());
		fileContent = readFileContents(reportDescriptionFile);
		assertEquals(
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
						+ "<Reports><Report name=\"MonSep16090034CST2013\" description=\"\" />"
						+ "<Report name=\"ThuOct10105702CST2013\" description=\"\" />"
						+ "</Reports>", fileContent);
		// setting file exist and add existed element
		reportDescription.getDescription("ThuOct10105702CST2013");
		reportDescription.writeXMLFile(reportDescriptionFile.getPath());
		fileContent = readFileContents(reportDescriptionFile);
		assertEquals(
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
				+ "<Reports><Report name=\"MonSep16090034CST2013\" description=\"\" />"
				+ "<Report name=\"ThuOct10105702CST2013\" description=\"\" />"
				+ "</Reports>", fileContent);
	}

	@Test
	public void testSetProjectDetectAttribute() throws Exception {
		reportDescriptionFile.createNewFile();
		reportDescription.setDescriptionAttribute("MonSep16090034CST2013", "description","report description");
		reportDescription.writeXMLFile(reportDescriptionFile.getPath());
		String fileContent = readFileContents(reportDescriptionFile);
		assertEquals(
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
						+ "<Reports><Report name=\"MonSep16090034CST2013\" description=\"report description\" />"
						+ "</Reports>", fileContent);
	}
	@Test
	public void getDescriptionAttribute() {
		assertFalse(reportDescriptionFile.exists());
		reportDescription.getDescription("MonSep16090034CST2013");
		reportDescription.writeXMLFile(reportDescriptionFile.getPath());
		assertTrue(reportDescriptionFile.exists());
		assertEquals("", reportDescription.getDescriptionAttribute("MonSep16090034CST2013"));
		reportDescription.setDescriptionAttribute("MonSep16090034CST2013",ReportDescription.ATTRIBUTE_DESCRIPTION,"report description");
		assertEquals("report description", reportDescription.getDescriptionAttribute("MonSep16090034CST2013"));
		
	}
	@Test 
	public void deleteElementInXml() throws Exception{
		assertFalse(reportDescriptionFile.exists());
		reportDescription.getDescription("MonSep16090034CST2013");
		reportDescription.setDescriptionAttribute("MonSep16090034CST2013",ReportDescription.ATTRIBUTE_DESCRIPTION, "new description");
		reportDescription.writeXMLFile(reportDescriptionFile.getPath());
		reportDescription.setDescriptionAttribute("ThuOct10105702CST2013",ReportDescription.ATTRIBUTE_DESCRIPTION, "create on Thu");
		reportDescription.writeXMLFile(reportDescriptionFile.getPath());
		String fileContent = readFileContents(reportDescriptionFile);
		assertEquals(
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
				+ "<Reports><Report name=\"MonSep16090034CST2013\" description=\"new description\" />"
				+ "<Report name=\"ThuOct10105702CST2013\" description=\"create on Thu\" />"
				+ "</Reports>", fileContent);
		reportDescription.deleteElementInXml("MonSep16090034CST2013");
		reportDescription.writeXMLFile(reportDescriptionFile.getPath());
		fileContent = readFileContents(reportDescriptionFile);
		assertEquals(
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
				+ "<Reports>"
				+ "<Report name=\"ThuOct10105702CST2013\" description=\"create on Thu\" />"
				+ "</Reports>", fileContent);
	}

	@Test
	public void testWriteXMLFile() throws Exception {
		reportDescription.writeXMLFile(reportDescriptionFile.getPath());
		assertTrue(reportDescriptionFile.exists());
		String fileContent=readFileContents(reportDescriptionFile);
		assertEquals(
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?><Reports />",
				fileContent);
	}
	
	@Test
	public void testWriteXMLFile_OverwriteNonXMLFormatFile()
			throws Exception{
		String chineseString = "天地玄黃宇宙洪荒";
		FileWriter fw = new FileWriter(reportDescriptionFile);
		fw.write(chineseString);
		fw.close();
		// 確認檔案裡面的中文字
		String chineseContent = readFileContents(reportDescriptionFile);
		assertEquals(chineseString, chineseContent);

		// 生成XML檔案
		reportDescription.writeXMLFile(reportDescriptionFile.getPath());
		// 檢查檔案是否生成
		assertTrue(reportDescriptionFile.exists());

		// 檢查檔案內容是否正確
		String fileContent = readFileContents(reportDescriptionFile);
		assertEquals(
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?><Reports />",
				fileContent);
	}
	
	@Test
	public void testWriteNewXMLFile_OtherTextWriterWriteRobustaSettingXMLFormatFileAfterSmellSettingInstanceIsCreated()
			throws Exception {
		String xmlString = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><Reports><Report name=\"MonSep16090034CST2013\" description=\"aaaa\"></report></Reports>";
		FileWriter fw = new FileWriter(reportDescriptionFile);
		fw.write(xmlString);
		fw.close();
		String xmlReadContent = readFileContents(reportDescriptionFile);
		assertEquals(xmlString, xmlReadContent);
		reportDescription.writeXMLFile(reportDescriptionFile.getPath());
		assertTrue(reportDescriptionFile.exists());
		String fileContent = readFileContents(reportDescriptionFile);
		assertEquals(
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?><Reports />",
				fileContent);

	}

	

}
