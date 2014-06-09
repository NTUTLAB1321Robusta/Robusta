package ntut.csie.csdet.report;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import ntut.csie.csdet.preference.RobustaSettings;
import ntut.csie.filemaker.JavaProjectMaker;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class PastReportHistoryTest {
	private File fistReportFolder = null;
	private File secondReportFolder = null;
	private File reportFolder = null;
	private JavaProjectMaker javaProjectMaker;
	private String projectName = "TestProject";
	private IProject project;
	private File firstReportDataFile;
	private File secondReportDataFile;
	private File thirdReportDataFile;
	InputStream input;

	@Before
	public void setUp() throws Exception {
		javaProjectMaker = new JavaProjectMaker(projectName);
		javaProjectMaker.setJREDefaultContainer();
		project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
		input = getClass().getResourceAsStream("/ntut/csie/csdet/report/ExampleBadSmellData.xml");
	}

	@After
	public void tearDown() throws Exception {
		javaProjectMaker.deleteProject();
	}

	@Test
	public void testGetFileList() throws Exception {

		firstReportDataFile = new File(project.getLocation() + "/" + RobustaSettings.SETTING_REPORTFOLDERNAME + "/FirstReport/FirstBadSmellData.xml");
		fistReportFolder = firstReportDataFile.getParentFile();

		secondReportDataFile = new File(project.getLocation() + "/" + RobustaSettings.SETTING_REPORTFOLDERNAME + "/SecondReport/SecondBadSmellData.xml");
		secondReportFolder = secondReportDataFile.getParentFile();

		thirdReportDataFile = new File(project.getLocation() + "/" + RobustaSettings.SETTING_REPORTFOLDERNAME + "/ThirdBadSmellData.xml");

		reportFolder = new File(project.getLocation() + "/" + RobustaSettings.SETTING_REPORTFOLDERNAME + "/report");

		PastReportsHistory pastReportsHistory = new PastReportsHistory();
		List<File> files = pastReportsHistory.getFileList(projectName);
		assertEquals(0, files.size());

		fistReportFolder.mkdirs();
		secondReportFolder.mkdirs();
		reportFolder.mkdirs();

		OutputStream output1 = null;
		OutputStream output2 = null;
		OutputStream output3 = null;
		try {
			output1 = new FileOutputStream(firstReportDataFile);
			output2 = new FileOutputStream(secondReportDataFile);
			output3 = new FileOutputStream(thirdReportDataFile);
			byte[] buf = new byte[1024];
			int bytesRead;
			while ((bytesRead = input.read(buf)) > 0) {
				output1.write(buf, 0, bytesRead);
				output2.write(buf, 0, bytesRead);
				output3.write(buf, 0, bytesRead);
			}
		} finally {
			input.close();
			output1.close();
			output2.close();
			output3.close();
		}
		files = pastReportsHistory.getFileList(projectName);
		assertEquals(2, files.size());
	}
}
