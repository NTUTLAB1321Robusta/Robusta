package ntut.csie.csdet.quickfix;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import ntut.csie.csdet.preference.SmellSettings;
import ntut.csie.csdet.visitor.UserDefinedMethodAnalyzer;
import ntut.csie.filemaker.JavaFileToString;
import ntut.csie.filemaker.JavaProjectMaker;
import ntut.csie.filemaker.RuntimeEnvironmentProjectReader;
import ntut.csie.filemaker.exceptionBadSmells.DummyAndIgnoreExample;
import ntut.csie.robusta.util.PathUtils;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.ltk.core.refactoring.Change;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class BaseQuickFixTest {
	String testProjectNameString;
	String testPackageNameString;
	String testClassSimpleNameString;
	JavaFileToString jfs;
	JavaProjectMaker jpm;
	CompilationUnit unit;
	SmellSettings smellSettings;
	
	public BaseQuickFixTest() {
		testProjectNameString = "DummyHandlerTest";
		testPackageNameString = DummyAndIgnoreExample.class.getPackage().getName();
		testClassSimpleNameString = DummyAndIgnoreExample.class.getSimpleName();
	}
	
	@Before
	public void setUp() throws Exception {
		// 讀取測試檔案樣本內容
		jfs = new JavaFileToString();
		jfs.read(DummyAndIgnoreExample.class, JavaProjectMaker.FOLDERNAME_TEST);
		
		jpm = new JavaProjectMaker(testProjectNameString);
		jpm.setJREDefaultContainer();
		// 新增欲載入的library
		jpm.addJarFromProjectToBuildPath(JavaProjectMaker.FOLDERNAME_LIB_JAR + "/log4j-1.2.15.jar");
		jpm.packAgileExceptionClasses2JarIntoLibFolder(JavaProjectMaker.FOLDERNAME_LIB_JAR, JavaProjectMaker.FOLDERNAME_BIN_CLASS);
		jpm.addJarFromTestProjectToBuildPath("/" + JavaProjectMaker.FOLDERNAME_LIB_JAR + JavaProjectMaker.FOLDERNAME_LIB_JAR);
		// 根據測試檔案樣本內容建立新的檔案
		jpm.createJavaFile(testPackageNameString,
				testClassSimpleNameString + JavaProjectMaker.JAVA_FILE_EXTENSION,
				"package " + testPackageNameString + ";\n" + jfs.getFileContent());
		// 建立XML
		CreateSettings();
		
		Path path = new Path(PathUtils.getPathOfClassUnderSrcFolder(DummyAndIgnoreExample.class, testProjectNameString));
		//Create AST to parse
		ASTParser parser = ASTParser.newParser(AST.JLS3);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		// 設定要被建立AST的檔案
		parser.setSource(JavaCore.createCompilationUnitFrom(ResourcesPlugin.getWorkspace().getRoot().getFile(path)));
		parser.setResolveBindings(true);
		// 取得AST
		unit = (CompilationUnit) parser.createAST(null); 
		unit.recordModifications();
	}

	@After
	public void tearDown() throws Exception {
		File xmlFile = new File(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
		// 如果xml檔案存在，則刪除之
		if(xmlFile.exists())
			assertTrue(xmlFile.delete());
		// 刪除專案
		jpm.deleteProject();
	}
	
	@Test
	public void testFindCurrentMethod() throws Exception {
		BaseQuickFix bqFix = new BaseQuickFix();
		
		Field actOpenable = BaseQuickFix.class.getDeclaredField("actOpenable");
		actOpenable.setAccessible(true);
		
		Field actRoot = BaseQuickFix.class.getDeclaredField("actRoot");
		actRoot.setAccessible(true);
		
		Field currentMethodNode = BaseQuickFix.class.getDeclaredField("currentMethodNode");
		currentMethodNode.setAccessible(true);
		
		// check precondition
		assertNull(actOpenable.get(bqFix));
		assertNull(actRoot.get(bqFix));
		assertNull(currentMethodNode.get(bqFix));
		
		// test
		Method findCurrentMethod = BaseQuickFix.class.getDeclaredMethod("findCurrentMethod", IResource.class, int.class);
		findCurrentMethod.setAccessible(true);
		findCurrentMethod.invoke(bqFix, RuntimeEnvironmentProjectReader.getType(testProjectNameString, testPackageNameString, testClassSimpleNameString).getResource(), 6);
		
		// check postcondition
		assertEquals(testClassSimpleNameString + JavaProjectMaker.JAVA_FILE_EXTENSION
				+ " (not open) [in " + testPackageNameString + " [in "
				+ JavaProjectMaker.FOLDERNAME_SOURCE + " [in " + testProjectNameString + "]]]",
				actOpenable.get(bqFix).toString());
		assertNotNull(actRoot.get(bqFix));
		assertEquals(	"public void true_systemErrPrint(){\n" +
				"  FileInputStream fis=null;\n" +
				"  try {\n" +
				"    fis=new FileInputStream(\"\");\n" +
				"    fis.read();\n" +
				"  }\n" +
				" catch (  IOException e) {\n" +
				"    System.err.println(e);\n" +
				"  }\n" +
				"}\n", currentMethodNode.get(bqFix).toString());
	}
	
	@Test
	public void testGetChange() throws Exception {
		BaseQuickFix bqFix = new BaseQuickFix();
		
		Method findCurrentMethod = BaseQuickFix.class.getDeclaredMethod("findCurrentMethod", IResource.class, int.class);
		findCurrentMethod.setAccessible(true);
		findCurrentMethod.invoke(bqFix, RuntimeEnvironmentProjectReader.getType(testProjectNameString, testPackageNameString, testClassSimpleNameString).getResource(), 6);
		
		Method getChange = BaseQuickFix.class.getDeclaredMethod("getChange", CompilationUnit.class, ASTRewrite.class);
		getChange.setAccessible(true);
		Change change = (Change)getChange.invoke(bqFix, unit, null);
		assertEquals(testClassSimpleNameString + JavaProjectMaker.JAVA_FILE_EXTENSION, change.getName());
		assertEquals("L/"+PathUtils.getPathOfClassUnderSrcFolder(DummyAndIgnoreExample.class, "DummyHandlerTest"), change.getModifiedElement().toString());
	}
	
//	@Test
	public void testPerformChange() throws Exception {
		fail("目前不知道如何在Unit Test中抓到EditorPart，所以未實作");
	}
	
//	@Test
	public void testApplyChange() throws Exception {
		fail("目前不知道如何在Unit Test中抓到EditorPart，所以未實作");
	}
	
	/**
	 * 建立xml檔案
	 */
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
