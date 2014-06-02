package ntut.csie.csdet.report;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import ntut.csie.csdet.preference.RobustaSettings;
import ntut.csie.filemaker.JavaProjectMaker;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.jdom.Document;
import org.jdom.Element;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TrendReportDocumentTest {
	private File fistReportFolder = null;
	private File secondReportFolder = null;
	private JavaProjectMaker javaProjectMaker;
	private String projectName = "TestProject";
	private IProject project;
	private File firstReportDataFile;
	private File secondReportDataFile;
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
	public void testCollectTrendReportData() throws Exception {
		firstReportDataFile = new File(project.getLocation() + "/" + RobustaSettings.SETTING_REPORTFOLDERNAME + "/FirstReport/FirstBadSmellData.xml");
		fistReportFolder = firstReportDataFile.getParentFile();
		fistReportFolder.mkdirs();

		secondReportDataFile = new File(project.getLocation() + "/" + RobustaSettings.SETTING_REPORTFOLDERNAME + "/SecondReport/SecondBadSmellData.xml");
		secondReportFolder = secondReportDataFile.getParentFile();
		secondReportFolder.mkdirs();

		PastReportsHistory pastReportsHistory = new PastReportsHistory();
		List<File> files = pastReportsHistory.getFileList(projectName);
		TrendReportDocument trendReportDocument = new TrendReportDocument(projectName);
		Document document = trendReportDocument.collectTrendReportData(files);
		Element root = document.getRootElement();
		assertEquals("TrendReports", root.getName());
		List<Element> childs = root.getChildren();
		assertEquals(1, childs.size());

		OutputStream output1 = null;
		OutputStream output2 = null;
		try {
			output1 = new FileOutputStream(firstReportDataFile);
			output2 = new FileOutputStream(secondReportDataFile);
			byte[] buf = new byte[1024];
			int bytesRead;
			while ((bytesRead = input.read(buf)) > 0) {
				output1.write(buf, 0, bytesRead);
				output2.write(buf, 0, bytesRead);
			}
		} finally {
			input.close();
			output1.close();
			output2.close();
		}

		files = pastReportsHistory.getFileList(projectName);
		document = trendReportDocument.collectTrendReportData(files);
		root = document.getRootElement();
		List<Element> reportElements = root.getChildren();
		assertEquals(3, reportElements.size());
	}
}
