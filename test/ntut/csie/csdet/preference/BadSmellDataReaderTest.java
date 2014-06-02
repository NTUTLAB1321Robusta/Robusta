package ntut.csie.csdet.preference;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ntut.csie.csdet.report.BadSmellDataStorage;
import ntut.csie.filemaker.JavaProjectMaker;

public class BadSmellDataReaderTest {
	private File destFolder = null;
	private JavaProjectMaker javaProjectMaker;
	private String projectName = "TestProject";
	private IProject project;
	private File reportDataFile;

	BadSmellDataReader badSmellDataReader;

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

		badSmellDataReader = new BadSmellDataReader(reportDataFile.getAbsolutePath().toString());
	}

	@After
	public void tearDown() throws Exception {
		javaProjectMaker.deleteProject();
	}

	@Test
	public void testGetDateTimeElement() {
		Element dateTimeElement = badSmellDataReader.getDateTimeElement();
		String content = dateTimeElement.getValue().toString();
		assertEquals("4/29/14 9:40:42 PM CST", content);
	}

	@Test
	public void testGetEHSmellListElement() {
		Element smellListElement = badSmellDataReader.getEHSmellListElement();

		Element emptyBadSmellElement = smellListElement.getChild("EmptyCatchBlock");
		String content = emptyBadSmellElement.getValue().toString();
		assertEquals("49", content);

		Element dummyHandlerElement = smellListElement.getChild("DummyHandler");
		content = dummyHandlerElement.getValue().toString();
		assertEquals("120", content);

		Element unprotectedMainProgramElement = smellListElement.getChild("UnprotectedMainProgram");
		content = unprotectedMainProgramElement.getValue().toString();
		assertEquals("8", content);

		Element nestedTryStatementElement = smellListElement.getChild("NestedTryStatement");
		content = nestedTryStatementElement.getValue().toString();
		assertEquals("77", content);

		Element carelessCleanupElement = smellListElement.getChild("CarelessCleanup");
		content = carelessCleanupElement.getValue().toString();
		assertEquals("110", content);

		Element overLoggingElement = smellListElement.getChild("OverLogging");
		content = overLoggingElement.getValue().toString();
		assertEquals("21", content);

		Element thrownExceptionInFinallyBlockElement = smellListElement.getChild("ThrownExceptionInFinallyBlock");
		content = thrownExceptionInFinallyBlockElement.getValue().toString();
		assertEquals("68", content);
	}
}
