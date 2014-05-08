package ntut.csie.csdet.report;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import ntut.csie.csdet.preference.RobustaSettings;
import ntut.csie.filemaker.JavaProjectMaker;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ReportContentCreatorTest {
	
	private File destFolder = null;
	private JavaProjectMaker javaProjectMaker;
	private String projectName = "TestProject";
	private IProject project;
	private File reportDataFile;

	@Before
	public void setUp() throws Exception {
		javaProjectMaker = new JavaProjectMaker(projectName);
		javaProjectMaker.setJREDefaultContainer();
		project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
		
		InputStream input = getClass().getResourceAsStream("/ntut/csie/csdet/report/ExampleBadSmellData.xml");
		reportDataFile = new File(project.getLocation() + "/" + RobustaSettings.SETTING_REPORTFOLDERNAME + "/report/ExampleBadSmellData.xml");
		destFolder = reportDataFile.getParentFile();
		destFolder.mkdirs();
		OutputStream output = null;
		try {
			output = new FileOutputStream(reportDataFile);
			byte[] buf = new byte[1024];
			int bytesRead;
			while ((bytesRead = input.read(buf)) > 0) {
				output.write(buf, 0, bytesRead);
			}
		} finally {
			input.close();
			output.close();
		}
	}

	@After
	public void tearDown() throws Exception {
		javaProjectMaker.deleteProject();
	}

	@Test
	public void testGetResultPath() throws Exception {
		ReportContentCreator contentCreator = new ReportContentCreator(reportDataFile.getCanonicalPath());
		assertTrue(contentCreator.getResultPath().endsWith("index.html"));
		File outputFile = new File(contentCreator.getResultPath()); 
		//Check to see if they are same directory
		assertTrue(outputFile.getParentFile().getCanonicalPath().equals(destFolder.getCanonicalPath()));
		//The folder must be report folder
		assertTrue(destFolder.getCanonicalPath().endsWith("report"));
	}

	@Test
	public void testBuildReportContent() throws Exception {
		ReportContentCreator contentCreator = new ReportContentCreator(reportDataFile.getCanonicalPath());
		//Delete the file if it existed;
		File file = new File(destFolder.getCanonicalPath() + "/js/data.js");
		if(file.exists())
			file.delete();
		contentCreator.buildReportContent();
		//Check to see if the data file was generated
		assertTrue(file.exists());
		File indexFile = new File(destFolder.getCanonicalPath() + "/index.html");
		assertTrue(indexFile.exists());
	}

}
