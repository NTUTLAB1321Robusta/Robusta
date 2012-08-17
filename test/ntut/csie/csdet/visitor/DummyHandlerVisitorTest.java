package ntut.csie.csdet.visitor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;

import ntut.csie.csdet.preference.JDomUtil;
import ntut.csie.csdet.preference.SmellSettings;
import ntut.csie.filemaker.JavaFileToString;
import ntut.csie.filemaker.JavaProjectMaker;
import ntut.csie.filemaker.exceptionBadSmells.DummyAndIgnoreExample;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class DummyHandlerVisitorTest {
	JavaFileToString jfs;
	JavaProjectMaker jpm;
	CompilationUnit unit;
	DummyHandlerVisitor dummyhandlerBSV;
	SmellSettings smellSettings;
	
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
		jpm.addJarFromProjectToBuildPath("lib\\log4j-1.2.15.jar");
		// 根據測試檔案樣本內容建立新的檔案
		jpm.createJavaFile("ntut.csie.exceptionBadSmells", "DummyAndIgnoreExample.java", "package ntut.csie.exceptionBadSmells;\n" + jfs.getFileContent());
		// 建立XML
		CreateSettings();
		
		Path path = new Path("DummyHandlerTest\\src\\ntut\\csie\\exceptionBadSmells\\DummyAndIgnoreExample.java");
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
	public void testVisitNode() {
		int dummy = 0;
		unit.accept(dummyhandlerBSV);
		if(dummyhandlerBSV.getDummyList() != null)
			dummy = dummyhandlerBSV.getDummyList().size();
		
		// 驗證總共抓到幾個bad smell
		assertEquals(14, dummy);
	}
	
	private void CreateSettings() {
		smellSettings = new SmellSettings(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
		smellSettings.addExtraRule(SmellSettings.SMELL_DUMMYHANDLER, SmellSettings.EXTRARULE_ePrintStackTrace);
		smellSettings.addExtraRule(SmellSettings.SMELL_DUMMYHANDLER, SmellSettings.EXTRARULE_SystemErrPrint);
		smellSettings.addExtraRule(SmellSettings.SMELL_DUMMYHANDLER, SmellSettings.EXTRARULE_SystemErrPrintln);
		smellSettings.addExtraRule(SmellSettings.SMELL_DUMMYHANDLER, SmellSettings.EXTRARULE_SystemOutPrint);
		smellSettings.addExtraRule(SmellSettings.SMELL_DUMMYHANDLER, SmellSettings.EXTRARULE_SystemOutPrintln);
		smellSettings.addExtraRule(SmellSettings.SMELL_DUMMYHANDLER, SmellSettings.EXTRARULE_JavaUtilLoggingLogger);
		smellSettings.addExtraRule(SmellSettings.SMELL_DUMMYHANDLER, SmellSettings.EXTRARULE_OrgApacheLog4j);
		smellSettings.writeXMLFile(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
	}
}
