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
import ntut.csie.analyzer.CommonExample;
import ntut.csie.analyzer.UserDefinedMethodAnalyzer;
import ntut.csie.filemaker.JavaFileToString;
import ntut.csie.filemaker.JavaProjectMaker;
import ntut.csie.filemaker.RuntimeEnvironmentProjectReader;
import ntut.csie.util.PathUtils;

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
		testPackageNameString = CommonExample.class.getPackage().getName();
		testClassSimpleNameString = CommonExample.class.getSimpleName();
	}
	
	@Before
	public void setUp() throws Exception {
		jfs = new JavaFileToString();
		jfs.read(CommonExample.class, JavaProjectMaker.FOLDERNAME_TEST);
		
		jpm = new JavaProjectMaker(testProjectNameString);
		jpm.setJREDefaultContainer();

		jpm.addJarFromProjectToBuildPath(JavaProjectMaker.FOLDERNAME_LIB_JAR + "/log4j-1.2.15.jar");
		jpm.packageAgileExceptionClassesToJarIntoLibFolder(JavaProjectMaker.FOLDERNAME_LIB_JAR, JavaProjectMaker.FOLDERNAME_BIN_CLASS);
		jpm.addJarFromTestProjectToBuildPath("/" + JavaProjectMaker.FOLDERNAME_LIB_JAR + JavaProjectMaker.FOLDERNAME_LIB_JAR);

		jpm.createJavaFile(testPackageNameString,
				testClassSimpleNameString + JavaProjectMaker.JAVA_FILE_EXTENSION,
				"package " + testPackageNameString + ";\n" + jfs.getFileContent());

		CreateSettings();
		
		Path path = new Path(PathUtils.getPathOfClassUnderSrcFolder(CommonExample.class, testProjectNameString));

		ASTParser parser = ASTParser.newParser(AST.JLS3);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);

		parser.setSource(JavaCore.createCompilationUnitFrom(ResourcesPlugin.getWorkspace().getRoot().getFile(path)));
		parser.setResolveBindings(true);

		unit = (CompilationUnit) parser.createAST(null); 
		unit.recordModifications();
	}

	@After
	public void tearDown() throws Exception {
		File xmlFile = new File(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
		if(xmlFile.exists())
			assertTrue(xmlFile.delete());
		jpm.deleteProject();
	}
	
	@Test
	public void testFindCurrentMethod() throws Exception {
		BaseQuickFix bqFix = new BaseQuickFix();
		
		Field actOpenable = BaseQuickFix.class.getDeclaredField("actOpenable");
		actOpenable.setAccessible(true);
		
		Field actRoot = BaseQuickFix.class.getDeclaredField("methodNodeWillBeQuickFixed");
		actRoot.setAccessible(true);
		
		Field currentMethodNode = BaseQuickFix.class.getDeclaredField("methodNodeWillBeQuickFixed");
		currentMethodNode.setAccessible(true);
		
		// check precondition
		assertNull(actOpenable.get(bqFix));
		assertNull(actRoot.get(bqFix));
		assertNull(currentMethodNode.get(bqFix));
		
		// test
		Method findCurrentMethod = BaseQuickFix.class.getDeclaredMethod("findMethodNodeWillBeQuickFixed", IResource.class, int.class);
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
		
		Method findCurrentMethod = BaseQuickFix.class.getDeclaredMethod("findMethodNodeWillBeQuickFixed", IResource.class, int.class);
		findCurrentMethod.setAccessible(true);
		findCurrentMethod.invoke(bqFix, RuntimeEnvironmentProjectReader.getType(testProjectNameString, testPackageNameString, testClassSimpleNameString).getResource(), 6);
		
		Method getChange = BaseQuickFix.class.getDeclaredMethod("getChange", CompilationUnit.class, ASTRewrite.class);
		getChange.setAccessible(true);
		Change change = (Change)getChange.invoke(bqFix, unit, null);
		assertEquals(testClassSimpleNameString + JavaProjectMaker.JAVA_FILE_EXTENSION, change.getName());
		assertEquals("L/"+PathUtils.getPathOfClassUnderSrcFolder(CommonExample.class, "DummyHandlerTest"), change.getModifiedElement().toString());
	}
	
//	@Test
	public void testPerformChange() throws Exception {
		fail("we don't know how to get editor part during unit test, so performChange has not been implemented");
	}
	
//	@Test
	public void testApplyChange() throws Exception {
		fail("we don't know how to get editor part during unit test, so performChange has not been implemented");
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
