package ntut.csie.csdet.report;

import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;

import ntut.csie.csdet.preference.RobustaSettings;
import ntut.csie.filemaker.JavaProjectMaker;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class BadSmellDataStorageTest {
	private JavaProjectMaker javaProjectMaker;
	private String projectName = "TestProject";
	private IProject project;
	private BadSmellDataStorage dataStorage;

	@Before
	public void setUp() throws Exception {
		javaProjectMaker = new JavaProjectMaker(projectName);
		javaProjectMaker.setJREDefaultContainer();
		project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
		dataStorage = new BadSmellDataStorage(project.getLocation().toString());
	}

	@After
	public void tearDown() throws Exception {
		javaProjectMaker.deleteProject();
	}

	@Test
	public void testGetRobustaReportPath() {
		String robustaReportFolder = project.getLocation() + "/Robusta_Report";
		assertTrue(dataStorage.getRobustaReportPath().equals(robustaReportFolder));
	}

	@Test
	public void testGetFilePath() {
		String myFileName = "filename.ext";
		String pattern = "^.*/[0-9]+/[0-9]+_filename.ext$";
		assertTrue(dataStorage.getFilePath(myFileName, true).matches(pattern));
	}

	@Test
	public void testGetResultDataPath() {
		String pattern = "^.*Robusta_Report/[0-9]+/[0-9]+_BSData.xml$";
		assertTrue(dataStorage.getResultDataPath().matches(pattern));
	}

	@Test
	public void testSave() throws Exception {
		ReportModel model = new ReportModel();
		dataStorage.save(model);
		File outputDataFile = new File(dataStorage.getResultDataPath());
		BufferedReader reader = new BufferedReader(new FileReader(outputDataFile));
		StringBuilder results = new StringBuilder();
		String line;
		while((line = reader.readLine()) != null) {
			results.append(line);
		}
		String expectStart = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><EHSmellReport>";
		String expectEnd = "</EHSmellReport>";
		String resultContent = results.toString();
		assertTrue(resultContent.startsWith(expectStart));
		assertTrue(resultContent.endsWith(expectEnd));
		reader.close();
	}
}
