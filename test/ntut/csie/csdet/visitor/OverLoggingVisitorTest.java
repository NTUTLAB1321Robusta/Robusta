package ntut.csie.csdet.visitor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import ntut.csie.csdet.data.MarkerInfo;
import ntut.csie.csdet.preference.SmellSettings;
import ntut.csie.filemaker.ASTNodeFinder;
import ntut.csie.filemaker.JavaFileToString;
import ntut.csie.filemaker.JavaProjectMaker;
import ntut.csie.filemaker.exceptionBadSmells.OverLogging.OverLoggingIntegrationExample;
import ntut.csie.filemaker.exceptionBadSmells.OverLogging.OverLoggingJavaLogExample;
import ntut.csie.filemaker.exceptionBadSmells.OverLogging.OverLoggingLog4JExample;
import ntut.csie.filemaker.exceptionBadSmells.OverLogging.OverLoggingSelf4JExample;
import ntut.csie.filemaker.exceptionBadSmells.OverLogging.OverLoggingTheFirstOrderClass;
import ntut.csie.filemaker.exceptionBadSmells.OverLogging.OverLoggingTheSecondOrderClass;
import ntut.csie.filemaker.exceptionBadSmells.OverLogging.OverLoggingTheThirdOrderClass;
import ntut.csie.rleht.builder.ASTMethodCollector;
import ntut.csie.robusta.util.PathUtils;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class OverLoggingVisitorTest {
	JavaFileToString javaFile2String;
	JavaProjectMaker javaProjectMaker;
	CompilationUnit overLoggingJavaLogExampleUnit;
	CompilationUnit overLoggingLog4JExampleUnit;
	CompilationUnit overLoggingSelf4JExampleUnit;
	SmellSettings smellSettings;
	OverLoggingVisitor overLoggingVisitor;
	String projectName;

	@Before
	public void setUp() throws Exception {
		projectName = "OverLoggingExampleProject";
		// 讀取測試檔案樣本內容
		javaFile2String = new JavaFileToString();
		javaProjectMaker = new JavaProjectMaker(projectName);
		// 新增欲載入的library
		javaProjectMaker.packAgileExceptionClasses2JarIntoLibFolder(JavaProjectMaker.FOLDERNAME_LIB_JAR, JavaProjectMaker.FOLDERNAME_BIN_CLASS);
		javaProjectMaker.addJarFromTestProjectToBuildPath("/" + JavaProjectMaker.RL_LIBRARY_PATH);
		javaProjectMaker.addJarFromProjectToBuildPath(JavaProjectMaker.FOLDERNAME_LIB_JAR + "/log4j-1.2.15.jar");
		javaProjectMaker.addJarFromProjectToBuildPath(JavaProjectMaker.FOLDERNAME_LIB_JAR + "/slf4j-api-1.5.0.jar");
		javaProjectMaker.setJREDefaultContainer();
		
		// 根據測試檔案樣本內容建立新的檔案
		javaFile2String.read(OverLoggingIntegrationExample.class, JavaProjectMaker.FOLDERNAME_TEST);
		javaProjectMaker.createJavaFile(OverLoggingIntegrationExample.class.getPackage().getName(),
										OverLoggingIntegrationExample.class.getSimpleName() + JavaProjectMaker.JAVA_FILE_EXTENSION,
										"package " + OverLoggingIntegrationExample.class.getPackage().getName()
										+ ";\n" + javaFile2String.getFileContent());
		javaFile2String.clear();
		
		javaFile2String.read(OverLoggingJavaLogExample.class, JavaProjectMaker.FOLDERNAME_TEST);
		javaProjectMaker.createJavaFile(OverLoggingJavaLogExample.class.getPackage().getName(),
										OverLoggingJavaLogExample.class.getSimpleName() + JavaProjectMaker.JAVA_FILE_EXTENSION,
										"package " + OverLoggingJavaLogExample.class.getPackage().getName()
										+ ";\n" + javaFile2String.getFileContent());
		javaFile2String.clear();
		
		javaFile2String.read(OverLoggingLog4JExample.class, JavaProjectMaker.FOLDERNAME_TEST);
		javaProjectMaker.createJavaFile(OverLoggingLog4JExample.class.getPackage().getName(),
										OverLoggingLog4JExample.class.getSimpleName() + JavaProjectMaker.JAVA_FILE_EXTENSION,
										"package " + OverLoggingLog4JExample.class.getPackage().getName()
										+ ";\n" + javaFile2String.getFileContent());
		javaFile2String.clear();
		
		javaFile2String.read(OverLoggingSelf4JExample.class, JavaProjectMaker.FOLDERNAME_TEST);
		javaProjectMaker.createJavaFile(OverLoggingSelf4JExample.class.getPackage().getName(),
										OverLoggingSelf4JExample.class.getSimpleName() + JavaProjectMaker.JAVA_FILE_EXTENSION,
										"package " + OverLoggingSelf4JExample.class.getPackage().getName()
										+ ";\n" + javaFile2String.getFileContent());
		javaFile2String.clear();
		
		javaFile2String.read(OverLoggingTheFirstOrderClass.class, JavaProjectMaker.FOLDERNAME_TEST);
		javaProjectMaker.createJavaFile(OverLoggingTheFirstOrderClass.class.getPackage().getName(),
										OverLoggingTheFirstOrderClass.class.getSimpleName() + JavaProjectMaker.JAVA_FILE_EXTENSION,
										"package " + OverLoggingTheFirstOrderClass.class.getPackage().getName()
										+ ";\n" + javaFile2String.getFileContent());
		javaFile2String.clear();
		
		javaFile2String.read(OverLoggingTheSecondOrderClass.class, JavaProjectMaker.FOLDERNAME_TEST);
		javaProjectMaker.createJavaFile(OverLoggingTheSecondOrderClass.class.getPackage().getName(),
										OverLoggingTheSecondOrderClass.class.getSimpleName() + JavaProjectMaker.JAVA_FILE_EXTENSION,
										"package " + OverLoggingTheSecondOrderClass.class.getPackage().getName()
										+ ";\n" + javaFile2String.getFileContent());
		javaFile2String.clear();
		
		javaFile2String.read(OverLoggingTheThirdOrderClass.class, JavaProjectMaker.FOLDERNAME_TEST);
		javaProjectMaker.createJavaFile(OverLoggingTheThirdOrderClass.class.getPackage().getName(),
										OverLoggingTheThirdOrderClass.class.getSimpleName() + JavaProjectMaker.JAVA_FILE_EXTENSION,
										"package " + OverLoggingTheThirdOrderClass.class.getPackage().getName()
										+ ";\n" + javaFile2String.getFileContent());
		javaFile2String.clear();
		
		// 建立XML
		CreateSettings();
		
		Path path = new Path(PathUtils.getPathOfClassUnderSrcFolder(OverLoggingJavaLogExample.class, projectName));
		
		// Create AST to parse
		ASTParser parser = ASTParser.newParser(AST.JLS3);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		// 設定要被建立AST的檔案
		parser.setSource(JavaCore.createCompilationUnitFrom(ResourcesPlugin.getWorkspace().getRoot().getFile(path)));
		parser.setResolveBindings(true);
		// 取得AST
		overLoggingJavaLogExampleUnit = (CompilationUnit) parser.createAST(null); 
		overLoggingJavaLogExampleUnit.recordModifications();
		
		path = new Path(PathUtils.getPathOfClassUnderSrcFolder(OverLoggingLog4JExample.class, projectName));
		
		// Create AST to parse
		parser = ASTParser.newParser(AST.JLS3);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		// 設定要被建立AST的檔案
		parser.setSource(JavaCore.createCompilationUnitFrom(ResourcesPlugin.getWorkspace().getRoot().getFile(path)));
		parser.setResolveBindings(true);
		// 取得AST
		overLoggingLog4JExampleUnit = (CompilationUnit) parser.createAST(null); 
		overLoggingLog4JExampleUnit.recordModifications();
		
		path = new Path(PathUtils.getPathOfClassUnderSrcFolder(OverLoggingSelf4JExample.class, projectName));

		// Create AST to parse
		parser = ASTParser.newParser(AST.JLS3);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		// 設定要被建立AST的檔案
		parser.setSource(JavaCore.createCompilationUnitFrom(ResourcesPlugin.getWorkspace().getRoot().getFile(path)));
		parser.setResolveBindings(true);
		// 取得AST
		overLoggingSelf4JExampleUnit = (CompilationUnit) parser.createAST(null); 
		overLoggingSelf4JExampleUnit.recordModifications();
	}

	@After
	public void tearDown() throws Exception {
		File xmlFile = new File(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
		// 如果xml檔案存在，則刪除之
		if(xmlFile.exists())
			assertTrue(xmlFile.delete());
		// 刪除專案
		javaProjectMaker.deleteProject();
	}
	
	@Test
	public void testAddOverLoggingMarkerInfo() throws Exception {
		overLoggingVisitor = new OverLoggingVisitor(overLoggingJavaLogExampleUnit, "");
		/* MarkerInfo in the same compilation unit */
		// check precondition
		Field suspectNode = OverLoggingVisitor.class.getDeclaredField("suspectNode");
		suspectNode.setAccessible(true);
		suspectNode.set(overLoggingVisitor, ASTNodeFinder.getNodeFromSpecifiedClass(OverLoggingJavaLogExample.class, projectName, 32));
		assertNotNull(suspectNode.get(overLoggingVisitor));
		assertEquals(0, overLoggingVisitor.getOverLoggingList().size());
		// test target
		Method addOverLoggingMarkerInfo = OverLoggingVisitor.class.getDeclaredMethod("addOverLoggingMarkerInfo", ASTNode.class);
		addOverLoggingMarkerInfo.setAccessible(true);
		addOverLoggingMarkerInfo.invoke(overLoggingVisitor, suspectNode.get(overLoggingVisitor));
		// check postcondition
		assertEquals(1, overLoggingVisitor.getOverLoggingList().size());
		assertNull(suspectNode.get(overLoggingVisitor));
		
		/* MarkerInfo in the other compilation unit */
		// check precondition
		overLoggingVisitor = new OverLoggingVisitor(overLoggingJavaLogExampleUnit, "");
		suspectNode.set(overLoggingVisitor, ASTNodeFinder.getNodeFromSpecifiedClass(OverLoggingLog4JExample.class, projectName, 32));
		assertNotNull(suspectNode.get(overLoggingVisitor));
		assertEquals(0, overLoggingVisitor.getOverLoggingList().size());
		// test target
		addOverLoggingMarkerInfo.invoke(overLoggingVisitor, suspectNode.get(overLoggingVisitor));
		// check postcondition
		assertEquals(0, overLoggingVisitor.getOverLoggingList().size());
		assertNotNull(suspectNode.get(overLoggingVisitor));
	}
	
	@Test
	public void testSatisfyLoggingStatement() throws Exception {
		/* libMap is empty */
		smellSettings.removeExtraRule(SmellSettings.SMELL_OVERLOGGING, SmellSettings.EXTRARULE_OVERLOGGING_DETECTWRAPPINGEXCEPTION);
		smellSettings.removeExtraRule(SmellSettings.SMELL_OVERLOGGING, SmellSettings.EXTRARULE_JavaUtilLoggingLogger);
		smellSettings.removeExtraRule(SmellSettings.SMELL_OVERLOGGING, SmellSettings.EXTRARULE_OrgApacheLog4j);
		smellSettings.writeXMLFile(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
		overLoggingVisitor = new OverLoggingVisitor(overLoggingJavaLogExampleUnit, "");
		List<MethodInvocation> methodInvocationJavaLogList = ASTNodeFinder.getMethodInvocationByMethodNameAndCode(overLoggingJavaLogExampleUnit, "theSecondOrderInTheSameClassWithJavaLog", "javaLogger.log(Level.WARNING,e.getMessage() + \"theSecondOrderInTheSameClassWithJavaLog\")");
		List<MethodInvocation> methodInvocationLog4JList = ASTNodeFinder.getMethodInvocationByMethodNameAndCode(overLoggingLog4JExampleUnit, "theSecondOrderInTheSameClassWithLog4J", "log4jLogger.error(e.getMessage() + \"theSecondOrderInTheSameClassWithLog4J\")");
		List<MethodInvocation> methodInvocationSelf4JList = ASTNodeFinder.getMethodInvocationByMethodNameAndCode(overLoggingSelf4JExampleUnit, "theSecondOrderInTheSameClassWithSelf4J", "self4jLogger.error(e.getMessage() + \"theSecondOrderInTheSameClassWithSelf4J\")");
		Method satisfyLoggingStatement = OverLoggingVisitor.class.getDeclaredMethod("satisfyLoggingStatement", MethodInvocation.class);
		satisfyLoggingStatement.setAccessible(true);
		assertFalse((Boolean)satisfyLoggingStatement.invoke(overLoggingVisitor, methodInvocationJavaLogList.get(0)));
		
		/* Only java.util.logging.Logger is checked */
		// pass java.util.logging.Logger
		smellSettings.addExtraRule(SmellSettings.SMELL_OVERLOGGING, SmellSettings.EXTRARULE_JavaUtilLoggingLogger);
		smellSettings.writeXMLFile(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
		overLoggingVisitor = new OverLoggingVisitor(overLoggingJavaLogExampleUnit, "");
		assertTrue((Boolean)satisfyLoggingStatement.invoke(overLoggingVisitor, methodInvocationJavaLogList.get(0)));
		// pass org.apache.log4j.Logger
		assertFalse((Boolean)satisfyLoggingStatement.invoke(overLoggingVisitor, methodInvocationLog4JList.get(0)));
		// pass org.slf4j.Logger
		assertFalse((Boolean)satisfyLoggingStatement.invoke(overLoggingVisitor, methodInvocationSelf4JList.get(0)));
		
		/* Only org.apache.log4j is checked */
		// pass java.util.logging.Logger
		smellSettings.removeExtraRule(SmellSettings.SMELL_OVERLOGGING, SmellSettings.EXTRARULE_JavaUtilLoggingLogger);
		smellSettings.addExtraRule(SmellSettings.SMELL_OVERLOGGING, SmellSettings.EXTRARULE_OrgApacheLog4j);
		smellSettings.writeXMLFile(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
		overLoggingVisitor = new OverLoggingVisitor(overLoggingJavaLogExampleUnit, "");
		assertFalse((Boolean)satisfyLoggingStatement.invoke(overLoggingVisitor, methodInvocationJavaLogList.get(0)));
		// pass org.apache.log4j.Logger
		assertTrue((Boolean)satisfyLoggingStatement.invoke(overLoggingVisitor, methodInvocationLog4JList.get(0)));
		// pass org.slf4j.Logger
		assertFalse((Boolean)satisfyLoggingStatement.invoke(overLoggingVisitor, methodInvocationSelf4JList.get(0)));
		
		/* Only org.slf4j.Logger is checked */
		smellSettings.removeExtraRule(SmellSettings.SMELL_OVERLOGGING, SmellSettings.EXTRARULE_OrgApacheLog4j);
		smellSettings.addOverLoggingPattern("org.slf4j.Logger", true);
		smellSettings.writeXMLFile(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
		overLoggingVisitor = new OverLoggingVisitor(overLoggingJavaLogExampleUnit, "");
		assertFalse((Boolean)satisfyLoggingStatement.invoke(overLoggingVisitor, methodInvocationJavaLogList.get(0)));
		// pass org.apache.log4j.Logger
		assertFalse((Boolean)satisfyLoggingStatement.invoke(overLoggingVisitor, methodInvocationLog4JList.get(0)));
		// pass org.slf4j.Logger
		assertTrue((Boolean)satisfyLoggingStatement.invoke(overLoggingVisitor, methodInvocationSelf4JList.get(0)));
		
		/* check all rules */
		CreateSettings();
		overLoggingVisitor = new OverLoggingVisitor(overLoggingJavaLogExampleUnit, "");
		// pass org.slf4j.Logger
		assertTrue((Boolean)satisfyLoggingStatement.invoke(overLoggingVisitor, methodInvocationJavaLogList.get(0)));
		// pass org.apache.log4j.Logger
		assertTrue((Boolean)satisfyLoggingStatement.invoke(overLoggingVisitor, methodInvocationLog4JList.get(0)));
		// pass org.slf4j.Logger
		assertTrue((Boolean)satisfyLoggingStatement.invoke(overLoggingVisitor, methodInvocationSelf4JList.get(0)));
	}

	@Test
	public void testVisit() {
		/* OverLoggingJavaLogExample */
		ASTMethodCollector methodCollector = new ASTMethodCollector();
		overLoggingJavaLogExampleUnit.accept(methodCollector);
		List<MethodDeclaration> methodList = methodCollector.getMethodList();
		List<MarkerInfo> markerInfoList = new ArrayList<MarkerInfo>();
		for(MethodDeclaration node : methodList) {
			// 在這裡用到別的class，其實是不好的，但是OverLoggingVisitor不像其他visitor只要accept一次就能全部抓到
			OverLoggingDetector detector = new OverLoggingDetector(overLoggingJavaLogExampleUnit, node);
			detector.detect();
			markerInfoList.addAll(detector.getOverLoggingList());
		}
		assertEquals(7, markerInfoList.size());
		
		/* OverLoggingLog4JExample */
		methodCollector = new ASTMethodCollector();
		overLoggingLog4JExampleUnit.accept(methodCollector);
		methodList = methodCollector.getMethodList();
		markerInfoList = new ArrayList<MarkerInfo>();
		for(MethodDeclaration node : methodList) {
			// 在這裡用到別的class，其實是不好的，但是OverLoggingVisitor不像其他visitor只要accept一次就能全部抓到
			OverLoggingDetector detector = new OverLoggingDetector(overLoggingLog4JExampleUnit, node);
			detector.detect();
			markerInfoList.addAll(detector.getOverLoggingList());
		}
		assertEquals(5, markerInfoList.size());
		
		/* OverLoggingSelf4JExample */
		smellSettings.addOverLoggingPattern(org.slf4j.Logger.class.getPackage().getName() + "." + org.slf4j.Logger.class.getSimpleName(), true);
		smellSettings.writeXMLFile(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
		methodCollector = new ASTMethodCollector();
		overLoggingSelf4JExampleUnit.accept(methodCollector);
		methodList = methodCollector.getMethodList();
		markerInfoList = new ArrayList<MarkerInfo>();
		for(MethodDeclaration node : methodList) {
			// 在這裡用到別的class，其實是不好的，但是OverLoggingVisitor不像其他visitor只要accept一次就能全部抓到
			OverLoggingDetector detector = new OverLoggingDetector(overLoggingSelf4JExampleUnit, node);
			detector.detect();
			markerInfoList.addAll(detector.getOverLoggingList());
		}
		assertEquals(5, markerInfoList.size());
	}
	
	private void CreateSettings() {
		smellSettings = new SmellSettings(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
		smellSettings.addExtraRule(SmellSettings.SMELL_OVERLOGGING, SmellSettings.EXTRARULE_OVERLOGGING_DETECTWRAPPINGEXCEPTION);
		smellSettings.addExtraRule(SmellSettings.SMELL_OVERLOGGING, SmellSettings.EXTRARULE_JavaUtilLoggingLogger);
		smellSettings.addExtraRule(SmellSettings.SMELL_OVERLOGGING, SmellSettings.EXTRARULE_OrgApacheLog4j);
		smellSettings.writeXMLFile(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
	}
}