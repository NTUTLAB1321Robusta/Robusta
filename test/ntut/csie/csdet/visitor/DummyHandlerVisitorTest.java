package ntut.csie.csdet.visitor;

import static org.junit.Assert.*;

import java.io.File;
import ntut.csie.csdet.preference.JDomUtil;
import ntut.csie.filemaker.JavaFileToString;
import ntut.csie.filemaker.JavaProjectMaker;
import ntut.csie.filemaker.exceptionBadSmells.DummyAndIgnoreExample;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.jdom.Element;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class DummyHandlerVisitorTest {
	JavaFileToString jfs;
	JavaProjectMaker jpm;
	CompilationUnit unit;
	DummyHandlerVisitor dummyhandlerBSV;
	
	public DummyHandlerVisitorTest() {
	}
	
	@Before
	public void setUp() throws Exception {
		// 讀取測試檔案樣本內容
		jfs = new JavaFileToString();
		jfs.read(DummyAndIgnoreExample.class, "test");
		
		jpm = new JavaProjectMaker("DummyHandlerTest");
		jpm.setJREDefaultContainer();
		// 新增欲載入的library
		jpm.addJarToBuildPath("lib\\log4j-1.2.15.jar");
		// 根據測試檔案樣本內容建立新的檔案
		jpm.createJavaFile("ntut.csie.exceptionBadSmells", "DummyHandlerExample.java", "package ntut.csie.exceptionBadSmells;\n" + jfs.getFileContent());
		// 建立XML
		CreateDummyHandlerXML();
		
		Path path = new Path("DummyHandlerTest\\src\\ntut\\csie\\exceptionBadSmells\\DummyHandlerExample.java");
		//Create AST to parse
		ASTParser parser = ASTParser.newParser(AST.JLS3);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		// 設定要被建立AST的檔案
		parser.setSource(JavaCore.createCompilationUnitFrom(ResourcesPlugin.getWorkspace().getRoot().getFile(path)));
		parser.setResolveBindings(true);
		// 取得AST
		unit = (CompilationUnit) parser.createAST(null); 
		unit.recordModifications();
		dummyhandlerBSV = new DummyHandlerVisitor(unit);
	}

	@After
	public void tearDown() throws Exception {
		File xmlFile = new File(JDomUtil.getWorkspace() + File.separator + "CSPreference.xml");
		// 如果xml檔案存在，則刪除之
		if(xmlFile.exists())
			assertTrue(xmlFile.delete());
		// 刪除專案
		jpm.deleteProject();
	}

	@Test
	public void testGetSpecifiedParentNode() {
		ASTNode result = dummyhandlerBSV.getSpecifiedParentNode(unit, ASTNode.COMPILATION_UNIT);
		assertNull(result);
	}
	
	@Test
	public void testVisitNode() {
		int dummy = 0;
		unit.accept(dummyhandlerBSV);
		if(dummyhandlerBSV.getDummyList() != null)
			dummy = dummyhandlerBSV.getDummyList().size();
		
		// 驗證總共抓到幾個bad smell
		assertEquals(14, dummy);
	}
	
	/**
	 * 建立CSPreference.xml檔案
	 */
	private void CreateDummyHandlerXML() {
		//取的XML的root
		Element root = JDomUtil.createXMLContent();

		//建立Dummy Handler的Tag
		Element dummyHandler = new Element(JDomUtil.DummyHandlerTag);
		Element rule = new Element("rule");
		//假如e.printStackTrace有被勾選起來
		rule.setAttribute(JDomUtil.e_printstacktrace,"Y");

		//假如system.out.println有被勾選起來
		rule.setAttribute(JDomUtil.systemout_print,"Y");
		
		rule.setAttribute(JDomUtil.apache_log4j,"Y");
		rule.setAttribute(JDomUtil.java_Logger,"Y");

		//把使用者自訂的Rule存入XML
		Element libRule = new Element("librule");
		
		//將新建的tag加進去
		dummyHandler.addContent(rule);
		dummyHandler.addContent(libRule);

		if (root.getChild(JDomUtil.DummyHandlerTag) != null)
			root.removeChild(JDomUtil.DummyHandlerTag);

		root.addContent(dummyHandler);

		//將檔案寫回
		String path = JDomUtil.getWorkspace() + File.separator + "CSPreference.xml";
		JDomUtil.OutputXMLFile(root.getDocument(), path);
	}
}
