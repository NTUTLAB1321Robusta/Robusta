package ntut.csie.csdet.preference;

import java.io.File;

import junit.framework.TestCase;
import ntut.csie.csdet.fixture.FixtureManager;

import org.eclipse.jdt.core.IType;
import org.jdom.Document;
import org.jdom.Element;

public class JDomUtilTest extends TestCase {
	FixtureManager fm;
	IType type;
	private String root = "CodeSmellDetect";
	private String DummyHandlerTag = "DummyHandler";
	protected void setUp() throws Exception {
		super.setUp();
		fm = new FixtureManager();
		fm.createProject("MM");
		type = fm.getProject().getIType("a.b.c.MM");
		
	}

	protected void tearDown() throws Exception {
		super.tearDown();
		String workpsace = JDomUtil.getWorkspace();
		workpsace += File.separator + "CSPreference.xml";
		File file = new File(workpsace);
		if(file.exists())
			file.delete();
		fm.dispose();
	}

	public void testCreateXMLContent() {
		Element root = JDomUtil.createXMLContent();
		Element dummyHandler= new Element(DummyHandlerTag);
		root.addContent(dummyHandler);
		String workpsace = JDomUtil.getWorkspace();
		workpsace += File.separator + "CSPreference.xml";
		JDomUtil.OutputXMLFile(root.getDocument(), workpsace);
		assertEquals(this.root, root.getName());
	}

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
	
	// 假設檔案不存在的話
	public void testReadXMLFile_1() {
		Document docJDOM = JDomUtil.readXMLFile();
		assertEquals(null, docJDOM);
		
	}
	
	// 假設檔案存在的話
	public void testReadXMLFile_2(){
		// 自己先製造一個xml file
		Element elementRoot = new Element(root);
		Document docJDOM = new Document(elementRoot);
		String workpsace = JDomUtil.getWorkspace();
		workpsace += File.separator + "CSPreference.xml";
		JDomUtil.OutputXMLFile(docJDOM, workpsace);
		File file = new File(workpsace);
		assertTrue(file.exists());		
	}

}
