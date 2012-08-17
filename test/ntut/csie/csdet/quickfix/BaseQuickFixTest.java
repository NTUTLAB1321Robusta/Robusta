package ntut.csie.csdet.quickfix;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import ntut.csie.csdet.preference.JDomUtil;
import ntut.csie.csdet.preference.SmellSettings;
import ntut.csie.csdet.visitor.UserDefinedMethodAnalyzer;
import ntut.csie.filemaker.JavaFileToString;
import ntut.csie.filemaker.JavaProjectMaker;
import ntut.csie.filemaker.RuntimeEnvironmentProjectReader;
import ntut.csie.filemaker.exceptionBadSmells.DummyAndIgnoreExample;

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
	JavaFileToString jfs;
	JavaProjectMaker jpm;
	CompilationUnit unit;
	SmellSettings smellSettings;
	
	@Before
	public void setUp() throws Exception {
		// Ū�������ɮ׼˥����e
		jfs = new JavaFileToString();
		jfs.read(DummyAndIgnoreExample.class, "test");
		
		jpm = new JavaProjectMaker("DummyHandlerTest");
		jpm.setJREDefaultContainer();
		// �s�W�����J��library
		jpm.addJarFromProjectToBuildPath("lib\\log4j-1.2.15.jar");
		jpm.addJarFromProjectToBuildPath("..\\SingleSharedLibrary\\common\\agile.rl.jar");
		// �ھڴ����ɮ׼˥����e�إ߷s���ɮ�
		jpm.createJavaFile("ntut.csie.exceptionBadSmells", "DummyHandlerExample.java", "package ntut.csie.exceptionBadSmells;\n" + jfs.getFileContent());
		// �إ�XML
		CreateSettings();
		
		Path path = new Path("DummyHandlerTest\\src\\ntut\\csie\\exceptionBadSmells\\DummyHandlerExample.java");
		//Create AST to parse
		ASTParser parser = ASTParser.newParser(AST.JLS3);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		// �]�w�n�Q�إ�AST���ɮ�
		parser.setSource(JavaCore.createCompilationUnitFrom(ResourcesPlugin.getWorkspace().getRoot().getFile(path)));
		parser.setResolveBindings(true);
		// ���oAST
		unit = (CompilationUnit) parser.createAST(null); 
		unit.recordModifications();
	}

	@After
	public void tearDown() throws Exception {
		File xmlFile = new File(JDomUtil.getWorkspace() + File.separator + "CSPreference.xml");
		// �p�Gxml�ɮצs�b�A�h�R����
		if(xmlFile.exists())
			assertTrue(xmlFile.delete());
		// �R���M��
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
		findCurrentMethod.invoke(bqFix, RuntimeEnvironmentProjectReader.getType(jpm.getProjectName(), "ntut.csie.exceptionBadSmells", "DummyHandlerExample").getResource(), 6);
		
		// check postcondition
		assertEquals("DummyHandlerExample.java (not open) [in ntut.csie.exceptionBadSmells [in src [in DummyHandlerTest]]]", actOpenable.get(bqFix).toString());
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
		findCurrentMethod.invoke(bqFix, RuntimeEnvironmentProjectReader.getType(jpm.getProjectName(), "ntut.csie.exceptionBadSmells", "DummyHandlerExample").getResource(), 6);
		
		Method getChange = BaseQuickFix.class.getDeclaredMethod("getChange", CompilationUnit.class, ASTRewrite.class);
		getChange.setAccessible(true);
		Change change = (Change)getChange.invoke(bqFix, unit, null);
		assertEquals("DummyHandlerExample.java", change.getName());
		assertEquals("L/DummyHandlerTest/src/ntut/csie/exceptionBadSmells/DummyHandlerExample.java", change.getModifiedElement().toString());
	}
	
	@Test
	public void testPerformChange() throws Exception {
		fail("�ثe�����D�p��bUnit Test�����EditorPart�A�ҥH����@");
	}
	
	@Test
	public void testApplyChange() throws Exception {
		fail("�ثe�����D�p��bUnit Test�����EditorPart�A�ҥH����@");
	}
	
	/**
	 * �إ�xml�ɮ�
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
