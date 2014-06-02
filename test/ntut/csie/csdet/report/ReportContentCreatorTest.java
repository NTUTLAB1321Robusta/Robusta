package ntut.csie.csdet.report;

import static org.junit.Assert.assertTrue;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import ntut.csie.csdet.preference.RobustaSettings;
import ntut.csie.filemaker.JavaProjectMaker;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.jdom.Document;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ReportContentCreatorTest {

	private File destFolder = null;
	private JavaProjectMaker javaProjectMaker;
	private String projectName = "TestProject";
	private IProject project;
	private File reportDataFile;

	ReportContentCreator reportContentCreator;
	ReportContentCreator trendReportContentCreator;

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

		Document inputXmlReportDoc;
		SAXBuilder builder = new SAXBuilder();
		try {
			inputXmlReportDoc = builder.build(reportDataFile);
		} catch (JDOMException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		String JS_DATA_PATH = "/js/data.js";
		String REPORT_DATA_TRANSFORM = "/report/datatransform.xsl";
		reportContentCreator = new ReportContentCreator(JS_DATA_PATH, REPORT_DATA_TRANSFORM, inputXmlReportDoc, projectName);

		Document inputXmlTrendReportDoc = null;
		String JS_TRENDREPORTDATA_PATH = "/js/datatrend.js";
		String TRENDREPORT_DATA_TRANSFORM = "/report/trenddatatransform.xsl";
		PastReportsHistory pastReportsHistory = new PastReportsHistory();
		List<File> files = pastReportsHistory.getFileList(projectName);
		TrendReportDocument trendReportDocument = new TrendReportDocument(projectName);
		inputXmlTrendReportDoc = trendReportDocument.collectTrendReportData(files);
		trendReportContentCreator = new ReportContentCreator(JS_TRENDREPORTDATA_PATH, TRENDREPORT_DATA_TRANSFORM, inputXmlTrendReportDoc, projectName);
	}

	@After
	public void tearDown() throws Exception {
		javaProjectMaker.deleteProject();
	}

	@Test
	public void testGetDestinationFolderPath() {
		String destFolderPath = reportContentCreator.getDestinationFolderPath();
		assertTrue(destFolderPath.endsWith("report"));
	}

	@Test
	public void testTransformDataFile() throws IOException {
		// For report
		File file = new File(destFolder.getCanonicalPath() + "/js/data.js");
		if (file.exists())
			file.delete();
		reportContentCreator.exportReportResources();
		reportContentCreator.transformDataFile();
		assertTrue(file.exists());
		File indexFile = new File(destFolder.getAbsolutePath() + "/index.html");
		assertTrue(indexFile.exists());

		// For Trend Report
		file = new File(destFolder.getCanonicalPath() + "/js/datatrend.js");
		if (file.exists())
			file.delete();
		trendReportContentCreator.exportReportResources();
		trendReportContentCreator.transformDataFile();
		assertTrue(file.exists());
		File trendReportFile = new File(destFolder.getAbsolutePath() + "/trendreport.html");
		assertTrue(trendReportFile.exists());
	}

}