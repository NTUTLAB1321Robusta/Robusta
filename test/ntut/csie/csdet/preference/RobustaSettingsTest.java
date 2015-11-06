package ntut.csie.csdet.preference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStreamReader;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.eclipse.core.resources.ResourcesPlugin;

public class RobustaSettingsTest {
	private File robustaSettingFile;
	private RobustaSettings robustaSettings;
	private IProject project;

	@Before
	public void setUp() throws Exception {
		robustaSettingFile = new File("./", RobustaSettings.SETTING_FILENAME);
		if (robustaSettingFile.exists()) {
			assertTrue(robustaSettingFile.delete());
		}
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IWorkspaceRoot root = workspace.getRoot();
		project  = root.getProject("FirstTest");
		robustaSettings = new RobustaSettings(robustaSettingFile, project);
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
				+ "<RobustaSettings />", fileContent);
	}

	@Test
	public void testConstructor_String() throws Exception {
		robustaSettings = new RobustaSettings(robustaSettingFile, project);
		robustaSettings.writeNewXMLFile(robustaSettingFile.getPath());
		assertTrue(robustaSettingFile.exists());
		String fileContent = readFileContents(robustaSettingFile);
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
				+ "<RobustaSettings />", fileContent);
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
						+ "<RobustaSettings><ProjectDetect FolderName=\"src\" enable=\"true\" />"
						+ "</RobustaSettings>", fileContent);

		// setting file exist
		robustaSettings = new RobustaSettings(robustaSettingFile, project);
		fileContent = readFileContents(robustaSettingFile);
		assertEquals(
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
						+ "<RobustaSettings><ProjectDetect FolderName=\"src\" enable=\"true\" />"
						+ "</RobustaSettings>", fileContent);

		// setting file exist and add new element
		robustaSettings = new RobustaSettings(robustaSettingFile, project);
		robustaSettings.getProjectDetect("srcTest");
		robustaSettings.writeNewXMLFile(robustaSettingFile.getPath());
		fileContent = readFileContents(robustaSettingFile);
		assertEquals(
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
						+ "<RobustaSettings><ProjectDetect FolderName=\"src\" enable=\"true\" />"
						+ "<ProjectDetect FolderName=\"srcTest\" enable=\"true\" />"
						+ "</RobustaSettings>", fileContent);

		// setting file exist and add existed element
		robustaSettings.getProjectDetect("srcTest");
		robustaSettings.writeNewXMLFile(robustaSettingFile.getPath());
		fileContent = readFileContents(robustaSettingFile);
		assertEquals(
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
						+ "<RobustaSettings><ProjectDetect FolderName=\"src\" enable=\"true\" />"
						+ "<ProjectDetect FolderName=\"srcTest\" enable=\"true\" />"
						+ "</RobustaSettings>", fileContent);

	}

	@Test
	public void testSetProjectDetectAttribute() throws Exception {
		robustaSettingFile.createNewFile();
		robustaSettings.setProjectDetectAttribute("src", "enable", false);
		robustaSettings.setProjectDetectAttribute("srcTest", "enable", false);
		robustaSettings.writeNewXMLFile(robustaSettingFile.getPath());
		String fileContent = readFileContents(robustaSettingFile);
		assertEquals(
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
						+ "<RobustaSettings><ProjectDetect FolderName=\"src\" enable=\"false\" />"
						+ "<ProjectDetect FolderName=\"srcTest\" enable=\"false\" />"
						+ "</RobustaSettings>", fileContent);
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
		assertTrue(robustaSettingFile.exists());

		String fileContent = readFileContents(robustaSettingFile);
		assertEquals(
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?><RobustaSettings />",
				fileContent);
	}

	@Test
	public void testWriteNewXMLFile_OverwriteNonXMLFormatFile()
			throws Exception {
		// generate a text file with Chinese character
		String chineseString = "天地玄黃宇宙洪荒";
		FileWriter fw = new FileWriter(robustaSettingFile);
		fw.write(chineseString);
		fw.close();
		String chineseContent = readFileContents(robustaSettingFile);
		assertEquals(chineseString, chineseContent);

		robustaSettings.writeNewXMLFile(robustaSettingFile.getPath());
		assertTrue(robustaSettingFile.exists());

		String fileContent = readFileContents(robustaSettingFile);
		assertEquals(
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?><RobustaSettings />",
				fileContent);
	}

	@Test
	public void testWriteNewXMLFile_OtherTextWriterWriteRobustaSettingXMLFormatFileAfterSmellSettingInstanceIsCreated()
			throws Exception {
		String xmlString = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><RobustaSettings><ProjectDetect FolderName=\"src\" enable=\"true\"></ProjectDetect></RobustaSettings>";
		FileWriter fw = new FileWriter(robustaSettingFile);
		fw.write(xmlString);
		fw.close();
		String xmlReadContent = readFileContents(robustaSettingFile);
		assertEquals(xmlString, xmlReadContent);

		robustaSettings.writeNewXMLFile(robustaSettingFile.getPath());
		assertTrue(robustaSettingFile.exists());

		String fileContent = readFileContents(robustaSettingFile);
		assertEquals(
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?><RobustaSettings />",
				fileContent);

	}
}
