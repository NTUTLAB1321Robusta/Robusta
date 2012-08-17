package ntut.csie.csdet.preference;

import static org.junit.Assert.*;

import java.io.File;

import ntut.csie.csdet.fixture.FixtureManager;

import org.eclipse.jdt.core.IType;
import org.jdom.Document;
import org.jdom.Element;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class JDomUtilTest {
	// �o��FixtureManager�O�M���Ψӻs�y���եαM�ת��C�bShiau���e���Ǫ��g���C
	FixtureManager fm;
	IType type;
	private String root = "CodeSmellDetect";
	private String DummyHandlerTag = "DummyHandler";
	
	@Before
	public void setUp() throws Exception {
		fm = new FixtureManager();
		fm.createProject("MM");
		type = fm.getProject().getIType("a.b.c.MM");
	}

	@After
	public void tearDown() throws Exception {
		String workpsace = JDomUtil.getWorkspace();
		workpsace += File.separator + "CSPreference.xml";
		File file = new File(workpsace);
		if(file.exists())
			file.delete();
		fm.dispose();
	}

	@Test
	public void testCreateXMLContent() {
		Element root = JDomUtil.createXMLContent();
		Element dummyHandler= new Element(DummyHandlerTag);
		root.addContent(dummyHandler);
		String workpsace = JDomUtil.getWorkspace();
		workpsace += File.separator + "CSPreference.xml";
		JDomUtil.OutputXMLFile(root.getDocument(), workpsace);
		assertEquals(this.root, root.getName());
	}

	@Test
	public void testOutputXMLFile() {
		Element root = JDomUtil.createXMLContent();
		Element dummyHandler= new Element(DummyHandlerTag);
		root.addContent(dummyHandler);
		String workspace = JDomUtil.getWorkspace();
		workspace += File.separator + "CSPreference.xml";
		JDomUtil.OutputXMLFile(root.getDocument(), workspace);
		File file = new File(workspace);
		assertTrue(file.exists());
	}
	
	/**
	 * ����XML�ɮפ��s�b������
	 */
	@Test
	public void testReadXMLFile_1() {
		Document docJDOM = JDomUtil.readXMLFile();
		assertEquals(null, docJDOM);
		
	}
	
	/**
	 * ���]�w�g���@��XML�ɮצs�b
	 */
	@Test
	public void testReadXMLFile_2(){
		// �ۤv���s�y�@��xml file
		Element elementRoot = new Element(root);
		Document docJDOM = new Document(elementRoot);
		String workpsace = JDomUtil.getWorkspace();
		workpsace += File.separator + "CSPreference.xml";
		JDomUtil.OutputXMLFile(docJDOM, workpsace);
		File file = new File(workpsace);
		assertTrue(file.exists());		
	}
}
