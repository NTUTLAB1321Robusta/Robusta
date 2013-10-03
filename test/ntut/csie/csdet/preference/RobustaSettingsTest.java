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

public class RobustaSettingsTest {
	private File robustaSettingFile;
	private RobustaSettings robustaSettings;

	@Before
	public void setUp() throws Exception {
		robustaSettingFile = new File("./", RobustaSettings.SETTING_FILENAME);
		if (robustaSettingFile.exists()) {
			assertTrue(robustaSettingFile.delete());
		}
		robustaSettings = new RobustaSettings(robustaSettingFile, "FirstTest");
	}

	@After
	public void tearDown() throws Exception {
		if (robustaSettingFile.exists()) {
			assertTrue(robustaSettingFile.delete());
		}
	}

	@Test
	public void testConstructor_File() throws Exception {
		robustaSettings.writeNewXMLFile(robustaSettingFile.getPath());
		assertTrue(robustaSettingFile.exists());
		String fileContent = readFileContents(robustaSettingFile);
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
				+ "<RobustaProjectSettings />", fileContent);
	}

	@Test
	public void testConstructor_String() throws Exception {
		robustaSettings = new RobustaSettings(robustaSettingFile, "FirstTest");
		robustaSettings.writeNewXMLFile(robustaSettingFile.getPath());
		assertTrue(robustaSettingFile.exists());
		String fileContent = readFileContents(robustaSettingFile);
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
				+ "<RobustaProjectSettings />", fileContent);
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
	public void testGetProjectDetect() throws Exception {
		// setting file dose not exist
		robustaSettings.getProjectDetect("src");
		robustaSettings.writeNewXMLFile(robustaSettingFile.getPath());
		assertTrue(robustaSettingFile.exists());
		String fileContent = readFileContents(robustaSettingFile);
		assertEquals(
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
						+ "<RobustaProjectSettings><ProjectDetect FolderName=\"src\" enable=\"true\" />"
						+ "</RobustaProjectSettings>", fileContent);

		// setting file exist
		robustaSettings = new RobustaSettings(robustaSettingFile, "FirstTest");
		fileContent = readFileContents(robustaSettingFile);
		assertEquals(
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
						+ "<RobustaProjectSettings><ProjectDetect FolderName=\"src\" enable=\"true\" />"
						+ "</RobustaProjectSettings>", fileContent);

		// setting file exist and add new element
		robustaSettings = new RobustaSettings(robustaSettingFile, "FirstTest");
		robustaSettings.getProjectDetect("srcTest");
		robustaSettings.writeNewXMLFile(robustaSettingFile.getPath());
		fileContent = readFileContents(robustaSettingFile);
		assertEquals(
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
						+ "<RobustaProjectSettings><ProjectDetect FolderName=\"src\" enable=\"true\" />"
						+ "<ProjectDetect FolderName=\"srcTest\" enable=\"true\" />"
						+ "</RobustaProjectSettings>", fileContent);

		// setting file exist and add existed element
		robustaSettings.getProjectDetect("srcTest");
		robustaSettings.writeNewXMLFile(robustaSettingFile.getPath());
		fileContent = readFileContents(robustaSettingFile);
		assertEquals(
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
						+ "<RobustaProjectSettings><ProjectDetect FolderName=\"src\" enable=\"true\" />"
						+ "<ProjectDetect FolderName=\"srcTest\" enable=\"true\" />"
						+ "</RobustaProjectSettings>", fileContent);

	}

	@Test
	public void testSetProjectDetectAttribute() throws Exception {
		robustaSettingFile.createNewFile();
		robustaSettings.setProjectDetectAttribute("src", "enable", false);
		robustaSettings.setProjectDetectAttribute("srcTest", "enable", false);
		robustaSettings.writeNewXMLFile(robustaSettingFile.getPath());
		// 驗證結果是否正確
		String fileContent = readFileContents(robustaSettingFile);
		assertEquals(
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
						+ "<RobustaProjectSettings><ProjectDetect FolderName=\"src\" enable=\"false\" />"
						+ "<ProjectDetect FolderName=\"srcTest\" enable=\"false\" />"
						+ "</RobustaProjectSettings>", fileContent);
	}

	@Test
	public void getProjectDetectAttribute() {
		assertFalse(robustaSettingFile.exists());
		robustaSettings.getProjectDetect("src");
		robustaSettings.writeNewXMLFile(robustaSettingFile.getPath());
		assertTrue(robustaSettingFile.exists());
		assertTrue(robustaSettings.getProjectDetectAttribute("src"));
	}

	@Test
	public void testWriteNewXMLFile() throws Exception {
		robustaSettings.writeNewXMLFile(robustaSettingFile.getPath());
		// 檢查檔案是否生成
		assertTrue(robustaSettingFile.exists());

		// 檢查檔案內容是否正確
		String fileContent = readFileContents(robustaSettingFile);
		assertEquals(
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?><RobustaProjectSettings />",
				fileContent);
	}

	@Test
	public void testWriteNewXMLFile_OverwriteNonXMLFormatFile()
			throws Exception {
		// 生成一個文字檔案，裡面都是中文字
		String chineseString = "天地玄黃宇宙洪荒";
		FileWriter fw = new FileWriter(robustaSettingFile);
		fw.write(chineseString);
		fw.close();
		// 確認檔案裡面的中文字
		String chineseContent = readFileContents(robustaSettingFile);
		assertEquals(chineseString, chineseContent);

		// 生成XML檔案
		robustaSettings.writeNewXMLFile(robustaSettingFile.getPath());
		// 檢查檔案是否生成
		assertTrue(robustaSettingFile.exists());

		// 檢查檔案內容是否正確
		String fileContent = readFileContents(robustaSettingFile);
		assertEquals(
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?><RobustaProjectSettings />",
				fileContent);
	}

	@Test
	public void testWriteNewXMLFile_OtherTextWriterWriteRobustaSettingXMLFormatFileAfterSmellSettingInstanceIsCreated()
			throws Exception {
		// 生成一個文字檔案，裡面是舊有的XML設定檔
		String xmlString = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><RobustaProjectSettings><ProjectDetect FolderName=\"src\" enable=\"true\"></ProjectDetect></RobustaProjectSettings>";
		FileWriter fw = new FileWriter(robustaSettingFile);
		fw.write(xmlString);
		fw.close();
		// 確認檔案裡面的XML設定檔內容
		String xmlReadContent = readFileContents(robustaSettingFile);
		assertEquals(xmlString, xmlReadContent);

		// 生成XML檔案
		robustaSettings.writeNewXMLFile(robustaSettingFile.getPath());
		// 檢查檔案是否生成
		assertTrue(robustaSettingFile.exists());

		// 檢查檔案內容是否正確
		String fileContent = readFileContents(robustaSettingFile);
		assertEquals(
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?><RobustaProjectSettings />",
				fileContent);

	}

}
