package ntut.csie.rleht.views;

import static org.junit.Assert.*;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import ntut.csie.csdet.data.SSMessage;
import ntut.csie.csdet.preference.JDomUtil;
import ntut.csie.csdet.preference.SmellSettings;
import ntut.csie.csdet.visitor.UserDefinedMethodAnalyzer;
import ntut.csie.filemaker.JavaFileToString;
import ntut.csie.filemaker.JavaProjectMaker;
import ntut.csie.filemaker.exceptionBadSmells.SuppressWarningExample;
import ntut.csie.filemaker.exceptionBadSmells.UnprotectedMainProgram.UnprotectedMainProgramWithoutTryExample;
import ntut.csie.rleht.builder.ASTMethodCollector;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ExceptionAnalyzerTest {
	JavaFileToString javaFileToString;
	JavaProjectMaker javaProjectMaker;
	CompilationUnit compilationUnit;
	SmellSettings smellSettings;
	ExceptionAnalyzer exceptionAnalyzer;
	
	@Before
	public void setUp() throws Exception {
		// 讀取測試檔案樣本內容
		javaFileToString = new JavaFileToString();
		javaFileToString.read(SuppressWarningExample.class, "test");
		javaProjectMaker = new JavaProjectMaker("ExceptionAnalyerTest");
		javaProjectMaker.setJREDefaultContainer();
		
		// 新增欲載入的 library
		javaProjectMaker.packAgileExceptionClasses2JarIntoLibFolder(JavaProjectMaker.LIB_JAR_FOLDERNAME, JavaProjectMaker.BIN_CLASS_FOLDERNAME);
		javaProjectMaker.addJarFromTestProjectToBuildPath("/lib/RL.jar");
		javaProjectMaker.addJarFromProjectToBuildPath("lib\\log4j-1.2.15.jar");
		
		// 根據測試檔案樣本內容建立新的檔案
		javaProjectMaker.createJavaFile("ntut.csie.exceptionBadSmells", "SuppressWarningExample.java", "package ntut.csie.exceptionBadSmells;\n" + javaFileToString.getFileContent());
		javaFileToString.clear();
		javaFileToString.read(UnprotectedMainProgramWithoutTryExample.class, "test");
		javaProjectMaker.createJavaFile(
				UnprotectedMainProgramWithoutTryExample.class.getPackage().getName()
				, UnprotectedMainProgramWithoutTryExample.class.getSimpleName() + ".java"
				, "package " + UnprotectedMainProgramWithoutTryExample.class.getPackage().getName() + ";\n"
				+ javaFileToString.getFileContent());
		
		// 建立 XML
		CreateSettings();
		Path path = new Path("ExceptionAnalyerTest\\src\\ntut\\csie\\exceptionBadSmells\\SuppressWarningExample.java");
		
		// Create AST to parse
		ASTParser parser = ASTParser.newParser(AST.JLS3);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		
		// 設定要被建立 AST 的檔案
		parser.setSource(JavaCore.createCompilationUnitFrom(ResourcesPlugin.getWorkspace().getRoot().getFile(path)));
		parser.setResolveBindings(true);
		
		// 取得 AST
		compilationUnit = (CompilationUnit) parser.createAST(null); 
		compilationUnit.recordModifications();
	}
	
	@After
	public void tearDown() throws Exception {
		File xmlFile = new File(JDomUtil.getWorkspace() + File.separator + "CSPreference.xml");
		// 如果 XML 檔案存在，則刪除之
		if(xmlFile.exists())
			assertTrue(xmlFile.delete());
		// 刪除專案
		javaProjectMaker.deleteProject();
	}
	
	@Test
	public void testVisitNode() throws Exception {
		fail("Not yet implemented");
	}
	
	@Test
	public void testFindExceptionTypes() throws Exception {
		fail("Not yet implemented");
	}
	
	@Test
	public void testAddRL() throws Exception {
		ASTMethodCollector astMethodCollector = new ASTMethodCollector();
		compilationUnit.accept(astMethodCollector);
		
		List<ASTNode> methodlist = astMethodCollector.getMethodList();
		List<RLMessage> totalRLList = new ArrayList<RLMessage>();
		
		Method methodAddRL = ExceptionAnalyzer.class.getDeclaredMethod("addRL", RLMessage.class, int.class);
		methodAddRL.setAccessible(true);
		
		Method methodGetMethodAnnotation = ExceptionAnalyzer.class.getDeclaredMethod("getMethodAnnotation", ASTNode.class);
		methodGetMethodAnnotation.setAccessible(true);
		
		exceptionAnalyzer = new ExceptionAnalyzer(compilationUnit, methodlist.get(7).getStartPosition(), 0);
		methodGetMethodAnnotation.invoke(exceptionAnalyzer, methodlist.get(7));
		totalRLList.addAll(exceptionAnalyzer.getMethodRLAnnotationList());
		assertEquals(0, exceptionAnalyzer.getExceptionList().size());
		methodAddRL.invoke(exceptionAnalyzer, totalRLList.get(0), 0);
		assertEquals(1, exceptionAnalyzer.getExceptionList().size());
	}

	@Test
	public void testExceptionAnalyzerCompilationUnitIntInt() {
		fail("Not yet implemented");
	}

	@Test
	public void testExceptionAnalyzerCompilationUnitBooleanString() {
		fail("Not yet implemented");
	}

	@Test
	public void testClear() {
		fail("Not yet implemented");
	}

	@Test
	public void testGetNestedTryList() {
		fail("Not yet implemented");
	}

	@Test
	public void testGetCurrentMethodNode() {
		fail("Not yet implemented");
	}

	@Test
	public void testGetCurrentRLAnnotationNode() {
		fail("Not yet implemented");
	}

	@Test
	public void testGetExceptionList() throws Exception {
		ASTMethodCollector astMethodCollector = new ASTMethodCollector();
		compilationUnit.accept(astMethodCollector);
		List<ASTNode> methodlist = astMethodCollector.getMethodList();
		List<RLMessage> totalList = new ArrayList<RLMessage>();
		Method methodGetMethodThrowsList = ExceptionAnalyzer.class.getDeclaredMethod("getMethodThrowsList", ASTNode.class);
		methodGetMethodThrowsList.setAccessible(true);
		
		for (int i = 0; i < methodlist.size(); i++) {
			exceptionAnalyzer = new ExceptionAnalyzer(compilationUnit, methodlist.get(i).getStartPosition(), 0);
			methodGetMethodThrowsList.invoke(exceptionAnalyzer, methodlist.get(i));
			totalList.addAll(exceptionAnalyzer.getExceptionList());
		}
		
		assertEquals("java.net.SocketTimeoutException",totalList.get(0).getRLData().getExceptionType().toString());
		assertEquals("java.io.InterruptedIOException",totalList.get(1).getRLData().getExceptionType().toString());
		assertEquals("java.net.SocketTimeoutException",totalList.get(2).getRLData().getExceptionType().toString());
		assertEquals("java.io.InterruptedIOException",totalList.get(3).getRLData().getExceptionType().toString());
		assertEquals("java.io.InterruptedIOException",totalList.get(4).getRLData().getExceptionType().toString());
		assertEquals("java.lang.ArithmeticException",totalList.get(5).getRLData().getExceptionType().toString());
		assertEquals("java.lang.Exception",totalList.get(6).getRLData().getExceptionType().toString());
		
		assertEquals(7, totalList.size());
	}

	@Test
	public void testGetMethodAnnotationForRLAnnotation() throws Exception {
		ASTMethodCollector astMethodCollector = new ASTMethodCollector();
		compilationUnit.accept(astMethodCollector);
		List<ASTNode> methodlist = astMethodCollector.getMethodList();
		List<RLMessage> totalList = new ArrayList<RLMessage>();
		Method methodGetMethodAnnotation = ExceptionAnalyzer.class.getDeclaredMethod("getMethodAnnotation", ASTNode.class);
		methodGetMethodAnnotation.setAccessible(true);
		
		for (int i = 0; i < methodlist.size(); i++) {
			exceptionAnalyzer = new ExceptionAnalyzer(compilationUnit, methodlist.get(i).getStartPosition(), 0);
			methodGetMethodAnnotation.invoke(exceptionAnalyzer, methodlist.get(i));
			totalList.addAll(exceptionAnalyzer.getMethodRLAnnotationList());
		}
		
		assertTrue(methodlist.get(7).toString().equals(totalList.get(0).getStatement().toString()));
		assertTrue(methodlist.get(8).toString().equals(totalList.get(1).getStatement().toString()));
		assertTrue(methodlist.get(9).toString().equals(totalList.get(2).getStatement().toString()));
		
		assertEquals(127, totalList.get(0).getLineNumber());
		assertEquals(144, totalList.get(1).getLineNumber());
		assertEquals(164, totalList.get(2).getLineNumber());
		
		assertEquals(3, totalList.size());
	}

	@Test
	public void testGetMethodAnnotationForSuppressSemllAnnotation() throws Exception {
		ASTMethodCollector astMethodCollector = new ASTMethodCollector();
		compilationUnit.accept(astMethodCollector);
		List<ASTNode> methodlist = astMethodCollector.getMethodList();
		List<SSMessage> totalList = new ArrayList<SSMessage>();
		Method methodGetMethodAnnotation = ExceptionAnalyzer.class.getDeclaredMethod("getMethodAnnotation", ASTNode.class);
		methodGetMethodAnnotation.setAccessible(true);
		
		for (int i = 0; i < methodlist.size(); i++) {
			exceptionAnalyzer = new ExceptionAnalyzer(compilationUnit, methodlist.get(i).getStartPosition(), 0);
			methodGetMethodAnnotation.invoke(exceptionAnalyzer, methodlist.get(i));
			totalList.addAll(exceptionAnalyzer.getSuppressSemllAnnotationList());
		}
		
		assertEquals("[Unprotected_Main_Program]", totalList.get(0).getSmellList().toString());
		assertEquals("[Dummy_Handler]", totalList.get(1).getSmellList().toString());
		assertEquals("[Nested_Try_Block, Dummy_Handler]", totalList.get(2).getSmellList().toString());
		assertEquals("[Nested_Try_Block, Dummy_Handler]", totalList.get(3).getSmellList().toString());
		assertEquals("[Ignore_Checked_Exception]",totalList.get(4).getSmellList().toString());
		assertEquals("[Careless_CleanUp]", totalList.get(5).getSmellList().toString());
		assertEquals("[Careless_CleanUp]", totalList.get(6).getSmellList().toString());
		assertEquals("[Careless_CleanUp]", totalList.get(7).getSmellList().toString());
		
		assertEquals(28, totalList.get(0).getLineNumber());
		assertEquals(37, totalList.get(1).getLineNumber());
		assertEquals(64, totalList.get(2).getLineNumber());
		assertEquals(81, totalList.get(3).getLineNumber());
		assertEquals(100, totalList.get(4).getLineNumber());
		assertEquals(127, totalList.get(5).getLineNumber());
		assertEquals(144, totalList.get(6).getLineNumber());
		assertEquals(164, totalList.get(7).getLineNumber());
		
		assertEquals(8, totalList.size());
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
